-- Reconciles legacy album captures that were migrated with stale tenant/turma
-- metadata. The source of truth is the current collector tenant and target turma.

with capture_targets as (
  select
    ac.id,
    collector.tenant_id as next_tenant_id,
    upper(coalesce(nullif(trim(target.turma), ''), 'OUTROS')) as next_turma,
    nullif(trim(target.nome), '') as next_nome,
    row_number() over (
      partition by collector.tenant_id, ac."collectorUserId", ac."targetUserId"
      order by ac."dataColada" desc nulls last, ac.id desc
    ) as keep_rank
  from public.album_captures ac
  join public.users collector
    on collector.uid = ac."collectorUserId"
   and collector.tenant_id is not null
  join public.users target
    on target.uid = ac."targetUserId"
   and target.tenant_id = collector.tenant_id
)
delete from public.album_captures ac
using capture_targets ct
where ac.id = ct.id
  and ct.keep_rank > 1;

with capture_targets as (
  select
    ac.id,
    collector.tenant_id as next_tenant_id,
    upper(coalesce(nullif(trim(target.turma), ''), 'OUTROS')) as next_turma,
    nullif(trim(target.nome), '') as next_nome
  from public.album_captures ac
  join public.users collector
    on collector.uid = ac."collectorUserId"
   and collector.tenant_id is not null
  join public.users target
    on target.uid = ac."targetUserId"
   and target.tenant_id = collector.tenant_id
)
update public.album_captures ac
set
  tenant_id = ct.next_tenant_id,
  turma = ct.next_turma,
  nome = coalesce(ct.next_nome, ac.nome)
from capture_targets ct
where ac.id = ct.id
  and (
    ac.tenant_id is distinct from ct.next_tenant_id
    or upper(coalesce(nullif(trim(ac.turma), ''), 'OUTROS')) is distinct from ct.next_turma
    or nullif(trim(coalesce(ac.nome, '')), '') is null
  );

drop index if exists public.uq_album_captures_collector_target;

create unique index if not exists uq_album_captures_tenant_collector_target
  on public.album_captures (tenant_id, "collectorUserId", "targetUserId");

truncate table public.album_summary;

insert into public.album_summary (
  "userId",
  "totalCollected",
  "lastCaptureId",
  "lastCaptureAt",
  "updatedAt",
  tenant_id
)
with ranked_captures as (
  select
    ac.tenant_id,
    ac."collectorUserId" as user_id,
    ac."targetUserId" as target_user_id,
    ac."dataColada" as captured_at,
    row_number() over (
      partition by ac.tenant_id, ac."collectorUserId"
      order by ac."dataColada" desc nulls last, ac.id desc
    ) as capture_rank
  from public.album_captures ac
),
capture_totals as (
  select
    rc.tenant_id,
    rc.user_id,
    count(distinct rc.target_user_id)::integer as total_collected,
    max(rc.captured_at) as updated_at
  from ranked_captures rc
  group by rc.tenant_id, rc.user_id
),
last_capture as (
  select
    rc.tenant_id,
    rc.user_id,
    rc.target_user_id as last_capture_id,
    rc.captured_at as last_capture_at
  from ranked_captures rc
  where rc.capture_rank = 1
)
select
  ct.user_id,
  ct.total_collected,
  lc.last_capture_id,
  lc.last_capture_at,
  coalesce(lc.last_capture_at, ct.updated_at, now()) as "updatedAt",
  ct.tenant_id
from capture_totals ct
left join last_capture lc
  on lc.tenant_id = ct.tenant_id
 and lc.user_id = ct.user_id;

truncate table public.album_summary_turmas;

insert into public.album_summary_turmas (
  "userId",
  turma,
  "capturedCount",
  tenant_id,
  "updatedAt"
)
select
  ac."collectorUserId",
  upper(coalesce(nullif(trim(ac.turma), ''), 'OUTROS')),
  count(distinct ac."targetUserId")::integer,
  ac.tenant_id,
  max(coalesce(ac."dataColada", now()))
from public.album_captures ac
group by ac.tenant_id, ac."collectorUserId", upper(coalesce(nullif(trim(ac.turma), ''), 'OUTROS'));

truncate table public.album_rankings;

insert into public.album_rankings (
  id,
  "userId",
  nome,
  foto,
  turma,
  "totalColetado",
  "scansT8",
  "ultimoScan",
  "updatedAt",
  tenant_id
)
with capture_stats as (
  select
    ac.tenant_id,
    ac."collectorUserId" as user_id,
    count(distinct ac."targetUserId")::integer as total_coletado,
    count(*) filter (where upper(coalesce(ac.turma, '')) = 'T8')::integer as scans_t8,
    max(ac."dataColada") as ultimo_scan
  from public.album_captures ac
  group by ac.tenant_id, ac."collectorUserId"
)
select
  'tenant:' || cs.tenant_id::text || '::' || cs.user_id as id,
  cs.user_id,
  coalesce(u.nome, 'Sem nome'),
  coalesce(nullif(trim(u.foto), ''), 'https://github.com/shadcn.png'),
  coalesce(u.turma, ''),
  cs.total_coletado,
  cs.scans_t8,
  cs.ultimo_scan,
  coalesce(cs.ultimo_scan, now()),
  cs.tenant_id
from capture_stats cs
left join public.users u
  on u.uid = cs.user_id
 and u.tenant_id = cs.tenant_id;
