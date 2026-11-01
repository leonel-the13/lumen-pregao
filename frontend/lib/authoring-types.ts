export type EditorialStatus = "DRAFT" | "IN_REVIEW" | "APPROVED" | "PUBLISHED" | "NEEDS_REVIEW" | "ARCHIVED";

export interface ReferenceDto { legalSourceId: string; title: string; identifier: string; version: string; articles?: string; consultedAt?: string; confirmed: boolean }

export interface VersionDto {
  id: string; number: number; type: string; title: string; statement: string; presentation: Record<string, unknown>;
  paramSpec: { params?: ParamSpec[] }; solutionSpec: Record<string, unknown>; tolerance: { type?: string; value?: number };
  hints: string[]; feedbackPolicy: string; difficulty: number; points: number; maxAttempts: number; pgcVersion?: string;
  validUntil?: string; changeNote?: string; status: EditorialStatus; reviewNotes?: string; legalAlertAt?: string;
  references: ReferenceDto[]; updatedAt: string;
}

export interface ExerciseDto { id: string; code: string; topic: string; authorId: string; authorName: string; versions: VersionDto[] }

export interface ExerciseSummary {
  id: string; code: string; topic: string; authorName: string; latestVersion: number; latestVersionId: string; title: string;
  type: string; status: EditorialStatus; legalAlertAt?: string; updatedAt: string;
}

export interface ParamSpec {
  name: string; type: "money" | "percent" | "int" | "text"; visible?: boolean;
  rule: { kind: "range" | "set" | "entity" | "constant" | "expr"; min?: number; max?: number; step?: number; values?: (string | number)[];
    source?: string; value?: number | string; legalSourceId?: string; expr?: string; scale?: number };
}

export interface LegalSourceDto { id: string; title: string; identifier: string; version: string; publishedAt?: string; effectiveFrom?: string; effectiveTo?: string; url?: string; confirmed: boolean; updatedAt: string }

export const STATUS_LABEL: Record<EditorialStatus, string> = {
  DRAFT: "Rascunho", IN_REVIEW: "Em revisão", APPROVED: "Aprovado", PUBLISHED: "Publicado", NEEDS_REVIEW: "Revisão necessária", ARCHIVED: "Arquivado",
};

export const STATUS_TONE: Record<EditorialStatus, "neutral" | "info" | "acc" | "pos" | "neg"> = {
  DRAFT: "neutral", IN_REVIEW: "info", APPROVED: "acc", PUBLISHED: "pos", NEEDS_REVIEW: "neg", ARCHIVED: "neutral",
};

export const TYPE_LABEL: Record<string, string> = {
  A: "Escolha múltipla", B: "Resposta numérica", C: "Lançamentos", D: "Demonstrações", E: "Cálculo fiscal", F: "Análise de rácios", G: "Caso aberto",
};
