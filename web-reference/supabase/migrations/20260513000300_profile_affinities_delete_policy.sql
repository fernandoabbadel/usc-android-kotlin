alter table public.profile_affinities
  alter column emoji set default '🔥🦈';

update public.profile_affinities
   set emoji = '🔥🦈'
 where emoji is distinct from '🔥🦈';

do $$
begin
  if not exists (
    select 1
      from pg_policies
     where schemaname = 'public'
       and tablename = 'profile_affinities'
       and policyname = 'profile_affinities_delete_own'
  ) then
    create policy profile_affinities_delete_own
      on public.profile_affinities
      for delete
      using (
        from_user_id = auth.uid()::text
        and public.mt_can_access_tenant_row(tenant_id)
      );
  end if;
end $$;

notify pgrst, 'reload schema';
