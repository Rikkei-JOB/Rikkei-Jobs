"use client";

import { usePathname } from "next/navigation";
import { Button } from "@/app/components/ui/button";
import { rememberNextPath } from "@/helper/next-path";
import { useLogout } from "@/hooks/use-logout";

export function SwitchAccountButton() {
  const pathname = usePathname();
  const logout = useLogout();
  return <Button variant="outline" onClick={() => { rememberNextPath(pathname); logout(); }}>Đăng nhập tài khoản khác</Button>;
}
