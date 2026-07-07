alter table public.eventos
  add column if not exists custo numeric(12, 2),
  add column if not exists custos jsonb not null default '[]'::jsonb,
  add column if not exists "breakEven" numeric(12, 2);

comment on column public.eventos.custo is
  'Custo total opcional do evento, usado no BI Estratégico para resultado com custo e ponto de equilíbrio.';

comment on column public.eventos.custos is
  'Lista opcional de custos detalhados do evento. Cada item pode conter valor/value/total/custo/cost.';

comment on column public.eventos."breakEven" is
  'Ponto de equilíbrio opcional do evento para futuras integrações financeiras.';

do $$
begin
  if not exists (
    select 1
      from pg_constraint
     where conname = 'eventos_custo_non_negative_ck'
  ) then
    alter table public.eventos
      add constraint eventos_custo_non_negative_ck
      check (custo is null or custo >= 0);
  end if;

  if not exists (
    select 1
      from pg_constraint
     where conname = 'eventos_break_even_non_negative_ck'
  ) then
    alter table public.eventos
      add constraint eventos_break_even_non_negative_ck
      check ("breakEven" is null or "breakEven" >= 0);
  end if;
end $$;
