"use client";
import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";

/** Mostra os códigos de recuperação uma única vez, com copiar e descarregar. */
export function RecoveryCodesView({ codes }: { codes: string[] }) {
  const [copied, setCopied] = useState(false);
  const text = `Lúmen Lab — códigos de recuperação do 2.º factor\nCada código só funciona uma vez.\n\n${codes.join("\n")}\n`;
  return (
    <div className="stack" style={{ gap: 12 }}>
      <Alert tone="info" title="Guarda estes códigos num local seguro">
        Se perderes o telemóvel, cada código substitui o código da aplicação uma única vez. Não voltam a ser mostrados.
      </Alert>
      <ol className="recovery-codes mono" aria-label="Códigos de recuperação">
        {codes.map((c) => <li key={c}>{c}</li>)}
      </ol>
      <div className="row" style={{ gap: 8, flexWrap: "wrap" }}>
        <Button variant="out" size="sm" icon="copy" onClick={async () => {
          await navigator.clipboard.writeText(codes.join("\n"));
          setCopied(true);
        }}>{copied ? "Copiados" : "Copiar"}</Button>
        <Button variant="out" size="sm" icon="download" onClick={() => {
          const a = document.createElement("a");
          a.href = URL.createObjectURL(new Blob([text], { type: "text/plain;charset=utf-8" }));
          a.download = "lumen-lab-codigos-recuperacao.txt";
          a.click();
          URL.revokeObjectURL(a.href);
        }}>Descarregar</Button>
      </div>
    </div>
  );
}
