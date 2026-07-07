begin;

alter table public.produtos enable row level security;
alter table public.eventos enable row level security;

drop policy if exists public_event_party_products_select on public.produtos;
create policy public_event_party_products_select
  on public.produtos
  for select
  to public
  using (
    aprovado is true
    and data ? 'eventParty'
    and nullif(data #>> '{eventParty,eventId}', '') is not null
  );

drop policy if exists public_event_party_events_select on public.eventos;
create policy public_event_party_events_select
  on public.eventos
  for select
  to public
  using (
    lower(coalesce(data_extra #>> '{eventParty,enabled}', 'false')) in ('true', '1', 'yes', 'sim')
  );

update public.produtos
   set active = false,
       data = jsonb_set(
         jsonb_set(
           coalesce(data, '{}'::jsonb),
           '{eventParty,onlyEventStore}',
           'true'::jsonb,
           true
         ),
         '{eventParty,hiddenFromStore}',
         'true'::jsonb,
         true
       ),
       "updatedAt" = now()
 where data ? 'eventParty'
   and nullif(data #>> '{eventParty,eventId}', '') is not null;

create index if not exists idx_produtos_event_party_public
  on public.produtos (tenant_id, ((data #>> '{eventParty,eventId}')), aprovado)
  where data ? 'eventParty';

commit;
