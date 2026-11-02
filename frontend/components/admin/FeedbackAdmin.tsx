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

export interface FeedbackRow { id: string; category: string; rating?: number; message: string; page?: string; status: string; author: string; at: string }

const CATEGORY: Record<string, string> = { SUGGESTION: "Sugestão", BUG: "Erro", CONTENT: "Conteúdo", PRAISE: "Elogio", OTHER: "Outro" };
const STATUS: Record<string, [string, "info" | "neutral" | "pos"]> = { NEW: ["Novo", "info"], READ: ["Lido", "neutral"], RESOLVED: ["Resolvido", "pos"] };

export function FeedbackAdmin({ rows }: { rows: FeedbackRow[] }) {
  const router = useRouter();
  const toast = useToast();
  const [busy, setBusy] = useState<string | null>(null);
  async function set(id: string, status: string) {
    setBusy(id);
    try { await patch(`/admin/feedback/${id}`, { status }); router.refresh(); }
    catch (e) { toast({ tone: "neg", title: "Não foi possível actualizar", body: (e as Error).message }); }
    finally { setBusy(null); }
  }
  if (rows.length === 0) return <Card className="state-box"><p className="muted">Sem feedback neste filtro.</p></Card>;
  return (
    <ul className="plain stack" style={{ gap: 10 }}>
      {rows.map((f) => {
        const [sl, st] = STATUS[f.status] ?? [f.status, "neutral"];
        return (
          <li key={f.id}>
            <Card className="stack" style={{ gap: 8, padding: 16 }}>
              <div className="row" style={{ gap: 8, flexWrap: "wrap" }}>
                <Badge tone={st}>{sl}</Badge><Badge>{CATEGORY[f.category] ?? f.category}</Badge>
                {f.rating && <StarRating value={f.rating} label="Nota" size={22} />}
                <div className="spacer" />
                <span className="small muted">{f.author} · <span className="mono">{formatDateTime(f.at)}</span>{f.page && <> · <span className="mono">{f.page}</span></>}</span>
              </div>
              <p style={{ margin: 0, whiteSpace: "pre-wrap" }}>{f.message}</p>
              <div className="row" style={{ gap: 6 }}>
                {f.status !== "READ" && f.status !== "RESOLVED" && <Button size="sm" variant="ghost" icon="eye" loading={busy === f.id} onClick={() => set(f.id, "READ")}>Marcar como lido</Button>}
                {f.status !== "RESOLVED" && <Button size="sm" variant="ghost" icon="check" loading={busy === f.id} onClick={() => set(f.id, "RESOLVED")}>Resolvido</Button>}
                {f.status === "RESOLVED" && <Button size="sm" variant="ghost" icon="refresh" loading={busy === f.id} onClick={() => set(f.id, "NEW")}>Reabrir</Button>}
              </div>
            </Card>
          </li>
        );
      })}
    </ul>
  );
}
