import Image from "next/image";
import Link from "next/link";
import { StarRating } from "@/components/feedback/StarRating";
import { CountUp } from "@/components/ui/CountUp";
import { serverFetchOptional } from "@/lib/api-server";
import { Icon } from "@/components/ui/Icon";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { Reveal } from "@/components/ui/Reveal";
import { HTML_LANG } from "@/lib/i18n/config";
import { getI18n } from "@/lib/i18n/server";

const TRACKS = [
  { level: "beginner", available: true, n: 1 },
  { level: "intermediate", available: true, n: 2 },
  { level: "intermediate", available: true, n: 3 },
  { level: "intermediate", available: false, n: 4 },
  { level: "advanced", available: false, n: 5 },
  { level: "advanced", available: false, n: 6 },
] as const;

interface PublicStats { tracks: number; rooms: number; exercises: number; students: number; gradedSubmissions: number; ratingCount: number; ratingAverage: number }

const FAQ = [1, 2, 3, 4, 5, 6] as const;

export default async function Landing() {
  const { t, locale } = await getI18n();
  const stats = await serverFetchOptional<PublicStats>("/public/stats").catch(() => null);
  const rating = stats && stats.ratingCount >= 5 ? Number(stats.ratingAverage) : null;
  return (
    <div className="stack" style={{ gap: 72, padding: "56px clamp(18px,4vw,56px)", maxWidth: 1360, margin: "0 auto" }}>
      <section className="grid" style={{ gridTemplateColumns: "repeat(auto-fit,minmax(min(100%,460px),1fr))", gap: 40, alignItems: "center" }}>
        <div className="stack" style={{ gap: 20 }}>
          <div className="label">{t("landing.hero.eyebrow")}</div>
          <h1 className="d1"><span className="hero-line">{t("landing.hero.line1")}</span><span className="hero-line">{t("landing.hero.line2")}<span className="serif">{t("landing.hero.line2em")}</span></span></h1>
          <p className="body-l muted hero-sub" style={{ maxWidth: 560 }}>
            {t("landing.hero.sub")}
          </p>
          <div className="row hero-cta">
            <Link className="btn pri" href="/registo">{t("landing.hero.cta")} <Icon name="arrowRight" /></Link>
            <Link className="btn out" href="#como-funciona">{t("landing.hero.how")}</Link>
          </div>
          <p className="small muted">{t("landing.hero.note")} <span className="mono">20221987@isptec.co.ao</span></p>
        </div>
        <Card lit aria-label={t("landing.demo.label")} className="stack demo">
          <div className="row"><span className="label">{t("landing.demo.room")}</span><div className="spacer" /><span className="badge mono">{t("landing.demo.task")}</span></div>
          <p className="small">{t("landing.demo.statementPre")} <b>Distribuidora Mussulo, Lda.</b> <span className="tbc">{t("landing.demo.fictional")}</span> {t("landing.demo.statementPost")}</p>
          <div className="tbl-wrap">
            <table className="tbl" style={{ minWidth: 0 }}>
              <thead><tr><th>{t("landing.demo.account")}</th><th className="num">{t("landing.demo.debit")}</th><th className="num">{t("landing.demo.credit")}</th></tr></thead>
              <tbody>
                <tr><td><span className="acct">26</span> {t("landing.demo.goods")}</td><td className="num">500 000,00</td><td /></tr>
                <tr><td><span className="acct">34.5.x</span> {t("landing.demo.vat")}</td><td className="num">70 000,00</td><td /></tr>
                <tr><td><span className="acct">32</span> {t("landing.demo.suppliers")}</td><td /><td className="num">570 000,00</td></tr>
              </tbody>
              <tfoot><tr><td>{t("landing.demo.totals")} <span className="badge pos badge-balance" style={{ marginLeft: 8 }}>{t("landing.demo.balanced")}</span></td>
                <td className="num">570 000,00</td><td className="num">570 000,00</td></tr></tfoot>
            </table>
          </div>
          <div className="alert pos"><Icon name="check" /><div><b>{t("landing.demo.correct")}</b>{t("landing.demo.correctBody")}</div></div>
        </Card>
      </section>

      {stats && stats.tracks > 0 && (
        <Reveal as="div" className="stats-band grid g4 stagger" aria-label={t("landing.stats.label")}>
          {([[t("landing.stats.tracks"), stats.tracks], [t("landing.stats.rooms"), stats.rooms], [t("landing.stats.exercises"), stats.exercises],
            [t("landing.stats.graded"), stats.gradedSubmissions]] as [string, number][]).map(([l, v]) => (
            <div key={l} className="stack" style={{ gap: 4 }}>
              <span className="stat-v mono"><CountUp value={v} /></span><span className="small muted">{l}</span>
            </div>
          ))}
        </Reveal>
      )}

      <Reveal className="campus grid" style={{ gridTemplateColumns: "repeat(auto-fit,minmax(min(100%,420px),1fr))", gap: 32, alignItems: "center" }}>
        <figure className="photo-frame" style={{ margin: 0 }}>
          <Image src="/img/campus-1100.webp" alt={t("landing.campus.alt")}
            width={1100} height={619} sizes="(max-width: 900px) 100vw, 640px" style={{ width: "100%", height: "auto" }} />
        </figure>
        <div className="stack" style={{ gap: 14 }}>
          <div className="label">{t("landing.campus.eyebrow")}</div>
          <h2 className="d2">{t("landing.campus.title")}<span className="serif">{t("landing.campus.titleEm")}</span></h2>
          <p className="muted">{t("landing.campus.body")}</p>
          <ul className="plain stack small" style={{ gap: 8 }}>
            {[t("landing.campus.b1"), t("landing.campus.b2"), t("landing.campus.b3")].map((x) => (
              <li key={x} className="row" style={{ gap: 8 }}><Icon name="check" />{x}</li>
            ))}
          </ul>
        </div>
      </Reveal>

      <Reveal id="como-funciona" className="stack" style={{ gap: 20 }}>
        <div className="label">{t("landing.how.eyebrow")}</div>
        <h2 className="d2">{t("landing.how.title")}<span className="serif">{t("landing.how.titleEm")}</span></h2>
        <div className="grid g4 stagger">
          {[
            ["route", t("landing.how.tracks"), t("landing.how.tracksBody")],
            ["door", t("landing.how.rooms"), t("landing.how.roomsBody")],
            ["building", t("landing.how.sims"), t("landing.how.simsBody")],
            ["trophy", t("landing.how.marathon"), t("landing.how.marathonBody")],
          ].map(([icon, title, b]) => (
            <Card key={title} className="stack hoverable"><Icon name={icon} /><h3 className="h3">{title}</h3><p className="small muted">{b}</p></Card>
          ))}
        </div>
      </Reveal>

      <Reveal id="trilhas" className="stack" style={{ gap: 20 }}>
        <div className="row"><div className="stack" style={{ gap: 6 }}><div className="label">{t("landing.tracks.eyebrow")}</div><h2 className="d2">{t("landing.tracks.title")}</h2></div>
          <div className="spacer" /><Link className="btn out" href="/trilhas">{t("landing.tracks.catalog")}</Link></div>
        <div className="grid g3 stagger">
          {TRACKS.map((tr) => (
            <Card key={tr.n} className="stack hoverable">
              <div className="row" style={{ gap: 8 }}><Badge>{t(`landing.tracks.${tr.level}`)}</Badge><Badge tone={tr.available ? "acc" : "neutral"}>{t(tr.available ? "landing.tracks.available" : "landing.tracks.soon")}</Badge></div>
              <h3 className="h3">{t(`landing.tracks.t${tr.n}`)}</h3><p className="small muted">{t(`landing.tracks.t${tr.n}b`)}</p>
            </Card>
          ))}
        </div>
      </Reveal>

      <Reveal id="maratona" className="card lit grid" style={{ gridTemplateColumns: "repeat(auto-fit,minmax(min(100%,420px),1fr))", gap: 24, alignItems: "center" }}>
        <div className="stack">
          <div className="label">{t("landing.marathon.eyebrow")}</div>
          <h2 className="d2">{t("landing.marathon.title")}<span className="serif">{t("landing.marathon.titleEm")}</span></h2>
          <p className="muted">{t("landing.marathon.body")}</p>
          <p className="small muted">{t("landing.marathon.note")}</p>
        </div>
        <div className="stack" style={{ alignItems: "flex-start" }}>
          <div className="row"><Badge tone="acc">{t("landing.marathon.dates")}</Badge><Badge>{t("landing.marathon.teams")}</Badge></div>
          <span className="timer"><span className="t">-- : -- : --</span><span className="src">{t("landing.marathon.opening")}</span></span>
        </div>
      </Reveal>

      <Reveal className="grid g3 stagger">
        {[
          ["shield", t("landing.trust.reviewed"), t("landing.trust.reviewedBody")],
          ["lock", t("landing.trust.server"), t("landing.trust.serverBody")],
          ["building", t("landing.trust.sim"), t("landing.trust.simBody")],
        ].map(([icon, title, b]) => (
          <Card key={title} flat className="stack hoverable"><Icon name={icon} /><h3 className="h3">{title}</h3><p className="small muted">{b}</p></Card>
        ))}
      </Reveal>

      <Reveal className="stack" style={{ gap: 20 }}>
        <div className="label">{t("landing.steps.eyebrow")}</div>
        <h2 className="d2">{t("landing.steps.title")}<span className="serif">{t("landing.steps.titleEm")}</span></h2>
        <ol className="plain grid g3 stagger steps">
          {[
            [t("landing.steps.s1"), t("landing.steps.s1b")],
            [t("landing.steps.s2"), t("landing.steps.s2b")],
            [t("landing.steps.s3"), t("landing.steps.s3b")],
          ].map(([title, b], i) => (
            <li key={title} className="card stack hoverable"><span className="step-n mono">{String(i + 1).padStart(2, "0")}</span><h3 className="h3">{title}</h3><p className="small muted">{b}</p></li>
          ))}
        </ol>
      </Reveal>

      <Reveal className="card grid" style={{ gridTemplateColumns: "repeat(auto-fit,minmax(min(100%,320px),1fr))", gap: 24, alignItems: "center" }}>
        <div className="stack" style={{ gap: 10 }}>
          <div className="label">{t("landing.ratings.eyebrow")}</div>
          <h2 className="d2">{t("landing.ratings.title")}<span className="serif">{t("landing.ratings.titleEm")}</span></h2>
          <p className="muted">{t("landing.ratings.body")}</p>
        </div>
        <div className="stack" style={{ alignItems: "flex-start", gap: 8 }}>
          {rating !== null ? (
            <>
              <span className="stat-v mono">{rating.toLocaleString(HTML_LANG[locale], { minimumFractionDigits: 1, maximumFractionDigits: 1 })}<span className="muted" style={{ fontSize: "0.5em" }}> / 5</span></span>
              <StarRating value={rating} label={t("landing.ratings.average")} size={30} />
              <span className="small muted">{t("landing.ratings.averageOf", { n: stats?.ratingCount ?? 0 })}</span>
            </>
          ) : (
            <>
              <StarRating value={0} label={t("landing.ratings.notEnough")} size={30} />
              <span className="small muted">{t("landing.ratings.notEnoughBody")}</span>
            </>
          )}
        </div>
      </Reveal>

      <Reveal className="grid" style={{ gridTemplateColumns: "repeat(auto-fit,minmax(min(100%,420px),1fr))", gap: 32, alignItems: "center" }}>
        <div className="stack" style={{ gap: 14 }}>
          <div className="label">{t("landing.who.eyebrow")}</div>
          <h2 className="d2">{t("landing.who.title")}<span className="serif">{t("landing.who.titleEm")}</span></h2>
          <p className="muted">{t("landing.who.body")}</p>
          <p className="small muted">{t("landing.who.note")}</p>
        </div>
        <figure className="photo-frame" style={{ margin: 0 }}>
          <Image src="/img/equipa-1600.webp" alt={t("landing.who.alt")}
            width={1600} height={800} sizes="(max-width: 900px) 100vw, 640px" style={{ width: "100%", height: "auto" }} />
        </figure>
      </Reveal>

      <Reveal id="perguntas" className="stack" style={{ gap: 16, maxWidth: 860 }}>
        <div className="label">{t("landing.faq.eyebrow")}</div>
        <h2 className="d2">{t("landing.faq.title")}<span className="serif">{t("landing.faq.titleEm")}</span></h2>
        <div className="stack faq" style={{ gap: 8 }}>
          {FAQ.map((n) => (
            <details key={n} className="card flat">
              <summary>{t(`landing.faq.q${n}`)}</summary>
              <p className="muted" style={{ margin: "10px 0 0" }}>{t(`landing.faq.a${n}`)}</p>
            </details>
          ))}
        </div>
      </Reveal>

      <Reveal className="card lit stack cta-final" style={{ alignItems: "center", textAlign: "center", gap: 14 }}>
        <h2 className="d2">{t("landing.final.title")}<span className="serif">{t("landing.final.titleEm")}</span></h2>
        <p className="muted" style={{ maxWidth: 520 }}>{t("landing.final.body")}</p>
        <div className="row" style={{ justifyContent: "center" }}>
          <Link className="btn pri" href="/registo">{t("landing.final.signUp")} <Icon name="arrowRight" /></Link>
          <Link className="btn out" href="/entrar">{t("landing.final.haveAccount")}</Link>
        </div>
        <p className="small muted">{t("landing.final.legalPre")}<Link href="/termos">{t("landing.final.terms")}</Link>{t("landing.final.legalMid")}<Link href="/privacidade">{t("landing.final.privacy")}</Link>{t("landing.final.legalPost")}</p>
      </Reveal>

      <p className="credits">{t("landing.credits")}</p>
    </div>
  );
}
