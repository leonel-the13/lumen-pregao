import { AuthCard } from "@/components/auth/AuthCard";
import { LoginForm } from "@/components/auth/LoginForm";

export const metadata = { title: "Entrar" };

export default async function Entrar({ searchParams }: { searchParams: Promise<{ next?: string; expirada?: string; definida?: string; verificado?: string }> }) {
  const sp = await searchParams;
  return (
    <AuthCard step="Entrar" title="Bem-vindo de volta">
      <LoginForm next={sp.next} expired={sp.expirada === "1"} passwordSet={sp.definida === "1"} verified={sp.verificado === "1"} />
    </AuthCard>
  );
}
