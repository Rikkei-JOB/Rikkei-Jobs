import { SkillProfile } from "@/app/components/skills/skill-profile"

/**
 * Mục menu "Hồ sơ" của ứng viên (/candidate/profile): Hồ sơ năng lực kỹ năng (Skill Profile, HS-01).
 * Đăng nhập và vai trò do layout (app) kiểm; đường dẫn tĩnh này được ưu tiên hơn trang tạm [role]/[...section].
 */
export default function CandidateProfilePage() {
  return (
    <div className="mx-auto max-w-6xl">
      <SkillProfile />
    </div>
  )
}
