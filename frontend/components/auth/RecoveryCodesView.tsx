"use client";
import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { useT } from "@/lib/i18n/client";

/** Mostra os códigos de recuperação uma única vez, com copiar e descarregar. */
export function RecoveryCodesView({ codes }: { codes: string[] }) {
  const t = useT();
  const [copied, setCopied] = useState(false);
  const text = `${t("auth.codes.fileHeader")}\n\n${codes.join("\n")}\n`;
  return (
    <div className="stack" style={{ gap: 12 }}>
      <Alert tone="info" title={t("auth.codes.keepSafe")}>{t("auth.codes.keepSafeBody")}</Alert>
      <ol className="recovery-codes mono" aria-label={t("auth.codes.list")}>
        {codes.map((c) => <li key={c}>{c}</li>)}
      </ol>
      <div className="row" style={{ gap: 8, flexWrap: "wrap" }}>
        <Button variant="out" size="sm" icon="copy" onClick={async () => {
          await navigator.clipboard.writeText(codes.join("\n"));
          setCopied(true);
        }}>{copied ? t("auth.codes.copied") : t("auth.codes.copy")}</Button>
        <Button variant="out" size="sm" icon="download" onClick={() => {
          const a = document.createElement("a");
          a.href = URL.createObjectURL(new Blob([text], { type: "text/plain;charset=utf-8" }));
          a.download = t("auth.codes.fileName");
          a.click();
          URL.revokeObjectURL(a.href);
        }}>{t("auth.codes.download")}</Button>
      </div>
    </div>
  );
}
