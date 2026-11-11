"use client"

import * as React from "react"
import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import { KeyRound, LogOut, Menu, X } from "lucide-react"
import { cn } from "cn"
import { Button } from "@/app/components/ui/button"
import { ROLE_MENU, type MenuItem } from "@/config/menu"
import { ROLE_HOME, ROLE_LABEL } from "@/config/roles"
import { useAuthStore } from "@/hooks/use-auth-store"
import type { User } from "@/interface/auth"

/** Khung chung sau đăng nhập: thanh trên (tên, vai trò, đăng xuất) và menu theo vai trò. */
export function AppShell({ user, children }: { user: User; children: React.ReactNode }) {
  const router = useRouter()
  const pathname = usePathname()
  const clear = useAuthStore((s) => s.clear)
  // Menu trên điện thoại mở cho một trang; chuyển trang thì tự đóng (pathname khác).
  const [menuOpenOn, setMenuOpenOn] = React.useState<string | null>(null)
  const menuOpen = menuOpenOn === pathname
  const items = ROLE_MENU[user.role]

  function logout() {
    clear()
    router.replace("/login")
  }

  return (
    <div className="flex min-h-screen flex-col bg-muted/30">
      <header className="sticky top-0 z-20 flex h-12 items-center gap-2 border-b bg-background px-4">
        <Button
          variant="ghost"
          size="icon"
          className="md:hidden"
          aria-label={menuOpen ? "Đóng menu" : "Mở menu"}
          aria-expanded={menuOpen}
          aria-controls="app-menu"
          onClick={() => setMenuOpenOn(menuOpen ? null : pathname)}
        >
          {menuOpen ? <X /> : <Menu />}
        </Button>
        <Link href={ROLE_HOME[user.role]} className="text-sm font-semibold tracking-tight">
          Rikkei Jobs
        </Link>
        <span className="ml-auto hidden min-w-0 text-right text-xs/tight sm:block">
          <span className="block truncate font-medium">{user.fullName}</span>
          <span className="block truncate text-muted-foreground">{ROLE_LABEL[user.role]}</span>
        </span>
        {user.role === "HR" ? (
          <Button
            variant="ghost"
            size="icon"
            className="ml-auto sm:ml-0"
            aria-label="Đổi mật khẩu"
            title="Đổi mật khẩu"
            render={<Link href="/change-password" />}
            nativeButton={false}
          >
            <KeyRound />
          </Button>
        ) : null}
        <Button
          variant="outline"
          size="sm"
          className={cn(user.role !== "HR" && "ml-auto sm:ml-0")}
          onClick={logout}
        >
          <LogOut data-icon="inline-start" />
          Đăng xuất
        </Button>
      </header>

      <div className="flex flex-1">
        <nav
          id="app-menu"
          aria-label="Menu chính"
          className={cn(
            "w-56 shrink-0 border-r bg-background p-2",
            "max-md:fixed max-md:inset-x-0 max-md:top-12 max-md:z-10 max-md:w-full max-md:border-r-0 max-md:border-b max-md:shadow-sm",
            !menuOpen && "max-md:hidden",
          )}
        >
          <p className="px-2 pt-1 pb-2 text-[0.625rem] font-medium tracking-wide text-muted-foreground uppercase sm:hidden">
            {user.fullName} · {ROLE_LABEL[user.role]}
          </p>
          <ul className="space-y-0.5">
            {items.map((item) => (
              <li key={item.href}>
                <NavLink item={item} active={pathname === item.href || pathname.startsWith(`${item.href}/`)} />
              </li>
            ))}
          </ul>
        </nav>

        <main className="min-w-0 flex-1 p-4 md:p-6">{children}</main>
      </div>
    </div>
  )
}

function NavLink({ item, active }: { item: MenuItem; active: boolean }) {
  const Icon = item.icon
  return (
    <Link
      href={item.href}
      aria-current={active ? "page" : undefined}
      className={cn(
        "flex items-center gap-2 rounded-md px-2 py-1.5 text-xs font-medium transition-colors outline-none",
        "focus-visible:ring-2 focus-visible:ring-ring/30",
        active ? "bg-primary text-primary-foreground" : "text-foreground hover:bg-muted",
      )}
    >
      <Icon className="size-4 shrink-0" aria-hidden />
      {item.label}
    </Link>
  )
}
