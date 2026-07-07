alter table public.users
  add column if not exists legal_terms_accepted_at timestamptz,
  add column if not exists legal_privacy_accepted_at timestamptz,
  add column if not exists legal_accepted_version text,
  add column if not exists legal_accepted_source text,
  add column if not exists legal_accepted_tenant_id uuid;

create index if not exists idx_users_legal_terms_accepted_at
  on public.users (legal_terms_accepted_at);
