import { AuthCard } from "@/components/auth/AuthCard";
import { VerifyEmail } from "@/components/auth/VerifyEmail";

export const metadata = { title: "Verificar email" };

export default async function Verificar({ searchParams }: { searchParams: Promise<{ token?: string; email?: string }> }) {
  const sp = await searchParams;
  return (
    <AuthCard step="Verificar email" title="Confirma o teu email">
      <VerifyEmail token={sp.token} email={sp.email} />
    </AuthCard>
  );
}
