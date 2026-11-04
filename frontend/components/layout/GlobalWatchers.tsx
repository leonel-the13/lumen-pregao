"use client";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Icon } from "@/components/ui/Icon";
import { useT } from "@/lib/i18n/client";

/** Falha de ligação e sessão expirada (estados globais). Os rascunhos ficam guardados antes do redireccionamento. */
export function GlobalWatchers() {
  const t = useT();
  const [online, setOnline] = useState(true);
  const router = useRouter();
  const path = usePathname();
  useEffect(() => {
    setOnline(navigator.onLine);
    const on = () => setOnline(true);
    const off = () => setOnline(false);
    const expired = () => router.push(`/entrar?expirada=1&next=${encodeURIComponent(path)}`);
    window.addEventListener("online", on);
    window.addEventListener("offline", off);
    window.addEventListener("lp:session-expired", expired);
    return () => {
      window.removeEventListener("online", on);
      window.removeEventListener("offline", off);
      window.removeEventListener("lp:session-expired", expired);
    };
  }, [router, path]);
  if (online) return null;
  return (
    <div className="offline-banner" role="status">
      <Icon name="wifiOff" />
      {t("shell.offline")}
    </div>
  );
}
