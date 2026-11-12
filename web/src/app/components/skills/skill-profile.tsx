"use client";

import * as React from "react";
import {
  CheckCircle2,
  ShieldCheck,
  AlertCircle,
  Calendar,
  ChevronDown,
  ChevronUp,
  RefreshCw,
  Award,
  Layers,
  FileBadge,
} from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/app/components/ui/card";
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
    label: "Tự khai báo",
    badgeClass: "bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-950/50 dark:text-amber-300 dark:border-amber-800",
  },
};

interface SkillProfileProps {
  userId?: string; // If provided, fetches public profile by userId. If omitted, fetches own skills.
  readOnly?: boolean;
}

export function SkillProfile(props: SkillProfileProps) {
  return <SkillProfileContent key={props.userId ?? "me"} {...props} />;
}

function SkillProfileContent({ userId, readOnly = false }: SkillProfileProps) {
  const [skills, setSkills] = React.useState<SkillProfileItem[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [recomputing, setRecomputing] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [actionMessage, setActionMessage] = React.useState<{ type: "success" | "error"; text: string } | null>(null);
  const [expandedSkillIds, setExpandedSkillIds] = React.useState<Set<string>>(new Set());
  const [loadVersion, setLoadVersion] = React.useState(0);

  React.useEffect(() => {
    let active = true;
    const request = userId ? skillService.getProfileSkills(userId) : skillService.getOwnSkills();
    request.then(data => {
      if (active) { setSkills(data); setError(null); }
    }).catch(err => {
      if (active) setError(getErrorMessage(err, "Không thể tải hồ sơ kỹ năng."));
    }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [userId, loadVersion]);

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
    const selfCount = skills.filter((s) => s.verifiedScore === null && s.selfScore !== null).length;
    const totalEvidences = skills.reduce((acc, s) => acc + (s.evidences?.length || 0), 0);
    return { total, verifiedCount, selfCount, totalEvidences };
  }, [skills]);

  if (loading) {
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
          <Button variant="outline" size="sm" onClick={() => { setLoading(true); setError(null); setLoadVersion(value => value + 1); }}>
            Thử lại
          </Button>
        </div>
      </Alert>
    );
  }

  return (
    <div className="space-y-6">
      {/* Top Header & Actions */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-xl font-semibold tracking-tight text-foreground">
              Hồ sơ năng lực kỹ năng
            </h2>
            <span className="rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">
              {stats.total} kỹ năng
            </span>
          </div>
          <p className="mt-1 text-xs text-muted-foreground">
            Chỉ số 0–100 và bằng chứng thực tế từ LMS, Khảo thí, Dự án và Tự khai báo.
          </p>
        </div>

        {!readOnly && (
          <Button
            onClick={handleRecompute}
            disabled={recomputing}
            className="self-start sm:self-auto"
          >
            <RefreshCw className={`mr-2 h-4 w-4 ${recomputing ? "animate-spin" : ""}`} />
            {recomputing ? "Đang tính lại..." : "Tính lại chỉ số"}
          </Button>
        )}
      </div>

      {actionMessage && (
        <Alert variant={actionMessage.type === "success" ? "success" : "destructive"}>
          <div className="flex items-center gap-2">
            {actionMessage.type === "success" ? (
              <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-600 dark:text-emerald-400" />
            ) : (
              <AlertCircle className="h-4 w-4 shrink-0" />
            )}
            <span className="font-medium">{actionMessage.text}</span>
          </div>
        </Alert>
      )}

      {/* Summary KPI Cards */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <div className="rounded-lg border bg-card p-3 shadow-xs">
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <Layers className="h-3.5 w-3.5" />
            <span>Tổng kỹ năng</span>
          </div>
          <div className="mt-1 text-lg font-bold text-foreground">{stats.total}</div>
        </div>

        <div className="rounded-lg border bg-card p-3 shadow-xs">
          <div className="flex items-center gap-2 text-xs text-emerald-600 dark:text-emerald-400">
            <ShieldCheck className="h-3.5 w-3.5" />
            <span>Đã xác thực</span>
          </div>
          <div className="mt-1 text-lg font-bold text-emerald-600 dark:text-emerald-400">
            {stats.verifiedCount}
          </div>
        </div>

        <div className="rounded-lg border bg-card p-3 shadow-xs">
          <div className="flex items-center gap-2 text-xs text-amber-600 dark:text-amber-400">
            <FileBadge className="h-3.5 w-3.5" />
            <span>Tự khai</span>
          </div>
          <div className="mt-1 text-lg font-bold text-amber-600 dark:text-amber-400">
            {stats.selfCount}
          </div>
        </div>

        <div className="rounded-lg border bg-card p-3 shadow-xs">
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <Award className="h-3.5 w-3.5" />
            <span>Tổng bằng chứng</span>
          </div>
          <div className="mt-1 text-lg font-bold text-foreground">{stats.totalEvidences}</div>
        </div>
      </div>

      {/* Grouped Skills */}
      {groupedSkills.length === 0 ? (
        <Card className="p-8 text-center">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-muted">
            <Layers className="h-6 w-6 text-muted-foreground" />
          </div>
          <h3 className="mt-3 text-sm font-medium text-foreground">Chưa có dữ liệu kỹ năng</h3>
          <p className="mt-1 text-xs text-muted-foreground">
            Hồ sơ chưa có kỹ năng hoặc kết quả học tập nào được đồng bộ.
          </p>
          {!readOnly && (
            <Button
              variant="outline"
              size="sm"
              onClick={handleRecompute}
              disabled={recomputing}
              className="mt-4"
            >
              <RefreshCw className="mr-2 h-3.5 w-3.5" />
              Đồng bộ ngay
            </Button>
          )}
        </Card>
      ) : (
        <div className="space-y-8">
          {groupedSkills.map(([groupName, groupItems]) => (
            <section key={groupName} className="space-y-3">
              <div className="flex items-center justify-between border-b pb-2">
                <div className="flex items-center gap-2">
                  <h3 className="text-base font-semibold text-foreground">{groupName}</h3>
                  <span className="rounded-md bg-muted px-2 py-0.5 text-xs text-muted-foreground">
                    {groupItems.length}
                  </span>
                </div>
              </div>

              <div className="grid gap-4 md:grid-cols-2">
                {groupItems.map((item) => {
                  const isVerified = item.verifiedScore !== null;
                  const isSelfOnly = !isVerified && item.selfScore !== null;
                  const levelInfo = item.level ? LEVEL_CONFIG[item.level] : null;
                  const isExpanded = expandedSkillIds.has(item.skill.id);
                  const displayScore = isVerified
                    ? item.verifiedScore!
                    : item.selfScore !== null
                      ? item.selfScore
                      : 0;

                  return (
                    <Card
                      key={item.skill.id}
                      className="border shadow-xs transition-shadow hover:shadow-sm"
                    >
                      <CardHeader className="pb-3">
                        <div className="flex items-start justify-between gap-2">
                          <div>
                            <div className="flex items-center gap-2">
                              <CardTitle className="text-sm font-semibold text-foreground">
                                {item.skill.name}
                              </CardTitle>
                              <span className="font-mono text-[10px] text-muted-foreground">
                                [{item.skill.code}]
                              </span>
                            </div>
                            <CardDescription className="text-[11px]">
                              Nhóm: {item.skill.group}
                            </CardDescription>
                          </div>

                          <div className="flex flex-wrap items-center justify-end gap-1.5">
                            {/* Verification badge */}
                            {isVerified ? (
                              <span className="inline-flex items-center gap-1 rounded-md border border-emerald-200 bg-emerald-50 px-2 py-0.5 text-[11px] font-medium text-emerald-700 dark:border-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300">
                                <ShieldCheck className="h-3 w-3" />
                                Đã xác thực
                              </span>
                            ) : isSelfOnly ? (
                              <span className="inline-flex items-center gap-1 rounded-md border border-amber-200 bg-amber-50 px-2 py-0.5 text-[11px] font-medium text-amber-700 dark:border-amber-800 dark:bg-amber-950/60 dark:text-amber-300">
                                <FileBadge className="h-3 w-3" />
                                Tự khai
                              </span>
                            ) : null}

                            {/* Level badge */}
                            {levelInfo ? (
                              <span
                                className={`inline-flex items-center rounded-md border px-2 py-0.5 text-[11px] font-medium ${levelInfo.badgeClass}`}
                              >
                                {levelInfo.label}
                              </span>
                            ) : isSelfOnly ? (
                              <span className="inline-flex items-center rounded-md border border-muted bg-muted px-2 py-0.5 text-[11px] text-muted-foreground">
                                Chưa xếp bậc
                              </span>
                            ) : null}
                          </div>
                        </div>
                      </CardHeader>

                      <CardContent className="space-y-3 pt-0">
                        {/* Score Indicator & Progress Bar */}
                        <div className="space-y-1.5">
                          <div className="flex items-center justify-between text-xs">
                            <span className="text-muted-foreground">
                              {isVerified
                                ? "Chỉ số xác thực"
                                : isSelfOnly
                                  ? "Điểm tự đánh giá"
                                  : "Chưa có chỉ số"}
                            </span>
                            <span className="font-semibold text-foreground">
                              {isVerified ? (
                                <>
                                  <span className="text-sm font-bold text-primary">
                                    {item.verifiedScore!.toFixed(1)}
                                  </span>
                                  <span className="text-muted-foreground"> / 100</span>
                                </>
                              ) : isSelfOnly ? (
                                <>
                                  <span className="text-sm font-bold text-amber-600 dark:text-amber-400">
                                    {item.selfScore!.toFixed(1)}
                                  </span>
                                  <span className="text-muted-foreground"> / 100 (tự khai)</span>
                                </>
                              ) : (
                                <span className="text-muted-foreground">--</span>
                              )}
                            </span>
                          </div>

                          {/* Progress bar */}
                          <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
                            <div
                              className={`h-full transition-all duration-500 ${
                                isVerified
                                  ? levelInfo?.barClass || "bg-primary"
                                  : isSelfOnly
                                    ? "bg-amber-500"
                                    : "bg-muted-foreground/30"
                              }`}
                              style={{ width: `${Math.min(100, Math.max(0, displayScore))}%` }}
                            />
                          </div>

                          {!isVerified && isSelfOnly && (
                            <p className="text-[11px] text-amber-600 dark:text-amber-400">
                              * Kỹ năng tự khai không có chỉ số xác thực chính thức.
                            </p>
                          )}
                        </div>

                        {/* Evidence Accordion Toggle */}
                        <div className="border-t pt-2">
                          <button
                            type="button"
                            onClick={() => toggleExpand(item.skill.id)}
                            className="flex w-full items-center justify-between py-1 text-xs font-medium text-muted-foreground hover:text-foreground"
                          >
                            <span className="flex items-center gap-1.5">
                              <Award className="h-3.5 w-3.5 text-primary" />
                              <span>Bằng chứng ({item.evidences?.length || 0})</span>
                            </span>
                            {isExpanded ? (
                              <ChevronUp className="h-3.5 w-3.5" />
                            ) : (
                              <ChevronDown className="h-3.5 w-3.5" />
                            )}
                          </button>

                          {/* Expandable Evidences List */}
                          {isExpanded && (
                            <div className="mt-2 space-y-2 pt-1">
                              {item.evidences && item.evidences.length > 0 ? (
                                item.evidences.map((ev, idx) => (
                                  <EvidenceItem key={`${ev.source}-${ev.recordedAt}-${idx}`} evidence={ev} />
                                ))
                              ) : (
                                <p className="py-2 text-center text-xs text-muted-foreground">
                                  Chưa có bằng chứng nào được ghi nhận.
                                </p>
                              )}
                            </div>
                          )}
                        </div>
                      </CardContent>
                    </Card>
                  );
                })}
              </div>
            </section>
          ))}
        </div>
      )}
    </div>
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
