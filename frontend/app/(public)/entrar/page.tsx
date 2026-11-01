import { AuthCard } from "@/components/auth/AuthCard";
import { LoginForm } from "@/components/auth/LoginForm";

export const metadata = { title: "Entrar" };

export default async function Entrar({ searchParams }: { searchParams: Promise<{ next?: string; expirada?: string }> }) {
  const sp = await searchParams;
  return (
    <AuthCard step="Entrar" title="Bem-vindo de volta">
      <LoginForm next={sp.next} expired={sp.expirada === "1"} />
    </AuthCard>
  );
}
