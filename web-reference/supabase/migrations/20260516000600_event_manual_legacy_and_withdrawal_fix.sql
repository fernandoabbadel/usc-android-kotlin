begin;

update public.solicitacoes_ingressos
   set "dataPagamento" = coalesce("dataPagamento", "paymentDate", "paidAt", "dataAprovacao"),
       "paymentDate" = coalesce("paymentDate", "dataPagamento", "paidAt", "dataAprovacao"),
       "paidAt" = coalesce("paidAt", "dataPagamento", "paymentDate", "dataAprovacao")
 where lower(coalesce(status, '')) in ('aprovado', 'approved', 'pago', 'paid')
   and ("dataPagamento" is null or "paymentDate" is null or "paidAt" is null);

with manual_users as (
  select
    e.id as event_id,
    e.tenant_id,
    e.titulo,
    coalesce(e.payment_config::jsonb, '{}'::jsonb) as event_payment_config,
    entry.value as row_data,
    entry.ordinality
  from public.eventos e
  cross join lateral jsonb_array_elements(
    case
      when jsonb_typeof(coalesce(e.data_extra::jsonb #> '{eventParty,manualUsers}', '[]'::jsonb)) = 'array'
        then coalesce(e.data_extra::jsonb #> '{eventParty,manualUsers}', '[]'::jsonb)
      else '[]'::jsonb
    end
  ) with ordinality as entry(value, ordinality)
),
normalized as (
  select
    event_id,
    tenant_id,
    titulo,
    event_payment_config,
    row_data,
    ordinality,
    nullif(trim(coalesce(row_data ->> 'orderId', row_data ->> 'ticketRequestId', row_data ->> 'saleId', '')), '') as existing_order_id,
    nullif(trim(coalesce(row_data ->> 'id', row_data ->> 'userId', '')), '') as manual_id,
    nullif(trim(coalesce(row_data ->> 'nome', row_data ->> 'name', row_data ->> 'userName', 'Usuário manual')), '') as nome,
    nullif(trim(coalesce(row_data ->> 'numero', row_data ->> 'number', row_data ->> 'externalNumber', row_data ->> 'ra', '')), '') as pulseira,
    nullif(trim(coalesce(row_data ->> 'turma', row_data ->> 'className', 'Porta')), '') as turma,
    nullif(trim(coalesce(row_data ->> 'cpf', '')), '') as cpf,
    nullif(trim(coalesce(row_data ->> 'telefone', row_data ->> 'phone', '')), '') as telefone,
    nullif(trim(coalesce(row_data ->> 'email', '')), '') as email,
    nullif(trim(coalesce(row_data ->> 'ra', '')), '') as ra,
    coalesce(nullif(trim(coalesce(row_data ->> 'valorPorta', row_data ->> 'gateValue', row_data ->> 'value', '')), ''), '0,00') as valor_porta,
    coalesce(nullif(trim(coalesce(row_data ->> 'createdByName', '')), ''), 'Admin') as created_by_name,
    case
      when nullif(row_data ->> 'createdAt', '') ~ '^\d{4}-\d{2}-\d{2}'
        then nullif(row_data ->> 'createdAt', '')::timestamptz
      else now()
    end as created_at
  from manual_users
),
ready as (
  select
    coalesce(existing_order_id, md5('legacy-manual-entry:' || event_id || ':' || coalesce(manual_id, '') || ':' || coalesce(pulseira, '') || ':' || ordinality::text)) as order_id,
    coalesce(manual_id, 'manual-porta-' || md5(event_id || ':' || coalesce(pulseira, '') || ':' || ordinality::text)) as manual_user_id,
    *
  from normalized
  where nome is not null
    and pulseira is not null
    and existing_order_id is null
)
insert into public.solicitacoes_ingressos (
  id,
  tenant_id,
  "userId",
  "userName",
  "userTurma",
  status,
  "eventoId",
  "eventoNome",
  "loteId",
  "loteNome",
  quantidade,
  "valorUnitario",
  "valorTotal",
  "dataSolicitacao",
  "dataAprovacao",
  "dataPagamento",
  "paymentDate",
  "paidAt",
  "aprovadoPor",
  "itemType",
  "itemName",
  "itemCategory",
  "approvalMethod",
  "checkinAt",
  "checkinByUserName",
  "checkinMethod",
  "checkinNote",
  "discountValue",
  "discountKind",
  "discountSource",
  payment_config,
  data,
  "createdAt"
)
select
  order_id,
  tenant_id,
  manual_user_id,
  nome,
  coalesce(turma, 'Porta'),
  'aprovado',
  event_id,
  coalesce(nullif(titulo, ''), 'Evento'),
  'entrada-porta',
  'Entrada/porta',
  1,
  valor_porta,
  valor_porta,
  created_at,
  created_at,
  created_at,
  created_at,
  created_at,
  created_by_name,
  'ingresso',
  'Entrada/porta',
  'Entrada/porta',
  'manual_porta_legacy',
  created_at,
  created_by_name,
  'manual',
  'Cadastro manual legado migrado para lista de presença',
  'R$ 0,00',
  '',
  '',
  event_payment_config || jsonb_build_object(
    'ticketEntries',
    jsonb_build_array(
      jsonb_build_object(
        'id', order_id || ':1',
        'token', gen_random_uuid()::text,
        'label', 'Pulseira ' || pulseira,
        'unitIndex', 1,
        'status', 'lido',
        'orderId', order_id,
        'orderCode', upper(left(order_id, 8)),
        'eventId', event_id,
        'eventTitle', coalesce(nullif(titulo, ''), 'Evento'),
        'loteName', 'Entrada/porta',
        'holderName', nome,
        'holderTurma', coalesce(turma, 'Porta'),
        'scannedAt', created_at,
        'scannedByUserName', created_by_name,
        'scanSource', 'manual',
        'checkinNote', 'Cadastro manual legado migrado para lista de presença'
      )
    )
  ),
  jsonb_build_object(
    'manualGateEntry', true,
    'legacyManualBackfill', true,
    'manualUserId', manual_user_id,
    'pulseira', pulseira,
    'braceletNumber', pulseira,
    'valorPorta', valor_porta,
    'cpf', coalesce(cpf, ''),
    'telefone', coalesce(telefone, ''),
    'email', coalesce(email, ''),
    'ra', coalesce(ra, pulseira),
    'dataPagamento', created_at,
    'paymentDate', created_at,
    'checkinAt', created_at,
    'approvedAt', created_at,
    'createdByName', created_by_name
  ),
  created_at
from ready r
where not exists (
  select 1
    from public.solicitacoes_ingressos s
   where s."eventoId" = r.event_id
     and (
       s.id = r.order_id
       or (
         coalesce(s.data::jsonb ->> 'manualGateEntry', 'false') = 'true'
         and (
           s.data::jsonb ->> 'pulseira' = r.pulseira
           or s."userId" = r.manual_user_id
           or (lower(coalesce(s."userName", '')) = lower(r.nome) and coalesce(s."loteNome", '') = 'Entrada/porta')
         )
       )
     )
);

with target_orders as (
  select
    id,
    data::jsonb as data_json
  from public.orders
  where coalesce("eventCreatedManually", false) = true
    and nullif(data #>> '{eventParty,manualCode}', '') is not null
    and lower(coalesce("eventCheckinMethod", data #>> '{eventParty,withdrawalMethod}', data #>> '{eventParty,usedMethod}', '')) = 'manual'
    and (
      "eventCheckinAt" is null
      or "eventApprovalAt" is null
      or "eventCheckinAt" <= "eventApprovalAt" + interval '5 seconds'
      or "eventCheckinAt" <= "createdAt" + interval '5 seconds'
    )
),
entry_updates as (
  select
    target_orders.id,
    jsonb_agg(
      (
        entry.value
          - 'usedAt'
          - 'usedByUserId'
          - 'usedByUserName'
          - 'usedMethod'
          - 'withdrawalAt'
          - 'withdrawalByUserId'
          - 'withdrawalByUserName'
          - 'withdrawalMethod'
      ) || jsonb_build_object('status', 'ativo')
      order by entry.ordinality
    ) as voucher_entries
  from target_orders
  cross join lateral jsonb_array_elements(coalesce(target_orders.data_json #> '{eventParty,voucherEntries}', '[]'::jsonb))
    with ordinality as entry(value, ordinality)
  group by target_orders.id
)
update public.orders o
   set "eventCheckinAt" = null,
       "eventCheckinByUserId" = null,
       "eventCheckinByUserName" = null,
       "eventCheckinMethod" = null,
       data = jsonb_set(
         jsonb_set(
           (
             (
               (
                 (
                   (
                     (
                       (
                         (
                           target_orders.data_json
                             #- '{eventParty,usedAt}'
                         ) #- '{eventParty,usedByUserId}'
                       ) #- '{eventParty,usedByUserName}'
                     ) #- '{eventParty,usedMethod}'
                   ) #- '{eventParty,withdrawalAt}'
                 ) #- '{eventParty,withdrawalByUserId}'
               ) #- '{eventParty,withdrawalByUserName}'
             ) #- '{eventParty,withdrawalMethod}'
           ),
           '{eventParty,voucherStatus}',
           '"ativo"'::jsonb,
           true
         ),
         '{eventParty,voucherEntries}',
         coalesce(entry_updates.voucher_entries, '[]'::jsonb),
         true
       ),
       "updatedAt" = now()
  from target_orders
  left join entry_updates on entry_updates.id = target_orders.id
 where o.id = target_orders.id;

notify pgrst, 'reload schema';

commit;
