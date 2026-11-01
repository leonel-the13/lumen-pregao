package ao.lumen.pregao.identity.domain;

/** INVITED: conta criada por um administrador; só entra depois de definir a palavra-passe pelo link do email. */
public enum UserStatus { PENDING_VERIFICATION, INVITED, ACTIVE, SUSPENDED, DELETION_REQUESTED }
