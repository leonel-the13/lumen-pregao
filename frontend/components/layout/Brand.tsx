"use client";
import Link from "next/link";
import { useT } from "@/lib/i18n/client";

export function Brand({ href = "/" }: { href?: string }) {
  const t = useT();
  return (
    <Link className="brand" href={href} aria-label={t("common.brandHome")}>
      <i className="dot" aria-hidden="true" />
      <b>LÚMEN</b>
      <span>Lab</span>
    </Link>
  );
}
