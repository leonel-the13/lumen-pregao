import type { Messages } from "./index";

/** Chaves em ponto («nav.home») de todas as folhas do dicionário: uma chave errada falha no typecheck. */
type Leaves<T, P extends string = ""> = {
  [K in keyof T & string]: T[K] extends string ? `${P}${K}` : Leaves<T[K], `${P}${K}.`>;
}[keyof T & string];

export type MessageKey = Leaves<Messages>;
export type Vars = Record<string, string | number>;
export type T = (key: MessageKey, vars?: Vars) => string;

/** Procura a chave e substitui «{nome}» pelas variáveis. Chave em falta → a própria chave (visível, nunca rebenta). */
export function translator(messages: Messages): T {
  return (key, vars) => {
    let v: unknown = messages;
    for (const part of key.split(".")) v = (v as Record<string, unknown> | undefined)?.[part];
    if (typeof v !== "string") return key;
    return vars ? v.replace(/\{(\w+)\}/g, (m, k: string) => (k in vars ? String(vars[k]) : m)) : v;
  };
}
