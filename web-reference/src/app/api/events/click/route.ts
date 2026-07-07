import { NextRequest, NextResponse } from "next/server";

import { supabaseAdmin } from "@/lib/supabaseAdmin";

export const runtime = "nodejs";

type Row = Record<string, unknown>;

const asObject = (value: unknown): Row =>
  value && typeof value === "object" && !Array.isArray(value) ? (value as Row) : {};

const asString = (value: unknown): string => (typeof value === "string" ? value.trim() : "");

const asNumber = (value: unknown): number => {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string") {
    const parsed = Number(value.replace(",", "."));
    return Number.isFinite(parsed) ? parsed : 0;
  }
  return 0;
};

const incrementKeys = (stats: Row, keys: string[]): Row => {
  const next = { ...stats };
  keys.forEach((key) => {
    next[key] = asNumber(next[key]) + 1;
  });
  return next;
};

export async function POST(request: NextRequest) {
  try {
    const body = asObject(await request.json().catch(() => null));
    const eventId = asString(body.eventId);
    const tenantId = asString(body.tenantId);
    const kind = asString(body.kind).toLowerCase();

    if (!eventId || !["card", "buy"].includes(kind)) {
      return NextResponse.json({ error: "Evento ou métrica inválida." }, { status: 400 });
    }

    let query = supabaseAdmin.from("eventos").select("id,tenant_id,stats").eq("id", eventId);
    if (tenantId) query = query.eq("tenant_id", tenantId);

    const { data, error } = await query.maybeSingle();
    if (error) {
      return NextResponse.json({ error: error.message }, { status: 400 });
    }
    const event = asObject(data);
    if (!asString(event.id)) {
      return NextResponse.json({ error: "Evento não encontrado." }, { status: 404 });
    }

    const currentStats = asObject(event.stats);
    const nextStats =
      kind === "card"
        ? incrementKeys(currentStats, ["cardClicks", "eventCardClicks", "cliquesCard"])
        : incrementKeys(currentStats, ["cliquesCompra", "buyClicks", "checkoutClicks"]);

    let updateQuery = supabaseAdmin
      .from("eventos")
      .update({ stats: nextStats, updatedAt: new Date().toISOString() })
      .eq("id", eventId);
    if (tenantId) updateQuery = updateQuery.eq("tenant_id", tenantId);
    const { error: updateError } = await updateQuery;

    if (updateError) {
      return NextResponse.json({ error: updateError.message }, { status: 400 });
    }

    return NextResponse.json({ ok: true });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : "Erro ao registrar clique.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
