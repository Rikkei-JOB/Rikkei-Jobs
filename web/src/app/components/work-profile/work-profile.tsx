"use client";

import { useMemo, useState, type FormEvent, type KeyboardEvent } from "react";
import { AlertCircle, BadgeCheck, Briefcase, Building2, CheckCircle2, CircleDashed, Link2, Lock, Mail, Phone, Plus, Save, Sparkles, X } from "lucide-react";
import { Button } from "@/app/components/ui/button";
import { useAuthStore } from "@/hooks/use-auth-store";
import { useSaveWorkProfile, useWorkProfile } from "@/hooks/use-work-profile";
import { useQuery } from "@tanstack/react-query";
import { ApiError, getErrorMessage } from "@/services/api";
import { jobBoardService } from "@/services/job-board.service";
import type { WorkProfile, WorkProfilePayload } from "@/services/work-profile.service";
import { formatTime } from "@/app/components/mentor/mentor-utils";
import "@/app/components/mentor/mentor.css";

/** Số liệu nhỏ ở góc phải phần đầu hồ sơ; value null khi đang tải. */
export interface ProfileStat { label: string; value: number | string | null }

/** Khác biệt giữa các vai trò: nhãn vai trò, dòng giới thiệu vai trò và số liệu công việc. */
export interface WorkProfileRole {
  roleLabel: string;
  badge: string;
  stats: ProfileStat[];
  /** Gợi ý lấy từ danh mục kỹ năng (hợp với Mentor chấm kỹ thuật); không có thì dùng danh sách riêng hoặc không gợi ý. */
  suggestions?: "skills" | string[];
  bioPlaceholder?: string;
}

type FormValues = { title: string; organization: string; yearsExperience: string; phone: string; linkedinUrl: string; expertise: string[]; bio: string };
type FieldErrors = Partial<Record<keyof FormValues, string>>;

const MAX_EXPERTISE = 12;
const toForm = (p: WorkProfile | undefined): FormValues => ({
  title: p?.title ?? "", organization: p?.organization ?? "", yearsExperience: p?.yearsExperience?.toString() ?? "",
  phone: p?.phone ?? "", linkedinUrl: p?.linkedinUrl ?? "", expertise: p?.expertise ?? [], bio: p?.bio ?? "",
});

/** Kiểm tra giống backend (WorkProfileDtos) để báo lỗi ngay khi nhập. */
function validate(v: FormValues): FieldErrors {
  const errors: FieldErrors = {};
  if (v.title.length > 255) errors.title = "Chức danh tối đa 255 ký tự.";
  if (v.organization.length > 255) errors.organization = "Đơn vị công tác tối đa 255 ký tự.";
  if (v.yearsExperience && !/^\d{1,2}$/.test(v.yearsExperience.trim())) errors.yearsExperience = "Nhập số nguyên từ 0 đến 60.";
  else if (Number(v.yearsExperience) > 60) errors.yearsExperience = "Số năm kinh nghiệm tối đa 60.";
  if (v.phone && !/^0\d{9}$/.test(v.phone.trim())) errors.phone = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.";
  if (v.linkedinUrl && !/^https?:\/\/\S+$/.test(v.linkedinUrl.trim())) errors.linkedinUrl = "Liên kết phải bắt đầu bằng http:// hoặc https://.";
  if (v.bio.length > 3000) errors.bio = "Giới thiệu tối đa 3.000 ký tự.";
  return errors;
}

const toPayload = (v: FormValues): WorkProfilePayload => ({
  title: v.title.trim() || null, organization: v.organization.trim() || null,
  yearsExperience: v.yearsExperience.trim() ? Number(v.yearsExperience) : null,
  phone: v.phone.trim() || null, linkedinUrl: v.linkedinUrl.trim() || null, expertise: v.expertise, bio: v.bio.trim() || null,
});

/**
 * Trang hồ sơ nghề nghiệp dùng chung cho Mentor, Staff, Admin (GET/PUT /profiles/work/me).
 * Họ tên, email từ LMS chỉ hiển thị; phần nghề nghiệp sửa được.
 */
export function WorkProfilePage({ role }: { role: WorkProfileRole }) {
  const profile = useWorkProfile();
  // Giữ trạng thái lưu ở đây: lưu xong form được tạo lại (key theo updatedAt) mà vẫn hiện "Đã lưu".
  const save = useSaveWorkProfile();
  if (profile.error) return <div className="mtr-page"><div role="alert" className="mtr-panel p-5"><p>{getErrorMessage(profile.error)}</p>
    <Button variant="outline" className="mt-3" onClick={() => profile.refetch()}>Thử lại</Button></div></div>;
  if (profile.isPending) return <div className="mtr-page" role="status" aria-label="Đang tải hồ sơ">
    <div className="h-56 rounded-[16px] bg-muted motion-safe:animate-pulse" /><div className="h-96 rounded-[16px] bg-muted motion-safe:animate-pulse" /></div>;
  // key: lưu xong (updatedAt đổi) thì form nhận lại dữ liệu chuẩn hoá từ server.
  return <ProfileEditor key={profile.data.updatedAt ?? "new"} profile={profile.data} save={save} role={role} />;
}

function ProfileEditor({ profile, save, role }: { profile: WorkProfile; save: ReturnType<typeof useSaveWorkProfile>; role: WorkProfileRole }) {
  const user = useAuthStore(state => state.user);
  const catalog = useQuery({ queryKey: ["job-board", "skills"], queryFn: jobBoardService.skillCatalog, staleTime: 10 * 60_000, enabled: role.suggestions === "skills" });
  const initial = useMemo(() => toForm(profile), [profile]);
  const [values, setValues] = useState<FormValues>(initial);
  const [draftTag, setDraftTag] = useState("");
  const [serverErrors, setServerErrors] = useState<FieldErrors>({});

  // Lỗi server chỉ đè khi có nội dung; ô vừa sửa được gán undefined và không được xoá lỗi kiểm tra phía trình duyệt.
  const errors: FieldErrors = { ...validate(values) };
  for (const [key, message] of Object.entries(serverErrors)) if (message) errors[key as keyof FormValues] = message;
  const dirty = JSON.stringify(values) !== JSON.stringify(initial);
  const fullName = user?.fullName?.trim() || role.roleLabel;
  const initials = fullName.split(/\s+/).slice(-2).map(part => part[0]).join("").toLocaleUpperCase("vi-VN");

  const pool = role.suggestions === "skills" ? (catalog.data ?? []).map(skill => skill.name) : role.suggestions ?? [];
  const suggestions = pool
    .filter(name => !values.expertise.some(item => item.toLowerCase() === name.toLowerCase())).slice(0, 10);

  function set<K extends keyof FormValues>(key: K, value: FormValues[K]) {
    setValues(current => ({ ...current, [key]: value }));
    setServerErrors(current => ({ ...current, [key]: undefined }));
  }
  function addTag(raw: string) {
    const tag = raw.trim().slice(0, 50);
    if (!tag || values.expertise.length >= MAX_EXPERTISE || values.expertise.some(item => item.toLowerCase() === tag.toLowerCase())) return;
    set("expertise", [...values.expertise, tag]);
    setDraftTag("");
  }
  function onTagKey(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === "Enter" || event.key === ",") { event.preventDefault(); addTag(draftTag); }
    else if (event.key === "Backspace" && !draftTag && values.expertise.length) set("expertise", values.expertise.slice(0, -1));
  }
  function submit(event: FormEvent) {
    event.preventDefault();
    if (Object.values(validate(values)).some(Boolean)) return;
    save.mutate(toPayload(values), {
      onError: error => {
        if (error instanceof ApiError && error.errors && typeof error.errors === "object") setServerErrors(error.errors as FieldErrors);
      },
    });
  }

  const field = (key: keyof FormValues) => ({
    id: `wp-${key}`,
    "aria-invalid": Boolean(errors[key]) || undefined,
    "aria-describedby": errors[key] ? `wp-${key}-error` : undefined,
  });
  const err = (name: keyof FormValues) => errors[name] ? <p id={`wp-${name}-error`} className="mtr-form-error" role="alert">{errors[name]}</p> : null;

  return <form className="mtr-page" onSubmit={submit} noValidate>
    <section className="mtr-profile-hero" aria-label={`Hồ sơ ${role.roleLabel}`}>
      <div className="mtr-profile-cover" aria-hidden="true" />
      <div className="mtr-profile-head">
        <span className="mtr-profile-avatar" aria-hidden="true">{initials}</span>
        <div className="min-w-0 flex-1">
          <h1>{fullName}</h1>
          <p className="mtr-profile-sub">{[values.title || "Chưa cập nhật chức danh", values.organization].filter(Boolean).join(" · ")}</p>
          <div className="mtr-profile-tags">
            <span><BadgeCheck aria-hidden="true" />{role.badge}</span>
            {user?.email && <span><Mail aria-hidden="true" />{user.email}</span>}
            {values.yearsExperience && <span><Briefcase aria-hidden="true" />{values.yearsExperience} năm kinh nghiệm</span>}
          </div>
        </div>
        <dl className="mtr-profile-stats">
          {role.stats.map(stat => <div key={stat.label}><dt>{stat.label}</dt><dd>{stat.value ?? "–"}</dd></div>)}
        </dl>
      </div>
    </section>

    <div className="mtr-profile-grid">
      <section className="mtr-panel" aria-labelledby="wp-lms">
        <div className="mtr-panel-head"><h2 id="wp-lms"><Lock aria-hidden="true" />Thông tin tài khoản</h2></div>
        <div className="mtr-form">
          <p className="mtr-hint mt-0">Họ tên và email đồng bộ từ LMS mỗi lần đăng nhập. Hãy cập nhật tại LMS nếu cần thay đổi.</p>
          <div className="mtr-readonly"><span>Họ và tên</span><strong>{fullName}</strong></div>
          <div className="mtr-readonly"><span>Email</span><strong>{user?.email ?? "—"}</strong></div>
          <div className="mtr-readonly"><span>Vai trò</span><strong>{role.roleLabel}</strong></div>
        </div>
      </section>

      <section className="mtr-panel" aria-labelledby="wp-work">
        <div className="mtr-panel-head"><h2 id="wp-work"><Building2 aria-hidden="true" />Thông tin nghề nghiệp</h2></div>
        <div className="mtr-form">
          <div><label className="mtr-field-label" htmlFor="wp-title">Chức danh</label>
            <input {...field("title")} className="mtr-input" maxLength={255} value={values.title} placeholder="Ví dụ: Senior Backend Engineer" onChange={e => set("title", e.target.value)} />{err("title")}</div>
          <div><label className="mtr-field-label" htmlFor="wp-organization">Đơn vị công tác</label>
            <input {...field("organization")} className="mtr-input" maxLength={255} value={values.organization} placeholder="Công ty hoặc khoa đang công tác" onChange={e => set("organization", e.target.value)} />{err("organization")}</div>
          <div><label className="mtr-field-label" htmlFor="wp-yearsExperience">Số năm kinh nghiệm</label>
            <input {...field("yearsExperience")} className="mtr-input mtr-input-short" inputMode="numeric" value={values.yearsExperience} placeholder="0 – 60" onChange={e => set("yearsExperience", e.target.value.replace(/[^\d]/g, ""))} />{err("yearsExperience")}</div>
        </div>
      </section>

      <section className="mtr-panel" aria-labelledby="wp-contact">
        <div className="mtr-panel-head"><h2 id="wp-contact"><Phone aria-hidden="true" />Liên hệ</h2></div>
        <div className="mtr-form">
          <div><label className="mtr-field-label" htmlFor="wp-phone">Số điện thoại</label>
            <input {...field("phone")} className="mtr-input" type="tel" inputMode="tel" autoComplete="tel" maxLength={10} value={values.phone} placeholder="09xxxxxxxx" onChange={e => set("phone", e.target.value.replace(/[^\d]/g, ""))} />{err("phone")}</div>
          <div><label className="mtr-field-label" htmlFor="wp-linkedinUrl">LinkedIn hoặc trang cá nhân</label>
            <div className="mtr-input-icon"><Link2 aria-hidden="true" /><input {...field("linkedinUrl")} className="mtr-input" type="url" maxLength={500} value={values.linkedinUrl} placeholder="https://linkedin.com/in/..." onChange={e => set("linkedinUrl", e.target.value)} /></div>{err("linkedinUrl")}</div>
        </div>
      </section>

      <section className="mtr-panel" aria-labelledby="wp-expertise">
        <div className="mtr-panel-head"><h2 id="wp-expertise"><Sparkles aria-hidden="true" />Lĩnh vực chuyên môn</h2><span className="text-xs text-[var(--text-muted)]">{values.expertise.length}/{MAX_EXPERTISE}</span></div>
        <div className="mtr-form">
          <div className="mtr-tag-box">
            {values.expertise.map(tag => <span key={tag} className="mtr-tag">{tag}
              <button type="button" aria-label={`Xoá ${tag}`} onClick={() => set("expertise", values.expertise.filter(item => item !== tag))}><X aria-hidden="true" /></button></span>)}
            <input id="wp-expertise-input" aria-label="Thêm lĩnh vực chuyên môn" value={draftTag} maxLength={50}
              disabled={values.expertise.length >= MAX_EXPERTISE} placeholder={values.expertise.length >= MAX_EXPERTISE ? "Đã đủ 12 lĩnh vực" : "Nhập rồi nhấn Enter…"}
              onChange={e => setDraftTag(e.target.value)} onKeyDown={onTagKey} onBlur={() => draftTag && addTag(draftTag)} />
          </div>
          {suggestions.length > 0 && values.expertise.length < MAX_EXPERTISE && <div>
            <p className="mtr-hint mt-0 mb-2">Gợi ý từ danh mục kỹ năng:</p>
            <div className="mtr-suggest">{suggestions.map(name => <button key={name} type="button" onClick={() => addTag(name)}><Plus aria-hidden="true" />{name}</button>)}</div>
          </div>}
        </div>
      </section>

      <section className="mtr-panel mtr-span-2" aria-labelledby="wp-bio">
        <div className="mtr-panel-head"><h2 id="wp-bio"><BadgeCheck aria-hidden="true" />Giới thiệu bản thân</h2></div>
        <div className="mtr-form">
          <label className="mtr-field-label" htmlFor="wp-bio-input">Giới thiệu ngắn với doanh nghiệp và ứng viên <span className="mtr-counter">{values.bio.length}/3000</span></label>
          <textarea id="wp-bio-input" className="mtr-input" rows={5} maxLength={3000} value={values.bio}
            aria-invalid={Boolean(errors.bio) || undefined} aria-describedby={errors.bio ? "wp-bio-error" : undefined}
            placeholder={role.bioPlaceholder ?? "Kinh nghiệm, dự án tiêu biểu, lĩnh vực phụ trách…"} onChange={e => set("bio", e.target.value)} />{err("bio")}
        </div>
      </section>
    </div>

    <div className="mtr-savebar">
      <span className="mtr-save-state" role="status" data-tone={dirty ? "dirty" : save.isSuccess ? "saved" : undefined}>
        {dirty ? <><CircleDashed aria-hidden="true" />Có thay đổi chưa lưu</>
          : save.isSuccess && profile.updatedAt ? <><CheckCircle2 aria-hidden="true" />Đã lưu lúc {formatTime(profile.updatedAt)}</>
          : profile.updatedAt ? <>Cập nhật lần cuối lúc {formatTime(profile.updatedAt)}</> : <>Hồ sơ chưa được lưu lần nào</>}
      </span>
      {save.isError && !Object.keys(serverErrors).length && <span role="alert" className="flex items-center gap-1.5 text-sm text-destructive"><AlertCircle className="size-4" aria-hidden="true" />{getErrorMessage(save.error)}</span>}
      <div className="flex gap-2">
        {dirty && <Button type="button" variant="outline" disabled={save.isPending} onClick={() => { setValues(initial); setServerErrors({}); setDraftTag(""); }}>Huỷ thay đổi</Button>}
        <Button type="submit" disabled={!dirty || save.isPending || Object.values(errors).some(Boolean)}><Save aria-hidden="true" />{save.isPending ? "Đang lưu…" : "Lưu hồ sơ"}</Button>
      </div>
    </div>
  </form>;
}
