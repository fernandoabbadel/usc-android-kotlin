import { NextRequest, NextResponse } from "next/server";

import { supabaseAdmin } from "@/lib/supabaseAdmin";

const ALLOWED_BOOLEAN_KEYS = [
  "analytics",
  "marketing",
  "profile_public",
  "photo_public",
  "phone_visibility",
  "email_notifications",
  "show_full_name",
  "show_turma",
  "show_plan",
  "show_achievements",
  "show_followers",
  "allow_discovery",
  "show_mini_vendor",
  "show_collectives",
] as const;

const asObject = (value: unknown): Record<string, unknown> | null =>
  typeof value === "object" && value !== null ? (value as Record<string, unknown>) : null;

const asString = (value: unknown): string =>
  typeof value === "string" ? value.trim() : "";

const pickBooleanPreferences = (value: unknown): Record<string, boolean> => {
  const raw = asObject(value);
  if (!raw) return {};

  return ALLOWED_BOOLEAN_KEYS.reduce<Record<string, boolean>>((payload, key) => {
    if (typeof raw[key] === "boolean") {
      payload[key] = raw[key];
    }
    return payload;
  }, {});
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

    const body = asObject(await request.json().catch(() => null));
    const preferences = pickBooleanPreferences(body?.preferences);
    if (!Object.keys(preferences).length) {
      return NextResponse.json({ error: "Nenhuma preferência válida informada." }, { status: 400 });
    }

    const userId = authData.user.id;
    const tenantId = asString(body?.tenantId) || null;
    const now = new Date().toISOString();

    const { error: upsertError } = await supabaseAdmin
      .from("user_privacy_preferences")
      .upsert(
        {
          user_id: userId,
          tenant_id: tenantId,
          ...preferences,
          updated_at: now,
        },
        { onConflict: "user_id" }
      );

    if (upsertError) {
      return NextResponse.json({ error: upsertError.message }, { status: 400 });
    }

    const userPatch: Record<string, unknown> = {};
    if (typeof preferences.phone_visibility === "boolean") {
      userPatch.whatsappPublico = preferences.phone_visibility;
    }
    if (typeof preferences.profile_public === "boolean") {
      userPatch.profile_public = preferences.profile_public;
      userPatch.profile_visibility_updated_at = now;
      if (!preferences.profile_public) {
        userPatch.status = "paused";
      } else {
        userPatch.status = "ativo";
      }
    }
    if (typeof preferences.photo_public === "boolean") {
      userPatch.profile_photo_public = preferences.photo_public;
    }

    if (Object.keys(userPatch).length > 0) {
      const { error: userUpdateError } = await supabaseAdmin
        .from("users")
        .update(userPatch)
        .eq("uid", userId);
      if (userUpdateError) {
        return NextResponse.json({ error: userUpdateError.message }, { status: 400 });
      }
    }

    return NextResponse.json({ ok: true });
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : "Erro inesperado.";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
