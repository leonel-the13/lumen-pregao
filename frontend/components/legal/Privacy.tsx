import Link from "next/link";
import { Tbc } from "@/components/ui/Badge";
import type { Locale } from "@/lib/i18n/config";

export const PRIVACY_UPDATED = "04/11/2026";

/** Política de privacidade: texto completo por língua. A versão portuguesa é a que vale. */
export function Privacy({ locale }: { locale: Locale }) {
  if (locale === "en") return <PrivacyEn />;
  if (locale === "fr") return <PrivacyFr />;
  return <PrivacyPt />;
}

function PrivacyPt() {
  return (
    <>
      <div className="label">Política de privacidade · versão de {PRIVACY_UPDATED}</div>
      <h1 className="h1">Como tratamos os teus dados</h1>
      <p className="muted">Texto preparado pela equipa do projecto. A versão final e o enquadramento na Lei n.º 22/11 (Lei da Protecção
        de Dados Pessoais) estão <Tbc />. Ver também os <Link href="/termos">termos e condições</Link>.</p>

      <h2 className="h2">Quem é responsável</h2>
      <p>O Lúmen Lab é uma iniciativa da Associação de Estudantes. Não é um canal oficial do ISPTEC e o ISPTEC não recebe os teus
        dados através desta plataforma.</p>

      <h2 className="h2">Que dados guardamos</h2>
      <ul>
        <li><b>Conta:</b> nome, email, curso, ano, palavra-passe (só em hash Argon2id, nunca em texto) e, se aplicável, o 2.º factor (cifrado).</li>
        <li><b>Actividade:</b> tentativas, respostas submetidas, pontos, conquistas, exames, equipas e participação em contests.</li>
        <li><b>Participação:</b> avaliações de salas, feedback e notificações.</li>
        <li><b>Preferências:</b> tema, língua, pseudónimo, visibilidade no ranking e portefólio público (desligado por defeito).</li>
        <li><b>Segurança:</b> registos de auditoria de operações sensíveis (ex.: entradas, mudanças de perfil), com data e endereço IP.</li>
      </ul>

      <h2 className="h2">Para quê</h2>
      <p>Para corrigir as tuas respostas, mostrar o progresso, calcular rankings, organizar exames e competições, enviar emails
        necessários (confirmação de conta, recuperação de acesso, convites), proteger a plataforma contra abusos e melhorar o conteúdo.
        <b> Não vendemos os teus dados nem os usamos para publicidade.</b></p>

      <h2 className="h2">Quem vê o quê</h2>
      <ul>
        <li>Outros estudantes só veem o teu nome ou pseudónimo no ranking (se não te esconderes) e na tua equipa.</li>
        <li>Avaliações e feedback são vistos pela administração; a média pública das salas é anónima.</li>
        <li>A administração e os revisores acedem aos dados necessários às suas funções, com registo em auditoria.</li>
      </ul>

      <h2 className="h2">Onde ficam os dados</h2>
      <p>Usamos fornecedores de alojamento na nuvem para o site (Vercel), o servidor (Render), a base de dados (Neon) e o envio de
        email (Resend). Estes serviços podem estar fora de Angola e tratam os dados apenas por nossa conta.</p>

      <h2 className="h2">Cookies</h2>
      <p>Só usamos cookies necessários: a sessão (<span className="mono">LP_SESSION</span>), a protecção contra pedidos forjados
        (<span className="mono">XSRF-TOKEN</span>) e as preferências de tema, língua e barra lateral
        (<span className="mono">lp_theme</span>, <span className="mono">lp_lang</span>, <span className="mono">lp_side</span>). Não usamos cookies de publicidade nem de rastreio de terceiros.</p>

      <h2 className="h2">Durante quanto tempo</h2>
      <p>Enquanto a conta existir. Quando pedes a eliminação, a administração executa o pedido: apagamos o nome, o email, o pseudónimo,
        os perfis e o 2.º factor, e retiramos a conta dos rankings. Os registos de pontos e de auditoria, que não podem ser alterados,
        ficam associados a uma conta anónima. Prazos exactos de conservação <Tbc />.</p>

      <h2 className="h2">Os teus direitos</h2>
      <ul>
        <li><b>Acesso e portabilidade:</b> exporta os teus dados em JSON em <b>Definições</b>.</li>
        <li><b>Rectificação:</b> actualiza o nome, o curso, o ano e o pseudónimo em <b>Editar perfil</b> e as preferências em <b>Definições</b>, ou pede ajuda pelo Feedback.</li>
        <li><b>Eliminação:</b> pede a eliminação da conta em <b>Definições</b>.</li>
        <li><b>Oposição:</b> podes esconder-te do ranking e manter o portefólio privado.</li>
      </ul>

      <h2 className="h2">Segurança</h2>
      <p>Ligações cifradas (HTTPS), palavras-passe com Argon2id, 2.º factor obrigatório para perfis com privilégios, limites de
        tentativas e registo de auditoria. Se suspeitares de acesso indevido à tua conta, muda a palavra-passe e avisa-nos.</p>

      <h2 className="h2">Contacto</h2>
      <p>Para questões de privacidade, usa o botão <b>Feedback</b> dentro da plataforma ou contacta a Associação de Estudantes.</p>
    </>
  );
}

function PrivacyEn() {
  return (
    <>
      <div className="label">Privacy policy · version of {PRIVACY_UPDATED}</div>
      <h1 className="h1">How we handle your data</h1>
      <p className="muted">Text prepared by the project team. The final version and its alignment with Law no. 22/11 (Personal Data
        Protection Law) are <Tbc />. See also the <Link href="/termos">terms and conditions</Link>. This is a convenience translation:
        if there is any difference, the Portuguese version prevails.</p>

      <h2 className="h2">Who is responsible</h2>
      <p>Lúmen Lab is a Students’ Union initiative. It is not an official ISPTEC channel, and ISPTEC does not receive your
        data through this platform.</p>

      <h2 className="h2">What data we keep</h2>
      <ul>
        <li><b>Account:</b> name, email, degree, year, password (only as an Argon2id hash, never in plain text) and, where applicable, your second factor (encrypted).</li>
        <li><b>Activity:</b> attempts, submitted answers, points, badges, exams, teams and contest participation.</li>
        <li><b>Participation:</b> room ratings, feedback and notifications.</li>
        <li><b>Preferences:</b> theme, language, nickname, ranking visibility and public portfolio (off by default).</li>
        <li><b>Security:</b> audit logs of sensitive operations (e.g. sign-ins, role changes), with date and IP address.</li>
      </ul>

      <h2 className="h2">What for</h2>
      <p>To grade your answers, show your progress, calculate rankings, run exams and competitions, send necessary emails
        (account confirmation, access recovery, invitations), protect the platform against abuse and improve the content.
        <b> We do not sell your data or use it for advertising.</b></p>

      <h2 className="h2">Who sees what</h2>
      <ul>
        <li>Other students only see your name or nickname in the ranking (unless you hide) and in your team.</li>
        <li>Ratings and feedback are seen by the administration; the public room average is anonymous.</li>
        <li>The administration and reviewers access the data they need for their roles, with an audit record.</li>
      </ul>

      <h2 className="h2">Where the data is stored</h2>
      <p>We use cloud hosting providers for the website (Vercel), the server (Render), the database (Neon) and email delivery
        (Resend). These services may be outside Angola and process data only on our behalf.</p>

      <h2 className="h2">Cookies</h2>
      <p>We only use necessary cookies: the session (<span className="mono">LP_SESSION</span>), protection against forged requests
        (<span className="mono">XSRF-TOKEN</span>) and your theme, language and sidebar preferences
        (<span className="mono">lp_theme</span>, <span className="mono">lp_lang</span>, <span className="mono">lp_side</span>).
        We do not use advertising or third-party tracking cookies.</p>

      <h2 className="h2">How long</h2>
      <p>For as long as the account exists. When you request deletion, the administration carries it out: we erase your name, email, nickname,
        roles and second factor, and remove the account from the rankings. Points and audit records, which cannot be changed,
        remain linked to an anonymous account. Exact retention periods are <Tbc />.</p>

      <h2 className="h2">Your rights</h2>
      <ul>
        <li><b>Access and portability:</b> export your data as JSON in <b>Settings</b>.</li>
        <li><b>Rectification:</b> update your name, degree, year and nickname in <b>Edit profile</b> and your preferences in <b>Settings</b>, or ask for help via Feedback.</li>
        <li><b>Erasure:</b> request account deletion in <b>Settings</b>.</li>
        <li><b>Objection:</b> you can hide from the ranking and keep your portfolio private.</li>
      </ul>

      <h2 className="h2">Security</h2>
      <p>Encrypted connections (HTTPS), Argon2id password hashing, mandatory second factor for privileged roles, attempt limits
        and audit logging. If you suspect someone has accessed your account, change your password and let us know.</p>

      <h2 className="h2">Contact</h2>
      <p>For privacy questions, use the <b>Feedback</b> button inside the platform or contact the Students’ Union.</p>
    </>
  );
}

function PrivacyFr() {
  return (
    <>
      <div className="label">Politique de confidentialité · version du {PRIVACY_UPDATED}</div>
      <h1 className="h1">Comment nous traitons tes données</h1>
      <p className="muted">Texte préparé par l’équipe du projet. La version finale et sa conformité à la loi n° 22/11 (loi sur la protection
        des données personnelles) sont <Tbc />. Voir aussi les <Link href="/termos">conditions générales</Link>. Ceci est une traduction de
        courtoisie : en cas de divergence, la version portugaise prévaut.</p>

      <h2 className="h2">Qui est responsable</h2>
      <p>Lúmen Lab est une initiative de l’Association des étudiants. Ce n’est pas un canal officiel de l’ISPTEC, et l’ISPTEC ne reçoit pas tes
        données par cette plateforme.</p>

      <h2 className="h2">Quelles données nous conservons</h2>
      <ul>
        <li><b>Compte :</b> nom, e-mail, filière, année, mot de passe (uniquement sous forme de hachage Argon2id, jamais en clair) et, le cas échéant, ton second facteur (chiffré).</li>
        <li><b>Activité :</b> tentatives, réponses soumises, points, badges, examens, équipes et participation aux compétitions.</li>
        <li><b>Participation :</b> notes des salles, avis et notifications.</li>
        <li><b>Préférences :</b> thème, langue, pseudonyme, visibilité dans le classement et portfolio public (désactivé par défaut).</li>
        <li><b>Sécurité :</b> journaux d’audit des opérations sensibles (ex. : connexions, changements de rôle), avec date et adresse IP.</li>
      </ul>

      <h2 className="h2">Dans quel but</h2>
      <p>Pour corriger tes réponses, afficher ta progression, calculer les classements, organiser les examens et compétitions, envoyer les e-mails
        nécessaires (confirmation du compte, récupération d’accès, invitations), protéger la plateforme contre les abus et améliorer le contenu.
        <b> Nous ne vendons pas tes données et ne les utilisons pas à des fins publicitaires.</b></p>

      <h2 className="h2">Qui voit quoi</h2>
      <ul>
        <li>Les autres étudiants ne voient que ton nom ou ton pseudonyme dans le classement (sauf si tu te masques) et dans ton équipe.</li>
        <li>Les notes et avis sont vus par l’administration ; la moyenne publique des salles est anonyme.</li>
        <li>L’administration et les relecteurs accèdent aux données nécessaires à leurs fonctions, avec un enregistrement d’audit.</li>
      </ul>

      <h2 className="h2">Où sont stockées les données</h2>
      <p>Nous utilisons des hébergeurs cloud pour le site (Vercel), le serveur (Render), la base de données (Neon) et l’envoi
        d’e-mails (Resend). Ces services peuvent se trouver hors d’Angola et ne traitent les données que pour notre compte.</p>

      <h2 className="h2">Cookies</h2>
      <p>Nous n’utilisons que des cookies nécessaires : la session (<span className="mono">LP_SESSION</span>), la protection contre les requêtes falsifiées
        (<span className="mono">XSRF-TOKEN</span>) et tes préférences de thème, de langue et de barre latérale
        (<span className="mono">lp_theme</span>, <span className="mono">lp_lang</span>, <span className="mono">lp_side</span>).
        Nous n’utilisons ni cookies publicitaires ni cookies de suivi tiers.</p>

      <h2 className="h2">Pendant combien de temps</h2>
      <p>Tant que le compte existe. Lorsque tu demandes la suppression, l’administration l’exécute : nous effaçons ton nom, ton e-mail, ton pseudonyme,
        tes rôles et ton second facteur, et retirons le compte des classements. Les points et les journaux d’audit, qui ne peuvent pas être modifiés,
        restent liés à un compte anonyme. Les durées exactes de conservation sont <Tbc />.</p>

      <h2 className="h2">Tes droits</h2>
      <ul>
        <li><b>Accès et portabilité :</b> exporte tes données en JSON dans <b>Paramètres</b>.</li>
        <li><b>Rectification :</b> mets à jour ton nom, ta filière, ton année et ton pseudonyme dans <b>Modifier le profil</b> et tes préférences dans <b>Paramètres</b>, ou demande de l’aide via Avis.</li>
        <li><b>Effacement :</b> demande la suppression du compte dans <b>Paramètres</b>.</li>
        <li><b>Opposition :</b> tu peux te masquer du classement et garder ton portfolio privé.</li>
      </ul>

      <h2 className="h2">Sécurité</h2>
      <p>Connexions chiffrées (HTTPS), mots de passe hachés avec Argon2id, second facteur obligatoire pour les rôles privilégiés, limitation des
        tentatives et journal d’audit. Si tu soupçonnes un accès non autorisé à ton compte, change ton mot de passe et préviens-nous.</p>

      <h2 className="h2">Contact</h2>
      <p>Pour toute question de confidentialité, utilise le bouton <b>Avis</b> dans la plateforme ou contacte l’Association des étudiants.</p>
    </>
  );
}
