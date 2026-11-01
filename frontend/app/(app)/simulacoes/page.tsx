import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { Card } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Icon } from "@/components/ui/Icon";

export const metadata = { title: "Simulações" };

const ITEMS = [
  ["Simulação de empresa", "Um balancete inicial e um mês de operações: compras, vendas, IVA, salários e fecho do período."],
  ["Mercado simulado", "Empresas fictícias com relatórios trimestrais e uma carteira virtual, com decisões justificadas."],
  ["Processo selectivo", "Casos com tempo limite e avaliação por critérios, ao estilo dos processos da banca e das Big 4."],
  ["Laboratório de dados", "Explorar datasets de empresas fictícias, construir gráficos e interpretar indicadores."],
];

export default function Simulacoes() {
  return (
    <>
      <SetCrumbs items={[{ label: "Aprender" }, { label: "Simulações" }]} />
      <div className="stack" style={{ gap: 6 }}><div className="label">Simulações</div><h1 className="h1">Simulações <span className="serif">em preparação</span></h1></div>
      <div className="sim-ribbon"><Icon name="building" />Simulação educativa · empresas fictícias · sem dinheiro real</div>
      <div className="grid g2">
        {ITEMS.map(([t, b]) => (
          <Card key={t} className="stack"><div className="row"><h2 className="h3">{t}</h2><div className="spacer" /><Badge>Fase posterior</Badge></div><p className="small muted">{b}</p></Card>
        ))}
      </div>
    </>
  );
}
