import { CandidateSkills } from "@/app/components/skills/candidate-skills"
import { PersonalProfile } from "@/app/components/profile/personal-profile"
import { Educations } from "@/app/components/profile/educations"
import { Certificates } from "@/app/components/profile/certificates"
import { Languages } from "@/app/components/profile/languages"
import { EmploymentRecords } from "@/app/components/profile/employment-records"
import { Portfolio } from "@/app/components/profile/portfolio"

/**
 * Hồ sơ cá nhân HS-02, portfolio HS-03 và hồ sơ năng lực kỹ năng HS-01 của ứng viên.
 */
export default function CandidateProfilePage() {
  return (
    <div className="ptit-page max-w-6xl">
      <div className="ptit-page-heading">
        <div>
          <p className="ptit-eyebrow">HỒ SƠ NĂNG LỰC CÁ NHÂN</p>
          <h1 className="ptit-page-title">Hồ sơ của tôi</h1>
          <p className="ptit-page-desc">
            Quản lý thông tin cá nhân, quá trình đào tạo, dự án thực tế và hồ sơ kỹ năng xác thực từ LMS.
          </p>
        </div>
      </div>
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
