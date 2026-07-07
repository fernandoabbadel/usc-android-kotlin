begin;

alter table public.solicitacoes_ingressos
  add column if not exists "itemType" text not null default 'ingresso',
  add column if not exists "itemName" text,
  add column if not exists "itemCategory" text,
  add column if not exists "approvalMethod" text,
  add column if not exists "checkinAt" timestamptz,
  add column if not exists "checkinByUserId" text,
  add column if not exists "checkinByUserName" text,
  add column if not exists "checkinMethod" text,
  add column if not exists "discountValue" text,
  add column if not exists "discountKind" text,
  add column if not exists "discountSource" text;

alter table public.orders
  add column if not exists "eventId" text,
  add column if not exists "eventItemType" text,
  add column if not exists "eventItemName" text,
  add column if not exists "eventLoteNome" text,
  add column if not exists "eventItemCategory" text,
  add column if not exists "eventApprovalAt" timestamptz,
  add column if not exists "eventApprovalMethod" text,
  add column if not exists "eventCheckinAt" timestamptz,
  add column if not exists "eventCheckinByUserId" text,
  add column if not exists "eventCheckinByUserName" text,
  add column if not exists "eventCheckinMethod" text,
  add column if not exists "eventDiscountValue" text,
  add column if not exists "eventDiscountKind" text,
  add column if not exists "eventDiscountSource" text,
  add column if not exists "eventCreatedManually" boolean not null default false,
  add column if not exists "eventCreatedByName" text;

update public.solicitacoes_ingressos
   set "itemType" = coalesce(nullif(trim("itemType"), ''), 'ingresso'),
       "itemName" = coalesce(nullif(trim("itemName"), ''), nullif(trim("loteNome"), ''), 'Ingresso'),
       "itemCategory" = coalesce(
         nullif(trim("itemCategory"), ''),
         case
           when lower(coalesce("loteNome", '')) like '%nao aluno%'
             or lower(coalesce("loteNome", '')) like '%não aluno%'
             or lower(coalesce("loteNome", '')) like '%externo%'
             then 'Não aluno'
           when lower(coalesce("loteNome", '')) like '%aluno%'
             then 'Aluno'
           else null
         end
       ),
       "approvalMethod" = case
         when "dataAprovacao" is not null and nullif(trim(coalesce("approvalMethod", '')), '') is null
           then coalesce(nullif(data ->> 'approvalMethod', ''), 'manual')
         else "approvalMethod"
       end,
       "discountValue" = coalesce(nullif(trim("discountValue"), ''), nullif(data ->> 'discountValue', ''), 'R$ 0,00'),
       "discountKind" = coalesce(nullif(trim("discountKind"), ''), nullif(data ->> 'discountKind', '')),
       "discountSource" = coalesce(nullif(trim("discountSource"), ''), nullif(data ->> 'discountSource', ''))
 where true;

with latest_ticket_scan as (
  select distinct on (s.id)
         s.id,
         nullif(entry ->> 'scannedAt', '')::timestamptz as scanned_at,
         nullif(entry ->> 'scannedByUserId', '') as scanned_by_user_id,
         nullif(entry ->> 'scannedByUserName', '') as scanned_by_user_name,
         case
           when lower(coalesce(entry ->> 'scanSource', '')) = 'manual' then 'manual'
           else 'qr'
         end as scan_method
    from public.solicitacoes_ingressos s
    cross join lateral jsonb_array_elements(coalesce(s.payment_config -> 'ticketEntries', '[]'::jsonb)) as ticket(entry)
   where nullif(entry ->> 'scannedAt', '') is not null
      or lower(coalesce(entry ->> 'status', '')) = 'lido'
   order by s.id, nullif(entry ->> 'scannedAt', '')::timestamptz desc nulls last
)
update public.solicitacoes_ingressos s
   set "checkinAt" = coalesce(s."checkinAt", latest.scanned_at),
       "checkinByUserId" = coalesce(nullif(trim(s."checkinByUserId"), ''), latest.scanned_by_user_id),
       "checkinByUserName" = coalesce(nullif(trim(s."checkinByUserName"), ''), latest.scanned_by_user_name),
       "checkinMethod" = coalesce(nullif(trim(s."checkinMethod"), ''), latest.scan_method)
  from latest_ticket_scan latest
 where latest.id = s.id;

update public.orders
   set "eventId" = coalesce(nullif(trim("eventId"), ''), nullif(data #>> '{eventParty,eventId}', '')),
       "eventItemType" = coalesce(nullif(trim("eventItemType"), ''), 'produto'),
       "eventItemName" = coalesce(nullif(trim("eventItemName"), ''), nullif("productName", ''), nullif(data #>> '{eventParty,productName}', ''), 'Produto'),
       "eventLoteNome" = coalesce(nullif(trim("eventLoteNome"), ''), '-'),
       "eventItemCategory" = coalesce(nullif(trim("eventItemCategory"), ''), nullif(data #>> '{eventParty,section}', '')),
       "eventApprovalAt" = coalesce("eventApprovalAt", nullif(data #>> '{eventParty,approvedAt}', '')::timestamptz),
       "eventApprovalMethod" = coalesce(nullif(trim("eventApprovalMethod"), ''), nullif(data #>> '{eventParty,approvalMethod}', '')),
       "eventCheckinAt" = coalesce("eventCheckinAt", nullif(data #>> '{eventParty,usedAt}', '')::timestamptz),
       "eventCheckinByUserId" = coalesce(nullif(trim("eventCheckinByUserId"), ''), nullif(data #>> '{eventParty,usedByUserId}', '')),
       "eventCheckinByUserName" = coalesce(nullif(trim("eventCheckinByUserName"), ''), nullif(data #>> '{eventParty,usedByUserName}', '')),
       "eventCheckinMethod" = coalesce(
         nullif(trim("eventCheckinMethod"), ''),
         case when nullif(data #>> '{eventParty,usedAt}', '') is not null then 'qr' else null end
       ),
       "eventDiscountValue" = coalesce(nullif(trim("eventDiscountValue"), ''), 'R$ 0,00'),
       "eventCreatedManually" = coalesce("eventCreatedManually", false) or
         case lower(coalesce(data #>> '{eventParty,createdManually}', ''))
           when 'true' then true
           when '1' then true
           when 'yes' then true
           else false
         end,
       "eventCreatedByName" = coalesce(nullif(trim("eventCreatedByName"), ''), nullif(data #>> '{eventParty,createdByName}', ''))
 where data ? 'eventParty';

create index if not exists idx_solic_ing_event_statement_type
  on public.solicitacoes_ingressos ("eventoId", "itemType", "dataSolicitacao" desc);

create index if not exists idx_orders_event_statement_type
  on public.orders ("eventId", "eventItemType", "createdAt" desc)
  where "eventId" is not null;

create or replace view public.eventos_extrato_unificado as
select
  s.tenant_id,
  s."eventoId" as "eventId",
  s.id as "pedidoId",
  coalesce(nullif(s."itemType", ''), 'ingresso') as tipo,
  s."userId",
  s."userName",
  s."userTurma",
  coalesce(nullif(s."itemName", ''), nullif(s."loteNome", ''), 'Ingresso') as "itemName",
  coalesce(nullif(s."loteNome", ''), '-') as lote,
  coalesce(nullif(s."itemCategory", ''), '-') as categoria,
  s.quantidade,
  s."valorTotal" as valor,
  coalesce(nullif(s."discountValue", ''), 'R$ 0,00') as desconto,
  coalesce(nullif(s."discountSource", ''), '-') as "discountSource",
  s."dataSolicitacao" as "pedidoAt",
  s."dataAprovacao" as "approvalAt",
  s."aprovadoPor" as "approvedBy",
  coalesce(nullif(s."approvalMethod", ''), '-') as "approvalMethod",
  s."checkinAt",
  s."checkinByUserName",
  coalesce(nullif(s."checkinMethod", ''), '-') as "checkinMethod",
  s.status,
  'solicitacoes_ingressos'::text as origem
from public.solicitacoes_ingressos s
union all
select
  o.tenant_id,
  o."eventId",
  o.id as "pedidoId",
  coalesce(nullif(o."eventItemType", ''), 'produto') as tipo,
  o."userId",
  o."userName",
  null::text as "userTurma",
  coalesce(nullif(o."eventItemName", ''), nullif(o."productName", ''), 'Produto') as "itemName",
  coalesce(nullif(o."eventLoteNome", ''), '-') as lote,
  coalesce(nullif(o."eventItemCategory", ''), '-') as categoria,
  o.quantidade,
  o.total::text as valor,
  coalesce(nullif(o."eventDiscountValue", ''), 'R$ 0,00') as desconto,
  coalesce(nullif(o."eventDiscountSource", ''), '-') as "discountSource",
  o."createdAt" as "pedidoAt",
  o."eventApprovalAt" as "approvalAt",
  o."approvedBy" as "approvedBy",
  coalesce(nullif(o."eventApprovalMethod", ''), '-') as "approvalMethod",
  o."eventCheckinAt" as "checkinAt",
  o."eventCheckinByUserName" as "checkinByUserName",
  coalesce(nullif(o."eventCheckinMethod", ''), '-') as "checkinMethod",
  o.status,
  'orders'::text as origem
from public.orders o
where o."eventId" is not null;

commit;
