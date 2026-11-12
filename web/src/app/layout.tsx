import type { Metadata } from "next";
import "./globals.css";
import { Inter } from "next/font/google";
import { cn } from "@/lib/utils";
import { AuthHydrator } from "@/app/components/auth/auth-hydrator";
import { QueryProvider } from "@/app/components/providers/query-provider";

const inter = Inter({subsets:['latin'],variable:'--font-sans'});

export const metadata: Metadata = { title: "Rikkei Jobs" };

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="vi" className={cn("font-sans", inter.variable)}>
      <body>
        <AuthHydrator />
        <QueryProvider>{children}</QueryProvider>
      </body>
    </html>
  );
}