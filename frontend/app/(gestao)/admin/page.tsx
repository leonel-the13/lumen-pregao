import Link from "next/link";
import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { ContestsAdmin, type AdminContestRow } from "@/components/admin/ContestsAdmin";
import { ContentAdmin, type ExamAdmin, type TrackAdmin } from "@/components/admin/ContentAdmin";
import { UsersTable, type AdminUserRow } from "@/components/admin/UsersTable";
import { InviteUser } from "@/components/admin/InviteUser";
import { FeedbackAdmin, type FeedbackRow } from "@/components/admin/FeedbackAdmin";
import { EmailsAdmin, type CampaignRow, type EmailTemplateView, type EmailVariable, type ReminderStats } from "@/components/admin/EmailsAdmin";
import { Card } from "@/components/ui/Card";
import { Tabs } from "@/components/ui/Tabs";
import { AccessDenied, LaterPhase } from "@/components/ui/States";
import { serverFetch } from "@/lib/api-server";
import type { ExerciseSummary } from "@/lib/authoring-types";
import { formatDateTime } from "@/lib/format";
import { hasAny, requireMe } from "@/lib/session";
import type { CursorPage } from "@/lib/types";

export const metadata = { title: "Administração" };

interface AuditRow { id: string; at: string; actorName: string; action: string; entityType: string; entityId?: string; ip?: string }

const MATRIX: [string, string[]][] = [
  ["Salas · resolver", ["R", "R", "R", "—", "—", "R"]], ["Exercícios", ["—", "C E", "R A", "—", "—", "A"]], ["Publicar conteúdo", ["—", "—", "P", "—", "—", "P"]],
  ["Contests", ["R", "—", "—", "R", "C E P", "C E P"]], ["Correcção de casos", ["—", "—", "—", "E", "R", "R"]],
  ["Utilizadores e perfis", ["—", "—", "—", "—", "—", "C E"]], ["Auditoria", ["—", "—", "—", "—", "—", "R"]],
];

export default async function Admin({ searchParams }: { searchParams: Promise<{ tab?: string; q?: string; cursor?: string; estado?: string; para?: string; nome?: string }> }) {
  const sp = await searchParams;
  const me = await requireMe("/admin");
  const admin = hasAny(me, "ADMIN");
  const organizer = hasAny(me, "CONTEST_ORGANIZER", "ADMIN");
  if (!organizer) return <AccessDenied area="A administração" />;
  const tab = sp.tab ?? (admin ? "utilizadores" : "contests");
  const tabs = [
    ...(admin ? [{ key: "utilizadores", label: "Utilizadores", href: "/admin?tab=utilizadores" }, { key: "perfis", label: "Perfis", href: "/admin?tab=perfis" }] : []),
    { key: "contests", label: "Contests", href: "/admin?tab=contests" },
    ...(admin ? [{ key: "auditoria", label: "Auditoria", href: "/admin?tab=auditoria" }, { key: "conteudos", label: "Conteúdos", href: "/admin?tab=conteudos" },
      { key: "emails", label: "Emails", href: "/admin?tab=emails" },
      { key: "feedback", label: "Feedback", href: "/admin?tab=feedback" },
      { key: "configuracoes", label: "Configurações", href: "/admin?tab=configuracoes" }] : []),
  ];
  return (
    <>
      <SetCrumbs items={[{ label: "Gestão" }, { label: "Administração" }]} />
      <div className="stack" style={{ gap: 6 }}><div className="label">Administração · Associação de Estudantes</div><h1 className="h1">Utilizadores e permissões</h1></div>
      <Tabs label="Administração" active={tab} items={tabs} />
      {tab === "utilizadores" && admin && <Users q={sp.q} cursor={sp.cursor} me={me.id} />}
      {tab === "perfis" && admin && (
        <Card className="stack">
          <div className="row"><h2 className="h2">Matriz de permissões</h2><span className="small muted">verificada no servidor em cada endpoint</span></div>
          <div className="tbl-wrap"><table className="tbl">
            <thead><tr><th>Recurso</th>{["Est.", "Autor", "Rev.", "Júri", "Org.", "Admin"].map((h) => <th key={h}>{h}</th>)}</tr></thead>
            <tbody>{MATRIX.map(([r, cells]) => <tr key={r}><td>{r}</td>{cells.map((c, i) => <td key={i} className="mono small">{c}</td>)}</tr>)}</tbody>
          </table></div>
          <p className="small muted">R ler · C criar · E editar · A aprovar · P publicar/administrar · propriedade dos recursos verificada à parte.</p>
        </Card>
      )}
      {tab === "contests" && <Contests />}
      {tab === "auditoria" && admin && <Audit cursor={sp.cursor} />}
      {tab === "conteudos" && admin && <Content />}
      {tab === "emails" && admin && <Emails cursor={sp.cursor} para={sp.para} nome={sp.nome} />}
      {tab === "feedback" && admin && <Feedback estado={sp.estado} cursor={sp.cursor} />}
      {tab === "configuracoes" && admin && <LaterPhase title="Configurações gerais" body="Domínio institucional, limites de rate limiting e valores de pontuação estão em variáveis de ambiente e ficam a confirmar." />}
    </>
  );
}

async function Users({ q, cursor, me }: { q?: string; cursor?: string; me: string }) {
  const [page, courses] = await Promise.all([
    serverFetch<CursorPage<AdminUserRow>>(`/admin/users?limit=25${q ? `&q=${encodeURIComponent(q)}` : ""}${cursor ? `&cursor=${cursor}` : ""}`),
    serverFetch<{ id: string; name: string }[]>("/courses"),
  ]);
  return (
    <div className="stack">
      <form className="row" role="search">
        <input type="hidden" name="tab" value="utilizadores" />
        <label className="sr-only" htmlFor="uq">Pesquisar utilizadores</label>
        <input id="uq" name="q" className="input" style={{ maxWidth: 360 }} placeholder="Nome ou email" defaultValue={q} />
        <button className="btn out" type="submit">Pesquisar</button>
        <div className="spacer" />
        <InviteUser courses={courses} />
      </form>
      <UsersTable rows={page.items} me={me} />
      {page.nextCursor && <div className="row"><div className="spacer" /><Link className="btn sm out" href={`/admin?tab=utilizadores&cursor=${page.nextCursor}${q ? `&q=${q}` : ""}`}>Seguinte</Link></div>}
    </div>
  );
}

async function Contests() {
  const [rows, exercises] = await Promise.all([
    serverFetch<AdminContestRow[]>("/admin/contests"),
    serverFetch<ExerciseSummary[]>("/authoring/exercises").catch(() => [] as ExerciseSummary[]),
  ]);
  const published = exercises.filter((e) => e.status === "PUBLISHED").map((e) => ({ versionId: e.latestVersionId, label: `${e.code} · ${e.title}` }));
  return <ContestsAdmin rows={rows} exercises={published} />;
}

async function Content() {
  const [tracks, exams, exercises] = await Promise.all([
    serverFetch<TrackAdmin[]>("/admin/content/tracks"),
    serverFetch<ExamAdmin[]>("/admin/content/exams"),
    serverFetch<ExerciseSummary[]>("/authoring/exercises"),
  ]);
  return <ContentAdmin tracks={tracks} exams={exams} inReview={exercises.filter((e) => e.status === "IN_REVIEW").length}
    exercises={exercises.map((e) => ({ versionId: e.latestVersionId, label: `${e.code} · ${e.title}`, status: e.status }))} />;
}

async function Emails({ cursor, para, nome }: { cursor?: string; para?: string; nome?: string }) {
  const [templates, stats, campaigns, courses, variables] = await Promise.all([
    serverFetch<EmailTemplateView[]>("/admin/email-templates"),
    serverFetch<ReminderStats>("/admin/emails/reminders"),
    serverFetch<CursorPage<CampaignRow>>(`/admin/emails/campaigns?limit=20${cursor ? `&cursor=${cursor}` : ""}`),
    serverFetch<{ id: string; name: string }[]>("/courses"),
    serverFetch<EmailVariable[]>("/admin/emails/variables"),
  ]);
  const preset = para && /^[0-9a-f-]{36}$/i.test(para) ? { id: para, name: nome || "Pessoa escolhida" } : undefined;
  return <EmailsAdmin templates={templates} stats={stats} campaigns={campaigns.items} nextCursor={campaigns.nextCursor ?? undefined}
    courses={courses} variables={variables} preset={preset} />;
}

async function Feedback({ estado, cursor }: { estado?: string; cursor?: string }) {
  const status = estado ?? "NEW";
  const qs = new URLSearchParams({ limit: "30", ...(status !== "ALL" ? { status } : {}), ...(cursor ? { cursor } : {}) });
  const page = await serverFetch<CursorPage<FeedbackRow>>(`/admin/feedback?${qs}`);
  const filters = [["NEW", "Novos"], ["READ", "Lidos"], ["RESOLVED", "Resolvidos"], ["ALL", "Todos"]];
  return (
    <div className="stack">
      <nav className="row" style={{ gap: 6 }} aria-label="Filtrar feedback">
        {filters.map(([k, l]) => (
          <Link key={k} className={`btn sm ${k === status ? "pri" : "ghost"}`} aria-current={k === status ? "page" : undefined}
            href={`/admin?tab=feedback&estado=${k}`}>{l}</Link>
        ))}
      </nav>
      <FeedbackAdmin rows={page.items} />
      {page.nextCursor && <Link className="btn out" href={`/admin?tab=feedback&estado=${status}&cursor=${page.nextCursor}`}>Mais antigos</Link>}
    </div>
  );
}

async function Audit({ cursor }: { cursor?: string }) {
  const page = await serverFetch<CursorPage<AuditRow>>(`/admin/audit?limit=30${cursor ? `&cursor=${cursor}` : ""}`);
  return (
    <div className="stack">
      <div className="row"><h2 className="h2">Auditoria</h2><div className="spacer" /><a className="btn out" href="/api/v1/admin/audit/export" download>Exportar</a></div>
      <div className="tbl-wrap"><table className="tbl">
        <thead><tr><th>Data/hora</th><th>Responsável</th><th>Operação</th><th>Entidade</th></tr></thead>
        <tbody>{page.items.map((a) => (
          <tr key={a.id}><td className="mono small">{formatDateTime(a.at)}</td><td>{a.actorName}</td><td className="mono small">{a.action}</td>
            <td className="small muted">{a.entityType}{a.entityId ? ` · ${a.entityId.slice(0, 8)}` : ""}</td></tr>
        ))}</tbody>
      </table></div>
      {page.nextCursor && <div className="row"><div className="spacer" /><Link className="btn sm out" href={`/admin?tab=auditoria&cursor=${page.nextCursor}`}>Seguinte</Link></div>}
    </div>
  );
}
