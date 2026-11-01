import Link from "next/link";

/** Rodapé institucional obrigatório em todas as páginas (spec §1.7). */
export function Footer() {
  return (
    <footer className="foot">
      <span>Iniciativa da Associação de Estudantes · Não é um canal oficial do ISPTEC</span>
      <nav aria-label="Informação" className="row" style={{ gap: 16 }}>
        <Link href="/regras">Regras e fontes</Link>
        <Link href="/privacidade">Privacidade</Link>
        <span>Lúmen Pregão · nome provisório</span>
      </nav>
    </footer>
  );
}
