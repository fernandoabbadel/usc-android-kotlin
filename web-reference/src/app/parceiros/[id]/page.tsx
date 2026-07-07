"use client";

import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  ArrowLeft,
  MapPin,
  Clock,
  Globe,
  QrCode,
  CheckCircle,
  Ticket,
  Instagram,
  MessageCircle,
  Loader2,
  Store,
} from "lucide-react";
import Link from "next/link";
import Image from "next/image";
import { useParams, useSearchParams } from "next/navigation";
import { QRCodeSVG } from "qrcode.react";

import { useToast } from "../../../context/ToastContext";
import { useTenantTheme } from "../../../context/TenantThemeContext";
import { useAuth } from "../../../context/AuthContext";
import { createPartnerScan, fetchPartnerById } from "../../../lib/partnersService";
import { withTenantSlug } from "../../../lib/tenantRouting";

interface Cupom {
  id: string;
  titulo: string;
  regra: string;
  valor: string;
  imagem?: string;
  tipo?: "percentual" | "valor";
  ativo?: boolean;
  codigoQr?: string;
}

interface Parceiro {
  id: string;
  nome: string;
  categoria: string;
  imgCapa?: string;
  imgLogo?: string;
  insta?: string;
  site?: string;
  telefone?: string;
  whats?: string;
  descricao?: string;
  endereco?: string;
  horario?: string;
  cupons?: Cupom[];
}

export default function ParceiroDetalhePage() {
  const { addToast } = useToast();
  const { tenantId: activeTenantId, tenantSlug: activeTenantSlug } = useTenantTheme();
  const { user } = useAuth();
  const params = useParams();
  const searchParams = useSearchParams();
  const parceiroId = typeof params.id === "string" ? params.id : "";

  const [parceiro, setParceiro] = useState<Parceiro | null>(null);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [activeCupom, setActiveCupom] = useState<Cupom | null>(null);
  const [countdown, setCountdown] = useState(300);
  const [activatingManual, setActivatingManual] = useState(false);

  const parceirosHref = useMemo(
    () =>
      activeTenantSlug.trim()
        ? withTenantSlug(activeTenantSlug, "/parceiros")
        : "/parceiros",
    [activeTenantSlug]
  );

  useEffect(() => {
    let active = true;

    const fetchParceiro = async () => {
      if (!parceiroId) {
        if (active) setLoading(false);
        return;
      }

      try {
        const foundPartner = await fetchPartnerById(parceiroId, {
          forceRefresh: false,
          tenantId: activeTenantId || undefined,
        });

        if (!active) return;

        if (foundPartner) {
          setParceiro(foundPartner as Parceiro);
        } else {
          addToast("Parceiro não encontrado.", "error");
        }
      } catch (error: unknown) {
        console.error(error);
      } finally {
        if (active) setLoading(false);
      }
    };

    void fetchParceiro();
    return () => {
      active = false;
    };
  }, [activeTenantId, addToast, parceiroId]);

  useEffect(() => {
    if (!isModalOpen || countdown <= 0) return;
    const timer = setInterval(() => setCountdown((prev) => prev - 1), 1000);
    return () => clearInterval(timer);
  }, [countdown, isModalOpen]);

  const handleOpenCupom = useCallback((cupom: Cupom) => {
    setActiveCupom(cupom);
    setIsModalOpen(true);
    setCountdown(300);
  }, []);

  useEffect(() => {
    if (!parceiro?.cupons?.length) return;
    const couponId = searchParams.get("cupom");
    if (!couponId) return;
    const targetCoupon = parceiro.cupons.find((cupom) => cupom.id === couponId);
    if (targetCoupon) {
      handleOpenCupom(targetCoupon);
    }
  }, [handleOpenCupom, parceiro, searchParams]);

  const buildCouponQrPayload = (cupom: Cupom) =>
    JSON.stringify({
      type: "usc_partner_coupon",
      version: 1,
      partnerId: parceiro?.id,
      partnerName: parceiro?.nome,
      couponId: cupom.id,
      cupom: cupom.titulo,
      couponValue: cupom.valor,
      couponType: cupom.tipo || (cupom.valor.includes("%") ? "percentual" : "valor"),
      approvalMode: searchParams.get("origem") === "qr_impresso" ? "printed_qr" : "direct_scan",
      qrCode: cupom.codigoQr || `USC-${parceiro?.id.slice(0, 8)}-${cupom.id.slice(0, 8)}`,
      userId: user?.uid || "",
      usuario: user?.nome || user?.email || "Usuário USC",
    });

  const handleManualActivation = async () => {
    if (!parceiro || !activeCupom) return;
    if (!user || user.isAnonymous) {
      addToast("Entre na USC para ativar o cupom manualmente.", "error");
      return;
    }

    setActivatingManual(true);
    try {
      const now = new Date();
      await createPartnerScan({
        partnerId: parceiro.id,
        partnerName: parceiro.nome,
        usuario: user.nome || user.email || "Usuário USC",
        userId: user.uid,
        cupom: activeCupom.titulo,
        valorEconomizado: activeCupom.valor,
        data: now.toLocaleDateString("pt-BR"),
        hora: now.toLocaleTimeString("pt-BR"),
        couponId: activeCupom.id,
        couponTitle: activeCupom.titulo,
        scanMethod: "manual",
        approvalMode: "manual_partner",
        qrCode: activeCupom.codigoQr || `USC-${parceiro.id.slice(0, 8)}-${activeCupom.id.slice(0, 8)}`,
        couponType: activeCupom.tipo || (activeCupom.valor.includes("%") ? "percentual" : "valor"),
        couponValue: activeCupom.valor,
        status: "pending",
        tenantId: activeTenantId || undefined,
      });
      addToast("Ativação enviada. Aguarde aprovação do parceiro.", "success");
    } catch (error: unknown) {
      console.error(error);
      addToast("Erro ao solicitar ativação manual.", "error");
    } finally {
      setActivatingManual(false);
    }
  };

  const formatTime = (seconds: number) =>
    `${Math.floor(seconds / 60)}:${seconds % 60 < 10 ? "0" : ""}${seconds % 60}`;

  if (loading) {
    return (
      <div className="min-h-screen bg-[#050505] flex items-center justify-center">
        <Loader2 className="animate-spin text-emerald-500" />
      </div>
    );
  }

  if (!parceiro) {
    return <div className="text-white text-center py-20">Empresa não encontrada.</div>;
  }

  return (
    <div className="min-h-screen bg-[#050505] text-white font-sans selection:bg-emerald-500 pb-10">
      <div className="relative h-[35vh] w-full bg-zinc-900 overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-b from-black/60 via-transparent to-[#050505] z-10" />
        {parceiro.imgCapa ? (
          <Image
            src={parceiro.imgCapa}
            alt={`Capa ${parceiro.nome}`}
            fill
            className="object-cover"
            priority
          />
        ) : (
          <div className="flex items-center justify-center h-full text-zinc-700">
            <Store size={64} />
          </div>
        )}
        <Link
          href={parceirosHref}
          className="absolute top-6 left-6 z-20 bg-black/40 backdrop-blur-md p-3 rounded-full hover:bg-black transition border border-white/10 text-white"
        >
          <ArrowLeft size={20} />
        </Link>
      </div>

      <div className="relative z-20 -mt-10 px-4 md:px-6">
        <div className="bg-zinc-900 border border-zinc-800 rounded-[2rem] p-6 shadow-2xl">
          <div className="flex items-center gap-4 mb-6">
            <div className="w-20 h-20 rounded-2xl bg-black border-4 border-[#050505] -mt-16 flex items-center justify-center overflow-hidden shadow-lg shrink-0 relative">
              {parceiro.imgLogo ? (
                <Image
                  src={parceiro.imgLogo}
                  alt={`Logo ${parceiro.nome}`}
                  width={80}
                  height={80}
                  className="object-cover w-full h-full"
                />
              ) : (
                <Store size={32} className="text-zinc-500" />
              )}
            </div>
            <div className="pt-2">
              <h1 className="text-2xl font-black text-white uppercase leading-none">
                {parceiro.nome}
              </h1>
              <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-widest">
                {parceiro.categoria}
              </span>
            </div>
          </div>

          <div className="flex gap-2 mb-6">
            {parceiro.insta ? (
              <a
                href={`https://instagram.com/${parceiro.insta.replace("@", "")}`}
                target="_blank"
                rel="noopener noreferrer"
                className="flex-1 bg-zinc-800 p-2.5 rounded-xl text-zinc-400 hover:text-white flex justify-center"
              >
                <Instagram size={20} />
              </a>
            ) : null}
            {parceiro.site ? (
              <a
                href={`https://${parceiro.site}`}
                target="_blank"
                rel="noopener noreferrer"
                className="flex-1 bg-zinc-800 p-2.5 rounded-xl text-zinc-400 hover:text-white flex justify-center"
              >
                <Globe size={20} />
              </a>
            ) : null}
            {(parceiro.whats || parceiro.telefone) ? (
              <a
                href={`https://wa.me/${(parceiro.whats || parceiro.telefone || "").replace(/\D/g, "")}`}
                target="_blank"
                rel="noopener noreferrer"
                className="flex-1 bg-zinc-800 p-2.5 rounded-xl text-zinc-400 hover:text-white flex justify-center"
              >
                <MessageCircle size={20} />
              </a>
            ) : null}
          </div>

          <p className="text-sm text-zinc-400 leading-relaxed mb-6">{parceiro.descricao}</p>

          <div className="space-y-2 text-xs text-zinc-300 mb-8">
            <div className="flex items-center gap-3">
              <MapPin size={16} className="text-emerald-500" />
              <span>{parceiro.endereco}</span>
            </div>
            <div className="flex items-center gap-3">
              <Clock size={16} className="text-emerald-500" />
              <span>{parceiro.horario}</span>
            </div>
          </div>

          <h3 className="text-sm font-bold text-white uppercase mb-3 flex items-center gap-2">
            <Ticket size={16} className="text-yellow-500" /> Cupons Disponíveis
          </h3>
          <div className="space-y-3">
            {parceiro.cupons && parceiro.cupons.filter((cupom) => cupom.ativo !== false).length > 0 ? (
              parceiro.cupons.filter((cupom) => cupom.ativo !== false).map((cupom) => (
                <div
                  key={cupom.id}
                  onClick={() => handleOpenCupom(cupom)}
                  className="bg-gradient-to-r from-zinc-800 to-zinc-900 border border-zinc-700 rounded-2xl p-4 flex justify-between items-center cursor-pointer hover:border-emerald-500 transition group relative overflow-hidden"
                >
                  <div className="absolute left-0 top-0 w-1 h-full bg-emerald-500" />
                  <div className="flex gap-3 items-center">
                    {cupom.imagem ? (
                      <div className="w-10 h-10 rounded-lg overflow-hidden bg-black shrink-0 relative">
                        <Image
                          src={cupom.imagem}
                          alt={cupom.titulo}
                          width={40}
                          height={40}
                          className="object-cover w-full h-full"
                        />
                      </div>
                    ) : null}
                    <div>
                      <h4 className="font-black text-white text-sm uppercase">{cupom.titulo}</h4>
                      <p className="text-[10px] text-zinc-400">{cupom.regra}</p>
                    </div>
                  </div>
                  <div className="bg-black/40 p-2 rounded-full text-zinc-500 group-hover:text-emerald-500 transition">
                    <QrCode size={20} />
                  </div>
                </div>
              ))
            ) : (
              <p className="text-zinc-500 text-xs italic text-center py-4">
                Nenhum cupom disponível no momento.
              </p>
            )}
          </div>
        </div>
      </div>

      {isModalOpen && activeCupom ? (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/95 backdrop-blur-xl p-6 animate-in zoom-in duration-300">
          <div className="bg-white w-full max-w-sm rounded-[2rem] p-8 text-center relative shadow-2xl overflow-hidden">
            <button
              onClick={() => setIsModalOpen(false)}
              className="absolute top-4 right-4 bg-zinc-100 p-2 rounded-full text-zinc-500 hover:bg-zinc-200"
            >
              <ArrowLeft size={20} />
            </button>
            <h3 className="text-black font-black text-xl uppercase mb-1">Cupom ativado</h3>
            <p className="text-zinc-500 text-xs font-medium mb-6">{activeCupom.titulo}</p>

            <div className="bg-black p-4 rounded-2xl inline-block mb-6 shadow-xl relative group">
              <div className="absolute inset-0 bg-emerald-500/20 animate-pulse rounded-2xl" />
              <div className="relative z-10 rounded-xl bg-white p-2">
                <QRCodeSVG value={buildCouponQrPayload(activeCupom)} size={160} includeMargin />
              </div>
            </div>

            <div className="bg-zinc-100 rounded-xl p-4 mb-6 flex justify-between items-center border border-zinc-200">
              <div className="text-left">
                <p className="text-[10px] text-zinc-400 font-bold uppercase">Valor</p>
                <span className="font-mono text-xl font-black text-black tracking-widest">
                  {activeCupom.valor}
                </span>
              </div>
              <button
                onClick={() => addToast("Mostre ao caixa.", "success")}
                className="bg-white p-2 rounded-lg border border-zinc-200 text-zinc-400 hover:text-emerald-600 shadow-sm"
              >
                <CheckCircle size={20} />
              </button>
            </div>

            <div className="text-zinc-400 text-xs font-bold flex items-center justify-center gap-2 bg-red-50 p-2 rounded-lg text-red-500">
              <Clock size={14} className="animate-pulse" /> Expira em:
              <span className="font-mono text-base">{formatTime(countdown)}</span>
            </div>
            <button
              onClick={() => void handleManualActivation()}
              disabled={activatingManual}
              className="mt-4 flex w-full items-center justify-center gap-2 rounded-xl bg-emerald-600 py-3 text-xs font-black uppercase text-white hover:bg-emerald-500 disabled:opacity-60"
            >
              {activatingManual ? <Loader2 size={15} className="animate-spin" /> : <CheckCircle size={15} />}
              Ativar manualmente e aguardar aprovação
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
