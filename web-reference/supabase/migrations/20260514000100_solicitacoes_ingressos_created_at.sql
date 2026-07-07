begin;

alter table public.solicitacoes_ingressos
  add column if not exists "createdAt" timestamptz;

update public.solicitacoes_ingressos
   set "createdAt" = coalesce("dataSolicitacao"::timestamptz, now())
 where "createdAt" is null;

alter table public.solicitacoes_ingressos
  alter column "createdAt" set default now(),
  alter column "createdAt" set not null;

create index if not exists idx_solic_ing_created_at
  on public.solicitacoes_ingressos ("createdAt" desc);

notify pgrst, 'reload schema';

commit;
