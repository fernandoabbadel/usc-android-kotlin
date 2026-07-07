-- Governança operacional de parceiros, cupons e leituras.
-- Inclui reset por código, aceite de visibilidade de contatos e histórico detalhado de scans.

do $$
begin
  if exists (
    select 1
    from information_schema.tables
    where table_schema = 'public'
      and table_name = 'parceiros'
  ) then
    alter table public.parceiros
      add column if not exists password_reset_code text,
      add column if not exists password_reset_expires_at timestamptz,
      add column if not exists password_reset_requested_at timestamptz,
      add column if not exists contact_visibility_ack jsonb not null default '{}'::jsonb,
      add column if not exists coupons_updated_at timestamptz;

    create index if not exists parceiros_tenant_email_idx
      on public.parceiros (tenant_id, email);

    create index if not exists parceiros_password_reset_idx
      on public.parceiros (tenant_id, email, password_reset_code)
      where password_reset_code is not null;
  end if;

  if exists (
    select 1
    from information_schema.tables
    where table_schema = 'public'
      and table_name = 'scans'
  ) then
    alter table public.scans
      add column if not exists coupon_id text,
      add column if not exists coupon_title text,
      add column if not exists scan_method text not null default 'qr_code',
      add column if not exists approval_mode text not null default 'direct_scan',
      add column if not exists qr_code text,
      add column if not exists coupon_type text,
      add column if not exists coupon_value text,
      add column if not exists coupon_value_numeric numeric(12, 2),
      add column if not exists status text not null default 'approved',
      add column if not exists approved_at timestamptz,
      add column if not exists approved_by_partner_id text,
      add column if not exists user_display_name text;

    update public.scans
      set coupon_title = coalesce(coupon_title, cupom),
          coupon_value = coalesce(coupon_value, "valorEconomizado"),
          user_display_name = coalesce(user_display_name, usuario),
          status = coalesce(nullif(status, ''), 'approved'),
          scan_method = coalesce(nullif(scan_method, ''), 'qr_code'),
          approval_mode = coalesce(nullif(approval_mode, ''), 'direct_scan'),
          approved_at = coalesce(approved_at, now())
      where coupon_title is null
         or coupon_value is null
         or user_display_name is null
         or status is null
         or scan_method is null
         or approval_mode is null
         or approved_at is null;

    create index if not exists scans_tenant_partner_timestamp_idx
      on public.scans (tenant_id, "empresaId", "timestamp" desc);

    create index if not exists scans_tenant_status_timestamp_idx
      on public.scans (tenant_id, status, "timestamp" desc);

    create index if not exists scans_tenant_coupon_idx
      on public.scans (tenant_id, coupon_id);
  end if;
end $$;
