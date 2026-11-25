"use client";

import * as React from "react";
import {
  CheckCircle2,
  AlertCircle,
  Calendar,
  ChevronDown,
  ChevronUp,
  RefreshCw,
  Award,
  Layers,
} from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { Card, CardContent, CardHeader } from "@/app/components/ui/card";
import { Alert } from "@/app/components/ui/alert";
import { skillService } from "@/services/skill.service";
import type { SkillProfileItem, SkillEvidence, SkillLevel, SkillSource } from "@/interface/skill";
import { getErrorMessage } from "@/services/api";

const LEVEL_CONFIG: Record<
  NonNullable<SkillLevel>,
  { label: string; badgeClass: string; barClass: string }
> = {
  EXCELLENT: {
    label: "Xuất sắc",
    badgeClass: "bg-purple-100 text-purple-800 border-purple-200 dark:bg-purple-950/60 dark:text-purple-300 dark:border-purple-800",
    barClass: "bg-purple-600",
  },
  GOOD: {
    label: "Tốt",
    badgeClass: "bg-blue-100 text-blue-800 border-blue-200 dark:bg-blue-950/60 dark:text-blue-300 dark:border-blue-800",
    barClass: "bg-blue-600",
  },
  FAIR: {
    label: "Khá",
    badgeClass: "bg-sky-100 text-sky-800 border-sky-200 dark:bg-sky-950/60 dark:text-sky-300 dark:border-sky-800",
    barClass: "bg-sky-500",
  },
  BASIC: {
    label: "Cơ bản",
    badgeClass: "bg-slate-100 text-slate-800 border-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:border-slate-700",
    barClass: "bg-slate-500",
  },
};

const SOURCE_LABEL: Record<SkillSource, { label: string; badgeClass: string }> = {
  LMS: {
    label: "LMS",
    badgeClass: "bg-blue-50 text-blue-700 border-blue-200 dark:bg-blue-950/50 dark:text-blue-300 dark:border-blue-800",
  },
  EXAM: {
    label: "Khảo thí (EXAM)",
    badgeClass: "bg-indigo-50 text-indigo-700 border-indigo-200 dark:bg-indigo-950/50 dark:text-indigo-300 dark:border-indigo-800",
  },
  INTERNSHIP: {
    label: "Thực tập (IOC/PM)",
    badgeClass: "bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-950/50 dark:text-emerald-300 dark:border-emerald-800",
  },
  MENTOR: {
    label: "Mentor đánh giá",
    badgeClass: "bg-teal-50 text-teal-700 border-teal-200 dark:bg-teal-950/50 dark:text-teal-300 dark:border-teal-800",
  },
  SELF: {
    label: "Tự khai",
    badgeClass: "bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-950/50 dark:text-amber-300 dark:border-amber-800",
  },
};

interface SkillProfileProps {
  userId?: string; // If provided, fetches public profile by userId. If omitted, fetches own skills.
  readOnly?: boolean;
  previewSkills?: SkillProfileItem[];
}

export function SkillProfile(props: SkillProfileProps) {
  return <SkillProfileContent key={props.userId ?? "me"} {...props} />;
}

function SkillProfileContent({ userId, readOnly = false, previewSkills }: SkillProfileProps) {
  const [loadedSkills, setSkills] = React.useState<SkillProfileItem[]>([]);
  const skills = previewSkills ?? loadedSkills;
  const [loading, setLoading] = React.useState(true);
  const [recomputing, setRecomputing] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [actionMessage, setActionMessage] = React.useState<{ type: "success" | "error"; text: string } | null>(null);
  const [expandedSkillIds, setExpandedSkillIds] = React.useState<Set<string>>(new Set());
  const [reloadKey, setReloadKey] = React.useState(0);

  React.useEffect(() => {
    if (previewSkills) return;
    let ignore = false;
    async function load() {
      try {
        const data = userId
          ? await skillService.getProfileSkills(userId)
          : await skillService.getOwnSkills();
        if (!ignore) {
          setSkills(data);
          setError(null);
        }
      } catch (err) {
        if (!ignore) {
          setError(getErrorMessage(err, "Không thể tải hồ sơ kỹ năng."));
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }
    void load();
    return () => {
      ignore = true;
    };
  }, [userId, reloadKey, previewSkills]);

  const handleRecompute = async () => {
    setRecomputing(true);
    setActionMessage(null);
    try {
      const updated = await skillService.recomputeSkills();
      setSkills(updated);
      setActionMessage({
        type: "success",
        text: "Đã tính lại chỉ số kỹ năng thành công từ các nguồn bằng chứng mới nhất.",
      });
    } catch (err) {
      setActionMessage({
        type: "error",
        text: getErrorMessage(err, "Tính lại chỉ số thất bại."),
      });
    } finally {
      setRecomputing(false);
    }
  };

  const toggleExpand = (skillId: string) => {
    setExpandedSkillIds((prev) => {
      const next = new Set(prev);
      if (next.has(skillId)) {
        next.delete(skillId);
      } else {
        next.add(skillId);
      }
      return next;
    });
  };

  // Group skills by group name
  const groupedSkills = React.useMemo(() => {
    const map = new Map<string, SkillProfileItem[]>();
    for (const item of skills) {
      const group = item.skill.group || "Chung";
      if (!map.has(group)) {
        map.set(group, []);
      }
      map.get(group)!.push(item);
    }
    return Array.from(map.entries());
  }, [skills]);

  // Overall statistics
  const stats = React.useMemo(() => {
    const total = skills.length;
    const verifiedCount = skills.filter((s) => s.verifiedScore !== null).length;
    const selfCount = skills.filter((s) => s.selfScore !== null).length;
    const totalEvidences = skills.reduce((acc, s) => acc + (s.evidences?.length || 0), 0);
    return { total, verifiedCount, selfCount, totalEvidences };
  }, [skills]);

  if (loading && !previewSkills) {
    return (
      <div className="space-y-4 py-8">
        <div className="flex items-center justify-between">
          <div className="h-6 w-48 animate-pulse rounded bg-muted" />
          <div className="h-9 w-32 animate-pulse rounded bg-muted" />
        </div>
        <div className="grid gap-4 md:grid-cols-2">
          {[1, 2, 3, 4].map((i) => (
            <Card key={i} className="animate-pulse">
              <CardHeader className="space-y-2">
                <div className="h-4 w-3/4 rounded bg-muted" />
                <div className="h-3 w-1/2 rounded bg-muted" />
              </CardHeader>
              <CardContent>
                <div className="h-8 rounded bg-muted" />
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <Alert variant="destructive" className="my-6">
        <div className="flex items-start justify-between gap-2">
          <div className="flex items-center gap-2">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span className="font-semibold">{error}</span>
          </div>
          <Button variant="outline" size="sm" onClick={() => { setLoading(true); setError(null); setReloadKey(value => value + 1); }}>
            Thử lại
          </Button>
        </div>
      </Alert>
    );
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h2 className="text-lg font-semibold">
              Hồ sơ năng lực kỹ năng
              <span className="font-normal text-muted-foreground"> · {stats.total}</span>
            </h2>
            <p className="text-xs text-muted-foreground">
              {stats.verifiedCount} đã xác thực · {stats.selfCount} có điểm tự đánh giá · {stats.totalEvidences} bằng chứng
            </p>
            <div className="mt-2 flex flex-wrap gap-4 text-[0.6875rem] text-muted-foreground">
              <span className="inline-flex items-center gap-1.5"><span className="h-1.5 w-3 rounded-full bg-gradient-to-r from-slate-500 via-blue-600 to-purple-600" aria-hidden />Điểm xác thực</span>
              <span className="inline-flex items-center gap-1.5"><span className="h-3 w-0.5 bg-foreground/60" aria-hidden />Tự đánh giá</span>
            </div>
          </div>
          {!readOnly && (
            <Button variant="outline" onClick={handleRecompute} disabled={recomputing} className="self-start sm:self-auto">
              <RefreshCw className={recomputing ? "animate-spin" : ""} aria-hidden />
              {recomputing ? "Đang tính lại..." : "Tính lại chỉ số"}
            </Button>
          )}
        </div>
      </CardHeader>

      <CardContent className="space-y-6">
        {actionMessage && (
          <Alert variant={actionMessage.type === "success" ? "success" : "destructive"}>
            <div className="flex items-center gap-2">
              {actionMessage.type === "success" ? <CheckCircle2 className="h-4 w-4 shrink-0" /> : <AlertCircle className="h-4 w-4 shrink-0" />}
              <span className="font-medium">{actionMessage.text}</span>
            </div>
          </Alert>
        )}

        {groupedSkills.length === 0 ? (
          <div className="rounded-xl border border-dashed p-8 text-center">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-muted">
              <Layers className="h-6 w-6 text-muted-foreground" />
            </div>
            <h3 className="mt-3 text-sm font-medium">Chưa có dữ liệu kỹ năng</h3>
            <p className="mt-1 text-xs text-muted-foreground">Hồ sơ chưa có kỹ năng hoặc kết quả học tập nào được đồng bộ.</p>
            {!readOnly && (
              <Button variant="outline" size="sm" onClick={handleRecompute} disabled={recomputing} className="mt-4">
                <RefreshCw aria-hidden />
                Đồng bộ ngay
              </Button>
            )}
          </div>
        ) : (
          groupedSkills.map(([groupName, groupItems]) => (
            <section key={groupName} aria-label={`Nhóm ${groupName}`}>
              <h3 className="mb-1 text-xs font-medium text-muted-foreground">
                {groupName} <span className="font-normal">· {groupItems.length}</span>
              </h3>
              <ul>
                {groupItems.map(item => (
                  <SkillRow key={item.skill.id} item={item} expanded={expandedSkillIds.has(item.skill.id)} onToggle={() => toggleExpand(item.skill.id)} />
                ))}
              </ul>
            </section>
          ))
        )}
      </CardContent>
    </Card>
  );
}

const SOURCE_SHORT: Record<SkillSource, string> = {
  LMS: "LMS", EXAM: "Khảo thí", INTERNSHIP: "Thực tập", MENTOR: "Mentor", SELF: "Tự khai",
};

function SkillRow({ item, expanded, onToggle }: { item: SkillProfileItem; expanded: boolean; onToggle: () => void }) {
  const verified = item.verifiedScore;
  const level = item.level ? LEVEL_CONFIG[item.level] : null;
  // Set: nguồn SELF từ bằng chứng và từ điểm tự khai chỉ hiện một chip.
  const sources = new Set<SkillSource>(item.evidences?.map(ev => ev.source) ?? []);
  if (item.selfScore !== null) sources.add("SELF");
  const clamp = (n: number) => Math.min(100, Math.max(0, n));
  const evidenceCount = item.evidences?.length ?? 0;
  return (
    <li data-testid="skill-row" className="border-t py-3 first:border-t-0">
      <div className="grid gap-3 sm:grid-cols-[11rem_minmax(0,1fr)_auto] sm:items-center">
        <div className="min-w-0">
          <p className="truncate text-sm font-medium" title={item.skill.code}>{item.skill.name}</p>
          {sources.size ? (
            <div className="mt-1 flex flex-wrap gap-1">
              {[...sources].map(source => (
                <span key={source} className={`rounded-full px-1.5 py-px text-[0.625rem] ${source === "SELF" ? "border border-dashed text-muted-foreground" : "bg-secondary text-secondary-foreground"}`}>
                  {SOURCE_SHORT[source]}
                </span>
              ))}
            </div>
          ) : null}
        </div>

        <div className="space-y-1">
          <div className="relative h-2 rounded-full bg-muted" role="img"
            aria-label={`${item.skill.name}: điểm xác thực ${verified?.toFixed(1) ?? "chưa có"}, tự đánh giá ${item.selfScore?.toFixed(1) ?? "chưa có"}`}>
            {verified !== null ? (
              <div className={`absolute inset-y-0 left-0 rounded-full transition-all duration-500 ${level?.barClass ?? "bg-primary"}`} style={{ width: `${clamp(verified)}%` }} />
            ) : null}
            {item.selfScore !== null ? (
              <div className="absolute -top-1 h-4 w-0.5 -translate-x-1/2 rounded bg-foreground/60" style={{ left: `${clamp(item.selfScore)}%` }} />
            ) : null}
          </div>
          <p className="flex flex-wrap gap-x-3 text-[0.6875rem] text-muted-foreground">
            {verified !== null ? <span>Xác thực <span className="font-semibold text-foreground">{verified.toFixed(1)}</span></span> : null}
            {item.selfScore !== null ? <span>Tự đánh giá <span className="font-semibold text-foreground">{item.selfScore.toFixed(1)}</span></span> : null}
            {verified === null && item.selfScore === null ? <span>Chưa có chỉ số</span> : null}
          </p>
        </div>

        <div className="flex items-center gap-2 sm:justify-end">
          {level ? (
            <span className={`rounded-full border px-2 py-0.5 text-[0.6875rem] font-medium ${level.badgeClass}`}>{level.label}</span>
          ) : (
            <span className="rounded-full bg-muted px-2 py-0.5 text-[0.6875rem] text-muted-foreground">Chưa xác thực</span>
          )}
          <button type="button" onClick={onToggle} aria-expanded={expanded}
            className="inline-flex items-center gap-1 rounded-md px-1.5 py-1 text-[0.6875rem] text-muted-foreground hover:bg-muted hover:text-foreground">
            <Award className="h-3.5 w-3.5" aria-hidden />
            Bằng chứng ({evidenceCount})
            {expanded ? <ChevronUp className="h-3.5 w-3.5" aria-hidden /> : <ChevronDown className="h-3.5 w-3.5" aria-hidden />}
          </button>
        </div>
      </div>

      {expanded ? (
        <div className="mt-3 grid gap-2 sm:grid-cols-2">
          {evidenceCount ? item.evidences.map((ev, idx) => <EvidenceItem key={`${ev.source}-${ev.recordedAt}-${idx}`} evidence={ev} />)
            : <p className="py-2 text-xs text-muted-foreground">Chưa có bằng chứng nào được ghi nhận.</p>}
        </div>
      ) : null}
    </li>
  );
}

function EvidenceItem({ evidence }: { evidence: SkillEvidence }) {
  const sourceInfo = SOURCE_LABEL[evidence.source] || {
    label: evidence.source,
    badgeClass: "bg-muted text-muted-foreground",
  };

  const formattedDate = React.useMemo(() => {
    try {
      const d = new Date(evidence.recordedAt);
      return d.toLocaleDateString("vi-VN", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
      });
    } catch {
      return evidence.recordedAt;
    }
  }, [evidence.recordedAt]);

  return (
    <div className="rounded-md border bg-muted/40 p-2.5 text-xs">
      <div className="flex items-start justify-between gap-2">
        <div className="font-medium text-foreground">{evidence.title}</div>
        <span
          className={`shrink-0 rounded px-1.5 py-0.5 text-[10px] font-medium border ${sourceInfo.badgeClass}`}
        >
          {sourceInfo.label}
        </span>
      </div>

      <div className="mt-2 flex flex-wrap items-center justify-between gap-2 text-[11px] text-muted-foreground">
        <div className="flex items-center gap-3">
          <span className="font-semibold text-foreground">
            Điểm: <span className="text-primary">{evidence.score}</span>/100
          </span>
          <span className="flex items-center gap-1">
            <Calendar className="h-3 w-3" />
            <span>Ngày: {formattedDate}</span>
          </span>
        </div>

        <div>
          {evidence.verified ? (
            <span className="inline-flex items-center gap-1 text-emerald-600 dark:text-emerald-400 font-medium">
              <CheckCircle2 className="h-3 w-3" />
              Đã xác thực
            </span>
          ) : (
            <span className="inline-flex items-center gap-1 text-amber-600 dark:text-amber-400">
              Tự khai báo
            </span>
          )}
        </div>
      </div>
    </div>
  );
}
