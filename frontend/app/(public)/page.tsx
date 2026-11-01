import Link from "next/link";
import { Icon } from "@/components/ui/Icon";
import { Badge, Tbc } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";

const TRACKS = [
  { level: "Iniciação", tag: "MVP", title: "Contabilidade Financeira segundo o PGC", body: "Balancete, lançamentos e demonstrações financeiras." },
  { level: "Intermédio", tag: "MVP · IVA", title: "Fiscalidade Angolana", body: "IVA, Imposto Industrial e IRT, passo a passo." },
  { level: "Intermédio", tag: "Em breve", title: "Análise de Demonstrações Financeiras", body: "Rácios, interpretação e comparação." },
  { level: "Intermédio", tag: "Em breve", title: "Power BI para Finanças", body: "Datasets, indicadores e visualizações." },
  { level: "Avançado", tag: "Em breve", title: "Mercado de Capitais", body: "Empresas fictícias, relatórios e carteira virtual." },
  { level: "Avançado", tag: "Em breve", title: "Gestão e Estratégia", body: "Casos empresariais e tomada de decisão." },
];

export default function Landing() {
  return (
    <div className="stack" style={{ gap: 72, padding: "56px clamp(18px,4vw,56px)", maxWidth: 1360, margin: "0 auto" }}>
      <section className="grid" style={{ gridTemplateColumns: "repeat(auto-fit,minmax(min(100%,460px),1fr))", gap: 40, alignItems: "center" }}>
        <div className="stack" style={{ gap: 20 }}>
          <div className="label">DCSA · Gestão · Economia · Contabilidade</div>
          <h1 className="d1">Aprender é saber <span className="serif">aplicar.</span></h1>
          <p className="body-l muted" style={{ maxWidth: 560 }}>
            Um laboratório de competências para o DCSA do ISPTEC: resolve lançamentos do PGC, calcula impostos, analisa
            demonstrações financeiras e compete em equipa na Maratona de casos.
          </p>
          <div className="row">
            <Link className="btn pri" href="/registo">Criar conta com email institucional <Icon name="arrowRight" /></Link>
            <Link className="btn out" href="#como-funciona">Ver como funciona</Link>
          </div>
          <p className="small muted">Acesso para estudantes do ISPTEC · domínio de email institucional <Tbc /></p>
        </div>
        <Card lit aria-label="Exemplo de sala" className="stack">
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
            </table>
          </div>
          <div className="alert pos"><Icon name="check" /><div><b>Correcto · +40 pts</b>Débitos e créditos equilibrados.</div></div>
        </Card>
      </section>

      <section id="como-funciona" className="stack" style={{ gap: 20 }}>
        <div className="label">Como funciona</div>
        <h2 className="d2">Quatro formas de ganhar <span className="serif">competência</span></h2>
        <div className="grid g4">
          {[
            ["route", "Trilhas", "Percursos por área, de módulo em módulo, com progresso e requisitos claros."],
            ["door", "Salas práticas", "Lançamentos, demonstrações, cálculos fiscais, rácios e casos abertos, corrigidos no servidor."],
            ["building", "Simulações", "Contabilidade de uma empresa durante um mês e um mercado de capitais simulado, sem dinheiro real."],
            ["trophy", "A Maratona", "Competição individual ou por equipas, com ranking ao vivo e fase final avaliada por júri."],
          ].map(([icon, t, b]) => (
            <Card key={t} className="stack"><Icon name={icon} /><h3 className="h3">{t}</h3><p className="small muted">{b}</p></Card>
          ))}
        </div>
      </section>

      <section id="trilhas" className="stack" style={{ gap: 20 }}>
        <div className="row"><div className="stack" style={{ gap: 6 }}><div className="label">Trilhas</div><h2 className="d2">Do PGC à análise financeira</h2></div>
          <div className="spacer" /><Link className="btn out" href="/trilhas">Ver catálogo</Link></div>
        <div className="grid g3">
          {TRACKS.map((t) => (
            <Card key={t.title} className="stack">
              <div className="row" style={{ gap: 8 }}><Badge>{t.level}</Badge><Badge tone={t.tag.startsWith("MVP") ? "acc" : "neutral"}>{t.tag}</Badge></div>
              <h3 className="h3">{t.title}</h3><p className="small muted">{t.body}</p>
            </Card>
          ))}
        </div>
      </section>

      <section id="maratona" className="card lit grid" style={{ gridTemplateColumns: "repeat(auto-fit,minmax(min(100%,420px),1fr))", gap: 24, alignItems: "center" }}>
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
      </section>

      <section className="grid g3">
        {[
          ["shield", "Conteúdo revisto", "Nenhum exercício é publicado sem revisão. Cada um indica a fonte e a versão da norma aplicada."],
          ["lock", "Correcção no servidor", "As respostas certas nunca chegam ao teu browser antes de submeteres. Cada estudante recebe valores próprios."],
          ["building", "Simulação educativa", "Empresas e mercados fictícios. Sem dinheiro real, sem transacções reais, sem dados de mercado em tempo real."],
        ].map(([icon, t, b]) => (
          <Card key={t} flat className="stack"><Icon name={icon} /><h3 className="h3">{t}</h3><p className="small muted">{b}</p></Card>
        ))}
      </section>
    </div>
  );
}
