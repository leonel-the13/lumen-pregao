import Image from "next/image";
import Link from "next/link";
import { StarRating } from "@/components/feedback/StarRating";
import { CountUp } from "@/components/ui/CountUp";
import { serverFetchOptional } from "@/lib/api-server";
import { Icon } from "@/components/ui/Icon";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { Reveal } from "@/components/ui/Reveal";

const TRACKS = [
  { level: "Iniciação", tag: "Disponível", title: "Contabilidade Financeira segundo o PGC", body: "Do quadro de contas às demonstrações financeiras: 6 módulos, 15 salas e exame final." },
  { level: "Intermédio", tag: "Disponível", title: "Fiscalidade Angolana · IVA", body: "Liquidação, dedução, apuramento e crédito de imposto, passo a passo." },
  { level: "Intermédio", tag: "Disponível", title: "Análise de Demonstrações Financeiras", body: "Liquidez, estrutura financeira e rendibilidade, com interpretação." },
  { level: "Intermédio", tag: "Em breve", title: "Power BI para Finanças", body: "Datasets, indicadores e visualizações." },
  { level: "Avançado", tag: "Em breve", title: "Mercado de Capitais", body: "Empresas fictícias, relatórios e carteira virtual." },
  { level: "Avançado", tag: "Em breve", title: "Gestão e Estratégia", body: "Casos empresariais e tomada de decisão." },
];

interface PublicStats { tracks: number; rooms: number; exercises: number; students: number; gradedSubmissions: number; ratingCount: number; ratingAverage: number }

const FAQ: [string, string][] = [
  ["Quem pode participar?", "Qualquer estudante do ISPTEC, de qualquer curso, com o email institucional de estudante (o que começa pelo número de 8 dígitos, ex.: 20221987@isptec.co.ao). O foco está nas Ciências Sociais Aplicadas, mas as salas de lógica contabilística, análise e casos são para todos."],
  ["Quanto custa?", "Nada. É uma iniciativa da Associação de Estudantes, feita por estudantes e para estudantes."],
  ["Conta para a minha nota?", "Não. O Lúmen Pregão não é um canal oficial do ISPTEC: os pontos e as conquistas servem para praticar e mostrar competências, não substituem avaliações académicas."],
  ["Como é corrigido?", "No servidor, com valores próprios para cada estudante. A resposta certa nunca chega ao teu browser antes de submeteres, por isso copiar o colega não ajuda."],
  ["Os exercícios são fiáveis?", "Cada exercício passa por revisão antes de ser publicado e indica a norma e a versão aplicadas. O que ainda não foi confirmado aparece marcado como «a confirmar»."],
  ["E os meus dados?", "Guardamos só o necessário, não vendemos nada e podes exportar ou eliminar a tua conta a qualquer momento. Lê a política de privacidade."],
];

export default async function Landing() {
  const stats = await serverFetchOptional<PublicStats>("/public/stats").catch(() => null);
  const rating = stats && stats.ratingCount >= 5 ? Number(stats.ratingAverage) : null;
  return (
    <div className="stack" style={{ gap: 72, padding: "56px clamp(18px,4vw,56px)", maxWidth: 1360, margin: "0 auto" }}>
      <section className="grid" style={{ gridTemplateColumns: "repeat(auto-fit,minmax(min(100%,460px),1fr))", gap: 40, alignItems: "center" }}>
        <div className="stack" style={{ gap: 20 }}>
          <div className="label">DCSA · Gestão · Economia · Contabilidade</div>
          <h1 className="d1"><span className="hero-line">Aprender é </span><span className="hero-line">saber <span className="serif">aplicar.</span></span></h1>
          <p className="body-l muted hero-sub" style={{ maxWidth: 560 }}>
            Um laboratório de competências para o DCSA do ISPTEC: resolve lançamentos do PGC, calcula impostos, analisa
            demonstrações financeiras e compete em equipa na Maratona de casos.
          </p>
          <div className="row hero-cta">
            <Link className="btn pri" href="/registo">Criar conta com email institucional <Icon name="arrowRight" /></Link>
            <Link className="btn out" href="#como-funciona">Ver como funciona</Link>
          </div>
          <p className="small muted">Aberto a todos os cursos do ISPTEC · entra com o email de estudante, ex.: <span className="mono">20221987@isptec.co.ao</span></p>
        </div>
        <Card lit aria-label="Exemplo de sala" className="stack demo">
          <div className="row"><span className="label">Sala · Compra de mercadorias com IVA</span><div className="spacer" /><span className="badge mono">Tarefa 3/5</span></div>
          <p className="small">A <b>Distribuidora Mussulo, Lda.</b> <span className="tbc">fictícia</span> comprou mercadorias a crédito. Regista o lançamento.</p>
          <div className="tbl-wrap">
            <table className="tbl" style={{ minWidth: 0 }}>
              <thead><tr><th>Conta</th><th className="num">Débito</th><th className="num">Crédito</th></tr></thead>
              <tbody>
                <tr><td><span className="acct">26</span> Mercadorias</td><td className="num">500 000,00</td><td /></tr>
                <tr><td><span className="acct">34.5.x</span> IVA suportado</td><td className="num">70 000,00</td><td /></tr>
                <tr><td><span className="acct">32</span> Fornecedores</td><td /><td className="num">570 000,00</td></tr>
              </tbody>
              <tfoot><tr><td>Totais <span className="badge pos badge-balance" style={{ marginLeft: 8 }}>Equilibrado</span></td>
                <td className="num">570 000,00</td><td className="num">570 000,00</td></tr></tfoot>
            </table>
          </div>
          <div className="alert pos"><Icon name="check" /><div><b>Correcto · +40 pts</b>Débitos e créditos equilibrados.</div></div>
        </Card>
      </section>

      {stats && stats.tracks > 0 && (
        <Reveal as="div" className="stats-band grid g4 stagger" aria-label="A plataforma em números">
          {([["Trilhas publicadas", stats.tracks], ["Salas práticas", stats.rooms], ["Exercícios revistos", stats.exercises],
            ["Respostas corrigidas", stats.gradedSubmissions]] as [string, number][]).map(([l, v]) => (
            <div key={l} className="stack" style={{ gap: 4 }}>
              <span className="stat-v mono"><CountUp value={v} /></span><span className="small muted">{l}</span>
            </div>
          ))}
        </Reveal>
      )}

      <Reveal className="campus grid" style={{ gridTemplateColumns: "repeat(auto-fit,minmax(min(100%,420px),1fr))", gap: 32, alignItems: "center" }}>
        <figure className="photo-frame" style={{ margin: 0 }}>
          <Image src="/img/campus-1100.webp" alt="Edifício do ISPTEC visto do parque de estacionamento, com a escultura e a placa amarela à entrada."
            width={1100} height={619} sizes="(max-width: 900px) 100vw, 640px" style={{ width: "100%", height: "auto" }} />
        </figure>
        <div className="stack" style={{ gap: 14 }}>
          <div className="label">Para todo o ISPTEC</div>
          <h2 className="d2">Feito no DCSA, <span className="serif">aberto a todos.</span></h2>
          <p className="muted">O conteúdo nasce nas Ciências Sociais Aplicadas (Contabilidade, Economia e Gestão), mas qualquer
            estudante pode participar. Engenharias, Informática ou Geociências: ler um balanço, perceber o IVA ou decidir com
            números é útil em qualquer carreira, e a Maratona ganha com equipas mistas.</p>
          <ul className="plain stack small" style={{ gap: 8 }}>
            {["Conta com o teu email institucional de estudante", "Trilhas por nível, do zero ao avançado", "Equipas de cursos diferentes nas competições"].map((x) => (
              <li key={x} className="row" style={{ gap: 8 }}><Icon name="check" />{x}</li>
            ))}
          </ul>
        </div>
      </Reveal>

      <Reveal id="como-funciona" className="stack" style={{ gap: 20 }}>
        <div className="label">Como funciona</div>
        <h2 className="d2">Quatro formas de ganhar <span className="serif">competência</span></h2>
        <div className="grid g4 stagger">
          {[
            ["route", "Trilhas", "Percursos por área, de módulo em módulo, com progresso e requisitos claros."],
            ["door", "Salas práticas", "Lançamentos, demonstrações, cálculos fiscais, rácios e casos abertos, corrigidos no servidor."],
            ["building", "Simulações", "Contabilidade de uma empresa durante um mês e um mercado de capitais simulado, sem dinheiro real."],
            ["trophy", "A Maratona", "Competição individual ou por equipas, com ranking ao vivo e fase final avaliada por júri."],
          ].map(([icon, t, b]) => (
            <Card key={t} className="stack hoverable"><Icon name={icon} /><h3 className="h3">{t}</h3><p className="small muted">{b}</p></Card>
          ))}
        </div>
      </Reveal>

      <Reveal id="trilhas" className="stack" style={{ gap: 20 }}>
        <div className="row"><div className="stack" style={{ gap: 6 }}><div className="label">Trilhas</div><h2 className="d2">Do PGC à análise financeira</h2></div>
          <div className="spacer" /><Link className="btn out" href="/trilhas">Ver catálogo</Link></div>
        <div className="grid g3 stagger">
          {TRACKS.map((t) => (
            <Card key={t.title} className="stack hoverable">
              <div className="row" style={{ gap: 8 }}><Badge>{t.level}</Badge><Badge tone={t.tag === "Disponível" ? "acc" : "neutral"}>{t.tag}</Badge></div>
              <h3 className="h3">{t.title}</h3><p className="small muted">{t.body}</p>
            </Card>
          ))}
        </div>
      </Reveal>

      <Reveal id="maratona" className="card lit grid" style={{ gridTemplateColumns: "repeat(auto-fit,minmax(min(100%,420px),1fr))", gap: 24, alignItems: "center" }}>
        <div className="stack">
          <div className="label">Pilar 03 · Prestígio em todos os cursos</div>
          <h2 className="d2">A Maratona de <span className="serif">casos</span></h2>
          <p className="muted">Equipas de 2 a 5 resolvem casos com empresas fictícias contra o relógio. O relógio oficial é o do servidor;
            a fase final é avaliada por júri com uma grelha publicada antes do início.</p>
          <p className="small muted">Proposta do programa da Chapa D, Lúmen, para o mandato 2026–2028. Não é um evento já realizado.</p>
        </div>
        <div className="stack" style={{ alignItems: "flex-start" }}>
          <div className="row"><Badge tone="acc">Datas a anunciar</Badge><Badge>Inscrição por equipas</Badge></div>
          <span className="timer"><span className="t">-- : -- : --</span><span className="src">abertura a anunciar</span></span>
        </div>
      </Reveal>

      <Reveal className="grid g3 stagger">
        {[
          ["shield", "Conteúdo revisto", "Nenhum exercício é publicado sem revisão. Cada um indica a fonte e a versão da norma aplicada."],
          ["lock", "Correcção no servidor", "As respostas certas nunca chegam ao teu browser antes de submeteres. Cada estudante recebe valores próprios."],
          ["building", "Simulação educativa", "Empresas e mercados fictícios. Sem dinheiro real, sem transacções reais, sem dados de mercado em tempo real."],
        ].map(([icon, t, b]) => (
          <Card key={t} flat className="stack hoverable"><Icon name={icon} /><h3 className="h3">{t}</h3><p className="small muted">{b}</p></Card>
        ))}
      </Reveal>

      <Reveal className="stack" style={{ gap: 20 }}>
        <div className="label">Em três passos</div>
        <h2 className="d2">Do registo à <span className="serif">primeira sala</span></h2>
        <ol className="plain grid g3 stagger steps">
          {[
            ["Cria a conta", "Usa o email de estudante e confirma-o pelo link que te enviamos."],
            ["Escolhe uma trilha", "Começa pela recomendada ou salta para o tema que precisas de praticar."],
            ["Resolve e evolui", "Cada resposta é corrigida na hora, com explicação. Ganha pontos, conquistas e sobe no ranking."],
          ].map(([t, b], i) => (
            <li key={t} className="card stack hoverable"><span className="step-n mono">{String(i + 1).padStart(2, "0")}</span><h3 className="h3">{t}</h3><p className="small muted">{b}</p></li>
          ))}
        </ol>
      </Reveal>

      <Reveal className="card grid" style={{ gridTemplateColumns: "repeat(auto-fit,minmax(min(100%,320px),1fr))", gap: 24, alignItems: "center" }}>
        <div className="stack" style={{ gap: 10 }}>
          <div className="label">Avaliações</div>
          <h2 className="d2">A tua opinião <span className="serif">conta.</span></h2>
          <p className="muted">Avalia cada sala que resolves e envia sugestões pelo botão Feedback. A equipa editorial lê tudo e
            melhora os enunciados com base nisso.</p>
        </div>
        <div className="stack" style={{ alignItems: "flex-start", gap: 8 }}>
          {rating !== null ? (
            <>
              <span className="stat-v mono">{rating.toLocaleString("pt-PT", { minimumFractionDigits: 1, maximumFractionDigits: 1 })}<span className="muted" style={{ fontSize: "0.5em" }}> / 5</span></span>
              <StarRating value={rating} label="Média das avaliações" size={30} />
              <span className="small muted">Média de {stats?.ratingCount} avaliações reais de estudantes</span>
            </>
          ) : (
            <>
              <StarRating value={0} label="Ainda sem avaliações suficientes" size={30} />
              <span className="small muted">A média aparece aqui quando houver pelo menos 5 avaliações. Só mostramos números reais.</span>
            </>
          )}
        </div>
      </Reveal>

      <Reveal className="grid" style={{ gridTemplateColumns: "repeat(auto-fit,minmax(min(100%,420px),1fr))", gap: 32, alignItems: "center" }}>
        <div className="stack" style={{ gap: 14 }}>
          <div className="label">Quem está por trás</div>
          <h2 className="d2">Estudantes a construir <span className="serif">para estudantes.</span></h2>
          <p className="muted">O Lúmen Pregão é uma proposta do Movimento Lúmen (Chapa D) para a Associação de Estudantes do ISPTEC,
            mandato 2026–2028: organizar os nossos talentos e treinar as equipas a tempo das competições.</p>
          <p className="small muted">Iniciativa da Associação de Estudantes · Não é um canal oficial do ISPTEC.</p>
        </div>
        <figure className="photo-frame" style={{ margin: 0 }}>
          <Image src="/img/equipa-1600.webp" alt="A equipa da Chapa D reunida ao fim da tarde, a conversar e a sorrir."
            width={1600} height={800} sizes="(max-width: 900px) 100vw, 640px" style={{ width: "100%", height: "auto" }} />
        </figure>
      </Reveal>

      <Reveal id="perguntas" className="stack" style={{ gap: 16, maxWidth: 860 }}>
        <div className="label">Perguntas frequentes</div>
        <h2 className="d2">Antes de <span className="serif">começares</span></h2>
        <div className="stack faq" style={{ gap: 8 }}>
          {FAQ.map(([q, a]) => (
            <details key={q} className="card flat">
              <summary>{q}</summary>
              <p className="muted" style={{ margin: "10px 0 0" }}>{a}</p>
            </details>
          ))}
        </div>
      </Reveal>

      <Reveal className="card lit stack cta-final" style={{ alignItems: "center", textAlign: "center", gap: 14 }}>
        <h2 className="d2">Pronto para <span className="serif">aplicar?</span></h2>
        <p className="muted" style={{ maxWidth: 520 }}>Cria a conta com o teu email de estudante e resolve a primeira sala em menos de 10 minutos.</p>
        <div className="row" style={{ justifyContent: "center" }}>
          <Link className="btn pri" href="/registo">Criar conta <Icon name="arrowRight" /></Link>
          <Link className="btn out" href="/entrar">Já tenho conta</Link>
        </div>
        <p className="small muted">Ao criar conta aceitas os <Link href="/termos">termos</Link> e a <Link href="/privacidade">política de privacidade</Link>.</p>
      </Reveal>

      <p className="credits">Desenvolvido por Victor Kangombe</p>
    </div>
  );
}
