import { expect, type Locator } from "@playwright/test";

/**
 * Ô chọn của giao diện là SelectMenu (ui/select-menu, Base UI Select), không phải <select>,
 * nên không dùng được locator.selectOption/toHaveValue. Nút và từng mục có data-value để chọn và kiểm tra theo giá trị.
 */
export async function chooseOption(trigger: Locator, value: string) {
  await trigger.click();
  await trigger.page().locator(`[role="option"][data-value="${value}"]`).click();
  await expectOption(trigger, value);
}

/** Chọn theo chữ hiển thị (khi test chỉ biết nhãn, ví dụ tên JD). */
export async function chooseOptionByLabel(trigger: Locator, label: string) {
  await trigger.click();
  await trigger.page().getByRole("option", { name: label, exact: true }).click();
}

/** Giá trị đang chọn của SelectMenu ("" là chưa chọn / tất cả). */
export async function expectOption(trigger: Locator, value: string) {
  await expect(trigger).toHaveAttribute("data-value", value);
}
