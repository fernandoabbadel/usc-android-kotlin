import EventosClientPage, { type Evento } from "./EventosClientPage";

import { fetchEventsFeed } from "@/lib/eventsNativeService";
import { resolveServerTenantScope } from "@/lib/serverTenantScope";
import {
  createDefaultTenantAppModulesConfig,
  fetchEffectiveTenantAppModulesConfig,
  type TenantAppModulesConfig,
} from "@/lib/tenantAppModulesService";

interface EventosPageContentProps {
  tenantSlugOverride?: string;
}

const serializeForClient = (value: unknown): unknown => {
  if (value === null || value === undefined) return value;
  if (value instanceof Date) return value.toISOString();

  if (typeof value === "object") {
    const maybeDateLike = value as { toDate?: unknown };
    if (typeof maybeDateLike.toDate === "function") {
      const parsed = maybeDateLike.toDate.call(value);
      if (parsed instanceof Date && !Number.isNaN(parsed.getTime())) {
        return parsed.toISOString();
      }
    }

    if (Array.isArray(value)) {
      return value.map((entry) => serializeForClient(entry));
    }

    return Object.fromEntries(
      Object.entries(value as Record<string, unknown>)
        .filter(([, entryValue]) => typeof entryValue !== "function")
        .map(([key, entryValue]) => [key, serializeForClient(entryValue)])
    );
  }

  return value;
};

export async function EventosPageContent({
  tenantSlugOverride = "",
}: EventosPageContentProps) {
  const scope = await resolveServerTenantScope({ tenantSlug: tenantSlugOverride });
  let initialEventos: Evento[] = [];
  let initialModulesConfig: TenantAppModulesConfig = createDefaultTenantAppModulesConfig();
  let initialModulesHydrated = false;

  const [eventosResult, modulesResult] = await Promise.allSettled([
    fetchEventsFeed({
      maxResults: 24,
      forceRefresh: false,
      tenantId: scope.tenantId || undefined,
    }),
    scope.tenantId
      ? fetchEffectiveTenantAppModulesConfig({
          tenantId: scope.tenantId,
          tenantSlug: scope.tenantSlug,
        })
      : Promise.resolve(createDefaultTenantAppModulesConfig()),
  ]);

  if (eventosResult.status === "fulfilled") {
    initialEventos = serializeForClient(eventosResult.value) as Evento[];
  }

  if (modulesResult.status === "fulfilled") {
    initialModulesConfig = modulesResult.value;
    initialModulesHydrated = true;
  }

  return (
    <EventosClientPage
      initialEventos={initialEventos}
      initialModulesConfig={initialModulesConfig}
      initialModulesHydrated={initialModulesHydrated}
    />
  );
}
