create extension if not exists pgcrypto;

alter table public.users
  add column if not exists legal_admin_required_at timestamptz,
  add column if not exists legal_admin_required_reason text,
  add column if not exists legal_admin_accepted_at timestamptz;

alter table public.user_legal_acceptances
  add column if not exists context_type text,
  add column if not exists context_id text,
  add column if not exists metadata jsonb not null default '{}'::jsonb;

alter table public.user_legal_acceptances
  drop constraint if exists user_legal_acceptances_source_chk;

alter table public.user_legal_acceptances
  add constraint user_legal_acceptances_source_chk check (
    source in (
      'cadastro',
      'primeiro_acesso',
      'admin',
      'tenant_admin',
      'cookie_banner',
      'app',
      'api',
      'role_upgrade',
      'event_creation',
      'mini_vendor_creation'
  )
);

drop index if exists public.uq_user_legal_acceptances_version;

create unique index if not exists uq_user_legal_acceptances_version
  on public.user_legal_acceptances (
    user_id,
    document_type,
    document_version,
    coalesce(tenant_id, '00000000-0000-0000-0000-000000000000'::uuid),
    source,
    coalesce(context_type, ''),
    coalesce(context_id, '')
  );

create table if not exists public.user_legal_read_receipts (
  id uuid primary key default gen_random_uuid(),
  user_id text not null references public.users(uid) on delete cascade,
  document_type text not null,
  document_version text not null,
  tenant_id uuid references public.tenants(id) on delete set null,
  source text not null default 'app',
  context_type text,
  context_id text,
  metadata jsonb not null default '{}'::jsonb,
  read_completed_at timestamptz not null default now(),
  marked_read_at timestamptz,
  accepted_at timestamptz,
  ip_address text,
  user_agent text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint user_legal_read_receipts_document_type_chk check (
    document_type in (
      'terms_of_service',
      'privacy_policy',
      'cookies_policy',
      'lgpd_rights',
      'admin_confidentiality',
      'tenant_terms'
    )
  ),
  constraint user_legal_read_receipts_source_chk check (
    source in (
      'cadastro',
      'primeiro_acesso',
      'admin',
      'tenant_admin',
      'cookie_banner',
      'app',
      'api',
      'role_upgrade',
      'event_creation',
      'mini_vendor_creation'
    )
  )
);

create unique index if not exists uq_user_legal_read_receipts_version
  on public.user_legal_read_receipts (
    user_id,
    document_type,
    document_version,
    coalesce(tenant_id, '00000000-0000-0000-0000-000000000000'::uuid),
    source,
    coalesce(context_type, ''),
    coalesce(context_id, '')
  );

create index if not exists idx_user_legal_read_receipts_user
  on public.user_legal_read_receipts (user_id, document_type, read_completed_at desc);

create index if not exists idx_user_legal_read_receipts_tenant
  on public.user_legal_read_receipts (tenant_id, document_type, read_completed_at desc);

create index if not exists idx_user_legal_read_receipts_context
  on public.user_legal_read_receipts (source, context_type, context_id);

do $$
begin
  if not exists (
    select 1 from pg_trigger
    where tgname = 'trg_user_legal_read_receipts_touch'
      and tgrelid = 'public.user_legal_read_receipts'::regclass
  ) then
    create trigger trg_user_legal_read_receipts_touch
      before update on public.user_legal_read_receipts
      for each row execute function public.mt_touch_updated_at();
  end if;
end $$;

alter table public.user_legal_read_receipts enable row level security;

drop policy if exists user_legal_read_receipts_select on public.user_legal_read_receipts;
create policy user_legal_read_receipts_select
  on public.user_legal_read_receipts
  for select
  to authenticated
  using (
    user_id = auth.uid()::text
    or public.mt_is_platform_master()
    or (tenant_id is not null and public.mt_can_manage_tenant(tenant_id))
  );

drop policy if exists user_legal_read_receipts_insert_own on public.user_legal_read_receipts;
create policy user_legal_read_receipts_insert_own
  on public.user_legal_read_receipts
  for insert
  to authenticated
  with check (user_id = auth.uid()::text);

drop policy if exists user_legal_read_receipts_update_own on public.user_legal_read_receipts;
create policy user_legal_read_receipts_update_own
  on public.user_legal_read_receipts
  for update
  to authenticated
  using (user_id = auth.uid()::text or public.mt_is_platform_master())
  with check (user_id = auth.uid()::text or public.mt_is_platform_master());
