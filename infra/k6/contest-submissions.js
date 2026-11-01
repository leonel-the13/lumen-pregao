// Carga no fim de um contest (CON-09): muitos participantes submetem ao mesmo tempo,
// com repetições da mesma Idempotency-Key (duplo clique / nova tentativa de rede).
// Uso: k6 run -e BASE=http://localhost:3000 -e CONTEST=<id> -e EMAILS=a@exemplo.ao,b@exemplo.ao infra/k6/contest-submissions.js
// Critérios: sem duplicados, sem pontos perdidos, ranking consistente. Metas quantitativas a confirmar.
import http from "k6/http";
import { check, sleep } from "k6";
import { uuidv4 } from "https://jslib.k6.io/k6-utils/1.4.0/index.js";

const BASE = __ENV.BASE || "http://localhost:3000";
const API = `${BASE}/api/v1`;
const CONTEST = __ENV.CONTEST;
const EMAILS = (__ENV.EMAILS || "").split(",").filter(Boolean);
const PASSWORD = __ENV.PASSWORD || "lumen-dev-2026!";

export const options = {
  scenarios: { fim_do_contest: { executor: "per-vu-iterations", vus: Math.max(1, EMAILS.length), iterations: 5, maxDuration: "2m" } },
  thresholds: { http_req_failed: ["rate<0.01"], "checks{kind:submission}": ["rate>0.99"] },
};

function csrf(jar) {
  http.get(`${API}/auth/csrf`);
  return jar.cookiesForURL(API)["XSRF-TOKEN"][0];
}

export default function () {
  const jar = http.cookieJar();
  const email = EMAILS[(__VU - 1) % EMAILS.length];
  let token = csrf(jar);
  const login = http.post(`${API}/auth/login`, JSON.stringify({ email, password: PASSWORD }),
    { headers: { "Content-Type": "application/json", "X-CSRF-Token": token } });
  check(login, { "login 204": (r) => r.status === 204 });
  token = csrf(jar);
  const key = uuidv4();
  const headers = { "Content-Type": "application/json", "X-CSRF-Token": token, "Idempotency-Key": key };
  const body = JSON.stringify({ value: Math.random() * 1000 });
  const responses = http.batch([0, 1, 2].map(() => ["POST", `${API}/contests/${CONTEST}/problems/A/submissions`, body, { headers }]));
  const ids = new Set(responses.map((r) => r.json("submissionId")));
  check(responses[0], { "202 ou 409/429 controlado": (r) => [202, 409, 429].includes(r.status) }, { kind: "submission" });
  check(ids, { "a mesma chave dá uma só submissão": (s) => s.size === 1 }, { kind: "submission" });
  sleep(1);
}
