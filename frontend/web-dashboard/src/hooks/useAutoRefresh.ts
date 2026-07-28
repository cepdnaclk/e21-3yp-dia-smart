import { useEffect, useRef } from "react";

export const useAutoRefresh = (
  callback: () => Promise<void> | void,
  intervalMs: number
) => {
  const savedCallback = useRef(callback);
  const isExecutingRef = useRef(false);

  useEffect(() => {
    savedCallback.current = callback;
  }, [callback]);

  useEffect(() => {
    let intervalId: any = null;

    const tick = async () => {
      // 1. Suspension on browser tab hide
      if (document.hidden) {
        return;
      }
      // 2. Avoid request overlapping
      if (isExecutingRef.current) {
        return;
      }

      isExecutingRef.current = true;
      try {
        await savedCallback.current();
      } catch (err) {
        console.error("Auto-refresh execution error:", err);
      } finally {
        isExecutingRef.current = false;
      }
    };

    // Initialize interval
    intervalId = setInterval(tick, intervalMs);

    // Dynamic visibility change event trigger
    const handleVisibilityChange = () => {
      if (!document.hidden) {
        tick();
      }
    };
    document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      if (intervalId) {
        clearInterval(intervalId);
      }
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [intervalMs]);
};
