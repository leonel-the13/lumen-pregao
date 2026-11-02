import Link from "next/link";
import { Tbc } from "@/components/ui/Badge";

export const metadata = { title: "Política de privacidade" };

const UPDATED = "01/11/2026";

export default function Privacidade() {
  return (
    <article className="content stack legal" style={{ maxWidth: 780, margin: "0 auto", gap: 14 }}>
      <div className="label">Política de privacidade · versão de {UPDATED}</div>
      <h1 className="h1">Como tratamos os teus dados</h1>
      <p className="muted">Texto preparado pela equipa do projecto. A versão final e o enquadramento na Lei n.º 22/11 (Lei da Protecção
        de Dados Pessoais) estão <Tbc />. Ver também os <Link href="/termos">termos e condições</Link>.</p>

      <h2 className="h2">Quem é responsável</h2>
      <p>O Lúmen Lab é uma iniciativa da Associação de Estudantes. Não é um canal oficial do ISPTEC e o ISPTEC não recebe os teus
        dados através desta plataforma.</p>

      <h2 className="h2">Que dados guardamos</h2>
      <ul>
        <li><b>Conta:</b> nome, email institucional, curso, ano, palavra-passe (só em hash Argon2id, nunca em texto) e, se aplicável, o 2.º factor (cifrado).</li>
        <li><b>Actividade:</b> tentativas, respostas submetidas, pontos, conquistas, exames, equipas e participação em contests.</li>
        <li><b>Participação:</b> avaliações de salas, feedback e notificações.</li>
        <li><b>Preferências:</b> tema, pseudónimo, visibilidade no ranking e portefólio público (desligado por defeito).</li>
        <li><b>Segurança:</b> registos de auditoria de operações sensíveis (ex.: entradas, mudanças de perfil), com data e endereço IP.</li>
      </ul>

      <h2 className="h2">Para quê</h2>
      <p>Para corrigir as tuas respostas, mostrar o progresso, calcular rankings, organizar exames e competições, enviar emails
        necessários (confirmação de conta, recuperação de acesso, convites), proteger a plataforma contra abusos e melhorar o conteúdo.
        <b> Não vendemos os teus dados nem os usamos para publicidade.</b></p>

      <h2 className="h2">Quem vê o quê</h2>
      <ul>
        <li>Outros estudantes só veem o teu nome ou pseudónimo no ranking (se não te esconderes) e na tua equipa.</li>
        <li>Avaliações e feedback são vistos pela administração; a média pública das salas é anónima.</li>
        <li>A administração e os revisores acedem aos dados necessários às suas funções, com registo em auditoria.</li>
      </ul>

      <h2 className="h2">Onde ficam os dados</h2>
      <p>Usamos fornecedores de alojamento na nuvem para o site (Vercel), o servidor (Render), a base de dados (Neon) e o envio de
        email (Resend). Estes serviços podem estar fora de Angola e tratam os dados apenas por nossa conta.</p>

      <h2 className="h2">Cookies</h2>
      <p>Só usamos cookies necessários: a sessão (<span className="mono">LP_SESSION</span>), a protecção contra pedidos forjados
        (<span className="mono">XSRF-TOKEN</span>) e a preferência de tema. Não usamos cookies de publicidade nem de rastreio de terceiros.</p>

      <h2 className="h2">Durante quanto tempo</h2>
      <p>Enquanto a conta existir. Quando pedes a eliminação, a administração executa o pedido: apagamos o nome, o email, o pseudónimo,
        os perfis e o 2.º factor, e retiramos a conta dos rankings. Os registos de pontos e de auditoria, que não podem ser alterados,
        ficam associados a uma conta anónima. Prazos exactos de conservação <Tbc />.</p>

      <h2 className="h2">Os teus direitos</h2>
      <ul>
        <li><b>Acesso e portabilidade:</b> exporta os teus dados em JSON em <b>Perfil e definições</b>.</li>
        <li><b>Rectificação:</b> actualiza o curso, o ano, o pseudónimo e as preferências nas definições, ou pede ajuda pelo Feedback.</li>
        <li><b>Eliminação:</b> pede a eliminação da conta em <b>Perfil e definições</b>.</li>
        <li><b>Oposição:</b> podes esconder-te do ranking e manter o portefólio privado.</li>
      </ul>

      <h2 className="h2">Segurança</h2>
      <p>Ligações cifradas (HTTPS), palavras-passe com Argon2id, 2.º factor obrigatório para perfis com privilégios, limites de
        tentativas e registo de auditoria. Se suspeitares de acesso indevido à tua conta, muda a palavra-passe e avisa-nos.</p>

      <h2 className="h2">Contacto</h2>
      <p>Para questões de privacidade, usa o botão <b>Feedback</b> dentro da plataforma ou contacta a Associação de Estudantes.</p>
    </article>
  );
}
