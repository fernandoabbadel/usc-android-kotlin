begin;

do $$
declare
  fn record;
  v_def text;
begin
  for fn in
    select p.oid
      from pg_proc p
      join pg_namespace n on n.oid = p.pronamespace
     where n.nspname = 'public'
       and p.proname in ('tenant_submit_onboarding_request', 'tenant_create_with_master')
  loop
    select pg_get_functiondef(fn.oid) into v_def;
    if v_def is null then
      continue;
    end if;

    v_def := replace(v_def, 'updatedAt = now()::text', '"updatedAt" = now()');
    v_def := replace(v_def, '"updatedAt" = now()::text', '"updatedAt" = now()');
    v_def := replace(v_def, 'on conflict (user_id) do update', 'on conflict (tenant_id, user_id) do update');

    execute v_def;
  end loop;
end;
$$;

notify pgrst, 'reload schema';

commit;
