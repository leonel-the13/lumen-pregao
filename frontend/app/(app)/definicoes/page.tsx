import { SetCrumbs } from "@/components/layout/CrumbsContext";
import { AccountSettings } from "@/components/settings/AccountSettings";
import { PrivacyCard } from "@/components/settings/PrivacyCard";
import { serverFetchOrLogin } from "@/lib/api-server";
import type { Me } from "@/lib/types";

export const metadata = { title: "Perfil e definições" };

export default async function Definicoes() {
  const me = await serverFetchOrLogin<Me>("/me", "/definicoes");
  return (
    <>
      <SetCrumbs items={[{ label: "Eu" }, { label: "Perfil e definições" }]} />
      <div className="stack" style={{ gap: 6 }}><div className="label">Eu</div><h1 className="h1">Perfil e definições</h1></div>
      <AccountSettings me={me} />
      <div style={{ maxWidth: 560 }}><PrivacyCard me={me} /></div>
    </>
  );
}
