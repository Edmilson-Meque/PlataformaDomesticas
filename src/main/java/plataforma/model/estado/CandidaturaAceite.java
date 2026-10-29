package plataforma.model.estado;

import plataforma.model.Candidatura;
import plataforma.model.EstadoCandidatura;

// Impede novas candidaturas (RF08). Só pode passar a CANCELADA, se a reunião não avançar.
public class CandidaturaAceite implements EstadoCandidaturaState {

    public EstadoCandidatura codigo() {
        return EstadoCandidatura.ACEITE;
    }

    public EstadoCandidaturaState cancelar() {
        return new CandidaturaCancelada();
    }

    public void verificarNovaCandidatura(Candidatura existente) {
        throw new IllegalStateException("[State ACEITE] " + existente.getTrabalhadora().getNome()
                + " já tem uma candidatura ACEITE. Não pode candidatar-se a outras vagas (RF08).");
    }
}
