package plataforma.model.estado;

import plataforma.model.EstadoCandidatura;

public class CandidaturaPendente implements EstadoCandidaturaState {

    public EstadoCandidatura codigo() {
        return EstadoCandidatura.PENDENTE;
    }

    public EstadoCandidaturaState aceitar() {
        return new CandidaturaAceite();
    }

    public EstadoCandidaturaState recusar() {
        return new CandidaturaRecusada();
    }
}
