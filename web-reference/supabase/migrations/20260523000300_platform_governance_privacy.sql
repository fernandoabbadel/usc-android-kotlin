create extension if not exists pgcrypto;

alter table public.users
  add column if not exists profile_public boolean not null default true,
  add column if not exists profile_photo_public boolean not null default true,
  add column if not exists allow_profile_discovery boolean not null default true,
  add column if not exists is_adult_confirmed boolean not null default false,
  add column if not exists adult_confirmed_at timestamptz,
  add column if not exists profile_visibility_updated_at timestamptz;

create table if not exists public.user_legal_acceptances (
  id uuid primary key default gen_random_uuid(),
  user_id text not null references public.users(uid) on delete cascade,
  document_type text not null,
  document_version text not null,
  accepted_at timestamptz not null default now(),
  ip_address text,
  user_agent text,
  tenant_id uuid references public.tenants(id) on delete set null,
  source text not null default 'app',
  created_at timestamptz not null default now(),
  constraint user_legal_acceptances_document_type_chk check (
    document_type in (
      'terms_of_service',
      'privacy_policy',
      'cookies_policy',
      'lgpd_rights',
      'admin_confidentiality',
      'tenant_terms'
    )
  ),
  constraint user_legal_acceptances_source_chk check (
    source in ('cadastro', 'primeiro_acesso', 'admin', 'tenant_admin', 'cookie_banner', 'app', 'api')
  )
);

create index if not exists idx_user_legal_acceptances_user
  on public.user_legal_acceptances (user_id, document_type, accepted_at desc);

create index if not exists idx_user_legal_acceptances_tenant
  on public.user_legal_acceptances (tenant_id, document_type, accepted_at desc);

create unique index if not exists uq_user_legal_acceptances_version
  on public.user_legal_acceptances (
    user_id,
    document_type,
    document_version,
    coalesce(tenant_id, '00000000-0000-0000-0000-000000000000'::uuid),
    source
  );

create table if not exists public.user_privacy_preferences (
  user_id text primary key references public.users(uid) on delete cascade,
  tenant_id uuid references public.tenants(id) on delete set null,
  analytics boolean not null default false,
  marketing boolean not null default false,
  profile_public boolean not null default true,
  photo_public boolean not null default true,
  phone_visibility boolean not null default false,
  email_notifications boolean not null default true,
  show_full_name boolean not null default true,
  show_turma boolean not null default true,
  show_plan boolean not null default true,
  show_achievements boolean not null default true,
  show_followers boolean not null default true,
  allow_discovery boolean not null default true,
  show_mini_vendor boolean not null default true,
  show_collectives boolean not null default true,
  updated_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

create index if not exists idx_user_privacy_preferences_tenant
  on public.user_privacy_preferences (tenant_id, profile_public, allow_discovery);

create table if not exists public.lgpd_requests (
  id uuid primary key default gen_random_uuid(),
  user_id text references public.users(uid) on delete set null,
  tenant_id uuid references public.tenants(id) on delete set null,
  requester_name text not null,
  requester_email text not null,
  request_type text not null,
  request_details text not null,
  status text not null default 'open',
  response text,
  ip_address text,
  user_agent text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint lgpd_requests_type_chk check (
    request_type in (
      'confirmacao_tratamento',
      'acesso',
      'correcao',
      'anonimizacao_bloqueio_eliminacao',
      'portabilidade',
      'eliminacao_consentimento',
      'compartilhamento',
      'informacao_consentimento',
      'revogacao_consentimento',
      'revisao_decisao_automatizada',
      'oposicao',
      'outro'
    )
  ),
  constraint lgpd_requests_status_chk check (
    status in ('open', 'in_review', 'waiting_identity_confirmation', 'answered', 'rejected', 'closed')
  )
);

create index if not exists idx_lgpd_requests_user
  on public.lgpd_requests (user_id, created_at desc);

create index if not exists idx_lgpd_requests_tenant_status
  on public.lgpd_requests (tenant_id, status, created_at desc);

create table if not exists public.admin_confidentiality_acceptances (
  id uuid primary key default gen_random_uuid(),
  user_id text not null references public.users(uid) on delete cascade,
  tenant_id uuid references public.tenants(id) on delete set null,
  role_context text not null,
  document_version text not null,
  accepted_at timestamptz not null default now(),
  ip_address text,
  user_agent text,
  source text not null default 'admin',
  created_at timestamptz not null default now()
);

create index if not exists idx_admin_confidentiality_user
  on public.admin_confidentiality_acceptances (user_id, tenant_id, accepted_at desc);

create unique index if not exists uq_admin_confidentiality_version
  on public.admin_confidentiality_acceptances (
    user_id,
    document_version,
    coalesce(tenant_id, '00000000-0000-0000-0000-000000000000'::uuid),
    role_context
  );

create table if not exists public.tenant_policy_documents (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references public.tenants(id) on delete cascade,
  module text not null,
  title text not null,
  content text not null default '',
  visible boolean not null default false,
  created_by_user_id text references public.users(uid) on delete set null,
  updated_by_user_id text references public.users(uid) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint tenant_policy_documents_module_chk check (
    module in (
      'eventos',
      'loja',
      'planos',
      'mini_vendor',
      'checkout',
      'reembolso_cancelamento',
      'bebidas_alcoolicas',
      'menores_de_idade',
      'termos_tenant'
    )
  )
);

create unique index if not exists uq_tenant_policy_documents_module
  on public.tenant_policy_documents (tenant_id, module);

create index if not exists idx_tenant_policy_documents_visible
  on public.tenant_policy_documents (tenant_id, module)
  where visible = true;

do $$
begin
  if not exists (
    select 1 from pg_trigger
    where tgname = 'trg_user_privacy_preferences_touch'
      and tgrelid = 'public.user_privacy_preferences'::regclass
  ) then
    create trigger trg_user_privacy_preferences_touch
      before update on public.user_privacy_preferences
      for each row execute function public.mt_touch_updated_at();
  end if;

  if not exists (
    select 1 from pg_trigger
    where tgname = 'trg_lgpd_requests_touch'
      and tgrelid = 'public.lgpd_requests'::regclass
  ) then
    create trigger trg_lgpd_requests_touch
      before update on public.lgpd_requests
      for each row execute function public.mt_touch_updated_at();
  end if;

  if not exists (
    select 1 from pg_trigger
    where tgname = 'trg_tenant_policy_documents_touch'
      and tgrelid = 'public.tenant_policy_documents'::regclass
  ) then
    create trigger trg_tenant_policy_documents_touch
      before update on public.tenant_policy_documents
      for each row execute function public.mt_touch_updated_at();
  end if;
end $$;

alter table public.user_legal_acceptances enable row level security;
alter table public.user_privacy_preferences enable row level security;
alter table public.lgpd_requests enable row level security;
alter table public.admin_confidentiality_acceptances enable row level security;
alter table public.tenant_policy_documents enable row level security;

drop policy if exists user_legal_acceptances_select on public.user_legal_acceptances;
create policy user_legal_acceptances_select
  on public.user_legal_acceptances
  for select
  to authenticated
  using (
    user_id = auth.uid()::text
    or public.mt_is_platform_master()
    or (tenant_id is not null and public.mt_can_manage_tenant(tenant_id))
  );

drop policy if exists user_legal_acceptances_insert_own on public.user_legal_acceptances;
create policy user_legal_acceptances_insert_own
  on public.user_legal_acceptances
  for insert
  to authenticated
  with check (user_id = auth.uid()::text);

drop policy if exists user_privacy_preferences_select_own on public.user_privacy_preferences;
create policy user_privacy_preferences_select_own
  on public.user_privacy_preferences
  for select
  to authenticated
  using (user_id = auth.uid()::text or public.mt_is_platform_master());

drop policy if exists user_privacy_preferences_insert_own on public.user_privacy_preferences;
create policy user_privacy_preferences_insert_own
  on public.user_privacy_preferences
  for insert
  to authenticated
  with check (user_id = auth.uid()::text);

drop policy if exists user_privacy_preferences_update_own on public.user_privacy_preferences;
create policy user_privacy_preferences_update_own
  on public.user_privacy_preferences
  for update
  to authenticated
  using (user_id = auth.uid()::text or public.mt_is_platform_master())
  with check (user_id = auth.uid()::text or public.mt_is_platform_master());

drop policy if exists lgpd_requests_select on public.lgpd_requests;
create policy lgpd_requests_select
  on public.lgpd_requests
  for select
  to authenticated
  using (
    user_id = auth.uid()::text
    or public.mt_is_platform_master()
    or (tenant_id is not null and public.mt_can_manage_tenant(tenant_id))
  );

drop policy if exists lgpd_requests_insert_own on public.lgpd_requests;
create policy lgpd_requests_insert_own
  on public.lgpd_requests
  for insert
  to authenticated
  with check (user_id is null or user_id = auth.uid()::text);

drop policy if exists admin_confidentiality_select on public.admin_confidentiality_acceptances;
create policy admin_confidentiality_select
  on public.admin_confidentiality_acceptances
  for select
  to authenticated
  using (
    user_id = auth.uid()::text
    or public.mt_is_platform_master()
    or (tenant_id is not null and public.mt_can_manage_tenant(tenant_id))
  );

drop policy if exists admin_confidentiality_insert_own on public.admin_confidentiality_acceptances;
create policy admin_confidentiality_insert_own
  on public.admin_confidentiality_acceptances
  for insert
  to authenticated
  with check (user_id = auth.uid()::text);

drop policy if exists tenant_policy_documents_select on public.tenant_policy_documents;
create policy tenant_policy_documents_select
  on public.tenant_policy_documents
  for select
  to anon, authenticated
  using (
    visible = true
    or public.mt_is_platform_master()
    or public.mt_can_manage_tenant(tenant_id)
  );

drop policy if exists tenant_policy_documents_insert on public.tenant_policy_documents;
create policy tenant_policy_documents_insert
  on public.tenant_policy_documents
  for insert
  to authenticated
  with check (public.mt_is_platform_master() or public.mt_can_manage_tenant(tenant_id));

drop policy if exists tenant_policy_documents_update on public.tenant_policy_documents;
create policy tenant_policy_documents_update
  on public.tenant_policy_documents
  for update
  to authenticated
  using (public.mt_is_platform_master() or public.mt_can_manage_tenant(tenant_id))
  with check (public.mt_is_platform_master() or public.mt_can_manage_tenant(tenant_id));

drop policy if exists tenant_policy_documents_delete on public.tenant_policy_documents;
create policy tenant_policy_documents_delete
  on public.tenant_policy_documents
  for delete
  to authenticated
  using (public.mt_is_platform_master() or public.mt_can_manage_tenant(tenant_id));
