import { AuthCard } from "@/components/auth/AuthCard";
import { RegisterForm } from "@/components/auth/RegisterForm";
import { serverFetch } from "@/lib/api-server";

export const metadata = { title: "Criar conta" };

export default async function Registo() {
  const courses = await serverFetch<{ id: string; name: string }[]>("/courses").catch(() => []);
  return (
    <AuthCard step="Criar conta" title="Cria a tua conta">
      <RegisterForm courses={courses} />
    </AuthCard>
  );
}
