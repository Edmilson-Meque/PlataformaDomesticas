package plataforma.model.estado;

import plataforma.model.EstadoCandidatura;

// Estado final: a candidatura foi aceite mas a reunião não avançou.
// Ao contrário do estado ACEITE, NÃO bloqueia novas candidaturas.
public class CandidaturaCancelada implements EstadoCandidaturaState {

    public EstadoCandidatura codigo() {
        return EstadoCandidatura.CANCELADA;
    }
}
