"use client";

import type { ReactNode } from "react";
import { Select } from "@base-ui/react/select";
import { Check, ChevronDown } from "lucide-react";
import "./select-menu.css";

export interface SelectOption {
  value: string;
  label: ReactNode;
  /** Chữ dùng khi gõ phím để nhảy tới mục (cần khi label không phải chuỗi). */
  textValue?: string;
  disabled?: boolean;
}

interface SelectMenuProps {
  value: string;
  onValueChange(value: string): void;
  options: SelectOption[];
  /** Nhãn cho trình đọc màn hình khi không có <label htmlFor>. */
  "aria-label"?: string;
  "aria-describedby"?: string;
  "aria-invalid"?: boolean;
  id?: string;
  name?: string;
  required?: boolean;
  disabled?: boolean;
  /** Chữ mờ hiển thị khi value rỗng ("") và không có mục nào mang value rỗng. */
  placeholder?: string;
  /** Biểu tượng đặt trước giá trị trên nút (ví dụ MapPin). */
  icon?: ReactNode;
  /** Chữ hiển thị trên nút khác chữ trong danh sách (ví dụ nút ghi "Cấp bậc" khi chọn "Tất cả cấp bậc"). */
  renderValue?(value: string, label: ReactNode): ReactNode;
  /** Class cho nút; mặc định là ô chọn của form (ui-select-trigger). */
  className?: string;
  /** Thêm vào nút khi đang chọn khác giá trị mặc định (bộ lọc đang bật). */
  active?: boolean;
  onBlur?(): void;
}

/**
 * Ô chọn thả xuống dùng chung thay cho <select> của trình duyệt (danh sách mặc định không đổi kiểu được).
 * Dựa trên Select của Base UI: điều khiển bằng bàn phím, đọc được bằng trình đọc màn hình, có input ẩn theo `name` cho form.
 */
export function SelectMenu({ value, onValueChange, options, placeholder, icon, renderValue, className, active, onBlur, ...props }: SelectMenuProps) {
  const labels = new Map(options.map(option => [option.value, option.label]));
  return <Select.Root
    value={value}
    onValueChange={next => onValueChange(next == null ? "" : String(next))}
    name={props.name}
    required={props.required}
    disabled={props.disabled}
    modal={false}
  >
    <Select.Trigger
      id={props.id}
      aria-label={props["aria-label"]}
      aria-describedby={props["aria-describedby"]}
      aria-invalid={props["aria-invalid"] || undefined}
      data-active={active || undefined}
      onBlur={onBlur}
      className={className ?? "ui-select-trigger"}
    >
      {icon && <span className="ui-select-icon" aria-hidden="true">{icon}</span>}
      <Select.Value className="ui-select-value">
        {(current: string | null) => {
          const key = current ?? "";
          if (!labels.has(key) && key === "" && placeholder) return <span className="ui-select-placeholder">{placeholder}</span>;
          const label = labels.get(key) ?? key;
          return renderValue ? renderValue(key, label) : label;
        }}
      </Select.Value>
      <Select.Icon className="ui-select-chevron"><ChevronDown aria-hidden="true" /></Select.Icon>
    </Select.Trigger>
    <Select.Portal>
      <Select.Positioner className="ui-select-positioner" side="bottom" align="start" sideOffset={6} alignItemWithTrigger={false} collisionPadding={12}>
        <Select.Popup className="ui-select-popup">
          <Select.List className="ui-select-list">
            {options.map(option => <Select.Item key={option.value} value={option.value} disabled={option.disabled}
              label={option.textValue ?? (typeof option.label === "string" ? option.label : undefined)} className="ui-select-item">
              <Select.ItemText className="ui-select-item-text">{option.label}</Select.ItemText>
              <Select.ItemIndicator className="ui-select-item-check"><Check aria-hidden="true" /></Select.ItemIndicator>
            </Select.Item>)}
          </Select.List>
        </Select.Popup>
      </Select.Positioner>
    </Select.Portal>
  </Select.Root>;
}
