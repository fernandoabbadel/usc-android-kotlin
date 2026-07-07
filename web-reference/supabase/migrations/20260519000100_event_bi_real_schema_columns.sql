begin;

create or replace function pg_temp.event_bi_integer(value text)
returns integer
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

  return greatest(0, floor(cleaned::numeric))::integer;
exception
  when others then
    return null;
end;
$$;

alter table public.eventos
  add column if not exists capacity integer,
  add column if not exists vagas integer;

comment on column public.eventos.capacity is
  'Alias numerico opcional de capacidade do evento usado por integracoes e BIs.';

comment on column public.eventos.vagas is
  'Alias numerico opcional de vagas/capacidade do evento usado por integracoes e BIs.';

update public.eventos
   set capacity = coalesce(
         capacity,
         capacidade,
         pg_temp.event_bi_integer(data_extra #>> '{capacity}'),
         pg_temp.event_bi_integer(data_extra #>> '{capacidade}'),
         pg_temp.event_bi_integer(data_extra #>> '{capacidadeTotal}'),
         pg_temp.event_bi_integer(data_extra #>> '{eventParty,capacity}'),
         pg_temp.event_bi_integer(data_extra #>> '{eventParty,capacidade}'),
         pg_temp.event_bi_integer(data_extra #>> '{eventParty,capacidadeTotal}')
       ),
       vagas = coalesce(
         vagas,
         capacidade,
         capacity,
         pg_temp.event_bi_integer(data_extra #>> '{vagas}'),
         pg_temp.event_bi_integer(data_extra #>> '{capacity}'),
         pg_temp.event_bi_integer(data_extra #>> '{capacidade}'),
         pg_temp.event_bi_integer(data_extra #>> '{capacidadeTotal}'),
         pg_temp.event_bi_integer(data_extra #>> '{eventParty,vagas}'),
         pg_temp.event_bi_integer(data_extra #>> '{eventParty,capacity}'),
         pg_temp.event_bi_integer(data_extra #>> '{eventParty,capacidade}'),
         pg_temp.event_bi_integer(data_extra #>> '{eventParty,capacidadeTotal}')
       )
 where true;

alter table public.users
  add column if not exists phone text;

comment on column public.users.phone is
  'Alias opcional de telefone usado por integracoes e BIs.';

update public.users
   set phone = coalesce(nullif(trim(phone), ''), nullif(trim(telefone), ''))
 where phone is null or trim(phone) = '';

do $$
begin
  if not exists (
    select 1
      from pg_constraint
     where conname = 'eventos_capacity_non_negative_ck'
       and conrelid = 'public.eventos'::regclass
  ) then
    alter table public.eventos
      add constraint eventos_capacity_non_negative_ck
      check (capacity is null or capacity >= 0);
  end if;

  if not exists (
    select 1
      from pg_constraint
     where conname = 'eventos_vagas_non_negative_ck'
       and conrelid = 'public.eventos'::regclass
  ) then
    alter table public.eventos
      add constraint eventos_vagas_non_negative_ck
      check (vagas is null or vagas >= 0);
  end if;
end
$$;

notify pgrst, 'reload schema';

commit;
