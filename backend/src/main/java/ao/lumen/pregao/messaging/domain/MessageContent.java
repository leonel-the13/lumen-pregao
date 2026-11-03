package ao.lumen.pregao.messaging.domain;

/** Conteúdo de um email da administração, com variáveis ({nome}, {curso}, …) ainda por substituir. */
public record MessageContent(String subject, String title, String body, String ctaLabel, String ctaPath) {}
