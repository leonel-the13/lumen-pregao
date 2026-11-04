import Link from "next/link";
import { Tbc } from "@/components/ui/Badge";
import type { Locale } from "@/lib/i18n/config";

export const TERMS_UPDATED = "01/11/2026";

/** Termos e condições: texto completo por língua. A versão portuguesa é a que vale. */
export function Terms({ locale }: { locale: Locale }) {
  if (locale === "en") return <TermsEn />;
  if (locale === "fr") return <TermsFr />;
  return <TermsPt />;
}

function TermsPt() {
  return (
    <>
      <div className="label">Termos e condições · versão de {TERMS_UPDATED}</div>
      <h1 className="h1">Termos e condições de utilização</h1>
      <p className="muted">Ao criar conta no Lúmen Lab aceitas estes termos. Lê também a <Link href="/privacidade">política de privacidade</Link> e
        as <Link href="/regras">regras e fontes</Link>. Texto preparado pela equipa do projecto; a revisão jurídica final está <Tbc />.</p>

      <h2 className="h2">1. O que é o Lúmen Lab</h2>
      <p>Uma plataforma educativa gratuita, criada por estudantes como iniciativa da Associação de Estudantes, para praticar
        contabilidade, fiscalidade, análise financeira e outras competências através de salas, exames e competições.
        <b> Não é um canal oficial do ISPTEC</b>: as notas, pontos e conquistas não substituem nem alteram avaliações académicas.</p>

      <h2 className="h2">2. Quem pode usar</h2>
      <ul>
        <li>Estudantes do ISPTEC de <b>qualquer curso</b>, com o email institucional de estudante (o que começa pelo número de estudante,
          ex.: <span className="mono">20221987@isptec.co.ao</span>). O conteúdo é focado nas Ciências Sociais Aplicadas, mas todos podem participar.</li>
        <li>Docentes, revisores, júris e membros da organização entram por convite da administração.</li>
        <li>A conta é pessoal e intransmissível. És responsável por manter a palavra-passe (e o 2.º factor, se o teu perfil o exigir) em segurança.</li>
      </ul>

      <h2 className="h2">3. Regras de conduta</h2>
      <ul>
        <li>Resolve as salas e exames por ti. Nos exames individuais não é permitida ajuda de terceiros.</li>
        <li>Nos contests, só colaboras com a tua equipa. Partilhar respostas entre equipas, usar várias contas ou automatizar submissões leva à desclassificação.</li>
        <li>Não tentes contornar a correcção, aceder a dados de outras pessoas, sobrecarregar o serviço nem explorar falhas de segurança.
          Se encontrares uma falha, comunica-a pelo botão <b>Feedback</b> (categoria «Algo não funciona»).</li>
        <li>Comentários, avaliações e feedback devem ser respeitosos. Não publiques dados pessoais de terceiros nem conteúdo ofensivo.</li>
      </ul>

      <h2 className="h2">4. Conteúdo académico</h2>
      <p>Os exercícios são originais e usam <b>empresas fictícias</b>. Cada exercício indica a norma e a versão aplicadas; referências
        ainda não confirmadas aparecem como «a confirmar». O conteúdo tem fins de prática e não constitui aconselhamento contabilístico,
        fiscal, jurídico nem de investimento. As simulações não envolvem dinheiro real.</p>

      <h2 className="h2">5. Avaliações e feedback</h2>
      <p>Podes avaliar as salas que já tentaste (1 a 5 estrelas e um comentário opcional) e enviar feedback sobre a plataforma.
        A equipa usa estas respostas para melhorar o conteúdo. A média pública só é mostrada com pelo menos 5 respostas, e nunca
        associada ao teu nome.</p>

      <h2 className="h2">6. Rankings, pontos e prémios</h2>
      <p>Os pontos e rankings servem para motivar a prática. Podes aparecer com pseudónimo ou esconder-te do ranking nas definições.
        Os resultados de um contest são publicados pela organização depois da verificação. Prémios, quando existirem, são anunciados
        pela organização com regras próprias.</p>

      <h2 className="h2">7. Suspensão e eliminação</h2>
      <p>A administração pode suspender contas que violem estes termos, com registo em auditoria. Podes pedir a eliminação da tua conta
        em <b>Definições</b>; depois de executado, o pedido apaga os teus dados pessoais (ver a política de privacidade).</p>

      <h2 className="h2">8. Disponibilidade</h2>
      <p>O serviço está em fase de MVP e é mantido por voluntários. Pode haver interrupções, alterações de conteúdo ou perda de
        funcionalidades. Fazemos o possível para avisar com antecedência, sobretudo antes de contests e exames.</p>

      <h2 className="h2">9. Alterações a estes termos</h2>
      <p>Se mudarmos estes termos, actualizamos a data no topo e avisamos na plataforma. Continuar a usar o serviço depois do aviso
        significa que aceitas a nova versão.</p>

      <h2 className="h2">10. Contacto</h2>
      <p>Para dúvidas sobre estes termos, usa o botão <b>Feedback</b> dentro da plataforma ou contacta a Associação de Estudantes.</p>
    </>
  );
}

function TermsEn() {
  return (
    <>
      <div className="label">Terms and conditions · version of {TERMS_UPDATED}</div>
      <h1 className="h1">Terms and conditions of use</h1>
      <p className="muted">By creating a Lúmen Lab account you accept these terms. Please also read the <Link href="/privacidade">privacy policy</Link> and
        the <Link href="/regras">rules and sources</Link>. Text prepared by the project team; the final legal review is <Tbc />.
        This is a convenience translation: if there is any difference, the Portuguese version prevails.</p>

      <h2 className="h2">1. What Lúmen Lab is</h2>
      <p>A free educational platform, created by students as a Students’ Union initiative, to practise accounting, taxation,
        financial analysis and other skills through rooms, exams and competitions.
        <b> It is not an official ISPTEC channel</b>: grades, points and badges do not replace or change academic assessment.</p>

      <h2 className="h2">2. Who can use it</h2>
      <ul>
        <li>ISPTEC students from <b>any degree</b>, with their institutional student email (the one starting with the student number,
          e.g. <span className="mono">20221987@isptec.co.ao</span>). The content focuses on the Applied Social Sciences, but everyone can take part.</li>
        <li>Lecturers, reviewers, jury members and organisers join by invitation from the administration.</li>
        <li>Your account is personal and non-transferable. You are responsible for keeping your password (and your second factor, if your role requires one) safe.</li>
      </ul>

      <h2 className="h2">3. Code of conduct</h2>
      <ul>
        <li>Solve rooms and exams yourself. Help from others is not allowed in individual exams.</li>
        <li>In contests, you only collaborate with your own team. Sharing answers between teams, using several accounts or automating submissions leads to disqualification.</li>
        <li>Do not try to bypass grading, access other people’s data, overload the service or exploit security flaws.
          If you find a flaw, report it with the <b>Feedback</b> button (category “Something isn’t working”).</li>
        <li>Comments, ratings and feedback must be respectful. Do not post other people’s personal data or offensive content.</li>
      </ul>

      <h2 className="h2">4. Academic content</h2>
      <p>The exercises are original and use <b>fictional companies</b>. Each exercise states the standard and version applied; references
        not yet confirmed are marked “to be confirmed”. The content is for practice and is not accounting, tax, legal or investment
        advice. Simulations do not involve real money.</p>

      <h2 className="h2">5. Ratings and feedback</h2>
      <p>You can rate rooms you have attempted (1 to 5 stars and an optional comment) and send feedback about the platform.
        The team uses these responses to improve the content. The public average is only shown with at least 5 responses, and is never
        linked to your name.</p>

      <h2 className="h2">6. Rankings, points and prizes</h2>
      <p>Points and rankings are there to motivate practice. You can appear with a nickname or hide from the ranking in your profile.
        Contest results are published by the organisers after verification. Prizes, when there are any, are announced by the
        organisers with their own rules.</p>

      <h2 className="h2">7. Suspension and deletion</h2>
      <p>The administration may suspend accounts that breach these terms, with an audit record. You can request the deletion of your account
        in <b>Settings</b>; once carried out, the request erases your personal data (see the privacy policy).</p>

      <h2 className="h2">8. Availability</h2>
      <p>The service is at MVP stage and is run by volunteers. There may be interruptions, content changes or loss of features.
        We do our best to give advance notice, especially before contests and exams.</p>

      <h2 className="h2">9. Changes to these terms</h2>
      <p>If we change these terms, we update the date at the top and announce it on the platform. Continuing to use the service after
        the notice means you accept the new version.</p>

      <h2 className="h2">10. Contact</h2>
      <p>For questions about these terms, use the <b>Feedback</b> button inside the platform or contact the Students’ Union.</p>
    </>
  );
}

function TermsFr() {
  return (
    <>
      <div className="label">Conditions générales · version du {TERMS_UPDATED}</div>
      <h1 className="h1">Conditions générales d’utilisation</h1>
      <p className="muted">En créant un compte sur Lúmen Lab, tu acceptes ces conditions. Lis aussi la <Link href="/privacidade">politique de confidentialité</Link> et
        les <Link href="/regras">règles et sources</Link>. Texte préparé par l’équipe du projet ; la relecture juridique finale est <Tbc />.
        Ceci est une traduction de courtoisie : en cas de divergence, la version portugaise prévaut.</p>

      <h2 className="h2">1. Ce qu’est Lúmen Lab</h2>
      <p>Une plateforme éducative gratuite, créée par des étudiants à l’initiative de l’Association des étudiants, pour s’entraîner en
        comptabilité, fiscalité, analyse financière et autres compétences grâce à des salles, des examens et des compétitions.
        <b> Ce n’est pas un canal officiel de l’ISPTEC</b> : les notes, points et badges ne remplacent ni ne modifient les évaluations académiques.</p>

      <h2 className="h2">2. Qui peut l’utiliser</h2>
      <ul>
        <li>Les étudiants de l’ISPTEC de <b>toutes les filières</b>, avec leur e-mail institutionnel étudiant (celui qui commence par le numéro d’étudiant,
          ex. : <span className="mono">20221987@isptec.co.ao</span>). Le contenu porte sur les Sciences sociales appliquées, mais tout le monde peut participer.</li>
        <li>Les enseignants, relecteurs, membres du jury et de l’organisation rejoignent la plateforme sur invitation de l’administration.</li>
        <li>Le compte est personnel et incessible. Tu es responsable de la sécurité de ton mot de passe (et de ton second facteur, si ton rôle l’exige).</li>
      </ul>

      <h2 className="h2">3. Règles de conduite</h2>
      <ul>
        <li>Résous les salles et les examens toi-même. L’aide de tiers est interdite pendant les examens individuels.</li>
        <li>En compétition, tu ne collabores qu’avec ton équipe. Partager des réponses entre équipes, utiliser plusieurs comptes ou automatiser les soumissions entraîne la disqualification.</li>
        <li>N’essaie pas de contourner la correction, d’accéder aux données d’autres personnes, de surcharger le service ni d’exploiter des failles de sécurité.
          Si tu trouves une faille, signale-la avec le bouton <b>Avis</b> (catégorie « Quelque chose ne fonctionne pas »).</li>
        <li>Les commentaires, notes et avis doivent rester respectueux. Ne publie ni données personnelles de tiers ni contenu offensant.</li>
      </ul>

      <h2 className="h2">4. Contenu académique</h2>
      <p>Les exercices sont originaux et utilisent des <b>entreprises fictives</b>. Chaque exercice indique la norme et la version appliquées ; les références
        pas encore confirmées sont marquées « à confirmer ». Le contenu sert à s’entraîner et ne constitue pas un conseil comptable,
        fiscal, juridique ou d’investissement. Les simulations n’impliquent pas d’argent réel.</p>

      <h2 className="h2">5. Notes et avis</h2>
      <p>Tu peux noter les salles que tu as tentées (1 à 5 étoiles et un commentaire facultatif) et envoyer un avis sur la plateforme.
        L’équipe s’en sert pour améliorer le contenu. La moyenne publique n’est affichée qu’à partir de 5 réponses et n’est jamais
        associée à ton nom.</p>

      <h2 className="h2">6. Classements, points et prix</h2>
      <p>Les points et les classements servent à motiver la pratique. Tu peux apparaître sous un pseudonyme ou te masquer du classement dans ton profil.
        Les résultats d’une compétition sont publiés par l’organisation après vérification. Les prix, s’il y en a, sont annoncés
        par l’organisation avec leurs propres règles.</p>

      <h2 className="h2">7. Suspension et suppression</h2>
      <p>L’administration peut suspendre les comptes qui enfreignent ces conditions, avec un enregistrement d’audit. Tu peux demander la suppression de ton compte
        dans <b>Paramètres</b> ; une fois exécutée, la demande efface tes données personnelles (voir la politique de confidentialité).</p>

      <h2 className="h2">8. Disponibilité</h2>
      <p>Le service est en phase de MVP et est maintenu par des bénévoles. Il peut y avoir des interruptions, des changements de contenu ou des pertes
        de fonctionnalités. Nous faisons de notre mieux pour prévenir à l’avance, surtout avant les compétitions et les examens.</p>

      <h2 className="h2">9. Modifications de ces conditions</h2>
      <p>Si nous modifions ces conditions, nous mettons à jour la date en haut de page et l’annonçons sur la plateforme. Continuer à utiliser le service
        après l’annonce signifie que tu acceptes la nouvelle version.</p>

      <h2 className="h2">10. Contact</h2>
      <p>Pour toute question sur ces conditions, utilise le bouton <b>Avis</b> dans la plateforme ou contacte l’Association des étudiants.</p>
    </>
  );
}
