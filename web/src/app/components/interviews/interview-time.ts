export const interviewZone = "Asia/Ho_Chi_Minh";
export function interviewTime(value: string) { return new Intl.DateTimeFormat("vi-VN", { timeZone: interviewZone, day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit", hourCycle: "h23" }).format(new Date(value)); }
export function interviewClock(value: string) { return new Intl.DateTimeFormat("vi-VN", { timeZone: interviewZone, hour: "2-digit", minute: "2-digit", hourCycle: "h23" }).format(new Date(value)); }
export function localInterviewInstant(value: string) { return new Date(`${value}+07:00`).toISOString(); }
export function dayLabel(value: string) { return new Intl.DateTimeFormat("vi-VN", { timeZone: interviewZone, weekday: "short", day: "2-digit", month: "2-digit" }).format(new Date(`${value}T00:00:00+07:00`)); }
export function shiftDay(day: string, offset: number) { return new Date(Date.parse(`${day}T00:00:00Z`) + offset * 86400000).toISOString().slice(0, 10); }
export function currentMonday() { const today = new Date(Date.now() + 7 * 3600000).toISOString().slice(0, 10); return shiftDay(today, -(new Date(`${today}T00:00:00Z`).getUTCDay() + 6) % 7); }
