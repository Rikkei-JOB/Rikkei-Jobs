import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  distDir: process.env.PLAYWRIGHT_TEST === "1" ? ".next-playwright" : ".next",
  /**
   * Trang chủ của ứng viên là trang chủ tìm việc "/"; trang việc làm chính là "/jobs".
   * Chuyển hướng ở server (307, tham số truy vấn được giữ nguyên) để chạy trước kiểm tra đăng nhập của layout (app).
   */
  async redirects() {
    // /candidate/jobs (UT-01) chỉ có dữ liệu khi cấu hình NEXT_PUBLIC_JOB_API_PATH (ví dụ fixture của e2e);
    // khi không cấu hình, trang đó chỉ là "Đang phát triển" nên chuyển sang bảng việc làm /jobs.
    const legacyJobs = process.env.NEXT_PUBLIC_JOB_API_PATH ? [] : [
      { source: "/candidate/jobs", destination: "/jobs", permanent: false },
      { source: "/candidate/jobs/:jobId", destination: "/jobs/:jobId", permanent: false },
    ];
    return [{ source: "/candidate", destination: "/", permanent: false }, ...legacyJobs];
  },
};
export default nextConfig;
