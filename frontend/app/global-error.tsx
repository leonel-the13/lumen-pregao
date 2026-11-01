"use client";

export default function GlobalError({ reset }: { error: Error; reset: () => void }) {
  return (
    <html lang="pt-PT" data-theme="dark">
      <body className="lp" style={{ padding: 24 }}>
        <div className="card state-box" role="alert">
          <h1 className="h3">Ocorreu um erro inesperado</h1>
          <p className="small muted">Tenta de novo dentro de instantes. Código: APP-500</p>
          <button className="btn out" onClick={reset}>Tentar novamente</button>
        </div>
        <footer className="foot"><span>Iniciativa da Associação de Estudantes · Não é um canal oficial do ISPTEC</span></footer>
      </body>
    </html>
  );
}
