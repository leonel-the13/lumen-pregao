import { DIFFICULTY_LABEL } from "@/lib/format";

export function Bar({ value, label, thin }: { value: number; label: string; thin?: boolean }) {
  const v = Math.max(0, Math.min(100, Math.round(value)));
  return (
    <div className={["bar", thin && "thin"].filter(Boolean).join(" ")} role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={v} aria-label={label}>
      <i style={{ width: `${v}%` }} />
    </div>
  );
}

export function Ring({ value, label, size }: { value: number; label: string; size?: number }) {
  const v = Math.max(0, Math.min(100, Math.round(value)));
  return (
    <div className="ring" style={{ ["--p" as string]: v, ...(size ? { width: size, height: size } : {}) }} role="img" aria-label={`${label}: ${v} %`}>
      <span aria-hidden="true">{v}%</span>
    </div>
  );
}

export function Steps({ total, done, current }: { total: number; done: number; current?: number }) {
  return (
    <div className="steps" aria-hidden="true">
      {Array.from({ length: total }, (_, i) => <i key={i} className={i < done ? "on" : i === current ? "cur" : undefined} />)}
    </div>
  );
}

/** Dificuldade 1–4 em barras, com texto para leitores de ecrã. */
export function Difficulty({ level }: { level: number }) {
  return (
    <span className="diff" title={`Dificuldade · ${DIFFICULTY_LABEL[level] ?? level}`}>
      {[1, 2, 3, 4].map((i) => <i key={i} className={i <= level ? "on" : undefined} aria-hidden="true" />)}
      <span className="sr-only">Dificuldade {level} de 4 · {DIFFICULTY_LABEL[level]}</span>
    </span>
  );
}
