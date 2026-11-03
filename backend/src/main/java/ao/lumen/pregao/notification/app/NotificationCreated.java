package ao.lumen.pregao.notification.app;

import java.util.UUID;

/** Publicado ao criar uma notificação; o módulo messaging envia por email as categorias importantes. */
public record NotificationCreated(UUID notificationId, UUID userId, String kind, String title, String body, String link) {}
