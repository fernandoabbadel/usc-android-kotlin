alter table public.users
  add column if not exists "instagramPublico" boolean not null default false,
  add column if not exists signo text,
  add column if not exists "signoPublico" boolean not null default false,
  add column if not exists ascendente text,
  add column if not exists "ascendentePublico" boolean not null default false,
  add column if not exists "lugarEspecial" text[] not null default '{}'::text[],
  add column if not exists "comidaPreferida" text[] not null default '{}'::text[],
  add column if not exists "musicaPreferida" text[] not null default '{}'::text[],
  add column if not exists "corPreferida" text;

update public.users
set
  "instagramPublico" = false,
  "whatsappPublico" = false,
  "relacionamentoPublico" = false,
  "signoPublico" = false,
  "ascendentePublico" = false;

alter table public.notifications
  add column if not exists "expiresAt" timestamptz;

create table if not exists public.profile_affinities (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references public.tenants(id) on delete cascade,
  from_user_id text not null references public.users(uid) on delete cascade,
  to_user_id text not null references public.users(uid) on delete cascade,
  from_nome text,
  from_foto text,
  from_turma text,
  to_nome text,
  to_foto text,
  to_turma text,
  emoji text not null default '🔥🦈',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint profile_affinities_no_self check (from_user_id <> to_user_id),
  constraint profile_affinities_tenant_pair_unique unique (tenant_id, from_user_id, to_user_id)
);

create index if not exists idx_profile_affinities_tenant_to
  on public.profile_affinities (tenant_id, to_user_id, created_at desc);

create index if not exists idx_profile_affinities_tenant_from
  on public.profile_affinities (tenant_id, from_user_id, created_at desc);

alter table public.profile_affinities enable row level security;

do $$
begin
  if not exists (
    select 1
    from pg_policies
    where schemaname = 'public'
      and tablename = 'profile_affinities'
      and policyname = 'profile_affinities_select_own_or_tenant_admin'
  ) then
    create policy profile_affinities_select_own_or_tenant_admin
      on public.profile_affinities
      for select
      using (
        from_user_id = auth.uid()::text
        or to_user_id = auth.uid()::text
        or public.mt_can_access_tenant_row(tenant_id)
      );
  end if;

  if not exists (
    select 1
    from pg_policies
    where schemaname = 'public'
      and tablename = 'profile_affinities'
      and policyname = 'profile_affinities_insert_own'
  ) then
    create policy profile_affinities_insert_own
      on public.profile_affinities
      for insert
      with check (
        from_user_id = auth.uid()::text
        and public.mt_can_access_tenant_row(tenant_id)
      );
  end if;

  if not exists (
    select 1
    from pg_policies
    where schemaname = 'public'
      and tablename = 'profile_affinities'
      and policyname = 'profile_affinities_update_own'
  ) then
    create policy profile_affinities_update_own
      on public.profile_affinities
      for update
      using (
        from_user_id = auth.uid()::text
        and public.mt_can_access_tenant_row(tenant_id)
      )
      with check (
        from_user_id = auth.uid()::text
        and public.mt_can_access_tenant_row(tenant_id)
      );
  end if;
end $$;

notify pgrst, 'reload schema';
