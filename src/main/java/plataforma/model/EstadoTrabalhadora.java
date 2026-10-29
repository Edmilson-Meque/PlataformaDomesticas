package plataforma.model;

public enum EstadoTrabalhadora {
    DISPONIVEL,
    EM_REUNIAO,    // reservada por uma contratante
    UNDER_OFFER,   // reservada E com outras contratantes na fila de espera
    CONTRATADA
}
