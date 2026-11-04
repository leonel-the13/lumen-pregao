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
import { getI18n } from "@/lib/i18n/server";

export async function generateMetadata() {
  const { t } = await getI18n();
  return { title: t("admin.page.title") };
}

interface AuditRow { id: string; at: string; actorName: string; action: string; entityType: string; entityId?: string; ip?: string }

const MATRIX: ["rooms" | "exercises" | "publish" | "contests" | "grading" | "users" | "audit", string[]][] = [
  ["rooms", ["R", "R", "R", "—", "—", "R"]], ["exercises", ["—", "C E", "R A", "—", "—", "A"]], ["publish", ["—", "—", "P", "—", "—", "P"]],
  ["contests", ["R", "—", "—", "R", "C E P", "C E P"]], ["grading", ["—", "—", "—", "E", "R", "R"]],
  ["users", ["—", "—", "—", "—", "—", "C E"]], ["audit", ["—", "—", "—", "—", "—", "R"]],
];
const COLS = ["student", "author", "reviewer", "jury", "organizer", "admin"] as const;

export default async function Admin({ searchParams }: { searchParams: Promise<{ tab?: string; q?: string; cursor?: string; estado?: string; para?: string; nome?: string }> }) {
  const sp = await searchParams;
  const [me, { t }] = await Promise.all([requireMe("/admin"), getI18n()]);
  const admin = hasAny(me, "ADMIN");
  const organizer = hasAny(me, "CONTEST_ORGANIZER", "ADMIN");
  if (!organizer) return <AccessDenied area={t("admin.page.area")} />;
  const tab = sp.tab ?? (admin ? "utilizadores" : "contests");
  const keys = admin ? (["utilizadores", "perfis", "contests", "auditoria", "conteudos", "emails", "feedback", "configuracoes"] as const) : (["contests"] as const);
  const tabs = keys.map((k) => ({ key: k, label: t(`admin.page.tabs.${k}`), href: `/admin?tab=${k}` }));
  return (
    <>
      <SetCrumbs items={[{ label: t("admin.page.group") }, { label: t("admin.page.title") }]} />
      <div className="stack" style={{ gap: 6 }}><div className="label">{t("admin.page.eyebrow")}</div><h1 className="h1">{t("admin.page.heading")}</h1></div>
      <Tabs label={t("admin.page.title")} active={tab} items={tabs} />
      {tab === "utilizadores" && admin && <Users q={sp.q} cursor={sp.cursor} me={me.id} />}
      {tab === "perfis" && admin && (
        <Card className="stack">
          <div className="row"><h2 className="h2">{t("admin.page.matrix")}</h2><span className="small muted">{t("admin.page.matrixNote")}</span></div>
          <div className="tbl-wrap"><table className="tbl">
            <thead><tr><th>{t("admin.page.resource")}</th>{COLS.map((h) => <th key={h}>{t(`admin.page.cols.${h}`)}</th>)}</tr></thead>
            <tbody>{MATRIX.map(([r, cells]) => <tr key={r}><td>{t(`admin.page.rows.${r}`)}</td>{cells.map((c, i) => <td key={i} className="mono small">{c}</td>)}</tr>)}</tbody>
          </table></div>
          <p className="small muted">{t("admin.page.legend")}</p>
        </Card>
      )}
      {tab === "contests" && <Contests />}
      {tab === "auditoria" && admin && <Audit cursor={sp.cursor} />}
      {tab === "conteudos" && admin && <Content />}
      {tab === "emails" && admin && <Emails cursor={sp.cursor} para={sp.para} nome={sp.nome} chosen={t("admin.page.chosenPerson")} />}
      {tab === "feedback" && admin && <Feedback estado={sp.estado} cursor={sp.cursor} />}
      {tab === "configuracoes" && admin && <LaterPhase title={t("admin.page.settings")} body={t("admin.page.settingsBody")} />}
    </>
  );
}

async function Users({ q, cursor, me }: { q?: string; cursor?: string; me: string }) {
  const [page, courses, { t }] = await Promise.all([
    serverFetch<CursorPage<AdminUserRow>>(`/admin/users?limit=25${q ? `&q=${encodeURIComponent(q)}` : ""}${cursor ? `&cursor=${cursor}` : ""}`),
    serverFetch<{ id: string; name: string }[]>("/courses"),
    getI18n(),
  ]);
  return (
    <div className="stack">
      {/* O convite fica fora do <form> da pesquisa: o modal dele também seria parte do formulário. */}
      <div className="row">
        <form className="row" role="search">
          <input type="hidden" name="tab" value="utilizadores" />
          <label className="sr-only" htmlFor="uq">{t("admin.page.searchUsers")}</label>
          <input id="uq" name="q" className="input" style={{ maxWidth: 360 }} placeholder={t("admin.page.searchPlaceholder")} defaultValue={q} />
          <button className="btn out" type="submit">{t("common.search")}</button>
        </form>
        <div className="spacer" />
        <InviteUser courses={courses} />
      </div>
      <UsersTable rows={page.items} me={me} />
      {page.nextCursor && <div className="row"><div className="spacer" /><Link className="btn sm out" href={`/admin?tab=utilizadores&cursor=${page.nextCursor}${q ? `&q=${q}` : ""}`}>{t("common.next")}</Link></div>}
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

async function Emails({ cursor, para, nome, chosen }: { cursor?: string; para?: string; nome?: string; chosen: string }) {
  const [templates, stats, campaigns, courses, variables] = await Promise.all([
    serverFetch<EmailTemplateView[]>("/admin/email-templates"),
    serverFetch<ReminderStats>("/admin/emails/reminders"),
    serverFetch<CursorPage<CampaignRow>>(`/admin/emails/campaigns?limit=20${cursor ? `&cursor=${cursor}` : ""}`),
    serverFetch<{ id: string; name: string }[]>("/courses"),
    serverFetch<EmailVariable[]>("/admin/emails/variables"),
  ]);
  const preset = para && /^[0-9a-f-]{36}$/i.test(para) ? { id: para, name: nome || chosen } : undefined;
  return <EmailsAdmin templates={templates} stats={stats} campaigns={campaigns.items} nextCursor={campaigns.nextCursor ?? undefined}
    courses={courses} variables={variables} preset={preset} />;
}

async function Feedback({ estado, cursor }: { estado?: string; cursor?: string }) {
  const status = estado ?? "NEW";
  const qs = new URLSearchParams({ limit: "30", ...(status !== "ALL" ? { status } : {}), ...(cursor ? { cursor } : {}) });
  const [page, { t }] = await Promise.all([serverFetch<CursorPage<FeedbackRow>>(`/admin/feedback?${qs}`), getI18n()]);
  const filters = ["NEW", "READ", "RESOLVED", "ALL"] as const;
  return (
    <div className="stack">
      <nav className="row" style={{ gap: 6 }} aria-label={t("admin.page.feedbackFilter")}>
        {filters.map((k) => (
          <Link key={k} className={`btn sm ${k === status ? "pri" : "ghost"}`} aria-current={k === status ? "page" : undefined}
            href={`/admin?tab=feedback&estado=${k}`}>{t(`admin.page.feedbackStates.${k}`)}</Link>
        ))}
      </nav>
      <FeedbackAdmin rows={page.items} />
      {page.nextCursor && <Link className="btn out" href={`/admin?tab=feedback&estado=${status}&cursor=${page.nextCursor}`}>{t("admin.page.older")}</Link>}
    </div>
  );
}

async function Audit({ cursor }: { cursor?: string }) {
  const [page, { t, locale }] = await Promise.all([serverFetch<CursorPage<AuditRow>>(`/admin/audit?limit=30${cursor ? `&cursor=${cursor}` : ""}`), getI18n()]);
  return (
    <div className="stack">
      <div className="row"><h2 className="h2">{t("admin.page.audit")}</h2><div className="spacer" /><a className="btn out" href="/api/v1/admin/audit/export" download>{t("admin.page.export")}</a></div>
      <div className="tbl-wrap"><table className="tbl">
        <thead><tr><th>{t("admin.page.when")}</th><th>{t("admin.page.actor")}</th><th>{t("admin.page.operation")}</th><th>{t("admin.page.entity")}</th></tr></thead>
        <tbody>{page.items.map((a) => (
          <tr key={a.id}><td className="mono small">{formatDateTime(a.at, locale)}</td><td>{a.actorName}</td><td className="mono small">{a.action}</td>
            <td className="small muted">{a.entityType}{a.entityId ? ` · ${a.entityId.slice(0, 8)}` : ""}</td></tr>
        ))}</tbody>
      </table></div>
      {page.nextCursor && <div className="row"><div className="spacer" /><Link className="btn sm out" href={`/admin?tab=auditoria&cursor=${page.nextCursor}`}>{t("common.next")}</Link></div>}
    </div>
  );
}
