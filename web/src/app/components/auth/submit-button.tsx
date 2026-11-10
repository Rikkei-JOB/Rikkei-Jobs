import * as React from "react"
import { Loader2 } from "lucide-react"
import { Button } from "@/app/components/ui/button"

interface SubmitButtonProps {
  loading: boolean
  children: React.ReactNode
}

export function SubmitButton({ loading, children }: SubmitButtonProps) {
  return (
    <Button type="submit" size="lg" className="w-full" disabled={loading}>
      {loading ? <Loader2 className="animate-spin" aria-hidden /> : null}
      {children}
    </Button>
  )
}
