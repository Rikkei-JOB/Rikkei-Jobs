"use client"

import { Dialog } from "@base-ui/react/dialog"
import * as React from "react"
import { Button } from "@/app/components/ui/button"
import { Input } from "@/app/components/ui/input"
import { useCreateCompany } from "@/hooks/use-staff-companies"
import type { Company } from "@/interface/company"
import { COMPANY_SIZES, createCompanySchema, toCreatePayload, type CreateCompanyValues } from "@/schemas/company.schema"
import { ApiError, getErrorMessage } from "@/services/api"
import { SelectMenu } from "@/app/components/ui/select-menu"

type Field = keyof CreateCompanyValues
type FieldErrors = Partial<Record<Field, string>>

const EMPTY: CreateCompanyValues = {
  name: "",
  taxCode: "",
  industry: "",
  size: "",
  address: "",
  city: "",
  website: "",
  description: "",
}

const FIELDS: Field[] = ["name", "taxCode", "industry", "size", "address", "city", "website", "description"]


/** Hộp cán bộ tạo công ty (POST /staff/companies); chỉ render khi mở (đóng = unmount nên form tự reset). */
export function CreateCompanyDialog({ onClose, onCreated }: { onClose: () => void; onCreated: (company: Company) => void }) {
  const create = useCreateCompany()
  const [values, setValues] = React.useState<CreateCompanyValues>(EMPTY)
  const [fieldErrors, setFieldErrors] = React.useState<FieldErrors>({})
  const [formError, setFormError] = React.useState("")

  function set<K extends Field>(field: K, value: CreateCompanyValues[K]) {
    setValues((prev) => ({ ...prev, [field]: value }))
  }

  function submit(event: React.FormEvent) {
    event.preventDefault()
    if (create.isPending) return
    const parsed = createCompanySchema.safeParse(values)
    if (!parsed.success) {
      const errors: FieldErrors = {}
      for (const issue of parsed.error.issues) {
        const key = issue.path[0] as Field
        if (!errors[key]) errors[key] = issue.message
      }
      setFieldErrors(errors)
      setFormError("")
      return
    }
    setFieldErrors({})
    setFormError("")
    create.mutate(toCreatePayload(parsed.data), {
      onSuccess: (company) => onCreated(company),
      onError: (err) => {
        if (err instanceof ApiError && err.status === 409) {
          setFieldErrors({ taxCode: "Mã số thuế đã tồn tại" })
          return
        }
        if (err instanceof ApiError && err.code === "VALIDATION_ERROR" && err.errors && typeof err.errors === "object") {
          const errors: FieldErrors = {}
          for (const [key, message] of Object.entries(err.errors as Record<string, string>)) {
            if ((FIELDS as string[]).includes(key)) errors[key as Field] = message
          }
          if (Object.keys(errors).length > 0) {
            setFieldErrors(errors)
            return
          }
        }
        setFormError(getErrorMessage(err))
      },
    })
  }

  const disabled = create.isPending

  function row(field: Field, label: string, control: React.ReactNode) {
    return (
      <div className="space-y-1">
        <label htmlFor={`create-company-${field}`} className="block text-xs font-medium">
          {label}
        </label>
        {control}
        {fieldErrors[field] ? <p className="text-xs text-destructive">{fieldErrors[field]}</p> : null}
      </div>
    )
  }

  return (
    <Dialog.Root
      open
      onOpenChange={(value) => {
        if (!value && !create.isPending) onClose()
      }}
    >
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-50 bg-black/45" />
        <Dialog.Popup className="fixed left-1/2 top-1/2 z-50 max-h-[calc(100%-2rem)] w-[calc(100%-2rem)] max-w-lg -translate-x-1/2 -translate-y-1/2 overflow-y-auto rounded-2xl border bg-background p-6 shadow-xl">
          <Dialog.Title className="text-base font-semibold">Tạo công ty</Dialog.Title>
          <Dialog.Description className="mb-4 mt-2 text-xs/relaxed text-muted-foreground">
            Công ty mới ở trạng thái hoạt động. Sau khi tạo, thêm tài khoản HR ở trang chi tiết.
          </Dialog.Description>
          <form onSubmit={submit} noValidate className="space-y-3">
            {row(
              "name",
              "Tên công ty *",
              <Input id="create-company-name" value={values.name} onChange={(e) => set("name", e.target.value)} disabled={disabled} />,
            )}
            {row(
              "taxCode",
              "Mã số thuế *",
              <Input
                id="create-company-taxCode"
                inputMode="numeric"
                value={values.taxCode}
                onChange={(e) => set("taxCode", e.target.value)}
                placeholder="10 hoặc 13 chữ số"
                disabled={disabled}
              />,
            )}
            <div className="grid gap-3 sm:grid-cols-2">
              {row(
                "industry",
                "Ngành",
                <Input id="create-company-industry" value={values.industry} onChange={(e) => set("industry", e.target.value)} disabled={disabled} />,
              )}
              {row(
                "size",
                "Quy mô",
                <SelectMenu
                  id="create-company-size"
                  value={values.size}
                  onValueChange={(value) => set("size", value as CreateCompanyValues["size"])}
                  disabled={disabled}
                  options={[{ value: "", label: "Chưa chọn" }, ...COMPANY_SIZES.map((size) => ({ value: size, label: `${size} nhân sự` }))]}
                />,
              )}
            </div>
            {row(
              "address",
              "Địa chỉ",
              <Input id="create-company-address" value={values.address} onChange={(e) => set("address", e.target.value)} disabled={disabled} />,
            )}
            <div className="grid gap-3 sm:grid-cols-2">
              {row(
                "city",
                "Thành phố",
                <Input id="create-company-city" value={values.city} onChange={(e) => set("city", e.target.value)} disabled={disabled} />,
              )}
              {row(
                "website",
                "Website",
                <Input
                  id="create-company-website"
                  value={values.website}
                  onChange={(e) => set("website", e.target.value)}
                  placeholder="https://"
                  disabled={disabled}
                />,
              )}
            </div>
            {row(
              "description",
              "Giới thiệu",
              <textarea
                id="create-company-description"
                rows={4}
                value={values.description}
                onChange={(e) => set("description", e.target.value)}
                disabled={disabled}
                className="w-full rounded-md border border-input bg-background px-2 py-1 text-xs"
              />,
            )}
            {formError ? (
              <p role="alert" className="text-xs text-destructive">
                {formError}
              </p>
            ) : null}
            <div className="flex justify-end gap-2">
              <Dialog.Close disabled={disabled} render={<Button type="button" variant="outline" />}>
                Huỷ
              </Dialog.Close>
              <Button type="submit" disabled={disabled}>
                {disabled ? "Đang xử lý..." : "Tạo công ty"}
              </Button>
            </div>
          </form>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
