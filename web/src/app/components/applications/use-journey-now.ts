"use client";

import { useEffect, useState } from "react";

/** Minute refresh plus an exact deadline wake-up; focus also refreshes throttled tabs. */
export function useJourneyNow(deadline?: string) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const update = () => setNow(Date.now());
    const interval = window.setInterval(update, 60000);
    const deadlineTime = deadline ? new Date(deadline).getTime() : 0;
    const delay = deadlineTime - Date.now();
    // A delayed GET may introduce a deadline that passed after this hook mounted.
    const timer = now < deadlineTime && delay < 2147483647 ? window.setTimeout(update, Math.max(0, delay + 1)) : undefined;
    window.addEventListener("focus", update);
    const onVisible = () => { if (document.visibilityState === "visible") update(); };
    document.addEventListener("visibilitychange", onVisible);
    return () => { clearInterval(interval); clearTimeout(timer); window.removeEventListener("focus", update); document.removeEventListener("visibilitychange", onVisible); };
  }, [deadline, now]);
  return now;
}
