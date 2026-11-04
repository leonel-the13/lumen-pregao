"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { useToast } from "@/components/ui/Toast";
import { patch } from "@/lib/api-client";
import { useT } from "@/lib/i18n/client";

export function PortfolioActions({ isPublic, handle }: { isPublic: boolean; handle: string }) {
  const router = useRouter();
  const toast = useToast();
  const t = useT();
  const [busy, setBusy] = useState(false);
  const url = typeof window !== "undefined" ? `${window.location.origin}/p/${handle}` : `/p/${handle}`;
  return (
    <div className="row" style={{ gap: 8 }}>
      {isPublic && <Link className="btn out" href={`/p/${handle}`} target="_blank">{t("portfolio.preview")}</Link>}
      {isPublic && <Button variant="out" icon="copy" onClick={async () => { await navigator.clipboard.writeText(url); toast({ tone: "pos", title: t("portfolio.copied") }); }}>{t("portfolio.copyLink")}</Button>}
      <Button variant={isPublic ? "ghost" : "pri"} loading={busy} onClick={async () => {
        setBusy(true);
        try { await patch("/me", { portfolioPublic: !isPublic }); router.refresh(); } finally { setBusy(false); }
      }}>{isPublic ? t("portfolio.makePrivate") : t("portfolio.makePublic")}</Button>
    </div>
  );
}
