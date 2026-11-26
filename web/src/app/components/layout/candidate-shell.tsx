"use client";

import type { ReactNode } from "react";
import { PublicHeader } from "./public-header";
import { SiteFooter } from "./site-footer";
import "./candidate-shell.css";

/**
 * Khung các trang /candidate. Dùng chung header và footer với trang chủ, trang việc làm và trang công ty
 * (PublicHeader nhận ra ứng viên và hiện menu ứng viên), để chuyển giữa các trang không đổi kiểu header.
 */
export function CandidateShell({ children }: { children: ReactNode }) {
  return <div className="candidate-shell">
    <PublicHeader />
    <main className="candidate-main">{children}</main>
    <SiteFooter />
  </div>;
}
