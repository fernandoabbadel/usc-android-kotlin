begin;

alter table public.solicitacoes_adesao
  add column if not exists "dataAprovacao" timestamptz,
  add column if not exists "aprovadoPor" text,
  add column if not exists "approvalMethod" text,
  add column if not exists "paymentSource" text,
  add column if not exists "discountValue" numeric(12,2) default 0,
  add column if not exists "discountSource" text;

alter table public.orders
  add column if not exists "eventCreatedByUserId" text,
  add column if not exists custo numeric(12,2) default 0,
  add column if not exists cost numeric(12,2) default 0;

alter table public.produtos
  add column if not exists custo numeric(12,2) default 0,
  add column if not exists cost numeric(12,2) default 0,
  add column if not exists custos jsonb not null default '[]'::jsonb;

update public.solicitacoes_adesao
   set "dataAprovacao" = coalesce("dataAprovacao", "updatedAt"),
       "approvalMethod" = coalesce("approvalMethod", 'Manual'),
       "paymentSource" = coalesce("paymentSource", metodo)
 where lower(coalesce(status, '')) in ('aprovado', 'aprovada', 'approved')
   and ("dataAprovacao" is null or "approvalMethod" is null or "paymentSource" is null);

create index if not exists idx_solicitacoes_adesao_financeiro_tenant_status
  on public.solicitacoes_adesao (tenant_id, status, "dataSolicitacao" desc);

create index if not exists idx_orders_financeiro_seller_created
  on public.orders (tenant_id, seller_type, seller_id, "createdAt" desc);

create index if not exists idx_orders_financeiro_event_created
  on public.orders (tenant_id, "eventId", "createdAt" desc)
  where "eventId" is not null;

notify pgrst, 'reload schema';

commit;
