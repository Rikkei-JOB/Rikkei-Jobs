import type { Metadata } from "next";
import "./globals.css";
import { Inter } from "next/font/google";
import { cn } from "@/lib/utils";
import { AuthHydrator } from "@/app/components/auth/auth-hydrator";

const inter = Inter({subsets:['latin'],variable:'--font-sans'});

export const metadata: Metadata = { title: "Rikkei Jobs" };

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="vi" className={cn("font-sans", inter.variable)}>
      <body>
        <AuthHydrator />
        {children}
      </body>
    </html>
  );
}