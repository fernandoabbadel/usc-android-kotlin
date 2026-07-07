update public.user_legal_acceptances
set authorization_scope = 'mini_vendor'
where context_type = 'mini_vendor_data_use'
  or source = 'mini_vendor_creation'
  or metadata ->> 'authorizationScope' = 'mini_vendor'
  or metadata ->> 'authorization_scope' = 'mini_vendor';

update public.user_legal_read_receipts
set authorization_scope = 'mini_vendor'
where context_type = 'mini_vendor_data_use'
  or source = 'mini_vendor_creation'
  or metadata ->> 'authorizationScope' = 'mini_vendor'
  or metadata ->> 'authorization_scope' = 'mini_vendor';

alter table public.user_legal_acceptances
  drop constraint if exists user_legal_acceptances_authorization_scope_chk;

alter table public.user_legal_acceptances
  add constraint user_legal_acceptances_authorization_scope_chk check (
    authorization_scope is null
    or authorization_scope in ('tenant', 'liga', 'comissao', 'diretorio', 'mini_vendor')
  );

alter table public.user_legal_read_receipts
  drop constraint if exists user_legal_read_receipts_authorization_scope_chk;

alter table public.user_legal_read_receipts
  add constraint user_legal_read_receipts_authorization_scope_chk check (
    authorization_scope is null
    or authorization_scope in ('tenant', 'liga', 'comissao', 'diretorio', 'mini_vendor')
  );

comment on column public.user_legal_acceptances.authorization_scope is
  'Grupo principal da autorização: tenant, liga, comissao, diretorio ou mini_vendor.';

comment on column public.user_legal_read_receipts.authorization_scope is
  'Grupo principal da autorização lida: tenant, liga, comissao, diretorio ou mini_vendor.';
