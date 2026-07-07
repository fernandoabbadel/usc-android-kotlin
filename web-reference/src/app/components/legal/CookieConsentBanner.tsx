"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

const COOKIE_CONSENT_KEY = "usc:cookie-consent:v1";

type CookieConsentChoice = {
  essential: true;
  analytics: boolean;
  marketing: boolean;
  savedAt: string;
};

const saveChoice = (analytics: boolean, marketing: boolean) => {
  const choice: CookieConsentChoice = {
    essential: true,
    analytics,
    marketing,
    savedAt: new Date().toISOString(),
  };
  window.localStorage.setItem(COOKIE_CONSENT_KEY, JSON.stringify(choice));
};

export default function CookieConsentBanner() {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    try {
      setVisible(!window.localStorage.getItem(COOKIE_CONSENT_KEY));
    } catch {
      setVisible(false);
    }
  }, []);

  if (!visible) return null;

  return (
    <div className="fixed inset-x-0 bottom-4 z-[90] px-4">
      <div className="mx-auto flex max-w-5xl flex-col gap-4 rounded-lg border border-white/10 bg-zinc-950/95 p-4 shadow-2xl backdrop-blur-xl sm:flex-row sm:items-center sm:justify-between">
        <div className="max-w-3xl">
          <p className="text-sm font-black uppercase text-white">Preferências de cookies</p>
          <p className="mt-1 text-xs leading-6 text-zinc-400">
            A USC usa cookies essenciais para login, segurança e funcionamento. Recursos
            analíticos ou de marketing só serão usados mediante consentimento quando exigido.
            Veja a{" "}
            <Link href="/politica-cookies" className="font-bold text-blue-200 hover:underline">
              Política de Cookies
            </Link>
            .
          </p>
        </div>
        <div className="flex shrink-0 flex-col gap-2 sm:flex-row">
          <button
            type="button"
            onClick={() => {
              saveChoice(false, false);
              setVisible(false);
            }}
            className="rounded-lg border border-white/10 bg-white/[0.04] px-4 py-3 text-xs font-black uppercase text-zinc-200 transition hover:bg-white/[0.08]"
          >
            Recusar não essenciais
          </button>
          <button
            type="button"
            onClick={() => {
              saveChoice(true, false);
              setVisible(false);
            }}
            className="rounded-lg bg-white px-4 py-3 text-xs font-black uppercase text-zinc-950 transition hover:bg-zinc-200"
          >
            Aceitar analytics
          </button>
        </div>
      </div>
    </div>
  );
}
