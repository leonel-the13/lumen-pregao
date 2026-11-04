"use client";

/** Fora dos providers (o layout raiz falhou): textos curtos nas três línguas, escolhidos pelo cookie lp_lang. */
const TEXT = {
  pt: ["Ocorreu um erro inesperado", "Tenta de novo dentro de instantes. Código: APP-500", "Tentar novamente", "Iniciativa da Associação de Estudantes · Não é um canal oficial do ISPTEC"],
  en: ["An unexpected error occurred", "Please try again in a moment. Code: APP-500", "Try again", "A Students’ Union initiative · Not an official ISPTEC channel"],
  fr: ["Une erreur inattendue s’est produite", "Réessaie dans quelques instants. Code : APP-500", "Réessayer", "Une initiative de l’Association des étudiants · Pas un canal officiel de l’ISPTEC"],
} as const;

function lang(): keyof typeof TEXT {
  if (typeof document === "undefined") return "pt";
  const m = /(?:^|; )lp_lang=(pt|en|fr)/.exec(document.cookie);
  return (m?.[1] as keyof typeof TEXT) ?? "pt";
}

export default function GlobalError({ reset }: { error: Error; reset: () => void }) {
  const l = lang();
  const [title, body, retry, foot] = TEXT[l];
  return (
    <html lang={l} data-theme="dark">
      <body className="lp" style={{ padding: 24 }}>
        <div className="card state-box" role="alert">
          <h1 className="h3">{title}</h1>
          <p className="small muted">{body}</p>
          <button className="btn out" onClick={reset}>{retry}</button>
        </div>
        <footer className="foot"><span>{foot}</span></footer>
      </body>
    </html>
  );
}
