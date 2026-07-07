begin;

alter table public.solicitacoes_ingressos
  add column if not exists "checkinNote" text,
  add column if not exists "checkinEditedAt" timestamptz,
  add column if not exists "checkinEditedByUserId" text,
  add column if not exists "checkinEditedByUserName" text,
  add column if not exists "checkinAuditLog" jsonb not null default '[]'::jsonb,
  add column if not exists "transferAt" timestamptz,
  add column if not exists "transferFromUserId" text,
  add column if not exists "transferFromUserName" text,
  add column if not exists "transferToUserId" text,
  add column if not exists "transferToUserName" text,
  add column if not exists "transferByUserId" text,
  add column if not exists "transferByUserName" text,
  add column if not exists "transferHistory" jsonb not null default '[]'::jsonb;

with latest_ticket_transfer as (
  select distinct on (s.id)
         s.id,
         nullif(entry ->> 'transferredAt', '')::timestamptz as transferred_at,
         nullif(entry ->> 'transferredFromUserId', '') as transferred_from_user_id,
         nullif(entry ->> 'transferredFromUserName', '') as transferred_from_user_name,
         nullif(entry ->> 'transferredToUserId', '') as transferred_to_user_id,
         nullif(entry ->> 'transferredToUserName', '') as transferred_to_user_name,
         nullif(entry ->> 'transferByUserId', '') as transfer_by_user_id,
         nullif(entry ->> 'transferByUserName', '') as transfer_by_user_name,
         entry as transfer_entry
    from public.solicitacoes_ingressos s
    cross join lateral jsonb_array_elements(coalesce(s.payment_config -> 'ticketEntries', '[]'::jsonb)) as ticket(entry)
   where nullif(entry ->> 'transferredAt', '') is not null
      or nullif(entry ->> 'transferredFromUserName', '') is not null
      or nullif(entry ->> 'transferredToUserName', '') is not null
   order by s.id, nullif(entry ->> 'transferredAt', '')::timestamptz desc nulls last
)
update public.solicitacoes_ingressos s
   set "transferAt" = coalesce(s."transferAt", latest.transferred_at),
       "transferFromUserId" = coalesce(nullif(trim(s."transferFromUserId"), ''), latest.transferred_from_user_id),
       "transferFromUserName" = coalesce(nullif(trim(s."transferFromUserName"), ''), latest.transferred_from_user_name),
       "transferToUserId" = coalesce(nullif(trim(s."transferToUserId"), ''), latest.transferred_to_user_id),
       "transferToUserName" = coalesce(nullif(trim(s."transferToUserName"), ''), latest.transferred_to_user_name),
       "transferByUserId" = coalesce(nullif(trim(s."transferByUserId"), ''), latest.transfer_by_user_id),
       "transferByUserName" = coalesce(nullif(trim(s."transferByUserName"), ''), latest.transfer_by_user_name),
       "transferHistory" = case
         when jsonb_array_length(coalesce(s."transferHistory", '[]'::jsonb)) > 0
           then s."transferHistory"
         else jsonb_build_array(latest.transfer_entry)
       end
  from latest_ticket_transfer latest
 where latest.id = s.id;

create index if not exists idx_solic_ing_checkin_at
  on public.solicitacoes_ingressos ("eventoId", "checkinAt" desc)
  where "checkinAt" is not null;

create index if not exists idx_solic_ing_transfer_at
  on public.solicitacoes_ingressos ("eventoId", "transferAt" desc)
  where "transferAt" is not null;

commit;
