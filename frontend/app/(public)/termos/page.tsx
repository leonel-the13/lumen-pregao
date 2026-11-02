import Link from "next/link";
import { Tbc } from "@/components/ui/Badge";

export const metadata = { title: "Termos e condições" };

const UPDATED = "01/11/2026";

export default function Termos() {
  return (
    <article className="content stack legal" style={{ maxWidth: 780, margin: "0 auto", gap: 14 }}>
      <div className="label">Termos e condições · versão de {UPDATED}</div>
      <h1 className="h1">Termos e condições de utilização</h1>
      <p className="muted">Ao criar conta no Lúmen Pregão aceitas estes termos. Lê também a <Link href="/privacidade">política de privacidade</Link> e
        as <Link href="/regras">regras e fontes</Link>. Texto preparado pela equipa do projecto; a revisão jurídica final está <Tbc />.</p>

      <h2 className="h2">1. O que é o Lúmen Pregão</h2>
      <p>Uma plataforma educativa gratuita, criada por estudantes como iniciativa da Associação de Estudantes, para praticar
        contabilidade, fiscalidade, análise financeira e outras competências através de salas, exames e competições.
        <b> Não é um canal oficial do ISPTEC</b>: as notas, pontos e conquistas não substituem nem alteram avaliações académicas.</p>

      <h2 className="h2">2. Quem pode usar</h2>
      <ul>
        <li>Estudantes do ISPTEC de <b>qualquer curso</b>, com o email institucional de estudante (o que começa pelo número de estudante,
          ex.: <span className="mono">20221987@isptec.co.ao</span>). O conteúdo é focado nas Ciências Sociais Aplicadas, mas todos podem participar.</li>
        <li>Docentes, revisores, júris e membros da organização entram por convite da administração.</li>
        <li>A conta é pessoal e intransmissível. És responsável por manter a palavra-passe (e o 2.º factor, se o teu perfil o exigir) em segurança.</li>
      </ul>

      <h2 className="h2">3. Regras de conduta</h2>
      <ul>
        <li>Resolve as salas e exames por ti. Nos exames individuais não é permitida ajuda de terceiros.</li>
        <li>Nos contests, só colaboras com a tua equipa. Partilhar respostas entre equipas, usar várias contas ou automatizar submissões leva à desclassificação.</li>
        <li>Não tentes contornar a correcção, aceder a dados de outras pessoas, sobrecarregar o serviço nem explorar falhas de segurança.
          Se encontrares uma falha, comunica-a pelo botão <b>Feedback</b> (categoria «Algo não funciona»).</li>
        <li>Comentários, avaliações e feedback devem ser respeitosos. Não publiques dados pessoais de terceiros nem conteúdo ofensivo.</li>
      </ul>

      <h2 className="h2">4. Conteúdo académico</h2>
      <p>Os exercícios são originais e usam <b>empresas fictícias</b>. Cada exercício indica a norma e a versão aplicadas; referências
        ainda não confirmadas aparecem como «a confirmar». O conteúdo tem fins de prática e não constitui aconselhamento contabilístico,
        fiscal, jurídico nem de investimento. As simulações não envolvem dinheiro real.</p>

      <h2 className="h2">5. Avaliações e feedback</h2>
      <p>Podes avaliar as salas que já tentaste (1 a 5 estrelas e um comentário opcional) e enviar feedback sobre a plataforma.
        A equipa usa estas respostas para melhorar o conteúdo. A média pública só é mostrada com pelo menos 5 respostas, e nunca
        associada ao teu nome.</p>

      <h2 className="h2">6. Rankings, pontos e prémios</h2>
      <p>Os pontos e rankings servem para motivar a prática. Podes aparecer com pseudónimo ou esconder-te do ranking nas definições.
        Os resultados de um contest são publicados pela organização depois da verificação. Prémios, quando existirem, são anunciados
        pela organização com regras próprias.</p>

      <h2 className="h2">7. Suspensão e eliminação</h2>
      <p>A administração pode suspender contas que violem estes termos, com registo em auditoria. Podes pedir a eliminação da tua conta
        em <b>Perfil e definições</b>; depois de executado, o pedido apaga os teus dados pessoais (ver a política de privacidade).</p>

      <h2 className="h2">8. Disponibilidade</h2>
      <p>O serviço está em fase de MVP e é mantido por voluntários. Pode haver interrupções, alterações de conteúdo ou perda de
        funcionalidades. Fazemos o possível para avisar com antecedência, sobretudo antes de contests e exames.</p>

      <h2 className="h2">9. Alterações a estes termos</h2>
      <p>Se mudarmos estes termos, actualizamos a data no topo e avisamos na plataforma. Continuar a usar o serviço depois do aviso
        significa que aceitas a nova versão.</p>

      <h2 className="h2">10. Contacto</h2>
      <p>Para dúvidas sobre estes termos, usa o botão <b>Feedback</b> dentro da plataforma ou contacta a Associação de Estudantes.</p>
    </article>
  );
}
