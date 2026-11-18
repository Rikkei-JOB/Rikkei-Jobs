/**
 * Thông báo chuyển giữa các trang JD trong cùng phiên trình duyệt. router.push/replace sang trang khác làm mất
 * state React (ví dụ /hr/jobs/new -> /hr/jobs/{id} sau lần lưu đầu), nên trang đích đọc lại ở đây một lần.
 * Chỉ là biến trong bộ nhớ: tải lại trang thì mất, đúng ý muốn.
 */

export interface JobFormHandoff {
  jobId: string;
  step: 1 | 2;
  saved: boolean;
  fieldErrors?: Record<string, string>;
  message?: string;
}

let listNotice: string | null = null;
let formHandoff: JobFormHandoff | null = null;

export function setJobListNotice(message: string) {
  listNotice = message;
}

/** Đọc thông báo cho trang danh sách mà không xoá (gọi trong khởi tạo state; xoá bằng {@link clearJobListNotice}). */
export function peekJobListNotice(): string | null {
  return listNotice;
}

export function clearJobListNotice() {
  listNotice = null;
}

export function setJobFormHandoff(handoff: JobFormHandoff) {
  formHandoff = handoff;
}

export function peekJobFormHandoff(jobId: string | undefined): JobFormHandoff | null {
  return jobId && formHandoff?.jobId === jobId ? formHandoff : null;
}

export function clearJobFormHandoff() {
  formHandoff = null;
}
