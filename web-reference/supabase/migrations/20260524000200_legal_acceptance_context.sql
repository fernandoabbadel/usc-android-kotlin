alter table public.user_legal_acceptances
  add column if not exists context_type text,
  add column if not exists context_id text,
  add column if not exists metadata jsonb not null default '{}'::jsonb;

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

alter table public.user_legal_read_receipts
  add column if not exists context_type text,
  add column if not exists context_id text,
  add column if not exists metadata jsonb not null default '{}'::jsonb;

drop index if exists public.uq_user_legal_read_receipts_version;

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

create index if not exists idx_user_legal_read_receipts_context
  on public.user_legal_read_receipts (source, context_type, context_id);
