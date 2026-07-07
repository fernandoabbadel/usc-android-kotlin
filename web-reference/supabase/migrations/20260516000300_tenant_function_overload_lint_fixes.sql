begin;

do $$
begin
  if not exists (
    select 1
      from pg_constraint
     where conrelid = 'public.tenant_memberships'::regclass
       and contype = 'u'
       and conkey = array[
         (select attnum from pg_attribute where attrelid = 'public.tenant_memberships'::regclass and attname = 'tenant_id'),
         (select attnum from pg_attribute where attrelid = 'public.tenant_memberships'::regclass and attname = 'user_id')
       ]::smallint[]
  ) then
    alter table public.tenant_memberships
      add constraint tenant_memberships_tenant_user_key unique (tenant_id, user_id);
  end if;
end;
$$;

do $$
declare
  fn record;
  v_def text;
begin
  for fn in
    select p.oid
      from pg_proc p
      join pg_namespace n on n.oid = p.pronamespace
     where n.nspname = 'public'
       and p.proname = 'tenant_submit_onboarding_request'
  loop
    select pg_get_functiondef(fn.oid) into v_def;
    if v_def is not null and position('"updatedAt" = now()::text' in v_def) = 0 then
      v_def := replace(v_def, 'updatedAt = now()::text', '"updatedAt" = now()::text');
      execute v_def;
    end if;
  end loop;
end;
$$;

notify pgrst, 'reload schema';

commit;
