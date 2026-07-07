begin;

with ranked_memberships as (
  select
    id,
    row_number() over (
      partition by tenant_id, user_id
      order by
        case status when 'approved' then 0 when 'pending' then 1 else 2 end,
        case role when 'master' then 0 when 'master_tenant' then 1 when 'admin_tenant' then 2 else 3 end,
        updated_at desc nulls last,
        created_at desc nulls last,
        id desc
    ) as rn
  from public.tenant_memberships
)
delete from public.tenant_memberships tm
using ranked_memberships ranked
where tm.id = ranked.id
  and ranked.rn > 1;

create unique index if not exists tenant_memberships_tenant_user_unique_idx
  on public.tenant_memberships (tenant_id, user_id);

do $$
declare
  v_def text;
begin
  select pg_get_functiondef(
    'public.mt_seed_new_tenant_bootstrap(uuid,text,text,text,text)'::regprocedure
  )
    into v_def;

  if v_def is not null and position('perform v_brand_name;' in v_def) = 0 then
    v_def := replace(
      v_def,
      E'begin\n  if v_tenant_id is null then',
      E'begin\n  perform v_brand_name;\n  if v_tenant_id is null then'
    );
    execute v_def;
  end if;
end;
$$;

do $$
declare
  v_def text;
begin
  select pg_get_functiondef(
    'public.tenant_submit_onboarding_request(text,text,text,text,text,text,text,text,text,text,text,boolean)'::regprocedure
  )
    into v_def;

  if v_def is not null and position('"updatedAt" = now()::text' in v_def) = 0 then
    v_def := replace(v_def, 'updatedAt = now()::text', '"updatedAt" = now()::text');
    execute v_def;
  end if;
end;
$$;

create or replace function public.tenant_create_invite(
  p_tenant_id uuid,
  p_role_to_assign text default 'user',
  p_max_uses integer default 25,
  p_expires_in_hours integer default 72,
  p_requires_approval boolean default true
)
returns table(invite_id uuid, token text, expires_at timestamptz)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid text;
  v_token text;
  v_expires_at timestamptz;
  v_role text;
begin
  v_uid := auth.uid()::text;
  if coalesce(v_uid, '') = '' then raise exception 'Usuario nao autenticado'; end if;
  if not public.mt_can_manage_tenant(p_tenant_id) then raise exception 'Sem permissao neste tenant'; end if;

  v_role := case
    when p_role_to_assign in ('visitante','user','treinador','empresa','admin_treino','admin_geral','admin_gestor','master','vendas','admin_tenant','master_tenant')
      then p_role_to_assign
    else 'user'
  end;
  v_expires_at := now() + (greatest(1, coalesce(p_expires_in_hours, 72)) || ' hours')::interval;

  loop
    v_token := public.mt_build_invite_token();
    exit when not exists (select 1 from public.tenant_invites i where i.token = v_token);
  end loop;

  insert into public.tenant_invites (
    tenant_id, token, role_to_assign, requires_approval, max_uses, uses_count, expires_at, is_active, created_by
  ) values (
    p_tenant_id,
    v_token,
    v_role,
    coalesce(p_requires_approval, true),
    greatest(1, coalesce(p_max_uses, 25)),
    0,
    v_expires_at,
    true,
    v_uid
  ) returning id, tenant_invites.token, tenant_invites.expires_at into invite_id, token, expires_at;

  return next;
end;
$$;

grant execute on function public.tenant_create_invite(uuid, text, integer, integer, boolean) to authenticated;

notify pgrst, 'reload schema';

commit;
