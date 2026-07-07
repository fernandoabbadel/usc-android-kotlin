begin;

alter table public.eventos
  add column if not exists custo numeric(12, 2),
  add column if not exists custos jsonb not null default '[]'::jsonb,
  add column if not exists "breakEven" numeric(12, 2),
  add column if not exists break_even numeric(12, 2),
  add column if not exists "totalCost" numeric(12, 2),
  add column if not exists cost numeric(12, 2);

alter table public.solicitacoes_ingressos
  add column if not exists email text,
  add column if not exists telefone text,
  add column if not exists phone text,
  add column if not exists "paymentSource" text,
  add column if not exists "paymentMethod" text,
  add column if not exists source text;

alter table public.orders
  add column if not exists "userTurma" text,
  add column if not exists email text,
  add column if not exists telefone text,
  add column if not exists phone text,
  add column if not exists "paymentSource" text,
  add column if not exists "paymentMethod" text,
  add column if not exists source text,
  add column if not exists "eventCreatedByUserId" text;

create or replace function pg_temp.event_bi_numeric(value text)
returns numeric
language plpgsql
as $$
declare
  cleaned text;
begin
  cleaned := trim(coalesce(value, ''));
  if cleaned = '' then
    return null;
  end if;

  cleaned := regexp_replace(cleaned, '[^0-9,.-]', '', 'g');
  if cleaned ~ ',[0-9]{1,2}$' then
    cleaned := replace(replace(cleaned, '.', ''), ',', '.');
  else
    cleaned := replace(cleaned, ',', '');
  end if;

  if cleaned = '' or cleaned = '-' or cleaned = '.' then
    return null;
  end if;

  return cleaned::numeric;
exception
  when others then
    return null;
end;
$$;

update public.eventos
   set custo = coalesce(
         custo,
         "totalCost",
         cost,
         pg_temp.event_bi_numeric(data_extra #>> '{custo}'),
         pg_temp.event_bi_numeric(data_extra #>> '{cost}'),
         pg_temp.event_bi_numeric(data_extra #>> '{totalCost}'),
         pg_temp.event_bi_numeric(stats #>> '{custo}'),
         pg_temp.event_bi_numeric(stats #>> '{cost}'),
         pg_temp.event_bi_numeric(stats #>> '{totalCost}')
       ),
       cost = coalesce(
         cost,
         custo,
         "totalCost",
         pg_temp.event_bi_numeric(data_extra #>> '{cost}'),
         pg_temp.event_bi_numeric(data_extra #>> '{custo}'),
         pg_temp.event_bi_numeric(data_extra #>> '{totalCost}')
       ),
       "totalCost" = coalesce(
         "totalCost",
         custo,
         cost,
         pg_temp.event_bi_numeric(data_extra #>> '{totalCost}'),
         pg_temp.event_bi_numeric(data_extra #>> '{custo}'),
         pg_temp.event_bi_numeric(data_extra #>> '{cost}')
       ),
       "breakEven" = coalesce(
         "breakEven",
         break_even,
         pg_temp.event_bi_numeric(data_extra #>> '{breakEven}'),
         pg_temp.event_bi_numeric(data_extra #>> '{break_even}')
       ),
       break_even = coalesce(
         break_even,
         "breakEven",
         pg_temp.event_bi_numeric(data_extra #>> '{break_even}'),
         pg_temp.event_bi_numeric(data_extra #>> '{breakEven}')
       )
 where true;

update public.solicitacoes_ingressos
   set source = coalesce(
         nullif(trim(source), ''),
         nullif(data ->> 'source', ''),
         nullif(data ->> 'origem', ''),
         case
           when coalesce((data ->> 'manualGateEntry')::boolean, false) then 'Cadastro manual'
           else null
         end
       ),
       "paymentSource" = coalesce(
         nullif(trim("paymentSource"), ''),
         nullif(data ->> 'paymentSource', ''),
         nullif(payment_config ->> 'method', ''),
         nullif(payment_config ->> 'provider', '')
       ),
       "paymentMethod" = coalesce(
         nullif(trim("paymentMethod"), ''),
         nullif(data ->> 'paymentMethod', ''),
         nullif(payment_config ->> 'method', '')
       )
 where true;

update public.orders
   set source = coalesce(
         nullif(trim(source), ''),
         nullif(data #>> '{eventParty,source}', ''),
         nullif(data ->> 'source', '')
       ),
       "paymentSource" = coalesce(
         nullif(trim("paymentSource"), ''),
         nullif(data #>> '{eventParty,paymentSource}', ''),
         nullif(data ->> 'paymentSource', ''),
         nullif(payment_config ->> 'method', ''),
         nullif(payment_config ->> 'provider', '')
       ),
       "paymentMethod" = coalesce(
         nullif(trim("paymentMethod"), ''),
         nullif(data #>> '{eventParty,paymentMethod}', ''),
         nullif(data ->> 'paymentMethod', ''),
         nullif(payment_config ->> 'method', '')
       ),
       "eventCreatedByUserId" = coalesce(
         nullif(trim("eventCreatedByUserId"), ''),
         nullif(data #>> '{eventParty,createdByUserId}', ''),
         nullif(data #>> '{eventParty,createdBy}', '')
       )
 where true;

do $$
begin
  if not exists (
    select 1 from pg_constraint where conname = 'eventos_custo_non_negative_ck'
  ) then
    alter table public.eventos
      add constraint eventos_custo_non_negative_ck
      check (custo is null or custo >= 0);
  end if;

  if not exists (
    select 1 from pg_constraint where conname = 'eventos_break_even_non_negative_ck'
  ) then
    alter table public.eventos
      add constraint eventos_break_even_non_negative_ck
      check ("breakEven" is null or "breakEven" >= 0);
  end if;

  if not exists (
    select 1 from pg_constraint where conname = 'eventos_break_even_snake_non_negative_ck'
  ) then
    alter table public.eventos
      add constraint eventos_break_even_snake_non_negative_ck
      check (break_even is null or break_even >= 0);
  end if;
end $$;

create index if not exists idx_solicitacoes_ingressos_source
  on public.solicitacoes_ingressos (tenant_id, source)
  where source is not null;

create index if not exists idx_orders_event_created_by_user_id
  on public.orders (tenant_id, "eventCreatedByUserId")
  where "eventCreatedByUserId" is not null;

notify pgrst, 'reload schema';

commit;
