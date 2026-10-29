package plataforma.model;

public enum EstadoCandidatura {
    PENDENTE,
    ACEITE,
    RECUSADA,
    CANCELADA   // foi aceite, mas a reunião não avançou (recusada, cancelada ou expirada)
}
