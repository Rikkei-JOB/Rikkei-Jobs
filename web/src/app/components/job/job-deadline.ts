/** Ngày hôm nay theo giờ Việt Nam, dạng YYYY-MM-DD. */
export function todayInVietnam(now: Date = new Date()): string {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Ho_Chi_Minh" }).format(now);
}

function toUtcDay(date: string): number {
  const [y, m, d] = date.slice(0, 10).split("-").map(Number);
  return Date.UTC(y, m - 1, d);
}

/** Số ngày từ hôm nay (giờ Việt Nam) đến hạn nộp; âm khi đã quá hạn. */
export function daysUntil(deadline: string, now: Date = new Date()): number {
  return Math.round((toUtcDay(deadline) - toUtcDay(todayInVietnam(now))) / 86_400_000);
}

/** Chữ nhắc hạn nộp; urgent khi còn tối đa 7 ngày (kể cả hôm nay). */
export function deadlineHint(deadline: string | null, now: Date = new Date()): { text: string; urgent: boolean } {
  if (!deadline) return { text: "—", urgent: false };
  const days = daysUntil(deadline, now);
  if (days < 0) return { text: "đã quá hạn", urgent: false };
  if (days === 0) return { text: "hết hạn hôm nay", urgent: true };
  return { text: `còn ${days} ngày`, urgent: days <= 7 };
}
