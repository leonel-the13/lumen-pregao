"use client";
import { ErrorState } from "@/components/ui/States";
import { useT } from "@/lib/i18n/client";

export default function AppError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  const t = useT();
  return (
    <ErrorState title={t("pages.error.page")} body={t("pages.error.tryLater")}
      code={error.digest ? `APP-${error.digest.slice(0, 6)}` : "APP-500"}
      action={<button className="btn out" onClick={reset}>{t("pages.error.retry")}</button>} />
  );
}
