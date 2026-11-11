import type { ReactNode } from "react"

export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-6 bg-muted/30 p-4">
      <h1 className="text-lg font-semibold tracking-tight">Rikkei Jobs</h1>
      {children}
    </main>
  )
}
