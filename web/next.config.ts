import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  distDir: process.env.PLAYWRIGHT_TEST === "1" ? ".next-playwright" : ".next",
  /**
   * Trang chủ của ứng viên là trang chủ tìm việc "/", và chỉ có một trang việc làm "/jobs".
   * Chuyển hướng ở server (307, tham số truy vấn được giữ nguyên) để chạy trước kiểm tra đăng nhập của layout (app).
   */
  async redirects() {
    return [
      { source: "/candidate", destination: "/", permanent: false },
      { source: "/candidate/jobs", destination: "/jobs", permanent: false },
      { source: "/candidate/jobs/:jobId", destination: "/jobs/:jobId", permanent: false },
    ];
  },
};
export default nextConfig;
