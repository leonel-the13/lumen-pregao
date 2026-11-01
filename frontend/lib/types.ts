// Tipos dos DTOs do backend (estudante). Nunca contêm solução nem regras de geração.
export type Role = "STUDENT" | "TEAM_CAPTAIN" | "AUTHOR" | "REVIEWER" | "JURY" | "CONTEST_ORGANIZER" | "ADMIN";
export type Theme = "DARK" | "LIGHT";
export type RoomState = "LOCKED" | "AVAILABLE" | "IN_PROGRESS" | "COMPLETED";
export type TaskState = "AVAILABLE" | "IN_PROGRESS" | "PASSED" | "FAILED" | "UNAVAILABLE";

export interface Me {
  id: string; name: string; shortName: string; initials: string; email: string;
  course?: { id: string; code: string; name: string }; year?: number; roles: Role[];
  pseudonym?: string; rankingVisibility: "NAME" | "PSEUDONYM" | "HIDDEN"; theme: Theme;
  portfolioPublic: boolean; portfolioHandle: string; mfaEnabled: boolean; status: string;
}

export interface ReferenceView { title: string; identifier: string; version: string; articles?: string; confirmed: boolean }

export interface TrackCard {
  slug: string; title: string; summary: string; difficulty: number; topic: string; courses: string[];
  status: "PUBLISHED" | "COMING_SOON" | "DRAFT"; modulesCount: number; roomsCount: number; estHours?: number;
  percent: number; nextRoomId?: string;
}

export interface RoomCard {
  id: string; title: string; description: string; trackSlug: string; trackTitle: string; modulePosition: number;
  moduleTitle: string; position: number; difficulty: number; estMinutes: number; state: RoomState;
  obtained: number; possible: number; lockedReason?: string;
}

export interface ModuleView { id: string; position: number; title: string; summary?: string; roomsCount: number; percent: number; state: RoomState; rooms: RoomCard[] }

export interface TrackDetail {
  slug: string; title: string; summary: string; description: string; difficulty: number; topic: string;
  competencies: string[]; completionRule?: string; status: string; percent: number; estHours?: number;
  nextRoomId?: string; modules: ModuleView[]; references: ReferenceView[];
}

export interface TaskView { id: string; position: number; title: string; points: number; state: TaskState; score: number; attemptsUsed: number; maxAttempts: number; type?: string }

export interface RoomDetail {
  id: string; title: string; description: string; objectives: string[]; difficulty: number; estMinutes: number;
  context?: string; materials?: { title: string; kind: string }[]; state: RoomState; obtained: number; possible: number;
  breadcrumb: { trackSlug: string; trackTitle: string; modulePosition: number; moduleTitle: string; roomPosition: number };
  tasks: TaskView[]; references: ReferenceView[]; lockedReason?: string;
}

export interface Segment { text: string; param?: string; unconfirmed: boolean }
export interface VisibleParam { name: string; value: string | number; display: string; type: string; unconfirmed: boolean }

export interface SubmissionView {
  id: string; status: "QUEUED" | "PROCESSING" | "GRADED" | "REJECTED" | "FAILED"; outcome?: "CORRECT" | "INCORRECT" | "STEP_OK" | "REJECTED";
  correct?: boolean; points?: number; summary?: string; hint?: string; details?: Record<string, unknown>;
  errorCode?: string; receivedAt: string; gradedAt?: string; queuePosition?: number;
}

export interface AttemptView {
  attemptId: string; taskId?: string; type: "A" | "B" | "C" | "D" | "E" | "F" | "G"; title: string; statementRendered: string;
  statementSegments: Segment[]; params: VisibleParam[]; presentation: Record<string, unknown>; status: "OPEN" | "PASSED" | "FAILED";
  attemptsUsed: number; maxAttempts: number; hintsUsed: number; hintsAvailable: number; revealedHints: string[];
  draft?: unknown; score: number; points: number; feedbackPolicy: string; pgcVersion?: string; references: ReferenceView[];
  lastSubmission?: SubmissionView; serverNow: string;
}

export interface SubmissionAccepted { submissionId: string; status: string; receivedAt: string; serverNow: string }

export interface MyPosition { position?: number; points: number; movement?: number; gapToNext?: number; weekPoints: number }

export interface Activity { at: string; kind: "CORRECT" | "INCORRECT" | "STEP" | "REJECTED" | "PENDING"; label: string; points?: number }

export interface ContestCard {
  id: string; slug: string; title: string; mode: "TEAM" | "INDIVIDUAL"; minTeam: number; maxTeam: number; state: ContestState;
  registrationOpensAt: string; registrationClosesAt: string; startsAt: string; freezeAt: string; endsAt: string;
  myTeamName?: string; myTeamConfirmed?: number;
}

export type ContestState = "DRAFT" | "REGISTRATION" | "WAITING_ROOM" | "RUNNING" | "FROZEN" | "SUBMISSIONS_CLOSED" | "FINAL_EVALUATION" | "RESULTS_PUBLISHED" | "CLOSED";

export interface Dashboard {
  firstName: string; today: string; points: number; weekPoints: number; streak: number; practicedToday: boolean;
  coursePosition: MyPosition; courseName?: string; roomsCompleted: number; roomsInActiveTrack: number;
  tracksInProgress: TrackCard[]; nextContest?: ContestCard; recommended: RoomCard[]; activity: Activity[];
  continueRoomId?: string; continueRoomTitle?: string; serverNow: string;
}

export interface MemberView { name: string; initials: string; course?: string; year?: number; role: "CAPTAIN" | "MEMBER"; confirmed: boolean; me: boolean }
export interface TeamView { id: string; name: string; individual: boolean; locked: boolean; lockedAt?: string; confirmed: number; maxTeam: number; minTeam: number; captain: boolean; members: MemberView[]; pendingInvites: string[] }
export interface InviteView { teamId: string; teamName: string; contestId: string; contestTitle: string; invitedBy: string }

export interface ContestDetail extends ContestCard {
  description: string; lockAt: string; lockMinutes: number; wrongPenaltyMinutes: number; problemsCount: number;
  myTeam?: TeamView; myInvites?: InviteView[]; serverNow: string; finalPhase?: unknown;
}

export interface ProblemView {
  label: string; title: string; companyName?: string; type: AttemptView["type"]; points: number; attemptId: string;
  statement: string; segments: Segment[]; presentation: Record<string, unknown>;
  status: { state: "UNSOLVED" | "QUEUED" | "PROCESSING" | "SOLVED" | "REJECTED" | "FAILED"; wrongAttempts: number; lastSubmissionId?: string; lastStatus?: string; points?: number };
}

export interface ScoreRow { position: number; name: string; initials: string; points: number; penaltyMinutes: number; solved: number; lastCorrectAt?: string; mine: boolean }
export interface ScoreboardView { state: ContestState; frozen: boolean; freezeAt: string; rows: ScoreRow[]; serverNow: string }

export interface RankingEntry { position: number; name: string; initials: string; course?: string; year?: number; points: number; movement?: number; me: boolean; portfolioHandle?: string }
export interface RankingPage { scope: string; label: string; entries: RankingEntry[]; me: MyPosition; total: number; nextCursor?: string; generatedAt: string }

export interface BadgeView { code: string; title: string; description: string; points: number; awardedAt?: string }
export interface Portfolio {
  name: string; initials: string; course?: string; year?: number; isPublic: boolean; handle: string; tracksCompleted: number;
  roomsCompleted: number; examAverage?: number; bestPosition?: number; bestPositionLabel?: string;
  competencies: { topic: string; rooms: number }[]; badges: BadgeView[]; badgeCatalog: BadgeView[];
  history: { at: string; label: string; result: string }[];
}

export interface ExamCard { id: string; slug: string; title: string; durationMin: number; questions: number; passMark: number; openSessionId?: string; bestGrade?: number; lastStatus?: string }
export interface QuestionView { n: number; type: string; statement: string; segments: Segment[]; presentation: Record<string, unknown> }
export interface ExamSessionView { id: string; examId: string; title: string; status: string; startedAt: string; endsAt: string; serverNow: string; questions: QuestionView[]; answers: Record<string, unknown>; marked: number[] }
export interface ExamReport {
  sessionId: string; title: string; status: string; grade: number; passMark: number; passed: boolean; secondsUsed: number;
  correct: number; total: number; topics: { topic: string; correct: number; total: number }[]; strengths: string[];
  weaknesses: string[]; perQuestion: boolean[]; recommendedRooms: RoomCard[]; autoSubmitted: boolean;
}

export interface CursorPage<T> { items: T[]; nextCursor?: string }

export interface Problem { type?: string; title?: string; status?: number; detail?: string; code?: string; fields?: Record<string, string> }
