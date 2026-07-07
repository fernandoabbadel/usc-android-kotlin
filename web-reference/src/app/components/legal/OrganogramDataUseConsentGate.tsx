"use client";

import { useEffect, useState } from "react";

import { useAuth } from "@/context/AuthContext";
import { useTenantTheme } from "@/context/TenantThemeContext";
import {
  ORGANOGRAM_DATA_USE_CONTEXT_TYPE,
  buildOrganogramDataUseContextId,
  fetchOrganogramConfig,
  isPublishedOrganogramMember,
  type OrganogramMemberRecord,
} from "@/lib/organogramService";
import { DataUseConsentModal, hasDataUseConsent } from "./DataUseConsentBox";

export default function OrganogramDataUseConsentGate() {
  const { user, loading: authLoading } = useAuth();
  const { tenantId, loading: tenantLoading } = useTenantTheme();
  const [member, setMember] = useState<OrganogramMemberRecord | null>(null);
  const [checking, setChecking] = useState(true);
  const [accepted, setAccepted] = useState(true);

  useEffect(() => {
    if (authLoading || tenantLoading) {
      setChecking(true);
      return;
    }
    if (!user?.uid || user.isAnonymous) {
      setMember(null);
      setAccepted(true);
      setChecking(false);
      return;
    }

    let mounted = true;
    const run = async () => {
      try {
        setChecking(true);
        const config = await fetchOrganogramConfig({
          forceRefresh: false,
          tenantId: tenantId || undefined,
        });
        const currentMember =
          config.membros.find(
            (entry) =>
              entry.userId?.trim() === user.uid.trim() &&
              isPublishedOrganogramMember(entry)
          ) || null;

        if (!currentMember) {
          if (!mounted) return;
          setMember(null);
          setAccepted(true);
          return;
        }

        const contextId = buildOrganogramDataUseContextId(user.uid);
        const hasConsent = await hasDataUseConsent({
          userId: user.uid,
          contextType: ORGANOGRAM_DATA_USE_CONTEXT_TYPE,
          contextId,
          tenantId: tenantId || null,
          source: "app",
        });

        if (!mounted) return;
        setMember(currentMember);
        setAccepted(hasConsent);
      } catch (error: unknown) {
        console.error("Erro ao verificar autorização do organograma:", error);
        if (!mounted) return;
        setMember(null);
        setAccepted(true);
      } finally {
        if (mounted) setChecking(false);
      }
    };

    void run();
    return () => {
      mounted = false;
    };
  }, [authLoading, tenantId, tenantLoading, user?.uid, user?.isAnonymous]);

  if (!user?.uid || checking || accepted || !member) return null;

  return (
    <DataUseConsentModal
      open
      contextType={ORGANOGRAM_DATA_USE_CONTEXT_TYPE}
      contextId={buildOrganogramDataUseContextId(user.uid)}
      tenantId={tenantId || null}
      source="app"
      title="Autorizar uso dos dados do organograma"
      description="Seu perfil faz parte do organograma. Confirme a autorização para continuar usando a plataforma."
      actionLabel="Autorizar e continuar"
      allowCancel={false}
      metadata={{
        authorizationScope: "tenant",
        memberId: member.id,
        cargo: member.cargo,
        secao: member.secao,
      }}
      onAccepted={() => setAccepted(true)}
    />
  );
}
