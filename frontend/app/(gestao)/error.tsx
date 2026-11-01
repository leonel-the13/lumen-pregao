"use client";
import { ErrorState } from "@/components/ui/States";

export default function AppError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <ErrorState title="Não foi possível carregar esta página" body="Tenta de novo dentro de instantes."
      code={error.digest ? `APP-${error.digest.slice(0, 6)}` : "APP-500"}
      action={<button className="btn out" onClick={reset}>Tentar novamente</button>} />
  );
}
