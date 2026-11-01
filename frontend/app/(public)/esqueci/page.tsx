import { AuthCard } from "@/components/auth/AuthCard";
import { ForgotForm } from "@/components/auth/ForgotForm";

export const metadata = { title: "Esqueci-me da palavra-passe" };

export default function Esqueci() {
  return <AuthCard step="Recuperar acesso" title="Esqueceste-te da palavra-passe?"><ForgotForm /></AuthCard>;
}
