import { AuthCard } from "@/components/auth/AuthCard";
import { ResetForm } from "@/components/auth/ResetForm";

export const metadata = { title: "Definir palavra-passe" };

export default async function Redefinir({ searchParams }: { searchParams: Promise<{ token?: string; convite?: string }> }) {
  const sp = await searchParams;
  const invite = sp.convite === "1";
  return (
    <AuthCard step={invite ? "Convite" : "Redefinir palavra-passe"} title={invite ? "Activa a tua conta" : "Nova palavra-passe"}>
      <ResetForm token={sp.token} invite={invite} />
    </AuthCard>
  );
}
