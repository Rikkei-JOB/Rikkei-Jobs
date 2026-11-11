"use client"

import * as React from "react"
import { Eye, EyeOff } from "lucide-react"
import { Input } from "@/app/components/ui/input"

/** Ô nhập mật khẩu có nút hiện/ẩn. */
export const PasswordInput = React.forwardRef<HTMLInputElement, Omit<React.ComponentProps<"input">, "type" | "ref">>(
  function PasswordInput(props, ref) {
    const [visible, setVisible] = React.useState(false)
    return (
      <div className="relative">
        <Input ref={ref} type={visible ? "text" : "password"} className="pr-8" {...props} />
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
          className="absolute inset-y-0 right-0 flex w-8 items-center justify-center text-muted-foreground hover:text-foreground"
        >
          {visible ? <EyeOff className="size-3.5" /> : <Eye className="size-3.5" />}
        </button>
      </div>
    )
  }
)
