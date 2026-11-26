"use client";

import { useEffect, useState } from "react";

/**
 * Thời điểm hiện tại (ms) giữ trong state, tự cập nhật theo chu kỳ.
 * Dùng thay cho Date.now() khi render để kết quả ổn định giữa các lần render (React Compiler yêu cầu render thuần).
 */
export function useNow(intervalMs = 60_000) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), intervalMs);
    return () => window.clearInterval(timer);
  }, [intervalMs]);
  return now;
}
