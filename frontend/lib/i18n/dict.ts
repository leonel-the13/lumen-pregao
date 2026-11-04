import type { Locale } from "./config";

/** Mesma forma que o português, com qualquer texto: o en e o fr têm de ter exactamente as mesmas chaves. */
export type DeepString<T> = { [K in keyof T]: T[K] extends string ? string : DeepString<T[K]> };
export type Dict<T> = Record<Locale, DeepString<T>>;
