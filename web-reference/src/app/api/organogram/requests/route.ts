import { randomUUID } from "crypto";
import { NextRequest, NextResponse } from "next/server";

import { supabaseAdmin } from "@/lib/supabaseAdmin";
import { buildTenantScopedRowId } from "@/lib/tenantScopedCatalog";

type Row = Record<string, unknown>;

const ORGANOGRAM_DOC_ID = "organograma";

const asObject = (value: unknown): Row | null =>
  typeof value === "object" && value !== null ? (value as Row) : null;

const asString = (value: unknown): string =>
  typeof value === "string" ? value.trim() : "";

const asArray = (value: unknown): unknown[] => (Array.isArray(value) ? value : []);

const normalizeSectionName = (value: unknown): string =>
  asString(value).replace(/\s+/g, " ").slice(0, 60) || "Diretoria";

const normalizeCargo = (value: unknown): string =>
  asString(value).replace(/\s+/g, " ").slice(0, 80) || "Membro";

const getOrganogramDocId = (tenantId?: string | null): string =>
  buildTenantScopedRowId(asString(tenantId), ORGANOGRAM_DOC_ID) || ORGANOGRAM_DOC_ID;

const getMissingColumn = (error: unknown): string => {
  const raw = asObject(error);
  const text = [asString(raw?.message), asString(raw?.details)].filter(Boolean).join(" | ");
  const match =
    text.match(/column\s+[a-z0-9_]+\.(\w+)\s+does not exist/i) ||
    text.match(/column\s+(\w+)\s+does not exist/i) ||
    text.match(/could not find the ['"]?(\w+)['"]? column/i);
  return match?.[1] || "";
};

export async function POST(request: NextRequest) {
  try {
    const authHeader = request.headers.get("authorization") || "";
    const accessToken = authHeader.replace(/^Bearer\s+/i, "").trim();
    if (!accessToken) {
      return NextResponse.json({ error: "Não autenticado." }, { status: 401 });
    }

    const { data: authData, error: authError } = await supabaseAdmin.auth.getUser(accessToken);
    if (authError || !authData.user) {
      return NextResponse.json({ error: "Sessão inválida." }, { status: 401 });
    }

    const body = asObject(await request.json().catch(() => null)) || {};
    const tenantId = asString(body.tenantId) || null;
    const cargo = normalizeCargo(body.cargo);
    const secao = normalizeSectionName(body.secao);
    const userId = authData.user.id;
    const docId = getOrganogramDocId(tenantId);

    const { data: userRow, error: userError } = await supabaseAdmin
      .from("users")
      .select("uid,nome,foto")
      .eq("uid", userId)
      .maybeSingle();
    if (userError) {
      return NextResponse.json({ error: userError.message }, { status: 400 });
    }

    const { data: configRow, error: configError } = await supabaseAdmin
      .from("app_config")
      .select("id,data")
      .eq("id", docId)
      .maybeSingle();
    if (configError) {
      return NextResponse.json({ error: configError.message }, { status: 400 });
    }

    const currentData = asObject(asObject(configRow)?.data) || {};
    const currentMembers = asArray(currentData.membros)
      .map((entry) => asObject(entry))
      .filter((entry): entry is Row => entry !== null);

    const existingMember = currentMembers.find(
      (member) =>
        asString(member.userId) === userId &&
        asString(member.status).toLowerCase() !== "rejected"
    );
    if (existingMember) {
      return NextResponse.json({
        member: existingMember,
        alreadyExists: true,
      });
    }

    const sectionMembersCount = currentMembers.filter(
      (member) => normalizeSectionName(member.secao) === secao
    ).length;
    const now = new Date().toISOString();
    const userData = asObject(userRow) || {};
    const member = {
      id: randomUUID(),
      cargo,
      secao,
      ordem: sectionMembersCount,
      userId,
      nome: asString(userData.nome) || asString(authData.user.email) || "Usuário",
      foto: asString(userData.foto),
      status: "pending",
      requestedAt: now,
      requestedBy: userId,
      updatedAt: now,
    };

    const currentSections = asArray(currentData.ordemSecoes)
      .map((entry) => asString(entry))
      .filter(Boolean);
    const nextSections = currentSections.includes(secao)
      ? currentSections
      : [...currentSections, secao];
    const nextData = {
      ...currentData,
      membros: [...currentMembers, member],
      ordemSecoes: nextSections,
    };

    const payload: Row = {
      id: docId,
      data: nextData,
      updatedAt: now,
      ...(tenantId ? { tenant_id: tenantId } : {}),
    };

    while (true) {
      const { error: upsertError } = await supabaseAdmin
        .from("app_config")
        .upsert(payload, { onConflict: "id" });
      if (!upsertError) {
        return NextResponse.json({ member });
      }

      const missingColumn = getMissingColumn(upsertError);
      const removableKey = Object.keys(payload).find(
        (key) => key.toLowerCase() === missingColumn.toLowerCase()
      );
      if (!removableKey || removableKey === "id") {
        return NextResponse.json({ error: upsertError.message }, { status: 400 });
      }
      delete payload[removableKey];
    }
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : "Erro inesperado.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
