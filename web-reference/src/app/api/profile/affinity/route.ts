import { NextResponse } from "next/server";

import { supabaseAdmin } from "@/lib/supabaseAdmin";

export const dynamic = "force-dynamic";

type UserRow = {
  uid?: string | null;
  nome?: string | null;
  foto?: string | null;
  turma?: string | null;
  tenant_id?: string | null;
};

const asObject = (value: unknown): Record<string, unknown> | null =>
  typeof value === "object" && value !== null ? (value as Record<string, unknown>) : null;

const asString = (value: unknown, fallback = ""): string =>
  typeof value === "string" ? value : fallback;

const cleanText = (value: unknown, fallback = ""): string =>
  asString(value, fallback).trim();

const getBearerToken = (request: Request): string => {
  const authorization = request.headers.get("authorization") || "";
  const match = authorization.match(/^Bearer\s+(.+)$/i);
  return match?.[1]?.trim() || "";
};

const jsonError = (message: string, status: number) =>
  NextResponse.json(
    { error: message },
    {
      status,
      headers: { "Cache-Control": "no-store, max-age=0" },
    }
  );

const isMissingSchemaColumn = (error: unknown, column: string): boolean => {
  const raw = asObject(error);
  const message = [raw?.message, raw?.details, raw?.hint]
    .map((entry) => (typeof entry === "string" ? entry : ""))
    .join(" ")
    .toLowerCase();
  return message.includes(column.toLowerCase()) && message.includes("column");
};

const userSummary = (row: UserRow, fallback: Record<string, unknown>) => ({
  uid: cleanText(row.uid, cleanText(fallback.uid)),
  nome: cleanText(row.nome, cleanText(fallback.nome, "Atleta")).slice(0, 120) || "Atleta",
  foto: cleanText(row.foto, cleanText(fallback.foto)),
  turma: cleanText(row.turma, cleanText(fallback.turma, "Geral")).slice(0, 40) || "Geral",
});

const insertNotification = async (payload: Record<string, unknown>) => {
  const nextPayload = { ...payload };

  for (let attempt = 0; attempt < 3; attempt += 1) {
    const { error } = await supabaseAdmin.from("notifications").insert(nextPayload);
    if (!error) return;

    if ("tenant_id" in nextPayload && isMissingSchemaColumn(error, "tenant_id")) {
      delete nextPayload.tenant_id;
      continue;
    }

    if ("expiresAt" in nextPayload && isMissingSchemaColumn(error, "expiresAt")) {
      delete nextPayload.expiresAt;
      continue;
    }

    throw error;
  }
};

export async function POST(request: Request) {
  const accessToken = getBearerToken(request);
  if (!accessToken) {
    return jsonError("Sessão ausente para atualizar Crush.", 401);
  }

  const body = asObject(await request.json().catch(() => null));
  if (!body) {
    return jsonError("Payload inválido.", 400);
  }

  const action = cleanText(body.action);
  const viewerUid = cleanText(body.viewerUid);
  const targetUid = cleanText(body.targetUid);
  const tenantIdFromClient = cleanText(body.tenantId);
  const viewerData = asObject(body.viewerData) ?? {};
  const targetData = asObject(body.targetData) ?? {};

  if (!["send", "remove"].includes(action)) {
    return jsonError("Ação de Crush inválida.", 400);
  }

  if (!viewerUid || !targetUid || viewerUid === targetUid) {
    return jsonError("Relação de Crush inválida.", 400);
  }

  try {
    const { data: authData, error: authError } = await supabaseAdmin.auth.getUser(accessToken);
    const authUid = cleanText(authData.user?.id);
    if (authError || !authUid) {
      return jsonError("Sessão inválida para atualizar Crush.", 401);
    }

    if (authUid !== viewerUid) {
      return jsonError("Sem permissão para atualizar Crush por outro usuário.", 403);
    }

    const [viewerUserRes, targetUserRes] = await Promise.all([
      supabaseAdmin
        .from("users")
        .select("uid,nome,foto,turma,tenant_id")
        .eq("uid", viewerUid)
        .maybeSingle(),
      supabaseAdmin
        .from("users")
        .select("uid,nome,foto,turma,tenant_id")
        .eq("uid", targetUid)
        .maybeSingle(),
    ]);

    if (viewerUserRes.error) throw viewerUserRes.error;
    if (targetUserRes.error) throw targetUserRes.error;
    if (!viewerUserRes.data || !targetUserRes.data) {
      return jsonError("Usuário não encontrado para Crush.", 404);
    }

    const viewerRow = viewerUserRes.data as UserRow;
    const targetRow = targetUserRes.data as UserRow;
    const effectiveTenantId = tenantIdFromClient || cleanText(viewerRow.tenant_id);
    if (!effectiveTenantId) {
      return jsonError("Tenant do Crush não resolvido.", 400);
    }
    if (
      effectiveTenantId &&
      (cleanText(viewerRow.tenant_id) !== effectiveTenantId ||
        cleanText(targetRow.tenant_id) !== effectiveTenantId)
    ) {
      return jsonError("Não é permitido enviar Crush para usuário de outra atlética.", 403);
    }

    const viewer = userSummary(viewerRow, viewerData);
    const target = userSummary(targetRow, targetData);

    if (action === "remove") {
      const { error } = await supabaseAdmin
        .from("profile_affinities")
        .delete()
        .eq("tenant_id", effectiveTenantId)
        .eq("from_user_id", viewerUid)
        .eq("to_user_id", targetUid);
      if (error) throw error;

      return NextResponse.json(
        { removed: true, sent: false, mutual: false },
        { headers: { "Cache-Control": "no-store, max-age=0" } }
      );
    }

    const now = new Date();
    const nowIso = now.toISOString();
    const expiresAt = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000).toISOString();
    const { error: upsertError } = await supabaseAdmin
      .from("profile_affinities")
      .upsert(
        {
          tenant_id: effectiveTenantId,
          from_user_id: viewerUid,
          to_user_id: targetUid,
          from_nome: viewer.nome,
          from_foto: viewer.foto,
          from_turma: viewer.turma,
          to_nome: target.nome,
          to_foto: target.foto,
          to_turma: target.turma,
          emoji: "🔥",
          updated_at: nowIso,
        },
        { onConflict: "tenant_id,from_user_id,to_user_id" }
      );
    if (upsertError) throw upsertError;

    const reverse = await supabaseAdmin
      .from("profile_affinities")
      .select("id")
      .eq("tenant_id", effectiveTenantId)
      .eq("from_user_id", targetUid)
      .eq("to_user_id", viewerUid)
      .limit(1)
      .maybeSingle();
    if (reverse.error) throw reverse.error;

    const mutual = Boolean(reverse.data);
    await insertNotification({
      userId: targetUid,
      title: mutual ? "Crush confirmado" : "Novo Crush",
      message: mutual
        ? `${viewer.nome} também enviou Crush para você.`
        : `${viewer.nome} te enviou um Crush.`,
      link: `/perfil/${viewerUid}`,
      read: false,
      type: "social",
      createdAt: nowIso,
      updatedAt: nowIso,
      expiresAt,
      ...(effectiveTenantId ? { tenant_id: effectiveTenantId } : {}),
    });

    if (mutual) {
      await insertNotification({
        userId: viewerUid,
        title: "Crush confirmado",
        message: `${target.nome} também tinha enviado Crush para você.`,
        link: `/perfil/${targetUid}`,
        read: false,
        type: "social",
        createdAt: nowIso,
        updatedAt: nowIso,
        expiresAt,
        ...(effectiveTenantId ? { tenant_id: effectiveTenantId } : {}),
      });
    }

    return NextResponse.json(
      { sent: true, mutual },
      { headers: { "Cache-Control": "no-store, max-age=0" } }
    );
  } catch (error) {
    console.error("Erro no Crush server-side:", error);
    return jsonError("Falha ao atualizar Crush.", 500);
  }
}
