"use client";

import React, { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useParams, usePathname, useRouter } from "next/navigation";
import { QRCodeSVG } from "qrcode.react";
import {
  ArrowLeft,
  CheckCircle2,
  Copy,
  Globe,
  Instagram,
  Loader2,
  MessageCircle,
  Plus,
  QrCode,
  Save,
  Ticket,
  Trash2,
} from "lucide-react";

import { useToast } from "@/context/ToastContext";
import { useTenantTheme } from "@/context/TenantThemeContext";
import {
  fetchPartnerById,
  updatePartnerProfile,
  type PartnerCoupon,
  type PartnerRecord,
} from "@/lib/partnersService";
import { parseTenantScopedPath, withTenantSlug } from "@/lib/tenantRouting";

type ContactAck = NonNullable<PartnerRecord["contactVisibilityAck"]>;

const emptyCoupon = (): PartnerCoupon => ({
  id: crypto.randomUUID(),
  titulo: "",
  regra: "",
  valor: "",
  tipo: "percentual",
  ativo: true,
});

const normalizeSiteInput = (value: string): string =>
  value.trim().replace(/^https?:\/\//i, "").slice(0, 240);

export default function PartnerEditPage() {
  const params = useParams();
  const router = useRouter();
  const pathname = usePathname() || "/empresa";
  const { tenantId } = useTenantTheme();
  const { addToast } = useToast();
  const partnerId = typeof params.id === "string" ? params.id : "";
  const pathInfo = useMemo(() => parseTenantScopedPath(pathname), [pathname]);
  const companyBasePath = pathInfo.tenantSlug
    ? withTenantSlug(pathInfo.tenantSlug, "/empresa")
    : "/empresa";
  const partnerDashboardPath = `${companyBasePath}/${partnerId}`;
  const publicPartnerPath = pathInfo.tenantSlug
    ? withTenantSlug(pathInfo.tenantSlug, `/parceiros/${partnerId}`)
    : `/parceiros/${partnerId}`;

  const [origin, setOrigin] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [partnerName, setPartnerName] = useState("Parceiro");
  const [whats, setWhats] = useState("");
  const [insta, setInsta] = useState("");
  const [site, setSite] = useState("");
  const [contactAck, setContactAck] = useState<ContactAck>({});
  const [coupons, setCoupons] = useState<PartnerCoupon[]>([]);

  useEffect(() => {
    setOrigin(window.location.origin);
  }, []);

  const loadPartner = useCallback(async () => {
    if (!partnerId) return;
    setLoading(true);
    try {
      const data = await fetchPartnerById(partnerId, {
        forceRefresh: true,
        tenantId: tenantId || undefined,
      });
      if (!data) {
        addToast("Parceiro não encontrado.", "error");
        router.push(companyBasePath);
        return;
      }

      setPartnerName(data.nome);
      setWhats(data.whats || data.telefone || "");
      setInsta(data.insta || "");
      setSite(data.site || "");
      setContactAck(data.contactVisibilityAck || {});
      setCoupons(data.cupons?.length ? data.cupons : [emptyCoupon()]);
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao carregar parceiro.", "error");
    } finally {
      setLoading(false);
    }
  }, [addToast, companyBasePath, partnerId, router, tenantId]);

  useEffect(() => {
    void loadPartner();
  }, [loadPartner]);

  const updateCoupon = (couponId: string, patch: Partial<PartnerCoupon>) => {
    setCoupons((prev) =>
      prev.map((coupon) =>
        coupon.id === couponId ? { ...coupon, ...patch } : coupon
      )
    );
  };

  const removeCoupon = (couponId: string) => {
    setCoupons((prev) => prev.filter((coupon) => coupon.id !== couponId));
  };

  const buildPrintedQrValue = (coupon: PartnerCoupon): string => {
    const url = new URL(`${origin}${publicPartnerPath}`);
    url.searchParams.set("cupom", coupon.id);
    url.searchParams.set("origem", "qr_impresso");
    return url.toString();
  };

  const handleSave = async () => {
    const normalizedCoupons = coupons
      .map((coupon) => ({
        ...coupon,
        titulo: coupon.titulo.trim(),
        regra: coupon.regra.trim(),
        valor: coupon.valor.trim(),
        tipo: coupon.tipo || "percentual",
        codigoQr: coupon.codigoQr || `USC-${partnerId.slice(0, 8)}-${coupon.id.slice(0, 8)}`,
        ativo: coupon.ativo !== false,
      }))
      .filter((coupon) => coupon.titulo && coupon.valor);

    if (whats.trim() && !contactAck.whats) {
      addToast("Confirme o aviso de visibilidade do WhatsApp.", "error");
      return;
    }
    if (insta.trim() && !contactAck.insta) {
      addToast("Confirme o aviso de visibilidade do Instagram.", "error");
      return;
    }
    if (site.trim() && !contactAck.site) {
      addToast("Confirme o aviso de visibilidade do site.", "error");
      return;
    }

    setSaving(true);
    try {
      await updatePartnerProfile({
        partnerId,
        tenantId: tenantId || undefined,
        data: {
          whats: whats.trim(),
          insta: insta.trim().replace("@", ""),
          site: normalizeSiteInput(site),
          cupons: normalizedCoupons,
          contactVisibilityAck: {
            whats: Boolean(contactAck.whats),
            insta: Boolean(contactAck.insta),
            site: Boolean(contactAck.site),
          },
        },
      });
      setCoupons(normalizedCoupons.length ? normalizedCoupons : [emptyCoupon()]);
      addToast("Página pública do parceiro atualizada.", "success");
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao salvar configurações.", "error");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#050505] text-white">
        <Loader2 className="animate-spin text-emerald-500" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#050505] pb-20 text-white">
      <header className="sticky top-0 z-20 border-b border-zinc-800 bg-[#050505]/90 px-6 py-5 backdrop-blur-md">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <Link
              href={partnerDashboardPath}
              className="rounded-full border border-zinc-800 bg-zinc-900 p-2 hover:bg-zinc-800"
            >
              <ArrowLeft size={18} className="text-zinc-300" />
            </Link>
            <div>
              <h1 className="text-xl font-black uppercase tracking-tight">Editar parceiro</h1>
              <p className="text-[11px] font-bold text-zinc-500">
                WhatsApp, Instagram, site, cupons e QR Codes de {partnerName}.
              </p>
            </div>
          </div>
          <button
            onClick={() => void handleSave()}
            disabled={saving}
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-emerald-600 px-4 py-2.5 text-xs font-black uppercase tracking-wide text-white hover:bg-emerald-500 disabled:opacity-60"
          >
            {saving ? <Loader2 size={15} className="animate-spin" /> : <Save size={15} />}
            Salvar
          </button>
        </div>
      </header>

      <main className="mx-auto max-w-6xl space-y-6 px-6 py-6">
        <section className="grid gap-4 md:grid-cols-3">
          {[
            {
              key: "whats" as const,
              label: "WhatsApp",
              icon: MessageCircle,
              value: whats,
              onChange: setWhats,
              placeholder: "55 12 99999-9999",
              warning: "O WhatsApp ficará disponível para usuários logados entrarem em contato com o parceiro.",
            },
            {
              key: "insta" as const,
              label: "Instagram",
              icon: Instagram,
              value: insta,
              onChange: setInsta,
              placeholder: "perfil ou @perfil",
              warning: "O Instagram ficará visível na página pública do parceiro dentro da USC.",
            },
            {
              key: "site" as const,
              label: "Site",
              icon: Globe,
              value: site,
              onChange: setSite,
              placeholder: "www.exemplo.com.br",
              warning: "O site poderá ser usado pelos usuários para acessar informações externas do parceiro.",
            },
          ].map((field) => {
            const Icon = field.icon;
            return (
              <div key={field.key} className="rounded-2xl border border-zinc-800 bg-zinc-900/80 p-4">
                <label className="space-y-2">
                  <span className="flex items-center gap-2 text-[10px] font-black uppercase tracking-widest text-zinc-500">
                    <Icon size={14} className="text-emerald-400" />
                    {field.label}
                  </span>
                  <input
                    value={field.value}
                    onChange={(event) => field.onChange(event.target.value)}
                    placeholder={field.placeholder}
                    className="w-full rounded-xl border border-zinc-700 bg-black/40 px-3 py-2.5 text-sm text-white outline-none focus:border-emerald-500"
                  />
                </label>
                <label className="mt-3 flex gap-2 rounded-xl border border-amber-500/20 bg-amber-500/10 p-3 text-[11px] leading-relaxed text-amber-100">
                  <input
                    type="checkbox"
                    checked={Boolean(contactAck[field.key])}
                    onChange={(event) =>
                      setContactAck((prev) => ({ ...prev, [field.key]: event.target.checked }))
                    }
                    className="mt-0.5"
                  />
                  <span>{field.warning}</span>
                </label>
              </div>
            );
          })}
        </section>

        <section className="rounded-3xl border border-zinc-800 bg-zinc-900/80 p-5">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="flex items-center gap-2 text-lg font-black uppercase tracking-tight">
                <Ticket size={18} className="text-yellow-400" />
                Cupons de desconto
              </h2>
              <p className="text-xs text-zinc-500">
                Crie quantos cupons quiser. Cada cupom pode ser ativado por QR do usuário, aprovação manual ou QR impresso.
              </p>
            </div>
            <button
              onClick={() => setCoupons((prev) => [...prev, emptyCoupon()])}
              className="inline-flex items-center gap-2 rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-2.5 text-xs font-black uppercase text-emerald-300 hover:bg-emerald-500/15"
            >
              <Plus size={14} />
              Novo cupom
            </button>
          </div>

          <div className="space-y-4">
            {coupons.map((coupon, index) => {
              const qrValue = origin ? buildPrintedQrValue(coupon) : publicPartnerPath;
              return (
                <div key={coupon.id} className="grid gap-4 rounded-2xl border border-zinc-800 bg-black/30 p-4 lg:grid-cols-[1fr_220px]">
                  <div className="grid gap-3 md:grid-cols-2">
                    <label className="space-y-1">
                      <span className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Nome do cupom</span>
                      <input
                        value={coupon.titulo}
                        onChange={(event) => updateCoupon(coupon.id, { titulo: event.target.value })}
                        placeholder={`Cupom ${index + 1}`}
                        className="w-full rounded-xl border border-zinc-700 bg-black/40 px-3 py-2.5 text-sm outline-none focus:border-emerald-500"
                      />
                    </label>
                    <label className="space-y-1">
                      <span className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Valor</span>
                      <input
                        value={coupon.valor}
                        onChange={(event) => updateCoupon(coupon.id, { valor: event.target.value })}
                        placeholder={coupon.tipo === "valor" ? "R$ 20" : "15%"}
                        className="w-full rounded-xl border border-zinc-700 bg-black/40 px-3 py-2.5 text-sm outline-none focus:border-emerald-500"
                      />
                    </label>
                    <label className="space-y-1">
                      <span className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Tipo</span>
                      <select
                        value={coupon.tipo || "percentual"}
                        onChange={(event) => updateCoupon(coupon.id, { tipo: event.target.value as PartnerCoupon["tipo"] })}
                        className="w-full rounded-xl border border-zinc-700 bg-black/40 px-3 py-2.5 text-sm outline-none focus:border-emerald-500"
                      >
                        <option value="percentual">% porcentagem de desconto</option>
                        <option value="valor">Valor de desconto</option>
                      </select>
                    </label>
                    <label className="space-y-1">
                      <span className="text-[10px] font-black uppercase tracking-widest text-zinc-500">Regra</span>
                      <input
                        value={coupon.regra}
                        onChange={(event) => updateCoupon(coupon.id, { regra: event.target.value })}
                        placeholder="Ex.: válido para pedidos acima de R$ 50"
                        className="w-full rounded-xl border border-zinc-700 bg-black/40 px-3 py-2.5 text-sm outline-none focus:border-emerald-500"
                      />
                    </label>
                    <label className="flex items-center gap-2 text-xs font-bold text-zinc-300">
                      <input
                        type="checkbox"
                        checked={coupon.ativo !== false}
                        onChange={(event) => updateCoupon(coupon.id, { ativo: event.target.checked })}
                      />
                      Cupom visível para usuários
                    </label>
                    <button
                      type="button"
                      onClick={() => removeCoupon(coupon.id)}
                      className="inline-flex items-center justify-center gap-2 rounded-xl border border-red-500/30 bg-red-500/10 px-3 py-2 text-xs font-black uppercase text-red-300 hover:bg-red-500/15 md:justify-self-start"
                    >
                      <Trash2 size={14} />
                      Remover
                    </button>
                  </div>

                  <div className="rounded-2xl border border-zinc-800 bg-zinc-950 p-4 text-center">
                    <div className="mx-auto mb-3 inline-block rounded-xl bg-white p-3">
                      <QRCodeSVG value={qrValue} size={150} includeMargin />
                    </div>
                    <p className="flex items-center justify-center gap-1 text-[10px] font-black uppercase tracking-widest text-zinc-500">
                      <QrCode size={12} />
                      QR impresso
                    </p>
                    <button
                      type="button"
                      onClick={() => {
                        void navigator.clipboard?.writeText(qrValue);
                        addToast("Link do QR copiado.", "success");
                      }}
                      className="mt-3 inline-flex items-center justify-center gap-2 rounded-xl border border-zinc-700 bg-zinc-900 px-3 py-2 text-[10px] font-black uppercase text-zinc-300 hover:bg-zinc-800"
                    >
                      <Copy size={12} />
                      Copiar link
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </section>

        <section className="rounded-2xl border border-emerald-500/20 bg-emerald-500/10 p-4 text-xs leading-relaxed text-emerald-50">
          <p className="flex items-start gap-2">
            <CheckCircle2 size={16} className="mt-0.5 shrink-0 text-emerald-300" />
            O QR exibido pelo usuário é validado no painel do parceiro. A ativação manual fica pendente até aprovação. O QR impresso abre a página pública do cupom para o usuário logado.
          </p>
        </section>
      </main>
    </div>
  );
}
