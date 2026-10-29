package plataforma.model.estado;

import plataforma.model.EstadoCandidatura;

// Estado final: nenhuma transição permitida.
public class CandidaturaRecusada implements EstadoCandidaturaState {

    public EstadoCandidatura codigo() {
        return EstadoCandidatura.RECUSADA;
    }
}
