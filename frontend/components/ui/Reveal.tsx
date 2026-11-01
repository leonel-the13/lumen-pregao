"use client";
import { useEffect, useRef, type CSSProperties, type ReactNode } from "react";

/** Aparece ao entrar no ecrã (uma vez). Com prefers-reduced-motion, mostra logo. */
export function Reveal({ children, className, as: Tag = "section", id, style }: { children: ReactNode; className?: string; as?: "section" | "div"; id?: string; style?: CSSProperties }) {
  const ref = useRef<HTMLElement>(null);
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches || !("IntersectionObserver" in window)) {
      el.classList.add("in");
      return;
    }
    const io = new IntersectionObserver((entries) => {
      entries.forEach((e) => { if (e.isIntersecting) { e.target.classList.add("in"); io.unobserve(e.target); } });
    }, { threshold: 0.12 });
    io.observe(el);
    return () => io.disconnect();
  }, []);
  return <Tag ref={ref as never} id={id} style={style} className={["reveal", className].filter(Boolean).join(" ")}>{children}</Tag>;
}
