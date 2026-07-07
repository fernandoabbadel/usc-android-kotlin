// ARQUIVO: src/app/perfil/[id]/page.tsx

"use client";

import React, { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Image from "next/image";
import { 
  ArrowLeft, MapPin, Edit3, Instagram, MessageCircle, Ghost, Fish, Share2, ShieldCheck, Loader2, 
  UserPlus, UserCheck, X, PawPrint, Users, Lock, Heart,
  Calendar, Clock, CheckCircle, EyeOff, Store, HeartHandshake, MoreHorizontal, Plus, Trash2,
  Flame, Sparkles, Music, Palette, Utensils
} from "lucide-react";
import { useAuth } from "../../../context/AuthContext"; 
import { useToast } from "../../../context/ToastContext";
import { useTenantTheme } from "../../../context/TenantThemeContext";
import {
  fetchMutualProfileAffinitiesPage,
  fetchFollowList,
  fetchProfileAffinityStatus,
  fetchPublicProfileBundle,
  fetchTenantAstroSigns,
  hasViewerCapturedProfile,
  removeProfileAffinity,
  sendProfileAffinity,
  type ProfileAffinityPerson,
  toggleFollowProfile
} from "../../../lib/profilePublicService";
import { dispatchBottomNavNotificationsChanged } from "@/lib/bottomNavService";
import { getBackendErrorCode } from "@/lib/backendErrors";
import {
  fetchMentorshipProfileBundle,
  respondToMentorshipInvite,
  resolveMentorshipRoleOptions,
  sendMentorshipInvite,
  type MentorshipProfileBundle,
  updateMentorshipRoleLabel,
} from "@/lib/mentorshipService";
import {
  fetchCurrentMiniVendorProfile,
  isMiniVendorProfilePublic,
  type MiniVendorProfile,
} from "@/lib/miniVendorService";
import {
  fetchCadastroConfig,
  getDefaultCadastroConfig,
  type CadastroConfig,
} from "@/lib/cadastroConfigService";
import { calculateAgeFromBirthDate } from "@/lib/birthDate";
import {
  getDefaultColorOptions,
  getSportPresentation,
  type CadastroChoiceOption,
  type CadastroColorOption,
} from "@/lib/cadastroOptions";
import {
  calculateAstroStats,
  getZodiacSignPresentation,
  resolveZodiacCompatibility,
  ZODIAC_ELEMENT_EMOJI,
  type AstroStats,
} from "@/lib/astroProfile";
import { isAdminLikeRole, resolveEffectiveAccessRole } from "@/lib/roles";
import Link from "next/link";
import { getTurmaImage } from "../../../constants/turmaImages";
import { resolvePlanIcon, resolvePlanTheme, resolveUserPlanIcon } from "../../../constants/planVisuals";
import { withTenantSlug } from "../../../lib/tenantRouting";
import { savePrivacyPreferences } from "@/lib/legalGovernanceService";

// --- TIPAGEM ---

// Interfaces auxiliares para remover 'any'
interface PostItem {
  id: string;
  texto?: string;
  likesCount?: number;
  comentarios?: number;
  createdAt?: unknown;
  userId?: string;
}

interface EventItem {
  id: string;
  titulo: string;
  data?: string;
  imagem?: string;
  imagePositionY?: number;
}

interface TreinoItem {
  id: string;
  modalidade: string;
  imagem?: string;
  dia?: string;
  horario?: string;
  local?: string;
}

interface LigaItem {
  id: string;
  sigla?: string;
  foto?: string;
  logo?: string;
}

interface UserProfile {
  uid: string;
  nome: string;
  apelido?: string;
  foto?: string;
  turma?: string;
  bio?: string;
  cidadeOrigem?: string;
  dataNascimento?: string;
  instagram?: string;
  instagramPublico?: boolean;
  whatsappPublico?: boolean;
  idadePublica?: boolean;
  relacionamentoPublico?: boolean;
  telefone?: string;
  signo?: string;
  signoPublico?: boolean;
  ascendente?: string;
  ascendentePublico?: boolean;
  lugarEspecial?: string[];
  comidaPreferida?: string[];
  musicaPreferida?: string[];
  corPreferida?: string;
  esportes?: string[];
  role?: string;
  tenant_role?: string;
  status?: string; 
  profile_public?: boolean;
  profile_photo_public?: boolean;
  allow_profile_discovery?: boolean;
  
  plano?: string;        
  plano_cor?: string; 
  plano_icon?: string;
  
  patente?: string;
  patente_icon?: string;
  patente_cor?: string;
  tier?: 'bicho' | 'atleta' | 'lenda'; 
  
  level?: number;
  xp?: number;
  pets?: string;
  statusRelacionamento?: string;
  stats?: {
    arenaWins?: number;
    arenaLosses?: number;
    followersCount?: number;
    followingCount?: number;
    [key: string]: number | undefined;
  };
  
  [key: string]: unknown; // Substituído any por unknown para segurança
}

interface FollowData {
    uid: string;
    nome: string;
    foto: string;
    turma: string;
}

type ProfileTab =
  | "posts"
  | "eventos"
  | "treinos"
  | "ligas"
  | "apadrinhamento"
  | "mini_vendor";

type PreferenceBadge = {
  id: string;
  label: string;
  icon: string;
};

const PROFILE_LIST_PAGE_SIZE = 20;
const CRUSHS_PAGE_SIZE = 20;

const normalizePreferenceKey = (value: string): string =>
  value
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "");

const resolvePreferenceBadges = (
  selected: readonly string[] | undefined,
  options: readonly CadastroChoiceOption[]
): PreferenceBadge[] => {
  const registry = new Map<string, CadastroChoiceOption>();
  options.forEach((option) => {
    registry.set(normalizePreferenceKey(option.id), option);
    registry.set(normalizePreferenceKey(option.label), option);
  });

  const seen = new Set<string>();
  return (selected ?? []).reduce<PreferenceBadge[]>((items, rawValue) => {
    const key = normalizePreferenceKey(String(rawValue || ""));
    if (!key || seen.has(key)) return items;
    const option = registry.get(key);
    const id = option?.id || key;
    if (seen.has(id)) return items;
    seen.add(key);
    seen.add(id);
    items.push({
      id,
      label: option?.label || String(rawValue).trim(),
      icon: option?.icon || "✨",
    });
    return items;
  }, []);
};

const resolveColorPreference = (
  selected: string | undefined,
  options: readonly CadastroColorOption[]
): CadastroColorOption | null => {
  const key = normalizePreferenceKey(String(selected || ""));
  if (!key) return null;
  return (
    options.find(
      (option) =>
        normalizePreferenceKey(option.id) === key || normalizePreferenceKey(option.label) === key
    ) || null
  );
};

// --- COMPONENTES VISUAIS ---

const LevelBadge = ({
  xp,
  patente,
  patenteIcon,
  patenteCor,
}: {
  xp: number;
  patente?: string;
  patenteIcon?: string;
  patenteCor?: string;
}) => {
    const IconComp = resolvePlanIcon(patenteIcon || "fish", Fish);
    const colorClass =
      typeof patenteCor === "string" && patenteCor.trim().startsWith("text-")
        ? patenteCor
        : "text-zinc-500";
    let borderClass = "border-zinc-700";
    if (colorClass.includes("orange")) borderClass = "border-orange-500/50";
    else if (colorClass.includes("red")) borderClass = "border-red-500/50";
    else if (colorClass.includes("emerald")) borderClass = "border-emerald-500/50";
    else if (colorClass.includes("blue")) borderClass = "border-blue-500/50";
    else if (colorClass.includes("yellow")) borderClass = "border-yellow-500/50";

    return (
        <div title={`${patente || "Plankton"} • ${xp} XP`} className={`relative group cursor-help p-3 rounded-full bg-zinc-900 border ${borderClass} shadow-lg transition-transform hover:scale-110`}>
            <IconComp size={20} className={colorClass} />
        </div>
    );
};
const PlanBadge = ({ nome, cor, iconName }: { nome?: string, cor?: string, iconName?: string }) => {
    const IconComponent = resolveUserPlanIcon(iconName, nome, Ghost);
    const title = nome || "Plano atual";
    const theme = resolvePlanTheme(cor);

    return (
        <div className={`relative group cursor-help p-3 rounded-full border shadow-lg transition-transform hover:scale-110 ${theme.badgeClass}`}>
            <IconComponent size={20} className="animate-pulse-slow" />
            <div className="absolute bottom-full mb-3 left-1/2 -translate-x-1/2 px-3 py-2 bg-black/95 text-white text-[10px] font-bold rounded-xl opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap border border-zinc-800 pointer-events-none z-50 shadow-2xl">
                <span className="uppercase tracking-wider">Plano {title}</span>
                <div className="w-2 h-2 bg-black border-r border-b border-zinc-800 absolute -bottom-1 left-1/2 -translate-x-1/2 rotate-45"></div>
            </div>
        </div>
    );
};

export default function PerfilPublicoPage() {
  const params = useParams();
  const router = useRouter();
  const { user } = useAuth();
  const { addToast } = useToast();
  const { tenantId: activeTenantId, tenantSlug } = useTenantTheme();
  const effectiveMentorshipTenantId =
    activeTenantId || (typeof user?.tenant_id === "string" ? user.tenant_id.trim() : "");

  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [profileHidden, setProfileHidden] = useState(false);
  
  const [isFollowing, setIsFollowing] = useState(false);
  const [followersCount, setFollowersCount] = useState(0);
  const [followingCount, setFollowingCount] = useState(0);
  const [followersList, setFollowersList] = useState<FollowData[]>([]);
  const [followingList, setFollowingList] = useState<FollowData[]>([]);
  const [mentorshipBundle, setMentorshipBundle] = useState<MentorshipProfileBundle | null>(null);
  const [miniVendorProfile, setMiniVendorProfile] = useState<MiniVendorProfile | null>(null);
  const [miniVendorHiddenByOwner, setMiniVendorHiddenByOwner] = useState(false);
  const [cadastroConfig, setCadastroConfig] = useState<CadastroConfig>(getDefaultCadastroConfig);
  const [sendingMentorshipMode, setSendingMentorshipMode] = useState<"" | "mentor" | "mentee">("");
  const [removingMentorshipId, setRemovingMentorshipId] = useState("");
  const [editingMentorshipId, setEditingMentorshipId] = useState("");
  const [showRelationshipMenu, setShowRelationshipMenu] = useState(false);
  const [profileVisibilityModalOpen, setProfileVisibilityModalOpen] = useState(false);
  const [updatingProfileVisibility, setUpdatingProfileVisibility] = useState(false);
  const [profileLockedByAlbum, setProfileLockedByAlbum] = useState(false);
  const [astroStats, setAstroStats] = useState<AstroStats | null>(null);
  const [sendingAffinity, setSendingAffinity] = useState(false);
  const [removingAffinity, setRemovingAffinity] = useState(false);
  const [affinitySent, setAffinitySent] = useState(false);
  const [showAffinityModal, setShowAffinityModal] = useState(false);
  const [mutualAffinities, setMutualAffinities] = useState<ProfileAffinityPerson[]>([]);
  const [loadingAffinityList, setLoadingAffinityList] = useState(false);
  const [loadingMoreAffinityList, setLoadingMoreAffinityList] = useState(false);
  const [affinityNextOffset, setAffinityNextOffset] = useState<number | null>(null);
  
  const [activeModal, setActiveModal] = useState<'followers' | 'following' | null>(null);
  const [loadingFollowList, setLoadingFollowList] = useState(false);
  const [loadingMoreFollowList, setLoadingMoreFollowList] = useState(false);
  const [activeTab, setActiveTab] = useState<ProfileTab>('posts');
  const tabs: Array<{ id: ProfileTab; label: string }> = [
    { id: "posts", label: "Posts" },
    { id: "eventos", label: "Eventos" },
    { id: "treinos", label: "Treinos" },
    { id: "ligas", label: "Ligas" },
    { id: "apadrinhamento", label: "Apadrinhamento" },
    { id: "mini_vendor", label: "Mini Vendor" },
  ];

  const [recentPosts, setRecentPosts] = useState<PostItem[]>([]);
  const [myEvents, setMyEvents] = useState<EventItem[]>([]);
  const [myTreinos, setMyTreinos] = useState<TreinoItem[]>([]);
  const [myLigas, setMyLigas] = useState<LigaItem[]>([]);
  const effectiveCadastroTenantId =
    activeTenantId ||
    (typeof profile?.tenant_id === "string" ? profile.tenant_id.trim() : "") ||
    (typeof user?.tenant_id === "string" ? user.tenant_id.trim() : "");

  // Verifica se sou eu mesmo
  const isOwnProfile = user?.uid === params.id;
  const tenantPath = useCallback(
    (path: string): string =>
      tenantSlug.trim() ? withTenantSlug(tenantSlug, path) : path,
    [tenantSlug]
  );

  useEffect(() => {
    if (!params.id) return;
    const uid = params.id as string;
    const effectiveTenantId =
      activeTenantId || (typeof user?.tenant_id === "string" ? user.tenant_id.trim() : "");

    const fetchProfile = async () => {
        try {
            const bundle = await fetchPublicProfileBundle(uid, user?.uid, {
              forceRefresh: true,
              tenantId: effectiveTenantId || undefined,
            });
            if (bundle?.profile) {
                const data = bundle.profile as UserProfile;

                // ðŸ¦ˆ VERIFICAÃ‡ÃƒO DE CONTA DESATIVADA 
                if ((data.role === 'inactive' || data.status === 'paused' || data.profile_public === false) && !isOwnProfile) {
                    setProfileHidden(true);
                    setLoading(false);
                    return; 
                }

                setProfile(data);
                if (isOwnProfile) {
                  setProfileLockedByAlbum(false);
                } else {
                  const captured = user?.uid
                    ? await hasViewerCapturedProfile({
                        viewerUid: user.uid,
                        targetUid: uid,
                        tenantId:
                          effectiveTenantId ||
                          (typeof data.tenant_id === "string" ? data.tenant_id : undefined),
                      }).catch(() => false)
                    : false;
                  setProfileLockedByAlbum(!captured);
                }
                
                // Seguidores
                setFollowersCount(bundle.followersCount);
                setFollowingCount(bundle.followingCount);

                setIsFollowing(bundle.isFollowing);
                if (!isOwnProfile && user?.uid && effectiveTenantId) {
                  const affinityStatus = await fetchProfileAffinityStatus({
                    viewerUid: user.uid,
                    targetUid: uid,
                    tenantId: effectiveTenantId,
                  }).catch(() => ({ sent: false, received: false, mutual: false }));
                  setAffinitySent(affinityStatus.sent);
                } else {
                  setAffinitySent(false);
                }

                setRecentPosts((bundle.posts as PostItem[]).slice(0, 5));
                setMyEvents((bundle.events as EventItem[]).slice(0, 5));

                setMyLigas((bundle.ligas as LigaItem[]).slice(0, 5));

                setMyTreinos((bundle.treinos as TreinoItem[]).slice(0, 5));

                if (effectiveTenantId) {
                  const [nextMentorshipBundle, nextMiniVendorProfile] = await Promise.all([
                    fetchMentorshipProfileBundle({
                      tenantId: effectiveTenantId,
                      targetUserId: uid,
                      viewerUserId: user?.uid,
                      forceRefresh: true,
                    }).catch(() => null),
                    fetchCurrentMiniVendorProfile({
                      tenantId: effectiveTenantId,
                      userId: uid,
                      forceRefresh: true,
                    }).catch(() => null),
                  ]);

                  setMentorshipBundle(nextMentorshipBundle);
                  const isMiniVendorHiddenFromProfile = Boolean(
                    nextMiniVendorProfile &&
                    nextMiniVendorProfile.status === "approved" &&
                    nextMiniVendorProfile.profileVisible === false
                  );
                  const canSeeMiniVendor =
                    isAdminLikeRole(resolveEffectiveAccessRole(user)) ||
                    isMiniVendorProfilePublic(nextMiniVendorProfile);
                  setMiniVendorHiddenByOwner(
                    isMiniVendorHiddenFromProfile && !canSeeMiniVendor
                  );
                  setMiniVendorProfile(canSeeMiniVendor ? nextMiniVendorProfile : null);
                } else {
                  setMentorshipBundle(null);
                  setMiniVendorProfile(null);
                  setMiniVendorHiddenByOwner(false);
                }

            } else {
                addToast("Usuário não encontrado.", "error");
                router.push(tenantPath("/dashboard"));
            }
        } catch (error: unknown) { console.error(error); } 
        finally { setLoading(false); }
    };
    void fetchProfile();
  }, [params.id, user, activeTenantId, isOwnProfile, addToast, router, tenantPath]); // ðŸ¦ˆ Dependências adicionadas

  useEffect(() => {
    setShowRelationshipMenu(false);
    setProfileLockedByAlbum(false);
    setAffinitySent(false);
    setShowAffinityModal(false);
    setMutualAffinities([]);
    setAffinityNextOffset(null);
  }, [params.id]);

  useEffect(() => {
    let mounted = true;

    if (!effectiveCadastroTenantId) {
      setCadastroConfig(getDefaultCadastroConfig());
      return () => {
        mounted = false;
      };
    }

    const loadCadastroConfig = async () => {
      try {
        const nextConfig = await fetchCadastroConfig({
          tenantId: effectiveCadastroTenantId,
          forceRefresh: true,
        });
        if (!mounted) return;
        setCadastroConfig(nextConfig);
      } catch (error: unknown) {
        console.error("Erro ao carregar configuracao de modalidades:", error);
        if (!mounted) return;
        setCadastroConfig(getDefaultCadastroConfig());
      }
    };

    void loadCadastroConfig();
    return () => {
      mounted = false;
    };
  }, [effectiveCadastroTenantId]);

  useEffect(() => {
    let mounted = true;

    const loadAstroStats = async () => {
      if (!profile || !effectiveCadastroTenantId) {
        setAstroStats(null);
        return;
      }

      try {
        const signs = await fetchTenantAstroSigns({ tenantId: effectiveCadastroTenantId });
        if (!mounted) return;
        setAstroStats(
          calculateAstroStats(
            signs,
            profile.signo
          )
        );
      } catch (error: unknown) {
        console.error("Erro ao carregar estatísticas de signo:", error);
        if (mounted) setAstroStats(null);
      }
    };

    void loadAstroStats();
    return () => {
      mounted = false;
    };
  }, [effectiveCadastroTenantId, profile]);

  const handleProfileVisibilityChange = async (nextProfilePublic: boolean) => {
      if (!user?.uid || !isOwnProfile || updatingProfileVisibility) return;
      try {
          setUpdatingProfileVisibility(true);
          await savePrivacyPreferences({
              tenantId: activeTenantId || user.tenant_id || null,
              source: "app",
              preferences: {
                  profile_public: nextProfilePublic,
                  allow_discovery: nextProfilePublic,
              },
          });
          setProfile((current) =>
            current
              ? {
                  ...current,
                  profile_public: nextProfilePublic,
                  allow_profile_discovery: nextProfilePublic,
                  status: nextProfilePublic ? "ativo" : "paused",
                }
              : current
          );
          setShowRelationshipMenu(false);
          setProfileVisibilityModalOpen(false);
          addToast(
            nextProfilePublic
              ? "Perfil público reativado."
              : "Perfil invisível ativado. Você também não poderá ver perfis de outras pessoas enquanto essa opção estiver ativa.",
            nextProfilePublic ? "success" : "info"
          );
      } catch (error: unknown) {
          console.error(error);
          addToast(
            error instanceof Error ? error.message : "Erro ao atualizar visibilidade do perfil.",
            "error"
          );
      } finally {
          setUpdatingProfileVisibility(false);
      }
  };

  const handleFollow = async () => {
      if (!user || !profile) return;
      try {
          const result = await toggleFollowProfile({
              viewerUid: user.uid,
              targetUid: profile.uid,
              currentlyFollowing: isFollowing,
              tenantId: activeTenantId || user?.tenant_id || undefined,
              viewerData: {
                  uid: user.uid,
                  nome: user.nome || "Atleta",
                  foto: user.foto || "",
                  turma: user.turma || "Geral",
              },
              targetData: {
                  uid: profile.uid,
                  nome: profile.nome,
                  foto: profile.foto || "",
                  turma: profile.turma || "Geral",
              },
          });
          setIsFollowing(result.isFollowing);
          setFollowersCount(result.followersCount);
          if (user.uid === profile.uid) {
              setFollowingCount(result.followingCount);
          }
          addToast(result.isFollowing ? "Seguindo!" : "Deixou de seguir.", result.isFollowing ? "success" : "info");
      } catch (error: unknown) {
          console.error(error);
          const code = getBackendErrorCode(error)?.toLowerCase() || "";
          const message = error instanceof Error ? error.message.toLowerCase() : "";
          if (
            code.includes("functions/not-found") ||
            code.includes("functions/unavailable") ||
            code.includes("functions/internal") ||
            code.includes("functions/unknown") ||
            message.includes("cors") ||
            message.includes("preflight")
          ) {
            addToast("Follow indisponivel no backend. Publique as Functions.", "error");
          } else if (code.includes("permission-denied")) {
            addToast("Sem permissão para seguir esse perfil.", "error");
          } else {
            addToast("Erro ao seguir.", "error");
          }
      }
  };

  const handleSendAffinity = async () => {
      if (!user?.uid || !profile?.uid || isOwnProfile || sendingAffinity) return;
      try {
          setSendingAffinity(true);
          const result = await sendProfileAffinity({
              tenantId: effectiveCadastroTenantId || activeTenantId || user.tenant_id,
              viewer: {
                  uid: user.uid,
                  nome: user.nome || "Atleta",
                  foto: user.foto || "",
                  turma: user.turma || "Geral",
              },
              target: {
                  uid: profile.uid,
                  nome: profile.nome,
                  foto: profile.foto || "",
                  turma: profile.turma || "Geral",
              },
          });
          setAffinitySent(result.sent);
          setShowRelationshipMenu(false);
          dispatchBottomNavNotificationsChanged();
          addToast(result.mutual ? "Crush confirmado! 🔥" : "Crush enviado! 🔥", "success");
      } catch (error: unknown) {
          console.error(error);
          addToast(error instanceof Error ? error.message : "Erro ao enviar Crush.", "error");
      } finally {
          setSendingAffinity(false);
      }
  };

  const handleRemoveAffinity = async (targetUid?: string) => {
      const cleanTargetUid = targetUid || profile?.uid || "";
      if (!user?.uid || !cleanTargetUid || removingAffinity) return;
      try {
          setRemovingAffinity(true);
          await removeProfileAffinity({
              tenantId: effectiveCadastroTenantId || activeTenantId || user.tenant_id,
              viewer: {
                  uid: user.uid,
                  nome: user.nome || "Atleta",
                  foto: user.foto || "",
                  turma: user.turma || "Geral",
              },
              target: {
                  uid: cleanTargetUid,
                  nome:
                    cleanTargetUid === profile?.uid
                      ? profile.nome
                      : mutualAffinities.find((person) => person.uid === cleanTargetUid)?.nome || "Atleta",
                  foto:
                    cleanTargetUid === profile?.uid
                      ? profile.foto || ""
                      : mutualAffinities.find((person) => person.uid === cleanTargetUid)?.foto || "",
                  turma:
                    cleanTargetUid === profile?.uid
                      ? profile.turma || "Geral"
                      : mutualAffinities.find((person) => person.uid === cleanTargetUid)?.turma || "Geral",
              },
          });
          if (cleanTargetUid === profile?.uid) setAffinitySent(false);
          setMutualAffinities((prev) => prev.filter((person) => person.uid !== cleanTargetUid));
          setShowRelationshipMenu(false);
          addToast("Crush removido.", "success");
      } catch (error: unknown) {
          console.error(error);
          addToast(error instanceof Error ? error.message : "Erro ao remover Crush.", "error");
      } finally {
          setRemovingAffinity(false);
      }
  };

  const handleOpenAffinityModal = async () => {
      if (!user?.uid || !effectiveCadastroTenantId) return;
      setShowAffinityModal(true);
      setShowRelationshipMenu(false);
      setLoadingAffinityList(true);
      try {
          const page = await fetchMutualProfileAffinitiesPage({
              userId: user.uid,
              tenantId: effectiveCadastroTenantId,
              limit: CRUSHS_PAGE_SIZE,
              offset: 0,
          });
          setMutualAffinities(page.rows);
          setAffinityNextOffset(page.nextOffset);
      } catch (error: unknown) {
          console.error(error);
          addToast("Erro ao carregar Crushs.", "error");
      } finally {
          setLoadingAffinityList(false);
      }
  };

  const handleLoadMoreAffinity = async () => {
      if (!user?.uid || !effectiveCadastroTenantId || affinityNextOffset === null || loadingMoreAffinityList) return;
      try {
          setLoadingMoreAffinityList(true);
          const page = await fetchMutualProfileAffinitiesPage({
              userId: user.uid,
              tenantId: effectiveCadastroTenantId,
              limit: CRUSHS_PAGE_SIZE,
              offset: affinityNextOffset,
          });
          setMutualAffinities((prev) => {
              const known = new Set(prev.map((person) => person.uid));
              return [...prev, ...page.rows.filter((person) => !known.has(person.uid))];
          });
          setAffinityNextOffset(page.nextOffset);
      } catch (error: unknown) {
          console.error(error);
          addToast("Erro ao carregar mais Crushs.", "error");
      } finally {
          setLoadingMoreAffinityList(false);
      }
  };

  const handleSendMentorshipInvite = async (mode: "mentor" | "mentee") => {
      if (!user?.uid || !profile?.uid || !effectiveMentorshipTenantId || sendingMentorshipMode) return;
      try {
          setSendingMentorshipMode(mode);
          await sendMentorshipInvite({
              tenantId: effectiveMentorshipTenantId,
              currentUserId: user.uid,
              targetUserId: profile.uid,
              mode,
          });
          const nextBundle = await fetchMentorshipProfileBundle({
              tenantId: effectiveMentorshipTenantId,
              targetUserId: profile.uid,
              viewerUserId: user.uid,
              forceRefresh: true,
          });
          setMentorshipBundle(nextBundle);
          setShowRelationshipMenu(false);
          addToast(
            mode === "mentor"
              ? "Convite para padrinho/madrinha enviado."
              : "Convite para afilhado/afilhada enviado.",
            "success"
          );
      } catch (error: unknown) {
          console.error(error);
          addToast(error instanceof Error ? error.message : "Erro ao enviar convite.", "error");
      } finally {
          setSendingMentorshipMode("");
      }
  };

  const handleRemoveMentorship = async (relationshipId: string) => {
      if (!isOwnProfile || !user?.uid || !profile?.uid || !effectiveMentorshipTenantId || !relationshipId || removingMentorshipId) return;
      try {
          setRemovingMentorshipId(relationshipId);
          await respondToMentorshipInvite({
              tenantId: effectiveMentorshipTenantId,
              relationshipId,
              currentUserId: user.uid,
              action: "remove",
          });
          const nextBundle = await fetchMentorshipProfileBundle({
              tenantId: effectiveMentorshipTenantId,
              targetUserId: profile.uid,
              viewerUserId: user.uid,
              forceRefresh: true,
          });
          setMentorshipBundle(nextBundle);
          addToast("Vinculo removido.", "success");
      } catch (error: unknown) {
          console.error(error);
          addToast(error instanceof Error ? error.message : "Erro ao remover vinculo.", "error");
      } finally {
          setRemovingMentorshipId("");
      }
  };

  const handleEditMentorshipLabel = async (
      relationshipId: string,
      roleSide: "mentor" | "mentee",
      roleLabel: string
  ) => {
      if (!isOwnProfile || !user?.uid || !profile?.uid || !effectiveMentorshipTenantId || !relationshipId || editingMentorshipId) return;
      try {
          setEditingMentorshipId(relationshipId);
          await updateMentorshipRoleLabel({
              tenantId: effectiveMentorshipTenantId,
              relationshipId,
              currentUserId: user.uid,
              roleSide,
              roleLabel,
          });
          const nextBundle = await fetchMentorshipProfileBundle({
              tenantId: effectiveMentorshipTenantId,
              targetUserId: profile.uid,
              viewerUserId: user.uid,
              forceRefresh: true,
          });
          setMentorshipBundle(nextBundle);
          addToast("Rotulo atualizado.", "success");
      } catch (error: unknown) {
          console.error(error);
          addToast(error instanceof Error ? error.message : "Erro ao editar rotulo.", "error");
      } finally {
          setEditingMentorshipId("");
      }
  };

  const handleOpenList = async (type: 'followers' | 'following') => {
      if (!profile) return;
      setActiveModal(type);
      setLoadingFollowList(true);
      try {
          const list = await fetchFollowList(profile.uid, type, {
              maxResults: PROFILE_LIST_PAGE_SIZE,
              forceRefresh: false,
              tenantId: activeTenantId || user?.tenant_id || undefined,
          });
          if (type === 'followers') {
              setFollowersList(list);
          } else {
              setFollowingList(list);
          }
      } catch (error: unknown) {
          console.error(error);
          addToast("Erro ao carregar lista.", "error");
      } finally {
          setLoadingFollowList(false);
      }
  };

  const handleLoadMoreFollowList = async () => {
      if (!profile || !activeModal || loadingMoreFollowList) return;
      const currentList = activeModal === "followers" ? followersList : followingList;
      const nextLimit = currentList.length + PROFILE_LIST_PAGE_SIZE;
      try {
          setLoadingMoreFollowList(true);
          const list = await fetchFollowList(profile.uid, activeModal, {
              maxResults: nextLimit,
              forceRefresh: true,
              tenantId: activeTenantId || user?.tenant_id || undefined,
          });
          if (activeModal === "followers") {
              setFollowersList(list);
          } else {
              setFollowingList(list);
          }
      } catch (error: unknown) {
          console.error(error);
          addToast("Erro ao carregar mais perfis.", "error");
      } finally {
          setLoadingMoreFollowList(false);
      }
  };

  const formatPostDate = (value: unknown): string => {
      if (!value) return "Hoje";
      if (value instanceof Date) return value.toLocaleDateString("pt-BR");
      if (typeof value === "string" || typeof value === "number") {
          const parsed = new Date(value);
          return Number.isNaN(parsed.getTime()) ? "Hoje" : parsed.toLocaleDateString("pt-BR");
      }
      if (typeof value === "object" && value !== null) {
          const toDate = (value as { toDate?: unknown }).toDate;
          if (typeof toDate === "function") {
              const parsed = toDate.call(value) as Date;
              if (parsed instanceof Date && !Number.isNaN(parsed.getTime())) {
                  return parsed.toLocaleDateString("pt-BR");
              }
          }
      }
      return "Hoje";
  };

  if (loading) return <div className="h-screen bg-[#050505] flex items-center justify-center"><Loader2 className="animate-spin text-emerald-500" size={40}/></div>;

  if (!isOwnProfile && user?.profile_public === false) {
      return (
          <div className="min-h-screen bg-[#050505] text-zinc-500 font-sans flex flex-col items-center justify-center p-6 text-center">
              <div className="w-24 h-24 bg-zinc-900 rounded-full flex items-center justify-center mb-6 shadow-2xl shadow-black border border-zinc-800">
                  <EyeOff size={40} className="text-zinc-700"/>
              </div>
              <h1 className="text-2xl font-black text-zinc-400 uppercase tracking-tighter mb-2">Perfil invisível ativo</h1>
              <p className="text-sm font-medium text-zinc-600 max-w-sm mb-8">
                  Você ocultou seu perfil. Enquanto essa opção estiver ativa, a USC também bloqueia a visualização de perfis de outras pessoas e o álbum mostra perfis como invisíveis.
              </p>
              <button onClick={() => router.back()} className="px-8 py-3 bg-zinc-900 border border-zinc-800 rounded-full text-xs font-bold uppercase tracking-widest text-zinc-400 hover:text-white hover:border-zinc-600 transition flex items-center gap-2">
                  <ArrowLeft size={14}/> Voltar
              </button>
          </div>
      );
  }

  // ðŸ¦ˆ TELA DE PERFIL OCULTO
  if (profileHidden) {
      return (
          <div className="min-h-screen bg-[#050505] text-zinc-500 font-sans flex flex-col items-center justify-center p-6 text-center">
              <div className="w-24 h-24 bg-zinc-900 rounded-full flex items-center justify-center mb-6 shadow-2xl shadow-black border border-zinc-800 animate-pulse">
                  <Ghost size={40} className="text-zinc-700"/>
              </div>
              <h1 className="text-2xl font-black text-zinc-400 uppercase tracking-tighter mb-2">Perfil indisponivel</h1>
              <p className="text-sm font-medium text-zinc-600 max-w-xs mb-8">
                  Esta conta foi desativada temporariamente pelo usuário e está inacessível no momento.
              </p>
              <button onClick={() => router.back()} className="px-8 py-3 bg-zinc-900 border border-zinc-800 rounded-full text-xs font-bold uppercase tracking-widest text-zinc-400 hover:text-white hover:border-zinc-600 transition flex items-center gap-2">
                  <ArrowLeft size={14}/> Voltar
              </button>
          </div>
      );
  }

  if (!profile) return null;

  const profileAge = calculateAgeFromBirthDate(profile.dataNascimento);
  const resolveSportInfo = (sport: string) => {
    const presentation = getSportPresentation(sport, cadastroConfig.sportOptions);
    return {
      emoji: presentation.emoji,
      label: presentation.label,
      color: presentation.colorClass,
    };
  };
  const showAge = isOwnProfile || profile.idadePublica;
  const showWhatsapp = isOwnProfile || profile.whatsappPublico;
  const showInstagram = isOwnProfile || profile.instagramPublico;
  const showRelacionamento = isOwnProfile || profile.relacionamentoPublico;
  const profileSign = getZodiacSignPresentation(profile.signo);
  const profileAscendant = getZodiacSignPresentation(profile.ascendente);
  const viewerSign = getZodiacSignPresentation(user?.signo);
  const viewerAscendant = getZodiacSignPresentation(typeof user?.ascendente === "string" ? user.ascendente : "");
  const compatibilityMatches =
    !isOwnProfile && profile.signoPublico
      ? resolveZodiacCompatibility({
          viewerSign: viewerSign?.id,
          viewerAscendant: viewerAscendant?.id,
          targetSign: profileSign?.id,
          targetAscendant: profile.ascendentePublico ? profileAscendant?.id : "",
        })
      : [];
  const specialPlaceBadges = resolvePreferenceBadges(
    profile.lugarEspecial,
    cadastroConfig.specialPlaceOptions
  );
  const foodBadges = resolvePreferenceBadges(profile.comidaPreferida, cadastroConfig.foodOptions);
  const musicBadges = resolvePreferenceBadges(profile.musicaPreferida, cadastroConfig.musicOptions);
  const colorPreference = resolveColorPreference(profile.corPreferida, getDefaultColorOptions());
  const sameElementPercent =
    profileSign && astroStats ? astroStats.elementPercentages[profileSign.element] || 0 : 0;
  const showOwnHoroscope = isOwnProfile && Boolean(profileSign) && Boolean(astroStats);
  const showOtherHoroscope =
    !isOwnProfile && Boolean(profile.signoPublico) && compatibilityMatches.length > 0;
  const turmaImage = getTurmaImage(profile.turma);
  const badgeProps = { nome: profile.plano, cor: profile.plano_cor, iconName: profile.plano_icon };
  const mentorshipLabels = mentorshipBundle?.labels || {
    hubTitle: "Apadrinhamento",
    mentorLabel: "Padrinho/Madrinha",
    menteeLabel: "Afilhado/Afilhada",
    inviteMentorLabel: "Adicionar como meu padrinho/madrinha",
    inviteMenteeLabel: "Adicionar como meu afilhado/afilhada",
    requestHelpText: "Cada perfil pode ter 1 padrinho/madrinha e 1 afilhado/afilhada por atlética.",
  };
  const activeFollowList =
    activeModal === "followers" ? followersList : activeModal === "following" ? followingList : [];
  const activeFollowCount =
    activeModal === "followers" ? followersCount : activeModal === "following" ? followingCount : 0;
  const canLoadMoreFollowList = activeModal !== null && activeFollowList.length < activeFollowCount;

  return (
    <div className="min-h-screen bg-[#050505] text-white font-sans pb-24">
      {/* CAPA + FOTO */}
      <div className="relative">
        <div className="h-48 w-full bg-zinc-900 overflow-hidden relative">
            <div className="absolute inset-0 bg-gradient-to-b from-emerald-900/20 via-[#050505]/50 to-[#050505] z-10"></div>
            {/* ðŸ¦ˆ Correção de Imagem: Capa */}
            <Image 
                src={turmaImage} 
                alt="Capa da Turma"
                fill
                sizes="100vw"
                className="object-cover opacity-60 blur-[2px]"
                priority
            />
            <button onClick={() => router.back()} className="absolute top-6 left-6 z-20 p-2 bg-black/40 backdrop-blur-md rounded-full border border-white/10 hover:bg-white hover:text-black transition"><ArrowLeft size={20}/></button>
        </div>
        
        <div className="px-6 relative z-20 -mt-16 flex flex-col items-center">
            
            <div className="relative mb-3 group">
                {/* ðŸ¦ˆ Correção de Imagem: Avatar */}
                <div className={`relative h-32 w-32 rounded-full p-1 shadow-brand-strong ${profile.status === 'paused' ? 'bg-gradient-to-tr from-zinc-600 via-zinc-800 to-zinc-900 grayscale opacity-80' : 'bg-brand-gradient'}`}>
                    <div className="w-full h-full rounded-full overflow-hidden relative border-4 border-[#050505]">
                        <Image 
                            src={profile.foto || "https://github.com/shadcn.png"} 
                            alt={profile.nome}
                            width={128}
                            height={128}
                            className="object-cover w-full h-full"
                            
                        />
                    </div>
                </div>
                {/* ðŸ¦ˆ Correção de Imagem: Badge da Turma Pequena */}
                <div className="absolute bottom-0 right-0 z-30 h-10 w-10 overflow-hidden rounded-full border-[3px] border-[#050505] bg-zinc-950 shadow-brand">
                    <Image 
                        src={turmaImage} 
                        alt="Badge Turma"
                        width={40}
                        height={40}
                        className="h-full w-full object-cover"
                    />
                </div>
            </div>

            <div className="text-center space-y-1 mb-4">
                <h1 className="text-2xl font-black text-white uppercase italic tracking-tighter flex items-center justify-center gap-2">
                    {profile.apelido || profile.nome.split(" ")[0]}
                    {isAdminLikeRole(resolveEffectiveAccessRole(profile)) && <ShieldCheck size={18} className="text-red-500" />}
                    {!isAdminLikeRole(resolveEffectiveAccessRole(profile)) && profile.tenant_role === 'mini_vendor' && <Store size={18} className="text-blue-400" />}
                </h1>
                <p className="text-zinc-400 text-xs font-bold uppercase tracking-widest">{profile.nome}</p>
                
                {profile.status === 'paused' && isOwnProfile && (
                    <div className="mt-2 inline-flex items-center gap-2 px-3 py-1 bg-red-900/30 border border-red-500/30 rounded-full text-[10px] font-bold text-red-400 uppercase tracking-wide">
                        <EyeOff size={10} /> Perfil Oculto (Conta Pausada)
                    </div>
                )}

                <div className="flex items-center justify-center gap-2 mt-2">
                    <span className="bg-zinc-800 border border-zinc-700 px-3 py-1 rounded-full text-[10px] font-black uppercase text-zinc-300">{profile.turma || "Sem Turma"}</span>
                    {showAge && profileAge !== null && (<div className="relative group/age"><span className="bg-zinc-800 border border-zinc-700 px-3 py-1 rounded-full text-[10px] font-black uppercase text-zinc-300 flex items-center gap-1">{profileAge} Anos{!profile.idadePublica && <Lock size={8} className="text-zinc-500"/>}</span></div>)}
                </div>
            </div>
            
            <div className="flex items-center gap-6 mb-6 justify-center w-full">
                <PlanBadge nome={badgeProps.nome} cor={badgeProps.cor} iconName={badgeProps.iconName} />

                {isOwnProfile ? (
                    <div className="relative flex items-center gap-2">
                        <Link href={tenantPath("/cadastro")} className="px-8 py-2 bg-zinc-800 rounded-full text-xs font-bold uppercase border border-zinc-700 hover:bg-zinc-700 hover:border-emerald-500 transition shadow-lg flex items-center gap-2"><Edit3 size={14}/> Editar Perfil</Link>
                        <button
                          type="button"
                          onClick={() => setShowRelationshipMenu((prev) => !prev)}
                          className="rounded-full border border-zinc-700 bg-zinc-900 p-2 text-zinc-300 transition hover:border-zinc-500 hover:text-white"
                          title="Mais ações"
                        >
                          <MoreHorizontal size={16} />
                        </button>
                        {showRelationshipMenu ? (
                          <div className="absolute top-full right-0 z-30 mt-2 min-w-[240px] rounded-2xl border border-zinc-800 bg-zinc-950 p-2 shadow-2xl">
                            <button
                              type="button"
                              onClick={() => void handleOpenAffinityModal()}
                              className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-left text-xs font-bold text-zinc-200 transition hover:bg-zinc-900"
                            >
                              <Flame size={14} className="text-orange-400" />
                              Crushs
                            </button>
                            <button
                              type="button"
                              onClick={() => {
                                if (profile.profile_public === false || profile.status === "paused") {
                                  void handleProfileVisibilityChange(true);
                                  return;
                                }
                                setProfileVisibilityModalOpen(true);
                                setShowRelationshipMenu(false);
                              }}
                              disabled={updatingProfileVisibility}
                              className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-left text-xs font-bold text-zinc-200 transition hover:bg-zinc-900 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                              {updatingProfileVisibility ? (
                                <Loader2 size={14} className="animate-spin text-zinc-400" />
                              ) : (
                                <EyeOff size={14} className="text-zinc-400" />
                              )}
                              {profile.profile_public === false || profile.status === "paused"
                                ? "Reativar perfil público"
                                : "Ocultar perfil"}
                            </button>
                          </div>
                        ) : null}
                    </div>
                ) : (
                    <div className="relative flex items-center gap-2">
                        <button onClick={handleFollow} className={`px-8 py-2 rounded-full text-xs font-bold uppercase border transition shadow-lg flex items-center gap-2 ${isFollowing ? 'bg-zinc-900 border-zinc-700 text-zinc-400' : 'bg-emerald-600 border-emerald-500 text-white hover:scale-105'}`}>{isFollowing ? <UserCheck size={14}/> : <UserPlus size={14}/>} {isFollowing ? "Seguindo" : "Seguir"}</button>
                        {user?.uid ? (
                          <>
                            <button
                              type="button"
                              onClick={() => setShowRelationshipMenu((prev) => !prev)}
                              className="rounded-full border border-zinc-700 bg-zinc-900 p-2 text-zinc-300 transition hover:border-zinc-500 hover:text-white"
                              title="Mais ações"
                            >
                              <MoreHorizontal size={16} />
                            </button>
                            {showRelationshipMenu ? (
                              <div className="absolute top-full right-0 z-30 mt-2 min-w-[240px] rounded-2xl border border-zinc-800 bg-zinc-950 p-2 shadow-2xl">
                                <button
                                  type="button"
                                  onClick={() => affinitySent ? void handleRemoveAffinity() : void handleSendAffinity()}
                                  disabled={sendingAffinity || removingAffinity}
                                  className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-left text-xs font-bold text-zinc-200 transition hover:bg-zinc-900 disabled:cursor-not-allowed disabled:opacity-50"
                                >
                                  {sendingAffinity || removingAffinity ? <Loader2 size={14} className="animate-spin text-orange-400" /> : <Flame size={14} className="text-orange-400" />}
                                  {affinitySent ? "Remover Crush 🔥" : "Enviar Crush 🔥"}
                                </button>
                                {effectiveMentorshipTenantId ? (
                                <>
                                <button
                                  type="button"
                                  onClick={() => void handleSendMentorshipInvite("mentor")}
                                  disabled={sendingMentorshipMode.length > 0 || (mentorshipBundle?.viewerMentorRequestStatus ?? "none") !== "none"}
                                  className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-left text-xs font-bold text-zinc-200 transition hover:bg-zinc-900 disabled:cursor-not-allowed disabled:opacity-50"
                                >
                                  <HeartHandshake size={14} className="text-emerald-400" />
                                  {mentorshipLabels.inviteMentorLabel}
                                </button>
                                <button
                                  type="button"
                                  onClick={() => void handleSendMentorshipInvite("mentee")}
                                  disabled={sendingMentorshipMode.length > 0 || (mentorshipBundle?.viewerMenteeRequestStatus ?? "none") !== "none"}
                                  className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-left text-xs font-bold text-zinc-200 transition hover:bg-zinc-900 disabled:cursor-not-allowed disabled:opacity-50"
                                >
                                  <Users size={14} className="text-cyan-400" />
                                  {mentorshipLabels.inviteMenteeLabel}
                                </button>
                                </>
                                ) : null}
                              </div>
                            ) : null}
                          </>
                        ) : null}
                    </div>
                )}
                
                <LevelBadge
                  xp={profile.xp || 0}
                  patente={profile.patente}
                  patenteIcon={profile.patente_icon}
                  patenteCor={profile.patente_cor}
                />
            </div>

            <div className="grid grid-cols-3 gap-3 w-full max-w-sm mb-8">
                <button onClick={() => handleOpenList('followers')} className="bg-zinc-900/50 border border-zinc-800 p-3 rounded-2xl flex flex-col items-center hover:bg-zinc-800 transition active:scale-95"><span className="text-xl font-black text-white">{followersCount}</span><span className="text-[9px] text-zinc-500 uppercase font-bold tracking-wider">Seguidores</span></button>
                <button onClick={() => handleOpenList('following')} className="bg-zinc-900/50 border border-zinc-800 p-3 rounded-2xl flex flex-col items-center hover:bg-zinc-800 transition active:scale-95"><span className="text-xl font-black text-white">{followingCount}</span><span className="text-[9px] text-zinc-500 uppercase font-bold tracking-wider">Seguindo</span></button>
                <div className="bg-zinc-900/50 border border-zinc-800 p-3 rounded-2xl flex flex-col items-center"><span className="text-xl font-black text-white">{profile.xp || 0}</span><span className="text-[9px] text-zinc-500 uppercase font-bold tracking-wider">XP Total</span></div>
            </div>

            {!profileLockedByAlbum && profile.bio && <div className="w-full max-w-sm bg-zinc-900/30 border border-zinc-800/50 p-4 rounded-2xl mb-6 backdrop-blur-sm"><p className="text-sm text-zinc-300 text-center italic leading-relaxed">&quot;{profile.bio}&quot;</p></div>}
            
            {!profileLockedByAlbum ? (
            <div className="flex gap-3 mb-8 justify-center w-full">
                {showInstagram && profile.instagram && <a href={`https://instagram.com/${profile.instagram.replace('@','')}`} target="_blank" rel="noreferrer" className="w-12 h-12 rounded-xl bg-gradient-to-br from-purple-600 to-pink-500 flex items-center justify-center text-white shadow-lg hover:scale-110 transition hover:shadow-purple-500/20"><Instagram size={24}/></a>}
                <div className="relative">
                    {showWhatsapp ? (
                          <a href={`https://wa.me/55${profile.telefone?.replace(/\D/g,'')}`} target="_blank" rel="noreferrer" className="w-12 h-12 rounded-xl bg-gradient-to-br from-green-500 to-emerald-600 flex items-center justify-center text-white shadow-lg hover:scale-110 transition hover:shadow-green-500/20"><MessageCircle size={24}/></a>
                    ) : (
                          <div className="w-12 h-12 rounded-xl bg-zinc-900 flex items-center justify-center text-zinc-600 border border-zinc-800 cursor-not-allowed"><Lock size={20}/></div>
                    )}
                    {profile.whatsappPublico === false && isOwnProfile && <div className="absolute -top-1 -right-1 bg-zinc-900 rounded-full p-0.5 border border-zinc-700"><Lock size={10} className="text-zinc-400"/></div>}
                </div>
                <button className="w-12 h-12 rounded-xl bg-zinc-800 flex items-center justify-center text-zinc-400 border border-zinc-700 hover:text-white hover:border-zinc-500 transition"><Share2 size={22}/></button>
            </div>
            ) : (
              <div className="mb-8 w-full max-w-sm rounded-3xl border border-zinc-800 bg-zinc-900/50 p-5 text-center">
                <Lock size={22} className="mx-auto text-zinc-500" />
                <p className="mt-3 text-xs font-black uppercase tracking-[0.18em] text-zinc-400">
                  Perfil invisível
                </p>
                <p className="mt-2 text-xs font-semibold leading-relaxed text-zinc-600">
                  Capture este usuário no álbum para liberar a ficha completa.
                </p>
              </div>
            )}

            {/* ABAS */}
            {!profileLockedByAlbum ? (
            <div className="w-full max-w-sm">
                <div className="flex justify-between border-b border-zinc-800 mb-4 overflow-x-auto">
                    {tabs.map((tab) => (
                        <button key={tab.id} onClick={() => setActiveTab(tab.id)} className={`px-4 py-2 text-xs font-bold uppercase tracking-wider border-b-2 transition-colors ${activeTab === tab.id ? 'border-emerald-500 text-emerald-500' : 'border-transparent text-zinc-500 hover:text-zinc-300'}`}>{tab.label}</button>
                    ))}
                </div>

                <div className="min-h-[200px]">
                    {/* POSTS */}
                    {activeTab === 'posts' && (
                        recentPosts.length > 0 ? (
                            <div className="space-y-2 animate-in fade-in">{recentPosts.map(p => (<div key={p.id} className="bg-zinc-900/50 border border-zinc-800 p-3 rounded-xl"><p className="text-xs text-zinc-300 truncate mb-1">&quot;{p.texto}&quot;</p><div className="flex justify-between items-center text-[10px] text-zinc-500"><div className="flex items-center gap-2"><span className="flex items-center gap-1"><Heart size={10}/> {p.likesCount || 0}</span><span className="flex items-center gap-1"><MessageCircle size={10}/> {p.comentarios || 0}</span></div><span>{formatPostDate(p.createdAt)}</span></div></div>))}<div className="text-center pt-2"><Link href={tenantPath("/comunidade")} className="text-[10px] text-emerald-500 font-bold hover:underline">Ver Mais na Comunidade</Link></div></div>
                        ) : <div className="text-center text-zinc-600 text-xs py-4">Nenhum post recente.</div>
                    )}

                    {/* EVENTOS */}
                    {activeTab === 'eventos' && (
                        myEvents.length > 0 ? (
                            <div className="grid grid-cols-2 gap-3 animate-in fade-in">{myEvents.map(e => (<Link href={tenantPath(`/eventos/${e.id}`)} key={e.id} className="group flex flex-col bg-zinc-900 border border-zinc-800 rounded-2xl overflow-hidden hover:border-emerald-500/50 transition-all shadow-lg hover:shadow-emerald-500/10"><div className="h-28 w-full bg-zinc-800 relative overflow-hidden"><Image src={e.imagem || "https://placehold.co/600x400/111/333?text=Evento"} alt={e.titulo} fill sizes="(max-width: 768px) 50vw, 220px" className="object-cover opacity-80 group-hover:opacity-100 group-hover:scale-110 transition-all duration-500" style={{ objectPosition: `50% ${e.imagePositionY || 50}%` }}/><div className="absolute inset-0 bg-gradient-to-t from-zinc-950 via-transparent to-transparent"/><div className="absolute bottom-2 left-2 right-2"><p className="text-[10px] font-black text-white uppercase truncate drop-shadow-md">{e.titulo}</p></div></div><div className="p-2 flex items-center justify-between bg-zinc-950"><div className="flex items-center gap-1 text-[9px] text-zinc-400 font-bold uppercase"><Calendar size={10} className="text-emerald-500"/><span>{e.data || "Data à definir"}</span></div><div className="w-1.5 h-1.5 rounded-full bg-emerald-500 shadow-[0_0_5px_#10b981]"></div></div></Link>))}</div>
                        ) : <div className="text-center text-zinc-600 text-xs py-4">Nenhum evento marcado.</div>
                    )}

                    {/* LIGAS */}
                    {activeTab === 'ligas' && (
                        myLigas.length > 0 ? (
                            <div className="grid grid-cols-3 gap-4 animate-in fade-in">
                                {myLigas.map(l => (
                                    <Link href={tenantPath("/ligas_usc")} key={l.id} className="flex flex-col items-center gap-2 group">
                                        <div className="w-24 h-24 rounded-full bg-black border-2 border-zinc-800 p-0.5 group-hover:border-emerald-500 group-hover:scale-105 transition-all shadow-lg">
                                            <div className="w-full h-full rounded-full overflow-hidden bg-zinc-900 flex items-center justify-center relative">
                                                {l.logo || l.foto ? (
                                                    <Image src={l.logo || l.foto || ""} alt={l.sigla || "Liga"} fill sizes="96px" className="object-cover" />
                                                ) : (
                                                    <Users size={32} className="text-zinc-500"/>
                                                )}
                                            </div>
                                        </div>
                                        <span className="text-[10px] font-bold text-zinc-400 group-hover:text-white uppercase tracking-wider text-center line-clamp-1 w-full">{l.sigla || "Liga"}</span>
                                    </Link>
                                ))}
                            </div>
                        ) : <div className="text-center text-zinc-600 text-xs py-4">Não participa de ligas.</div>
                    )}

                    {/* TREINOS */}
                    {activeTab === 'treinos' && (
                        myTreinos.length > 0 ? (
                             <div className="grid gap-3 animate-in fade-in">
                                {myTreinos.map(t => (
                                    <Link href={tenantPath(`/treinos/${t.id}`)} key={t.id} className="group flex items-center bg-zinc-900 border border-zinc-800 rounded-2xl overflow-hidden hover:border-emerald-500/50 transition-all shadow-lg h-24">
                                            <div className="w-24 h-full bg-zinc-800 relative overflow-hidden shrink-0">
                                                 <Image src={t.imagem || "https://placehold.co/400x400/111/333?text=Treino"} alt={t.modalidade} fill sizes="96px" className="object-cover opacity-80 group-hover:opacity-100 group-hover:scale-110 transition-all duration-500"/>
                                                 <div className="absolute inset-0 bg-gradient-to-r from-transparent to-zinc-900"/>
                                            </div>
                                            <div className="flex-1 p-3 flex flex-col justify-center">
                                                <div className="flex justify-between items-start mb-1">
                                                    <p className="text-sm font-black text-white uppercase truncate">{t.modalidade}</p>
                                                    <div className="bg-emerald-500/10 text-emerald-500 px-2 py-0.5 rounded text-[8px] font-black uppercase flex items-center gap-1"><CheckCircle size={8}/> Eu Vou</div>
                                                </div>
                                                <div className="flex flex-col gap-1 text-[10px] text-zinc-400 font-bold uppercase">
                                                    <span className="flex items-center gap-1.5"><Calendar size={10} className="text-emerald-500"/> {t.dia}</span>
                                                    <span className="flex items-center gap-1.5"><Clock size={10} className="text-emerald-500"/> {t.horario}</span>
                                                    <span className="flex items-center gap-1.5"><MapPin size={10} className="text-emerald-500"/> {t.local}</span>
                                                </div>
                                            </div>
                                    </Link>
                                ))}
                             </div>
                        ) : <div className="text-center text-zinc-600 text-xs py-4">Nenhum treino confirmado.</div>
                    )}

                    {activeTab === 'apadrinhamento' && (
                        <div className="animate-in fade-in space-y-4">
                            <div className="grid gap-4 md:grid-cols-2">
                                {[{
                                  title: mentorshipLabels.mentorLabel,
                                  item: mentorshipBundle?.mentor,
                                  inviteMode: "mentor" as const,
                                  requestStatus: mentorshipBundle?.viewerMentorRequestStatus || "none",
                                }, {
                                  title: mentorshipLabels.menteeLabel,
                                  item: mentorshipBundle?.mentee,
                                  inviteMode: "mentee" as const,
                                  requestStatus: mentorshipBundle?.viewerMenteeRequestStatus || "none",
                                }].map((section) => (
                                  <div key={section.title} className="rounded-3xl border border-zinc-800 bg-zinc-900/70 p-5 text-center">
                                      <p className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">
                                        {section.item?.roleLabel || section.title}
                                      </p>
                                      {section.item ? (
                                        <div className="mt-4">
                                          <Link href={tenantPath(`/perfil/${section.item.user.uid}`)} className="block">
                                            <div className="mx-auto relative h-32 w-32 overflow-hidden rounded-full border-4 border-zinc-800 bg-zinc-950 shadow-2xl">
                                                <Image
                                                  src={section.item.user.foto || "https://github.com/shadcn.png"}
                                                  alt={section.item.user.nome}
                                                  fill
                                                  sizes="128px"
                                                  className="object-cover"
                                                />
                                            </div>
                                            <p className="mt-4 text-base font-black uppercase text-white">{section.item.user.nome}</p>
                                            <p className="text-[11px] font-bold uppercase text-zinc-500">{section.item.user.turma || "Sem turma"}</p>
                                            <p className="mt-2 text-[11px] font-bold text-emerald-300">Abrir perfil</p>
                                          </Link>
                                          {isOwnProfile ? (
                                            <div className="mt-4 flex flex-wrap justify-center gap-2">
                                              {resolveMentorshipRoleOptions(
                                                mentorshipLabels,
                                                section.item!.ownerRoleSide
                                              ).length > 1 ? (
                                                <div className="w-full text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">
                                                  Meu rotulo
                                                </div>
                                              ) : null}
                                              {resolveMentorshipRoleOptions(
                                                mentorshipLabels,
                                                section.item!.ownerRoleSide
                                              ).length > 1
                                                ? resolveMentorshipRoleOptions(
                                                    mentorshipLabels,
                                                    section.item!.ownerRoleSide
                                                  ).map((option) => (
                                                      <button
                                                        key={option}
                                                        type="button"
                                                        onClick={() =>
                                                          void handleEditMentorshipLabel(
                                                            section.item!.relationshipId,
                                                            section.item!.ownerRoleSide,
                                                            option
                                                          )
                                                        }
                                                        disabled={
                                                          editingMentorshipId === section.item!.relationshipId ||
                                                          option === section.item!.ownerRoleLabel
                                                        }
                                                        className={`inline-flex items-center gap-2 rounded-full border px-4 py-2 text-[11px] font-black uppercase disabled:opacity-60 ${
                                                          option === section.item!.ownerRoleLabel
                                                            ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-300"
                                                            : "border-zinc-700 bg-zinc-900 text-zinc-300 hover:border-zinc-500 hover:text-white"
                                                        }`}
                                                      >
                                                        {editingMentorshipId === section.item!.relationshipId &&
                                                        option !== section.item!.ownerRoleLabel ? (
                                                          <Loader2 size={14} className="animate-spin" />
                                                        ) : (
                                                          <HeartHandshake size={14} />
                                                        )}
                                                        {option}
                                                      </button>
                                                    ))
                                                : null}
                                              <button
                                                type="button"
                                                onClick={() => void handleRemoveMentorship(section.item!.relationshipId)}
                                                disabled={removingMentorshipId === section.item!.relationshipId}
                                                className="inline-flex items-center gap-2 rounded-full border border-red-500/30 bg-red-500/10 px-4 py-2 text-[11px] font-black uppercase text-red-300 hover:bg-red-500/20 disabled:opacity-60"
                                              >
                                                {removingMentorshipId === section.item.relationshipId ? (
                                                  <Loader2 size={14} className="animate-spin" />
                                                ) : (
                                                  <Trash2 size={14} />
                                                )}
                                                Remover
                                              </button>
                                            </div>
                                          ) : null}
                                        </div>
                                      ) : (
                                        <div className="mt-4 flex flex-col items-center">
                                            <div className="relative flex h-32 w-32 items-center justify-center rounded-full border border-dashed border-zinc-800 bg-black/20">
                                              {isOwnProfile ? (
                                                <Link
                                                  href={tenantPath(`/configuracoes/apadrinhamento?tipo=${section.inviteMode}`)}
                                                  className="inline-flex h-11 w-11 items-center justify-center rounded-full border border-emerald-500/30 bg-emerald-500/10 text-emerald-300 transition hover:bg-emerald-500/20"
                                                  title="Adicionar"
                                                >
                                                  <Plus size={20} />
                                                </Link>
                                              ) : user?.uid && effectiveMentorshipTenantId ? (
                                                <button
                                                  type="button"
                                                  onClick={() => void handleSendMentorshipInvite(section.inviteMode)}
                                                  disabled={sendingMentorshipMode.length > 0 || section.requestStatus !== "none"}
                                                  className="inline-flex h-11 w-11 items-center justify-center rounded-full border border-emerald-500/30 bg-emerald-500/10 text-emerald-300 transition hover:bg-emerald-500/20 disabled:cursor-not-allowed disabled:opacity-50"
                                                  title={
                                                    section.requestStatus === "none"
                                                      ? "Adicionar"
                                                      : section.requestStatus === "pending"
                                                      ? "Convite pendente"
                                                      : "Vinculo existente"
                                                  }
                                                >
                                                  {sendingMentorshipMode === section.inviteMode ? (
                                                    <Loader2 size={18} className="animate-spin" />
                                                  ) : (
                                                    <Plus size={20} />
                                                  )}
                                                </button>
                                              ) : null}
                                            </div>
                                            {section.requestStatus !== "none" && !isOwnProfile ? (
                                              <p className="mt-3 text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">
                                                {section.requestStatus === "pending" ? "Convite pendente" : "Vinculo existente"}
                                              </p>
                                            ) : null}
                                        </div>
                                      )}
                                  </div>
                                ))}
                            </div>
                        </div>
                    )}

                    {activeTab === 'mini_vendor' && (
                        miniVendorProfile ? (
                            <div className="animate-in fade-in rounded-3xl border border-zinc-800 bg-zinc-900/70 p-6 text-center">
                                <p className="text-[10px] font-black uppercase tracking-[0.18em] text-cyan-300">Mini Vendor</p>
                                <div className="mx-auto mt-5 relative h-32 w-32 overflow-hidden rounded-full border-4 border-zinc-800 bg-zinc-950 shadow-2xl">
                                    <Image
                                      src={miniVendorProfile.logoUrl || profile.foto || "https://github.com/shadcn.png"}
                                      alt={miniVendorProfile.storeName || "Mini Vendor"}
                                      fill
                                      sizes="128px"
                                      className="object-cover"
                                    />
                                </div>
                                <p className="mt-4 text-lg font-black uppercase text-white">{miniVendorProfile.storeName || "Mini Vendor"}</p>
                                <p className="text-[11px] font-bold uppercase text-zinc-500">{profile.turma || "Sem turma"}</p>
                                <p className="mt-3 text-sm text-zinc-400">{miniVendorProfile.description || "Loja mini vendor ativa neste perfil."}</p>
                                <div className="mt-4 flex justify-center">
                                    <Link href={tenantPath(`/perfil/mini-vendor/${miniVendorProfile.id}`)} className="inline-flex items-center gap-2 rounded-full border border-cyan-500/30 bg-cyan-500/10 px-4 py-2 text-[11px] font-black uppercase text-cyan-300 hover:bg-cyan-500/20">
                                        <Store size={14} />
                                        Abrir Mini Vendor
                                    </Link>
                                </div>
                            </div>
                        ) : miniVendorHiddenByOwner ? (
                            <div className="rounded-3xl border border-dashed border-zinc-800 bg-zinc-900/40 p-6 text-center text-sm text-zinc-400">
                                {isOwnProfile
                                  ? "Sua Mini Vendor está oculta nesta aba enquanto o perfil público da loja estiver desligado."
                                  : "Essa Mini Vendor esta oculta neste perfil no momento."}
                            </div>
                        ) : (
                            <div className="text-center text-zinc-600 text-xs py-4">Esse perfil ainda não criou Mini Vendor público.</div>
                        )
                    )}
                </div>
            </div>
            ) : null}

            {/* FICHA TÉCNICA */}
            {!profileLockedByAlbum ? (
            <div className="w-full max-w-sm mt-8 border-t border-zinc-800 pt-6">
                <h3 className="text-xs font-black text-zinc-500 uppercase tracking-widest pl-2 border-l-2 border-zinc-500 mb-3">Ficha Técnica</h3>
                <div className="grid grid-cols-2 gap-3">
                    <div className="bg-zinc-900 border border-zinc-800 p-3 rounded-xl flex items-center gap-3"><div className="p-2 bg-zinc-800 rounded-lg text-emerald-500"><MapPin size={16}/></div><div><p className="text-[9px] text-zinc-500 uppercase font-bold">Origem</p><p className="text-xs font-bold text-white">{profile.cidadeOrigem || "N/A"}</p></div></div>
                    <div className="bg-zinc-900 border border-zinc-800 p-3 rounded-xl flex items-center gap-3"><div className="p-2 bg-zinc-800 rounded-lg text-emerald-500"><Heart size={16}/></div><div><p className="text-[9px] text-zinc-500 uppercase font-bold">Status</p><div className="flex items-center gap-1"><p className="text-xs font-bold text-white uppercase truncate max-w-[80px]">{showRelacionamento ? (profile.statusRelacionamento || "N/A") : "Privado"}</p>{!showRelacionamento && !isOwnProfile && <Lock size={10} className="text-zinc-600"/>}{profile.relacionamentoPublico === false && isOwnProfile && <Lock size={10} className="text-zinc-500"/>}</div></div></div>
                    {(isOwnProfile || profile.signoPublico) && profileSign ? (
                      <div className="bg-zinc-900 border border-zinc-800 p-3 rounded-xl flex items-center gap-3 col-span-2">
                        <div className="p-2 bg-zinc-800 rounded-lg text-violet-300"><Sparkles size={16}/></div>
                        <div className="min-w-0">
                          <p className="text-[9px] text-zinc-500 uppercase font-bold">Signo</p>
                          <p className="text-xs font-bold text-white uppercase">
                            {profileSign.emoji} {profileSign.label} · {ZODIAC_ELEMENT_EMOJI[profileSign.element]} {profileSign.element}
                            {profileAscendant && (isOwnProfile || profile.ascendentePublico) ? ` · Asc. ${profileAscendant.emoji} ${profileAscendant.label}` : ""}
                          </p>
                        </div>
                      </div>
                    ) : null}
                    {profile.pets && (<div className="bg-zinc-900 border border-zinc-800 p-3 rounded-xl flex items-center gap-3 col-span-2"><div className="p-2 bg-zinc-800 rounded-lg text-emerald-500"><PawPrint size={16}/></div><div><p className="text-[9px] text-zinc-500 uppercase font-bold">Mascote</p><p className="text-xs font-bold text-white uppercase">{profile.pets}</p></div></div>)}
                    {specialPlaceBadges.length > 0 ? (
                      <div className="col-span-2 rounded-xl border border-zinc-800 bg-zinc-900 p-3">
                        <div className="mb-2 flex items-center gap-2">
                          <span className="rounded-lg bg-zinc-800 p-2 text-amber-300"><MapPin size={14} /></span>
                          <p className="text-[9px] font-bold uppercase text-zinc-500">Lugares</p>
                        </div>
                        <div className="flex flex-wrap gap-2">
                          {specialPlaceBadges.map((place) => (
                            <span key={place.id} className="rounded-lg border border-amber-500/20 bg-amber-500/10 px-2.5 py-1.5 text-[10px] font-black uppercase text-amber-200">
                              <span className="mr-1 text-xs">{place.icon}</span>{place.label}
                            </span>
                          ))}
                        </div>
                      </div>
                    ) : null}
                    {foodBadges.length > 0 ? (
                      <div className="col-span-2 rounded-xl border border-zinc-800 bg-zinc-900 p-3">
                        <div className="mb-2 flex items-center gap-2">
                          <span className="rounded-lg bg-zinc-800 p-2 text-orange-300"><Utensils size={14} /></span>
                          <p className="text-[9px] font-bold uppercase text-zinc-500">Comidas</p>
                        </div>
                        <div className="flex flex-wrap gap-2">
                          {foodBadges.map((food) => (
                            <span key={food.id} className="rounded-lg border border-orange-500/20 bg-orange-500/10 px-2.5 py-1.5 text-[10px] font-black uppercase text-orange-200">
                              <span className="mr-1 text-xs">{food.icon}</span>{food.label}
                            </span>
                          ))}
                        </div>
                      </div>
                    ) : null}
                    {musicBadges.length > 0 ? (
                      <div className="col-span-2 rounded-xl border border-zinc-800 bg-zinc-900 p-3">
                        <div className="mb-2 flex items-center gap-2">
                          <span className="rounded-lg bg-zinc-800 p-2 text-cyan-200"><Music size={14} /></span>
                          <p className="text-[9px] font-bold uppercase text-zinc-500">Músicas</p>
                        </div>
                        <div className="flex flex-wrap gap-2">
                          {musicBadges.map((music) => (
                            <span key={music.id} className="rounded-lg border border-cyan-400/20 bg-cyan-500/10 px-2.5 py-1.5 text-[10px] font-black uppercase text-cyan-100">
                              <span className="mr-1 text-xs">{music.icon}</span>{music.label}
                            </span>
                          ))}
                        </div>
                      </div>
                    ) : null}
                    {colorPreference ? (
                      <div className="col-span-2 flex items-center gap-3 rounded-xl border border-zinc-800 bg-zinc-900 p-3">
                        <div className="rounded-lg bg-zinc-800 p-2 text-pink-300"><Palette size={16}/></div>
                        <div className="min-w-0">
                          <p className="text-[9px] font-bold uppercase text-zinc-500">Cor preferida</p>
                          <div className="mt-1 flex items-center gap-2">
                            <span className="h-4 w-4 rounded-full border border-white/30" style={{ backgroundColor: colorPreference.hex }} />
                            <p className="text-xs font-bold uppercase text-white">{colorPreference.label}</p>
                          </div>
                        </div>
                      </div>
                    ) : null}
                </div>
                {showOwnHoroscope && astroStats && profileSign ? (
                    <div className="mt-4 rounded-2xl border border-violet-500/20 bg-zinc-900 p-3">
                        <p className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">Horóscopo</p>
                        <div className="mt-2 grid grid-cols-2 gap-2">
                            <div className="rounded-xl border border-zinc-800 bg-black/30 p-2.5">
                                <p className="text-[9px] font-black uppercase text-zinc-500">Mesmo signo</p>
                                <p className="mt-0.5 text-base font-black text-white">{astroStats.sameSignPercent}%</p>
                            </div>
                            <div className="rounded-xl border border-zinc-800 bg-black/30 p-2.5">
                                <p className="text-[9px] font-black uppercase text-zinc-500">{ZODIAC_ELEMENT_EMOJI[profileSign.element]} Mesmo elemento</p>
                                <p className="mt-0.5 text-base font-black text-white">{sameElementPercent}%</p>
                            </div>
                        </div>
                    </div>
                ) : null}
                {showOtherHoroscope ? (
                    <div className="mt-4 rounded-2xl border border-violet-500/20 bg-zinc-900 p-3">
                        <div className="flex items-center justify-between gap-3">
                          <p className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">Horóscopo</p>
                          <p className="text-[9px] font-black uppercase text-violet-200">Crushs</p>
                        </div>
                        <div className="mt-2 flex flex-wrap gap-2">
                          {compatibilityMatches.map((match) => (
                            <span key={match.reason} className="rounded-lg border border-violet-500/25 bg-violet-500/10 px-2.5 py-1.5 text-[10px] font-black uppercase text-violet-100">
                              {match.label}
                            </span>
                          ))}
                        </div>
                    </div>
                ) : null}
                {profile.esportes && profile.esportes.length > 0 && (
                    <div className="pt-4"><h3 className="text-xs font-black text-zinc-500 uppercase tracking-widest pl-2 border-l-2 border-blue-500 mb-3">Modalidades</h3><div className="flex flex-wrap gap-2">{profile.esportes.map((sport, i) => { const info = resolveSportInfo(sport); return <span key={i} className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-[10px] font-black uppercase tracking-wide border border-white/5 shadow-sm ${info.color}`}><span className="text-sm">{info.emoji}</span> {info.label}</span>; })}</div></div>
                )}
            </div>
            ) : null}
        </div>
      </div>
      {activeModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/90 p-4 backdrop-blur-sm animate-in fade-in">
              <div className="bg-zinc-950 w-full max-w-sm rounded-3xl border border-zinc-800 overflow-hidden shadow-2xl flex flex-col max-h-[80vh]">
                  <div className="p-4 border-b border-zinc-800 flex justify-between items-center bg-zinc-900/50">
                      <h3 className="text-sm font-bold text-white uppercase flex items-center gap-2">{activeModal === 'followers' ? <Users size={16} className="text-emerald-500"/> : <UserCheck size={16} className="text-blue-500"/>} {activeModal === 'followers' ? `Seguidores (${followersCount})` : `Seguindo (${followingCount})`}</h3>
                      <button onClick={() => setActiveModal(null)} className="p-1 text-zinc-500 hover:text-white"><X size={20}/></button>
                  </div>
                  <div className="flex-1 overflow-y-auto p-2 space-y-1">
                      {loadingFollowList ? (
                        <div className="py-10 text-center text-zinc-500">
                          <Loader2 size={26} className="mx-auto mb-2 animate-spin text-emerald-400" />
                          <p className="text-xs font-bold uppercase">Carregando perfis...</p>
                        </div>
                      ) : activeFollowList.length === 0 ? (
                        <div className="text-center py-10 text-zinc-600"><Ghost size={32} className="mx-auto mb-2 opacity-50"/><p className="text-xs">Nada por aqui.</p></div>
                      ) : (
                        <>
                          {activeFollowList.map(f => (<Link href={tenantPath(`/perfil/${f.uid}`)} key={f.uid} onClick={() => setActiveModal(null)} className="flex items-center gap-3 p-3 rounded-xl hover:bg-zinc-900 transition border border-transparent hover:border-zinc-800"><div className="w-10 h-10 rounded-full bg-black overflow-hidden border border-zinc-700 relative"><Image src={f.foto || "https://github.com/shadcn.png"} alt={f.nome} fill sizes="40px" className="object-cover" /></div><div><p className="text-sm font-bold text-white">{f.nome}</p><p className="text-[10px] text-zinc-500 font-bold uppercase">{f.turma || "Bicho"}</p></div></Link>))}
                          {canLoadMoreFollowList ? (
                            <button
                              type="button"
                              onClick={() => void handleLoadMoreFollowList()}
                              disabled={loadingMoreFollowList}
                              className="mt-2 w-full rounded-xl border border-zinc-800 bg-zinc-900 px-3 py-2 text-[10px] font-black uppercase text-zinc-300 transition hover:border-emerald-400/40 hover:text-white disabled:cursor-not-allowed disabled:opacity-60"
                            >
                              {loadingMoreFollowList ? "Carregando..." : "Carregar mais 20"}
                            </button>
                          ) : null}
                        </>
                      )}
                  </div>
              </div>
          </div>
      )}
      {profileVisibilityModalOpen ? (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/90 p-4 backdrop-blur-sm animate-in fade-in">
              <div className="w-full max-w-sm rounded-3xl border border-zinc-800 bg-zinc-950 p-5 shadow-2xl">
                  <div className="flex items-center gap-3">
                      <div className="rounded-2xl border border-amber-400/30 bg-amber-400/10 p-3 text-amber-200">
                          <EyeOff size={18} />
                      </div>
                      <div>
                          <p className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">
                            Privacidade do perfil
                          </p>
                          <h2 className="text-base font-black uppercase text-white">Ocultar perfil</h2>
                      </div>
                  </div>
                  <p className="mt-4 text-sm font-semibold leading-relaxed text-zinc-300">
                    Ao ocultar seu perfil, outras pessoas verão você como perfil invisível no app e no álbum da turma.
                  </p>
                  <p className="mt-3 rounded-2xl border border-amber-400/20 bg-amber-400/10 p-3 text-xs font-semibold leading-relaxed text-amber-100">
                    Enquanto essa opção estiver ativa, você também não poderá ver perfis de outras pessoas.
                  </p>
                  <div className="mt-5 grid grid-cols-2 gap-3">
                      <button
                        type="button"
                        onClick={() => setProfileVisibilityModalOpen(false)}
                        className="rounded-2xl border border-zinc-700 bg-zinc-900 px-4 py-3 text-[11px] font-black uppercase text-zinc-300 transition hover:bg-zinc-800"
                      >
                        Cancelar
                      </button>
                      <button
                        type="button"
                        onClick={() => void handleProfileVisibilityChange(false)}
                        disabled={updatingProfileVisibility}
                        className="rounded-2xl border border-amber-400/40 bg-amber-400/10 px-4 py-3 text-[11px] font-black uppercase text-amber-100 transition hover:bg-amber-400/20 disabled:cursor-not-allowed disabled:opacity-60"
                      >
                        {updatingProfileVisibility ? "Salvando..." : "Ocultar"}
                      </button>
                  </div>
              </div>
          </div>
      ) : null}
      {showAffinityModal ? (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/90 p-4 backdrop-blur-sm animate-in fade-in">
              <div className="bg-zinc-950 w-full max-w-sm rounded-3xl border border-zinc-800 overflow-hidden shadow-2xl flex flex-col max-h-[80vh]">
                  <div className="p-4 border-b border-zinc-800 flex justify-between items-center bg-zinc-900/50">
                      <h3 className="text-sm font-bold text-white uppercase flex items-center gap-2">
                        <Flame size={16} className="text-orange-400"/> Crushs
                      </h3>
                      <button onClick={() => setShowAffinityModal(false)} className="p-1 text-zinc-500 hover:text-white"><X size={20}/></button>
                  </div>
                  <div className="flex-1 overflow-y-auto p-2 space-y-1">
                      {loadingAffinityList ? (
                        <div className="py-10 text-center text-zinc-500">
                          <Loader2 size={26} className="mx-auto mb-2 animate-spin text-orange-400" />
                          <p className="text-xs font-bold uppercase">Carregando Crushs...</p>
                        </div>
                      ) : mutualAffinities.length === 0 ? (
                        <div className="text-center py-10 text-zinc-600">
                          <Flame size={32} className="mx-auto mb-2 opacity-50"/>
                          <p className="text-xs">Nenhum Crush confirmado ainda.</p>
                        </div>
                      ) : (
                        <>
                          {mutualAffinities.map((person) => (
                            <div key={person.uid} className="flex items-center gap-2 rounded-xl border border-transparent p-2 transition hover:border-zinc-800 hover:bg-zinc-900">
                              <Link href={tenantPath(`/perfil/${person.uid}`)} onClick={() => setShowAffinityModal(false)} className="flex min-w-0 flex-1 items-center gap-3">
                                <div className="relative h-10 w-10 overflow-hidden rounded-full border border-zinc-700 bg-black">
                                  <Image src={person.foto || "https://github.com/shadcn.png"} alt={person.nome} fill sizes="40px" className="object-cover" />
                                </div>
                                <div className="min-w-0">
                                  <p className="truncate text-sm font-bold text-white">{person.nome}</p>
                                  <p className="text-[10px] font-bold uppercase text-zinc-500">{person.turma || "Geral"} · 🔥</p>
                                </div>
                              </Link>
                              <button
                                type="button"
                                onClick={() => void handleRemoveAffinity(person.uid)}
                                disabled={removingAffinity}
                                className="inline-flex h-9 w-9 items-center justify-center rounded-full border border-red-500/20 bg-red-500/10 text-red-300 transition hover:bg-red-500/20 disabled:cursor-not-allowed disabled:opacity-50"
                                title="Remover Crush"
                              >
                                {removingAffinity ? <Loader2 size={14} className="animate-spin" /> : <Trash2 size={14} />}
                              </button>
                            </div>
                          ))}
                          {affinityNextOffset !== null ? (
                            <button
                              type="button"
                              onClick={() => void handleLoadMoreAffinity()}
                              disabled={loadingMoreAffinityList}
                              className="mt-2 w-full rounded-xl border border-zinc-800 bg-zinc-900 px-3 py-2 text-[10px] font-black uppercase text-zinc-300 transition hover:border-orange-400/40 hover:text-white disabled:cursor-not-allowed disabled:opacity-60"
                            >
                              {loadingMoreAffinityList ? "Carregando..." : "Carregar mais 20"}
                            </button>
                          ) : null}
                        </>
                      )}
                  </div>
              </div>
          </div>
      ) : null}
    </div>
  );
}


