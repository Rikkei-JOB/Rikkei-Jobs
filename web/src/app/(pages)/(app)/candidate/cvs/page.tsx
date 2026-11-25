import { CvBuilder } from "@/app/components/cv/cv-builder"

/** HS-04: CV của ứng viên. Đăng nhập và vai trò do layout (app) kiểm; UT-01 trỏ "Tạo CV" tới đường dẫn này. */
export default function CandidateCvsPage() {
  return <div className="mx-auto max-w-7xl"><CvBuilder /></div>
}
