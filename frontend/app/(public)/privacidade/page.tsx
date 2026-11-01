import { Tbc } from "@/components/ui/Badge";

export const metadata = { title: "Política de privacidade" };

export default function Privacidade() {
  return (
    <article className="content stack" style={{ maxWidth: 760, margin: "0 auto", gap: 16 }}>
      <div className="label">Política de privacidade</div>
      <h1 className="h1">Como tratamos os teus dados</h1>
      <p className="muted">Texto provisório preparado pela equipa do projecto. A versão final e o enquadramento legal (Lei n.º 22/11) estão <Tbc />.</p>
      <h2 className="h2">Que dados guardamos</h2>
      <p>Nome, email institucional, curso, ano, as tuas tentativas, submissões, pontos e preferências (tema, pseudónimo, visibilidade no ranking).</p>
      <h2 className="h2">Para quê</h2>
      <p>Para corrigir as tuas respostas, calcular o progresso e os rankings, e organizar competições. Não vendemos nem partilhamos os teus dados.</p>
      <h2 className="h2">Os teus direitos</h2>
      <p>Podes exportar os teus dados e pedir a eliminação da conta em <b>Perfil e definições</b>. O portefólio público só existe se o activares.</p>
      <p className="small muted">Iniciativa da Associação de Estudantes · Não é um canal oficial do ISPTEC.</p>
    </article>
  );
}
