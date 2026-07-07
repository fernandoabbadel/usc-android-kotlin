import { NextRequest, NextResponse } from "next/server";

import { supabaseAdmin } from "@/lib/supabaseAdmin";

const REQUEST_TYPES = new Set([
  "confirmacao_tratamento",
  "acesso",
  "correcao",
  "anonimizacao_bloqueio_eliminacao",
  "portabilidade",
  "eliminacao_consentimento",
  "compartilhamento",
  "informacao_consentimento",
  "revogacao_consentimento",
  "revisao_decisao_automatizada",
  "oposicao",
  "outro",
]);

const asObject = (value: unknown): Record<string, unknown> | null =>
  typeof value === "object" && value !== null ? (value as Record<string, unknown>) : null;

const asString = (value: unknown): string =>
  typeof value === "string" ? value.trim() : "";

const getClientIp = (request: NextRequest): string => {
  const forwardedFor = request.headers.get("x-forwarded-for") || "";
  return (
    forwardedFor
      .split(",")
      .map((part) => part.trim())
      .find(Boolean) ||
    request.headers.get("x-real-ip") ||
    ""
  );
};

export async function POST(request: NextRequest) {
  try {
    const body = asObject(await request.json().catch(() => null));
    const requesterName = asString(body?.requesterName);
    const requesterEmail = asString(body?.requesterEmail).toLowerCase();
    const requestType = asString(body?.requestType);
    const requestDetails = asString(body?.requestDetails);
    const tenantId = asString(body?.tenantId) || null;

    if (!requesterName || !requesterEmail || !requestType || !requestDetails) {
      return NextResponse.json({ error: "Preencha nome, e-mail, tipo e detalhes da solicitação." }, { status: 400 });
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(requesterEmail)) {
      return NextResponse.json({ error: "Informe um e-mail válido." }, { status: 400 });
    }
    if (!REQUEST_TYPES.has(requestType)) {
      return NextResponse.json({ error: "Tipo de solicitação inválido." }, { status: 400 });
    }

    let userId: string | null = null;
    const authHeader = request.headers.get("authorization") || "";
    const accessToken = authHeader.replace(/^Bearer\s+/i, "").trim();
    if (accessToken) {
      const { data } = await supabaseAdmin.auth.getUser(accessToken);
      userId = data.user?.id || null;
    }

    const { error } = await supabaseAdmin.from("lgpd_requests").insert({
      user_id: userId,
      tenant_id: tenantId,
      requester_name: requesterName.slice(0, 180),
      requester_email: requesterEmail.slice(0, 180),
      request_type: requestType,
      request_details: requestDetails.slice(0, 4000),
      ip_address: getClientIp(request),
      user_agent: request.headers.get("user-agent") || "",
    });

    if (error) {
      return NextResponse.json({ error: error.message }, { status: 400 });
    }

    return NextResponse.json({ ok: true });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : "Erro inesperado.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
