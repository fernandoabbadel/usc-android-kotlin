begin;

alter table public.eventos
  add column if not exists capacidade integer;

comment on column public.eventos.capacidade is
  'Capacidade total opcional do evento, usada pelo BI Portaria para ocupação e capacidade restante.';

with raw_capacity as (
  select
    id,
    coalesce(
      nullif(trim(data_extra #>> '{capacidade}'), ''),
      nullif(trim(data_extra #>> '{capacity}'), ''),
      nullif(trim(data_extra #>> '{capacidadeTotal}'), ''),
      nullif(trim(data_extra #>> '{eventParty,capacidade}'), ''),
      nullif(trim(data_extra #>> '{eventParty,capacity}'), ''),
      nullif(trim(data_extra #>> '{eventParty,capacidadeTotal}'), '')
    ) as value
  from public.eventos
  where capacidade is null
),
parsed_capacity as (
  select
    id,
    case
      when value ~ '^[0-9]+([.,][0-9]+)?$'
        then replace(value, ',', '.')::numeric
      else null
    end as value
  from raw_capacity
)
update public.eventos e
   set capacidade = greatest(0, floor(parsed_capacity.value)::integer)
  from parsed_capacity
 where parsed_capacity.id = e.id
   and parsed_capacity.value is not null;

do $$
begin
  if not exists (
    select 1
      from pg_constraint
     where conname = 'eventos_capacidade_non_negative_ck'
       and conrelid = 'public.eventos'::regclass
  ) then
    alter table public.eventos
      add constraint eventos_capacidade_non_negative_ck
      check (capacidade is null or capacidade >= 0);
  end if;
end
$$;

notify pgrst, 'reload schema';

commit;
