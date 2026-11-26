import type { Metadata } from "next";
import "./globals.css";
import { Lexend } from "next/font/google";
import { cn } from "@/lib/utils";
import { AuthHydrator } from "@/app/components/auth/auth-hydrator";
import { QueryProvider } from "@/app/components/providers/query-provider";

/** Font chung Lexend (font biến thiên). Cần subset "vietnamese" để chữ có dấu không rơi về font dự phòng. */
const lexend = Lexend({ subsets: ["latin", "latin-ext", "vietnamese"], variable: "--font-sans", display: "swap" });

export const metadata: Metadata = { title: "Rikkei Jobs" };

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="vi" className={cn("font-sans", lexend.variable)}>
      <body>
        <QueryProvider><AuthHydrator />{children}</QueryProvider>
      </body>
    </html>
  );
}
