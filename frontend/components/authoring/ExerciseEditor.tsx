"use client";
import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { StatementText } from "@/components/task/StatementText";
import { Alert } from "@/components/ui/Alert";
import { Badge, Tbc } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card, CardHeader } from "@/components/ui/Card";
import { Field, Input, Select, TextArea } from "@/components/ui/Field";
import { Modal } from "@/components/ui/Modal";
import { Seg } from "@/components/ui/Tabs";
import { useToast } from "@/components/ui/Toast";
import { patch, post } from "@/lib/api-client";
import { STATUS_LABEL, STATUS_TONE, TYPE_LABEL, type ExerciseDto, type LegalSourceDto, type ParamSpec, type VersionDto } from "@/lib/authoring-types";
import { formatDate } from "@/lib/format";
import type { Segment } from "@/lib/types";

interface Line { account: string; side: "D" | "C"; expr: string }
interface Ref { legalSourceId: string; articles: string; consultedAt: string }

interface State {
  type: string; title: string; topic: string; statement: string; difficulty: number; points: number; maxAttempts: number;
  feedbackPolicy: string; params: ParamSpec[]; options: { key: string; text: string }[]; correct: string[]; multiple: boolean;
  expr: string; scale: string; unit: string; lines: Line[]; tolType: string; tolValue: string;
  presentationJson: string; solutionJson: string; hints: string; references: Ref[]; changeNote: string;
}

const STRUCTURED = ["A", "B", "C"];

function fromVersion(v: VersionDto | undefined, topic: string): State {
  const p = (v?.presentation ?? {}) as Record<string, unknown>;
  const s = (v?.solutionSpec ?? {}) as Record<string, unknown>;
  return {
    type: v?.type ?? "C", title: v?.title ?? "", topic, statement: v?.statement ?? "", difficulty: v?.difficulty ?? 2, points: v?.points ?? 40,
    maxAttempts: v?.maxAttempts ?? 3, feedbackPolicy: v?.feedbackPolicy ?? "DETAILED", params: v?.paramSpec?.params ?? [],
    options: (p.options as State["options"]) ?? [{ key: "A", text: "" }, { key: "B", text: "" }], correct: (s.correct as string[]) ?? [],
    multiple: !!p.multiple, expr: (s.expr as string) ?? "", scale: s.scale !== undefined ? String(s.scale) : "2", unit: (p.unit as string) ?? "Kz",
    lines: (s.lines as Line[]) ?? [{ account: "", side: "D", expr: "" }], tolType: v?.tolerance?.type ?? "ABS",
    tolValue: v?.tolerance?.value !== undefined ? String(v.tolerance.value) : "0.01",
    presentationJson: JSON.stringify(v?.presentation ?? {}, null, 2), solutionJson: JSON.stringify(v?.solutionSpec ?? {}, null, 2),
    hints: (v?.hints ?? []).join("\n"), changeNote: v?.changeNote ?? "",
    references: (v?.references ?? []).map((r) => ({ legalSourceId: r.legalSourceId, articles: r.articles ?? "", consultedAt: r.consultedAt ?? "" })),
  };
}

function toInput(s: State) {
  let presentation: unknown;
  let solution: unknown;
  if (s.type === "A") { presentation = { options: s.options, multiple: s.multiple }; solution = { correct: s.correct }; }
  else if (s.type === "B") { presentation = { unit: s.unit, precision: Number(s.scale) || 2 }; solution = { expr: s.expr, scale: Number(s.scale) || 2 }; }
  else if (s.type === "C") { presentation = { currency: "Kz" }; solution = { lines: s.lines }; }
  else { presentation = JSON.parse(s.presentationJson || "{}"); solution = JSON.parse(s.solutionJson || "{}"); }
  return {
    type: s.type, title: s.title, topic: s.topic, statement: s.statement, difficulty: s.difficulty, points: s.points, maxAttempts: s.maxAttempts,
    feedbackPolicy: s.feedbackPolicy, presentation, paramSpec: { params: s.params }, solutionSpec: solution,
    tolerance: { type: s.tolType, value: Number(s.tolValue) }, hints: s.hints.split("\n").map((h) => h.trim()).filter(Boolean),
    changeNote: s.changeNote || null,
    references: s.references.filter((r) => r.legalSourceId).map((r) => ({ legalSourceId: r.legalSourceId, articles: r.articles || null, consultedAt: r.consultedAt || null })),
  };
}

const STEPS = ["DRAFT", "IN_REVIEW", "APPROVED", "PUBLISHED"] as const;

export function ExerciseEditor({ exercise, sources, canReview, isAuthor }:
  { exercise?: ExerciseDto; sources: LegalSourceDto[]; canReview: boolean; isAuthor: boolean }) {
  const router = useRouter();
  const toast = useToast();
  const [versionNo, setVersionNo] = useState(exercise?.versions[0]?.number);
  const version = exercise?.versions.find((v) => v.number === versionNo) ?? exercise?.versions[0];
  const [s, setS] = useState<State>(() => fromVersion(version, exercise?.topic ?? ""));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [preview, setPreview] = useState<null | { statementRendered: string; statementSegments: Segment[]; expectedAnswer: unknown; selfCheck: string; params: unknown }>(null);
  const [notes, setNotes] = useState("");
  const [checked, setChecked] = useState(false);
  const status = version?.status ?? "DRAFT";
  const editable = !exercise || (status === "DRAFT" && isAuthor);
  const set = <K extends keyof State>(k: K, v: State[K]) => setS((x) => ({ ...x, [k]: v }));
  const paramNames = useMemo(() => new Set(s.params.map((p) => p.name)), [s.params]);
  const missing = useMemo(() => [...s.statement.matchAll(/\[([a-z_][a-z0-9_]*)\]/g)].map((m) => m[1]).filter((n) => !paramNames.has(n)), [s.statement, paramNames]);

  const run = async (fn: () => Promise<void>) => {
    setBusy(true); setError(null);
    try { await fn(); } catch (e) { setError((e as Error).message); } finally { setBusy(false); }
  };

  const save = () => run(async () => {
    let body;
    try { body = toInput(s); } catch { throw new Error("A apresentação ou a solução em JSON tem um erro de sintaxe."); }
    if (!exercise) {
      const r = await post<ExerciseDto>("/authoring/exercises", body);
      toast({ tone: "pos", title: `Exercício ${r.code} criado` });
      router.push(`/autoria/${r.id}`);
    } else {
      await patch(`/authoring/exercises/${exercise.id}/versions/${version!.number}`, body);
      toast({ tone: "pos", title: "Rascunho guardado" });
      router.refresh();
    }
  });

  const transition = (to: string, extra: Record<string, unknown> = {}) => run(async () => {
    await post(`/authoring/versions/${version!.id}/transition`, { to, ...extra });
    toast({ tone: "pos", title: `Estado: ${STATUS_LABEL[to as keyof typeof STATUS_LABEL]}` });
    router.refresh();
  });

  return (
    <div className="ex-grid">
      <div className="stack" style={{ gap: 18 }}>
        <div className="row" style={{ alignItems: "flex-end" }}>
          <div className="stack" style={{ gap: 6 }}>
            <div className="row" style={{ gap: 8 }}>
              <span className="label">Exercício · {exercise?.code ?? "novo"}</span>
              <Badge tone={STATUS_TONE[status]}>{STATUS_LABEL[status]}</Badge>
              {version && <Badge mono>v{version.number}</Badge>}
            </div>
            <h1 className="h1">{s.title || "Novo exercício"}</h1>
          </div>
          <div className="spacer" />
          {exercise && <Button variant="out" icon="eye" loading={busy} onClick={() => run(async () => setPreview(await post(`/authoring/versions/${version!.id}/preview`)))}>Pré-visualizar como estudante</Button>}
          {editable && <Button variant="out" loading={busy} onClick={save}>Guardar</Button>}
          {exercise && editable && <Button variant="pri" loading={busy} onClick={() => transition("IN_REVIEW")}>Enviar para revisão</Button>}
        </div>

        <ol className="plain steps" aria-label="Fluxo editorial" style={{ gap: 6 }}>
          {STEPS.map((st, i) => <li key={st} style={{ flex: 1 }} className="stack"><i className={STEPS.indexOf(status as never) >= i ? "on" : undefined} style={{ display: "block", height: 4, borderRadius: 2, background: STEPS.indexOf(status as never) >= i ? "var(--accent)" : "var(--surface-3)" }} /><span className="small muted">{i + 1} · {STATUS_LABEL[st]}</span></li>)}
        </ol>
        <p className="small muted">Nada é visível para estudantes sem aprovação. O autor não aprova o próprio exercício.</p>
        {version?.legalAlertAt && <Alert tone="neg" title="Norma alterada">Uma referência deste exercício tem nova versão. Revê os valores e envia de novo para revisão.</Alert>}
        {version?.reviewNotes && <Alert tone="info" title="Notas do revisor">{version.reviewNotes}</Alert>}
        {error && <Alert tone="neg" title="Não foi possível concluir">{error}</Alert>}

        <Card className="stack">
          <CardHeader title="Tipo de tarefa" />
          <Seg label="Tipo de tarefa" value={s.type} onChange={(v) => editable && set("type", v)}
            options={["A", "B", "C", "D", "E", "F"].map((t) => ({ value: t, label: TYPE_LABEL[t] }))} />
          <Field label="Título">{({ id }) => <Input id={id} value={s.title} disabled={!editable} onChange={(e) => set("title", e.target.value)} />}</Field>
          <Field label="Enunciado" hint="Usa [parâmetros] para valores gerados no servidor." error={missing.length ? `Parâmetros sem regra: ${missing.join(", ")}` : undefined}>
            {({ id, describedBy, invalid }) => <TextArea id={id} aria-describedby={describedBy} invalid={invalid} value={s.statement} disabled={!editable} onChange={(e) => set("statement", e.target.value)} />}
          </Field>
        </Card>

        <Card className="stack">
          <CardHeader title="Parâmetros (gerados no servidor)">{editable && <Button size="sm" variant="ghost" icon="plus" onClick={() => set("params", [...s.params, { name: "", type: "money", rule: { kind: "range", min: 0, max: 0, step: 1 } }])}>Parâmetro</Button>}</CardHeader>
          <div className="tbl-wrap">
            <table className="tbl">
              <thead><tr><th>Nome</th><th>Tipo</th><th>Regra de geração</th><th>Visível</th><th /></tr></thead>
              <tbody>
                {s.params.map((p, i) => {
                  const up = (patch: Partial<ParamSpec>) => set("params", s.params.map((x, j) => (j === i ? { ...x, ...patch } : x)));
                  const upRule = (patch: Partial<ParamSpec["rule"]>) => up({ rule: { ...p.rule, ...patch } });
                  return (
                    <tr key={i}>
                      <td style={{ width: 150 }}><Input aria-label="Nome do parâmetro" className="mono" value={p.name} disabled={!editable} onChange={(e) => up({ name: e.target.value.replace(/[^a-z0-9_]/g, "") })} /></td>
                      <td style={{ width: 130 }}>
                        <Select aria-label="Tipo" value={p.type} disabled={!editable} onChange={(e) => up({ type: e.target.value as ParamSpec["type"] })}>
                          <option value="money">moeda</option><option value="percent">percentagem</option><option value="int">inteiro</option><option value="text">texto</option>
                        </Select>
                      </td>
                      <td>
                        <div className="row" style={{ gap: 6 }}>
                          <Select aria-label="Regra" style={{ width: 140 }} value={p.rule.kind} disabled={!editable} onChange={(e) => up({ rule: { kind: e.target.value as ParamSpec["rule"]["kind"] } })}>
                            <option value="range">intervalo</option><option value="set">conjunto</option><option value="entity">empresas fictícias</option>
                            <option value="constant">constante (norma)</option><option value="expr">expressão</option>
                          </Select>
                          {p.rule.kind === "range" && <>
                            <Input aria-label="Mínimo" style={{ width: 110 }} value={p.rule.min ?? ""} disabled={!editable} onChange={(e) => upRule({ min: Number(e.target.value) })} />
                            <Input aria-label="Máximo" style={{ width: 110 }} value={p.rule.max ?? ""} disabled={!editable} onChange={(e) => upRule({ max: Number(e.target.value) })} />
                            <Input aria-label="Passo" style={{ width: 90 }} value={p.rule.step ?? ""} disabled={!editable} onChange={(e) => upRule({ step: Number(e.target.value) })} />
                          </>}
                          {p.rule.kind === "set" && <Input aria-label="Valores separados por vírgula" value={(p.rule.values ?? []).join(", ")} disabled={!editable}
                            onChange={(e) => upRule({ values: e.target.value.split(",").map((v) => v.trim()).filter(Boolean).map((v) => (isNaN(Number(v)) ? v : Number(v))) })} />}
                          {p.rule.kind === "entity" && <span className="small muted">Empresas fictícias aprovadas</span>}
                          {p.rule.kind === "constant" && <>
                            <Input aria-label="Valor" style={{ width: 90 }} value={p.rule.value ?? ""} disabled={!editable} onChange={(e) => upRule({ value: isNaN(Number(e.target.value)) ? e.target.value : Number(e.target.value) })} />
                            <Select aria-label="Fonte normativa" value={p.rule.legalSourceId ?? ""} disabled={!editable} onChange={(e) => upRule({ legalSourceId: e.target.value || undefined })}>
                              <option value="">Sem fonte</option>{sources.map((src) => <option key={src.id} value={src.id}>{src.title} · v. {src.version}</option>)}
                            </Select>
                          </>}
                          {p.rule.kind === "expr" && <Input aria-label="Expressão" className="mono" value={p.rule.expr ?? ""} disabled={!editable} onChange={(e) => upRule({ expr: e.target.value })} />}
                        </div>
                      </td>
                      <td><input type="checkbox" aria-label="Visível ao estudante" checked={p.visible !== false} disabled={!editable} onChange={(e) => up({ visible: e.target.checked })} /></td>
                      <td>{editable && <Button size="sm" variant="ghost" icon="trash" aria-label="Remover parâmetro" onClick={() => set("params", s.params.filter((_, j) => j !== i))} />}</td>
                    </tr>
                  );
                })}
                {s.params.length === 0 && <tr><td colSpan={5} className="small muted">Sem parâmetros: o exercício é igual para todos.</td></tr>}
              </tbody>
            </table>
          </div>
          <p className="small muted">A solução é calculada no servidor a partir dos parâmetros; nunca é enviada ao cliente antes da submissão.</p>
        </Card>

        <Card className="stack">
          <CardHeader title="Solução esperada e tolerância" />
          {s.type === "A" && (
            <div className="stack">
              {s.options.map((o, i) => (
                <div key={i} className="row" style={{ gap: 8 }}>
                  <span className="mono" style={{ width: 24 }}>{o.key}</span>
                  <Input aria-label={`Texto da opção ${o.key}`} value={o.text} disabled={!editable} onChange={(e) => set("options", s.options.map((x, j) => (j === i ? { ...x, text: e.target.value } : x)))} />
                  <label className="check" style={{ paddingTop: 0 }}><input type="checkbox" checked={s.correct.includes(o.key)} disabled={!editable}
                    onChange={(e) => set("correct", e.target.checked ? [...s.correct, o.key] : s.correct.filter((k) => k !== o.key))} />Correcta</label>
                </div>
              ))}
              {editable && <div className="row"><Button size="sm" variant="ghost" icon="plus" onClick={() => set("options", [...s.options, { key: String.fromCharCode(65 + s.options.length), text: "" }])}>Opção</Button>
                <label className="check"><input type="checkbox" checked={s.multiple} onChange={(e) => set("multiple", e.target.checked)} />Várias respostas</label></div>}
            </div>
          )}
          {s.type === "B" && (
            <div className="grid g3" style={{ gap: 12 }}>
              <Field label="Expressão da solução">{({ id }) => <Input id={id} className="mono" value={s.expr} disabled={!editable} onChange={(e) => set("expr", e.target.value)} />}</Field>
              <Field label="Casas decimais">{({ id }) => <Input id={id} value={s.scale} disabled={!editable} onChange={(e) => set("scale", e.target.value)} />}</Field>
              <Field label="Unidade">{({ id }) => <Input id={id} value={s.unit} disabled={!editable} onChange={(e) => set("unit", e.target.value)} />}</Field>
            </div>
          )}
          {s.type === "C" && (
            <div className="tbl-wrap">
              <table className="tbl">
                <thead><tr><th>Conta</th><th>Lado</th><th>Valor (expressão)</th><th /></tr></thead>
                <tbody>
                  {s.lines.map((l, i) => (
                    <tr key={i}>
                      <td style={{ width: 120 }}><Input aria-label="Conta" className="mono" value={l.account} disabled={!editable} onChange={(e) => set("lines", s.lines.map((x, j) => (j === i ? { ...x, account: e.target.value } : x)))} /></td>
                      <td style={{ width: 110 }}><Select aria-label="Lado" value={l.side} disabled={!editable} onChange={(e) => set("lines", s.lines.map((x, j) => (j === i ? { ...x, side: e.target.value as "D" | "C" } : x)))}><option value="D">Débito</option><option value="C">Crédito</option></Select></td>
                      <td><Input aria-label="Expressão" className="mono" value={l.expr} disabled={!editable} onChange={(e) => set("lines", s.lines.map((x, j) => (j === i ? { ...x, expr: e.target.value } : x)))} /></td>
                      <td>{editable && <Button size="sm" variant="ghost" icon="trash" aria-label="Remover linha" onClick={() => set("lines", s.lines.filter((_, j) => j !== i))} />}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {editable && <div style={{ padding: 8 }}><Button size="sm" variant="ghost" icon="plus" onClick={() => set("lines", [...s.lines, { account: "", side: "C", expr: "" }])}>Linha</Button></div>}
            </div>
          )}
          {!STRUCTURED.includes(s.type) && (
            <div className="grid g2" style={{ gap: 12 }}>
              <Field label="Apresentação (JSON)" hint="Estrutura visível ao estudante: passos, rubricas, balancete.">{({ id, describedBy }) => <TextArea id={id} aria-describedby={describedBy} className="mono" style={{ minHeight: 220, fontSize: 12.5 }} value={s.presentationJson} disabled={!editable} onChange={(e) => set("presentationJson", e.target.value)} />}</Field>
              <Field label="Solução (JSON)" hint="Expressões sobre os parâmetros. Nunca é enviada ao estudante.">{({ id, describedBy }) => <TextArea id={id} aria-describedby={describedBy} className="mono" style={{ minHeight: 220, fontSize: 12.5 }} value={s.solutionJson} disabled={!editable} onChange={(e) => set("solutionJson", e.target.value)} />}</Field>
            </div>
          )}
          <div className="row" style={{ gap: 12 }}>
            <Field label="Tolerância">{({ id }) => <Select id={id} value={s.tolType} disabled={!editable} onChange={(e) => set("tolType", e.target.value)}><option value="ABS">Absoluta</option><option value="REL">Relativa</option></Select>}</Field>
            <Field label="Valor">{({ id }) => <Input id={id} className="mono" value={s.tolValue} disabled={!editable} onChange={(e) => set("tolValue", e.target.value)} />}</Field>
          </div>
          <Field label="Dicas (uma por linha · cada uma retira 5 pts)">{({ id }) => <TextArea id={id} value={s.hints} disabled={!editable} onChange={(e) => set("hints", e.target.value)} />}</Field>
        </Card>
      </div>

      <aside className="stack" style={{ gap: 16 }}>
        <Card className="stack">
          <CardHeader title="Metadados" />
          <Field label="Tema">{({ id }) => <Input id={id} value={s.topic} disabled={!editable} onChange={(e) => set("topic", e.target.value)} />}</Field>
          <div className="field"><span className="small" style={{ fontWeight: 600 }}>Dificuldade</span>
            <Seg label="Dificuldade" value={String(s.difficulty)} onChange={(v) => editable && set("difficulty", Number(v))} options={["1", "2", "3", "4"].map((d) => ({ value: d, label: d }))} /></div>
          <div className="grid g2" style={{ gap: 10 }}>
            <Field label="Pontos">{({ id }) => <Input id={id} className="mono" value={s.points} disabled={!editable} onChange={(e) => set("points", Number(e.target.value) || 0)} />}</Field>
            <Field label="Tentativas">{({ id }) => <Input id={id} className="mono" value={s.maxAttempts} disabled={!editable} onChange={(e) => set("maxAttempts", Number(e.target.value) || 1)} />}</Field>
          </div>
          <Field label="Feedback">{({ id }) => <Select id={id} value={s.feedbackPolicy} disabled={!editable} onChange={(e) => set("feedbackPolicy", e.target.value)}><option value="DETAILED">Detalhe por linha</option><option value="BINARY">Só certo/errado</option></Select>}</Field>
        </Card>
        <Card className="stack">
          <CardHeader title="Referência normativa">{editable && <Button size="sm" variant="ghost" icon="plus" onClick={() => set("references", [...s.references, { legalSourceId: "", articles: "", consultedAt: "" }])}>Fonte</Button>}</CardHeader>
          {s.references.map((r, i) => (
            <div key={i} className="stack card flat" style={{ padding: 12 }}>
              <Field label="Fonte">{({ id }) => (
                <Select id={id} value={r.legalSourceId} disabled={!editable} onChange={(e) => set("references", s.references.map((x, j) => (j === i ? { ...x, legalSourceId: e.target.value } : x)))}>
                  <option value="">Escolhe…</option>{sources.map((src) => <option key={src.id} value={src.id}>{src.title} · {src.identifier}</option>)}
                </Select>
              )}</Field>
              <Field label="Artigos">{({ id }) => <Input id={id} value={r.articles} disabled={!editable} onChange={(e) => set("references", s.references.map((x, j) => (j === i ? { ...x, articles: e.target.value } : x)))} />}</Field>
              <Field label="Consultado em">{({ id }) => <Input id={id} type="date" value={r.consultedAt} disabled={!editable} onChange={(e) => set("references", s.references.map((x, j) => (j === i ? { ...x, consultedAt: e.target.value } : x)))} />}</Field>
              {(() => { const src = sources.find((x) => x.id === r.legalSourceId); return src && !src.confirmed ? <Alert tone="acc" title="Referência por confirmar">O revisor tem de validar a versão em vigor antes da aprovação.</Alert> : null; })()}
            </div>
          ))}
          {s.references.length === 0 && <p className="small muted">Sem referência: a publicação falha até acrescentares uma fonte.</p>}
        </Card>
        {exercise && canReview && !isAuthor && (status === "IN_REVIEW" || status === "APPROVED") && (
          <Card lit className="stack">
            <CardHeader title="Revisão" />
            <Field label="Notas para o autor">{({ id }) => <TextArea id={id} value={notes} onChange={(e) => setNotes(e.target.value)} />}</Field>
            {status === "IN_REVIEW" && <>
              <label className="check"><input type="checkbox" checked={checked} onChange={(e) => setChecked(e.target.checked)} />Validei a versão em vigor das referências</label>
              <div className="row"><Button variant="out" onClick={() => transition("DRAFT", { notes })}>Devolver ao autor</Button>
                <Button variant="pri" disabled={!checked} onClick={() => transition("APPROVED", { notes, referencesChecked: true })}>Aprovar</Button></div>
            </>}
            {status === "APPROVED" && <div className="row"><Button variant="out" onClick={() => transition("DRAFT", { notes })}>Devolver ao autor</Button><Button variant="pri" onClick={() => transition("PUBLISHED")}>Publicar</Button></div>}
          </Card>
        )}
        {exercise && canReview && status === "PUBLISHED" && <Button variant="ghost" onClick={() => transition("ARCHIVED")}>Arquivar</Button>}
        {exercise && (
          <Card className="stack">
            <CardHeader title="Versões">{isAuthor && status !== "DRAFT" && <Button size="sm" variant="ghost" icon="plus" onClick={() => run(async () => { await post(`/authoring/exercises/${exercise.id}/versions`); router.refresh(); })}>Nova versão</Button>}</CardHeader>
            <ul className="plain stack small">
              {exercise.versions.map((v) => (
                <li key={v.id}><button type="button" className={["task", v.number === version?.number && "cur"].filter(Boolean).join(" ")} style={{ width: "100%", background: "none", border: 0, textAlign: "left" }}
                  onClick={() => { setVersionNo(v.number); setS(fromVersion(v, exercise.topic)); }}>
                  <span className="mono">v{v.number}</span><span style={{ flex: 1 }}>{v.changeNote ?? STATUS_LABEL[v.status]}</span><span className="muted">{formatDate(v.updatedAt)}</span>
                </button></li>
              ))}
            </ul>
          </Card>
        )}
      </aside>

      <Modal open={!!preview} onClose={() => setPreview(null)} title="Pré-visualização como estudante" actions={<Button variant="out" onClick={() => setPreview(null)}>Fechar</Button>}>
        {preview && (
          <div className="stack" style={{ color: "var(--text)" }}>
            <StatementText segments={preview.statementSegments} fallback={preview.statementRendered} />
            {preview.selfCheck === "OK" ? <Badge tone="pos">A solução esperada passa na correcção automática</Badge> : <Alert tone="neg" title="Auto-verificação">{preview.selfCheck}</Alert>}
            <details><summary className="small">Resposta esperada (só autor/revisor) <Tbc>não enviada ao estudante</Tbc></summary>
              <pre className="mono small" style={{ whiteSpace: "pre-wrap" }}>{JSON.stringify(preview.expectedAnswer, null, 2)}</pre></details>
          </div>
        )}
      </Modal>
    </div>
  );
}
