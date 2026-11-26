import { CandidateSkills } from "@/app/components/skills/candidate-skills"
import { PersonalProfile } from "@/app/components/profile/personal-profile"
import { Educations } from "@/app/components/profile/educations"
import { Certificates } from "@/app/components/profile/certificates"
import { Languages } from "@/app/components/profile/languages"
import { EmploymentRecords } from "@/app/components/profile/employment-records"
import { Portfolio } from "@/app/components/profile/portfolio"
import { TaskBanner } from "@/app/components/ui/task-banner"

/**
 * Hồ sơ cá nhân HS-02, portfolio HS-03 và hồ sơ năng lực kỹ năng HS-01 của ứng viên.
 */
export default function CandidateProfilePage() {
  return (
    <div className="ptit-page candidate-brand max-w-6xl">
      <TaskBanner eyebrow="Hồ sơ năng lực cá nhân" title="Hồ sơ của tôi" illustration="applications"
        description="Hồ sơ đầy đủ giúp nhà tuyển dụng IT thấy kỹ năng đã xác thực từ LMS, dự án thực tế và quá trình đào tạo của bạn." />
      <PersonalProfile />
      <Educations />
      <Certificates />
      <Languages />
      <EmploymentRecords />
      <Portfolio />
      <CandidateSkills />
    </div>
  )
}
