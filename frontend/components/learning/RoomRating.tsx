"use client";
import { useEffect, useState } from "react";
import { StarRating } from "@/components/feedback/StarRating";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { TextArea } from "@/components/ui/Field";
import { useToast } from "@/components/ui/Toast";
import { get, put } from "@/lib/api-client";
import { isApiError } from "@/lib/errors";
import { HTML_LANG } from "@/lib/i18n/config";
import { useLocale, useT } from "@/lib/i18n/client";

interface Summary { average: number; count: number; myStars?: number | null; myComment?: string | null; canRate: boolean }

/** Avaliação da sala: média pública e a avaliação do próprio estudante (depois de iniciar uma tarefa). */
export function RoomRating({ roomId }: { roomId: string }) {
  const toast = useToast();
  const t = useT();
  const locale = useLocale();
  const [s, setS] = useState<Summary | null>(null);
  const [stars, setStars] = useState<number | null>(null);
  const [comment, setComment] = useState("");
  const [busy, setBusy] = useState(false);
  const [editing, setEditing] = useState(false);

  useEffect(() => {
    get<Summary>(`/rooms/${roomId}/rating`).then((r) => {
      setS(r);
      setStars(r.myStars ?? null);
      setComment(r.myComment ?? "");
    }).catch(() => {});
  }, [roomId]);

  async function save() {
    if (!stars) return;
    setBusy(true);
    try {
      const r = await put<Summary>(`/rooms/${roomId}/rating`, { stars, comment: comment.trim() || null });
      setS(r);
      setEditing(false);
      toast({ tone: "pos", title: t("learn.rating.thanks"), body: t("learn.rating.thanksBody") });
    } catch (e) {
      toast({ tone: "neg", title: t("common.couldNotSave"), body: isApiError(e) ? e.message : t("auth.login.tryAgain") });
    } finally {
      setBusy(false);
    }
  }

  if (!s) return null;
  const avg = Number(s.average);
  return (
    <Card className="stack" style={{ padding: 16, gap: 10 }}>
      <div className="row" style={{ gap: 8 }}>
        <b>{t("learn.rating.title")}</b>
        <div className="spacer" />
        {s.count > 0 && <span className="row small muted" style={{ gap: 6 }}>
          <StarRating value={avg} label={t("learn.rating.average")} size={22} />
          <span className="mono">{avg.toLocaleString(HTML_LANG[locale], { minimumFractionDigits: 1, maximumFractionDigits: 1 })} · {s.count}</span>
        </span>}
      </div>
      {!s.canRate && s.myStars == null ? (
        <p className="small muted" style={{ margin: 0 }}>{t("learn.rating.startFirst")}</p>
      ) : s.myStars != null && !editing ? (
        <div className="row small" style={{ gap: 8 }}>
          <span className="muted">{t("learn.rating.yours")}</span>
          <StarRating value={s.myStars} label={t("learn.rating.yoursLabel")} size={22} />
          <div className="spacer" />
          <button type="button" className="btn ghost sm" onClick={() => setEditing(true)}>{t("learn.rating.change")}</button>
        </div>
      ) : (
        <div className="stack" style={{ gap: 8 }}>
          <StarRating value={stars} onChange={setStars} label={t("learn.rating.rateThis")} />
          <label className="sr-only" htmlFor={`rate-${roomId}`}>{t("learn.rating.comment")}</label>
          <TextArea id={`rate-${roomId}`} rows={2} maxLength={1000} placeholder={t("learn.rating.commentPlaceholder")}
            value={comment} onChange={(e) => setComment(e.target.value)} />
          <div className="row" style={{ gap: 8 }}>
            <Button variant="pri" size="sm" onClick={save} loading={busy} disabled={!stars}>{t("learn.rating.save")}</Button>
            {editing && <Button variant="ghost" size="sm" onClick={() => setEditing(false)}>{t("common.cancel")}</Button>}
          </div>
        </div>
      )}
    </Card>
  );
}
