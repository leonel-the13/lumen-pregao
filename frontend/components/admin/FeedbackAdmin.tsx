"use client";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { StarRating } from "@/components/feedback/StarRating";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { useToast } from "@/components/ui/Toast";
import { patch } from "@/lib/api-client";
import { formatDateTime } from "@/lib/format";
import { useLocale, useT } from "@/lib/i18n/client";

export interface FeedbackRow { id: string; category: string; rating?: number; message: string; page?: string; status: string; author: string; at: string }

const CATEGORIES = ["SUGGESTION", "BUG", "CONTENT", "PRAISE", "OTHER"] as const;
const STATUS_TONE: Record<string, "info" | "neutral" | "pos"> = { NEW: "info", READ: "neutral", RESOLVED: "pos" };

export function FeedbackAdmin({ rows }: { rows: FeedbackRow[] }) {
  const router = useRouter();
  const toast = useToast();
  const t = useT();
  const locale = useLocale();
  const [busy, setBusy] = useState<string | null>(null);
  async function set(id: string, status: string) {
    setBusy(id);
    try { await patch(`/admin/feedback/${id}`, { status }); router.refresh(); }
    catch (e) { toast({ tone: "neg", title: t("admin.feedback.couldNotUpdate"), body: (e as Error).message }); }
    finally { setBusy(null); }
  }
  if (rows.length === 0) return <Card className="state-box"><p className="muted">{t("admin.feedback.empty")}</p></Card>;
  return (
    <ul className="plain stack" style={{ gap: 10 }}>
      {rows.map((f) => {
        const st = STATUS_TONE[f.status] ?? "neutral";
        const sl = f.status in STATUS_TONE ? t(`admin.feedback.status.${f.status as "NEW" | "READ" | "RESOLVED"}`) : f.status;
        return (
          <li key={f.id}>
            <Card className="stack" style={{ gap: 8, padding: 16 }}>
              <div className="row" style={{ gap: 8, flexWrap: "wrap" }}>
                <Badge tone={st}>{sl}</Badge><Badge>{(CATEGORIES as readonly string[]).includes(f.category) ? t(`admin.feedback.category.${f.category as (typeof CATEGORIES)[number]}`) : f.category}</Badge>
                {f.rating && <StarRating value={f.rating} label={t("admin.feedback.rating")} size={22} />}
                <div className="spacer" />
                <span className="small muted">{f.author} · <span className="mono">{formatDateTime(f.at, locale)}</span>{f.page && <> · <span className="mono">{f.page}</span></>}</span>
              </div>
              <p style={{ margin: 0, whiteSpace: "pre-wrap" }}>{f.message}</p>
              <div className="row" style={{ gap: 6 }}>
                {f.status !== "READ" && f.status !== "RESOLVED" && <Button size="sm" variant="ghost" icon="eye" loading={busy === f.id} onClick={() => set(f.id, "READ")}>{t("admin.feedback.markRead")}</Button>}
                {f.status !== "RESOLVED" && <Button size="sm" variant="ghost" icon="check" loading={busy === f.id} onClick={() => set(f.id, "RESOLVED")}>{t("admin.feedback.resolved")}</Button>}
                {f.status === "RESOLVED" && <Button size="sm" variant="ghost" icon="refresh" loading={busy === f.id} onClick={() => set(f.id, "NEW")}>{t("admin.feedback.reopen")}</Button>}
              </div>
            </Card>
          </li>
        );
      })}
    </ul>
  );
}
