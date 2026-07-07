begin;

alter table public.solicitacoes_ingressos
  add column if not exists "dataPagamento" timestamptz,
  add column if not exists "paymentDate" timestamptz,
  add column if not exists "paidAt" timestamptz;

alter table public.users
  add column if not exists email text,
  add column if not exists telefone text,
  add column if not exists ra text,
  add column if not exists cpf text;

create index if not exists idx_solicitacoes_ingressos_evento_data_pagamento
  on public.solicitacoes_ingressos ("eventoId", "dataPagamento" desc)
  where "dataPagamento" is not null;

create index if not exists idx_users_tenant_email_lower
  on public.users (tenant_id, lower(email))
  where email is not null;

create index if not exists idx_users_tenant_cpf
  on public.users (tenant_id, cpf)
  where cpf is not null;

create index if not exists idx_users_tenant_ra
  on public.users (tenant_id, ra)
  where ra is not null;

notify pgrst, 'reload schema';

commit;
