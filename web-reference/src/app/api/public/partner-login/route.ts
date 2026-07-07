import { NextResponse } from "next/server";

import { supabaseAdmin } from "@/lib/supabaseAdmin";

export const dynamic = "force-dynamic";

const PARTNER_LOGIN_SELECT =
  "id,nome,email,senha,status,tenant_id,password_reset_code,password_reset_expires_at";

const asObject = (value: unknown): Record<string, unknown> | null =>
  typeof value === "object" && value !== null ? (value as Record<string, unknown>) : null;

const asString = (value: unknown, fallback = ""): string =>
  typeof value === "string" ? value.trim() : fallback;

const normalizeStatus = (value: unknown): "active" | "pending" | "disabled" => {
  const cleanValue = asString(value).toLowerCase();
  if (cleanValue === "pending" || cleanValue === "disabled") return cleanValue;
  return "active";
};

const toMillis = (value: unknown): number => {
  if (value instanceof Date) return value.getTime();
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value.trim()) {
    const parsed = Date.parse(value);
    return Number.isFinite(parsed) ? parsed : 0;
  }
  return 0;
};

export async function POST(request: Request) {
  try {
    const body = asObject(await request.json().catch(() => null));
    const email = asString(body?.email).toLowerCase();
    const senha = asString(body?.senha);

    if (!email || !senha) {
      return NextResponse.json({ result: null });
    }

    const { data: partnerRows, error: partnerError } = await supabaseAdmin
      .from("parceiros")
      .select(PARTNER_LOGIN_SELECT)
      .eq("email", email)
      .limit(20);

    if (partnerError) {
      return NextResponse.json({ error: partnerError.message }, { status: 400 });
    }

    const partners = (Array.isArray(partnerRows) ? partnerRows : [])
      .map((row) => asObject(row))
      .filter((row): row is Record<string, unknown> => Boolean(row));

    if (!partners.length) {
      return NextResponse.json({ result: null });
    }

    const matchingPartner = partners.find((row) => asString(row.senha) === senha);
    if (!matchingPartner) {
      const firstPartner = partners[0];
      return NextResponse.json({
        result: {
          id: asString(firstPartner.id),
          nome: asString(firstPartner.nome, "Parceiro"),
          status: normalizeStatus(firstPartner.status),
          passwordValid: false,
          hasPasswordResetCode:
            Boolean(asString(firstPartner.password_reset_code)) &&
            toMillis(firstPartner.password_reset_expires_at) > Date.now(),
          passwordResetExpiresAt: firstPartner.password_reset_expires_at ?? null,
        },
      });
    }

    const tenantId = asString(matchingPartner.tenant_id);
    if (!tenantId) {
      return NextResponse.json({ result: null });
    }

    const { data: tenantRow, error: tenantError } = await supabaseAdmin
      .from("tenants")
      .select("id,slug,status")
      .eq("id", tenantId)
      .maybeSingle();

    if (tenantError) {
      return NextResponse.json({ error: tenantError.message }, { status: 400 });
    }

    const tenant = asObject(tenantRow);
    const tenantSlug = asString(tenant?.slug).toLowerCase();
    const tenantStatus = asString(tenant?.status, "active").toLowerCase();
    if (!tenantSlug || tenantStatus !== "active") {
      return NextResponse.json({ result: null });
    }

    return NextResponse.json({
      result: {
        id: asString(matchingPartner.id),
        nome: asString(matchingPartner.nome, "Parceiro"),
        status: normalizeStatus(matchingPartner.status),
        passwordValid: true,
        tenantId,
        tenantSlug,
      },
    });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : "Erro inesperado.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
