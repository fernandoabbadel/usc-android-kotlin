alter table public.user_legal_acceptances
  add column if not exists user_name text,
  add column if not exists authorization_scope text;

alter table public.user_legal_read_receipts
  add column if not exists user_name text,
  add column if not exists authorization_scope text;

update public.user_legal_acceptances ula
set user_name = coalesce(
  nullif(trim(u.nome), ''),
  nullif(trim(ula.metadata ->> 'userName'), ''),
  nullif(trim(ula.user_name), ''),
  ula.user_id
)
from public.users u
where u.uid = ula.user_id
  and (ula.user_name is null or trim(ula.user_name) = '');

update public.user_legal_read_receipts ulr
set user_name = coalesce(
  nullif(trim(u.nome), ''),
  nullif(trim(ulr.metadata ->> 'userName'), ''),
  nullif(trim(ulr.user_name), ''),
  ulr.user_id
)
from public.users u
where u.uid = ulr.user_id
  and (ulr.user_name is null or trim(ulr.user_name) = '');

with classified as (
  select
    id,
    case
      when scope_text like '%commission%' or scope_text like '%comiss%' then 'comissao'
      when scope_text like '%directory%' or scope_text like '%diret%' then 'diretorio'
      when scope_text like '%league%' or scope_text like '%liga%' then 'liga'
      else 'tenant'
    end as next_scope
  from (
    select
      id,
      lower(concat_ws(
        ' ',
        context_type,
        metadata ->> 'authorizationScope',
        metadata ->> 'authorization_scope',
        metadata ->> 'scope',
        metadata ->> 'area',
        metadata ->> 'category',
        source
      )) as scope_text
    from public.user_legal_acceptances
    where authorization_scope is null
      or trim(authorization_scope) = ''
      or authorization_scope not in ('tenant', 'liga', 'comissao', 'diretorio')
  ) pending
)
update public.user_legal_acceptances ula
set authorization_scope = classified.next_scope
from classified
where classified.id = ula.id;

with classified as (
  select
    id,
    case
      when scope_text like '%commission%' or scope_text like '%comiss%' then 'comissao'
      when scope_text like '%directory%' or scope_text like '%diret%' then 'diretorio'
      when scope_text like '%league%' or scope_text like '%liga%' then 'liga'
      else 'tenant'
    end as next_scope
  from (
    select
      id,
      lower(concat_ws(
        ' ',
        context_type,
        metadata ->> 'authorizationScope',
        metadata ->> 'authorization_scope',
        metadata ->> 'scope',
        metadata ->> 'area',
        metadata ->> 'category',
        source
      )) as scope_text
    from public.user_legal_read_receipts
    where authorization_scope is null
      or trim(authorization_scope) = ''
      or authorization_scope not in ('tenant', 'liga', 'comissao', 'diretorio')
  ) pending
)
update public.user_legal_read_receipts ulr
set authorization_scope = classified.next_scope
from classified
where classified.id = ulr.id;

alter table public.user_legal_acceptances
  drop constraint if exists user_legal_acceptances_authorization_scope_chk;

alter table public.user_legal_acceptances
  add constraint user_legal_acceptances_authorization_scope_chk check (
    authorization_scope is null
    or authorization_scope in ('tenant', 'liga', 'comissao', 'diretorio')
  );

alter table public.user_legal_read_receipts
  drop constraint if exists user_legal_read_receipts_authorization_scope_chk;

alter table public.user_legal_read_receipts
  add constraint user_legal_read_receipts_authorization_scope_chk check (
    authorization_scope is null
    or authorization_scope in ('tenant', 'liga', 'comissao', 'diretorio')
  );

create index if not exists idx_user_legal_acceptances_scope
  on public.user_legal_acceptances (tenant_id, authorization_scope, accepted_at desc);

create index if not exists idx_user_legal_acceptances_user_name
  on public.user_legal_acceptances (user_name);

create index if not exists idx_user_legal_read_receipts_scope
  on public.user_legal_read_receipts (tenant_id, authorization_scope, read_completed_at desc);

create index if not exists idx_user_legal_read_receipts_user_name
  on public.user_legal_read_receipts (user_name);

comment on column public.user_legal_acceptances.user_name is
  'Nome do usuário no momento do registro da autorização.';

comment on column public.user_legal_acceptances.authorization_scope is
  'Grupo principal da autorização: tenant, liga, comissao ou diretorio.';

comment on column public.user_legal_read_receipts.user_name is
  'Nome do usuário no momento do registro de leitura da autorização.';

comment on column public.user_legal_read_receipts.authorization_scope is
  'Grupo principal da autorização lida: tenant, liga, comissao ou diretorio.';
