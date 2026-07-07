import { NextRequest, NextResponse } from "next/server";

import { supabaseAdmin } from "@/lib/supabaseAdmin";

export const runtime = "nodejs";

const asObject = (value: unknown): Record<string, unknown> | null =>
  value && typeof value === "object" && !Array.isArray(value) ? (value as Record<string, unknown>) : null;

const asString = (value: unknown): string => (typeof value === "string" ? value.trim() : "");

const asNumber = (value: unknown): number =>
  typeof value === "number" && Number.isFinite(value) ? value : 0;

const productEventId = (row: Record<string, unknown>): string => {
  const data = asObject(row.data) ?? {};
  const eventParty = asObject(data.eventParty) ?? {};
  return asString(eventParty.eventId || row.eventId || row.eventoId);
};

export async function POST(request: NextRequest) {
  try {
    const body = asObject(await request.json().catch(() => null)) ?? {};
    const productId = asString(body.productId);
    const eventId = asString(body.eventId);
    const tenantId = asString(body.tenantId);

    if (!productId || !eventId) {
      return NextResponse.json({ error: "Produto ou evento inválido." }, { status: 400 });
    }

    let query = supabaseAdmin
      .from("produtos")
      .select("id,tenant_id,cliques,data")
      .eq("id", productId);
    if (tenantId) query = query.eq("tenant_id", tenantId);

    const { data, error } = await query.maybeSingle();
    if (error) {
      return NextResponse.json({ error: error.message }, { status: 400 });
    }

    const product = asObject(data);
    if (!product || productEventId(product) !== eventId) {
      return NextResponse.json({ error: "Produto fora deste evento." }, { status: 404 });
    }

    const { error: updateError } = await supabaseAdmin
      .from("produtos")
      .update({
        cliques: asNumber(product.cliques) + 1,
        updatedAt: new Date().toISOString(),
      })
      .eq("id", productId);

    if (updateError) {
      return NextResponse.json({ error: updateError.message }, { status: 400 });
    }

    return NextResponse.json({ ok: true });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : "Erro ao registrar clique.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
