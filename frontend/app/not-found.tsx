import Link from "next/link";
import { Footer } from "@/components/layout/Footer";

export default function NotFound() {
  return (
    <>
    <div style={{ display: "grid", placeItems: "center", minHeight: "60vh", padding: 24 }}>
      <div className="card state-box center">
        <div className="label">Erro 404</div>
        <h1 className="h2">Esta página não existe</h1>
        <p className="small muted">O endereço pode estar errado ou o conteúdo já não estar disponível.</p>
        <Link className="btn out" href="/inicio">Voltar ao início</Link>
      </div>
    </div>
    <Footer />
    </>
  );
}
