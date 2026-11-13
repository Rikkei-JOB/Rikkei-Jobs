import { SkillProfile } from "@/app/components/skills/skill-profile"
import { PersonalProfile } from "@/app/components/profile/personal-profile"
import { Educations } from "@/app/components/profile/educations"
import { Certificates } from "@/app/components/profile/certificates"

/**
 * Hồ sơ cá nhân HS-02 và hồ sơ năng lực kỹ năng HS-01 của ứng viên.
 * Đăng nhập và vai trò do layout (app) kiểm; đường dẫn tĩnh này được ưu tiên hơn trang tạm [role]/[...section].
 */
export default function CandidateProfilePage() {
  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <h1 className="text-xl font-semibold">Hồ sơ của tôi</h1>
      <PersonalProfile />
      <Educations />
      <Certificates />
      <SkillProfile />
    </div>
  )
}
