"use client";
import { useEffect, useRef } from "react";
import type { EChartsOption } from "echarts";

function token(name: string) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

/** Wrapper do Apache ECharts com as cores dos tokens; entra uma vez (320 ms) e respeita reduced-motion. */
export function EChart({ option, height = 260, label }: { option: (t: (n: string) => string) => EChartsOption; height?: number; label: string }) {
  const el = useRef<HTMLDivElement>(null);
  useEffect(() => {
    let disposed = false;
    let chart: import("echarts").ECharts | undefined;
    const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    import("echarts").then((ec) => {
      if (disposed || !el.current) return;
      chart = ec.init(el.current, undefined, { renderer: "svg" });
      chart.setOption({ animation: !reduce, animationDuration: 320, textStyle: { fontFamily: "inherit", color: token("--muted") }, ...option(token) });
    });
    const onResize = () => chart?.resize();
    window.addEventListener("resize", onResize);
    return () => { disposed = true; window.removeEventListener("resize", onResize); chart?.dispose(); };
  }, [option]);
  return <div ref={el} role="img" aria-label={label} style={{ width: "100%", height }} />;
}
