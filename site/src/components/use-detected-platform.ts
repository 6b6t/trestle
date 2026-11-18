import { useEffect } from "react";
import { detectPlatform, type Platform } from "../lib/platforms";

/**
 * Selects the visitor's platform after hydration. The prerendered HTML stays
 * the same for every visitor, so detection must wait for the browser.
 */
export function useDetectedPlatform(select: (platform: Platform) => void, enabled = true) {
  useEffect(() => {
    if (!enabled) return;
    const platform = detectPlatform(navigator.userAgent, navigator.maxTouchPoints);
    if (platform) select(platform);
  }, [select, enabled]);
}
