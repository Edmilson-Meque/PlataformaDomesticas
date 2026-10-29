package plataforma.model.estado;

import plataforma.model.Candidatura;
import plataforma.model.EstadoCandidatura;

// PADRÃO STATE (Paradigma Orientado a Objectos)
// Cada estado da candidatura é uma CLASSE e decide, por polimorfismo, o que é permitido.
// Por omissão TUDO é proibido; cada estado só permite as transições que fazem sentido.
//   PENDENTE --aceitar--> ACEITE --cancelar--> CANCELADA
//   PENDENTE --recusar--> RECUSADA
public interface EstadoCandidaturaState {

    EstadoCandidatura codigo();

    default EstadoCandidaturaState aceitar() {
        throw new IllegalStateException("Não é permitido aceitar uma candidatura " + codigo());
    }

    default EstadoCandidaturaState recusar() {
        throw new IllegalStateException("Não é permitido recusar uma candidatura " + codigo());
    }

    default EstadoCandidaturaState cancelar() {
        throw new IllegalStateException("Não é permitido cancelar uma candidatura " + codigo());
    }

    // RF08 - submeter nova candidatura: por omissão é permitido
    default void verificarNovaCandidatura(Candidatura existente) {
    }

    // Converte o texto guardado na base de dados no objecto de estado certo
    static EstadoCandidaturaState de(EstadoCandidatura codigo) {
        return switch (codigo) {
            case PENDENTE -> new CandidaturaPendente();
            case ACEITE -> new CandidaturaAceite();
            case RECUSADA -> new CandidaturaRecusada();
            case CANCELADA -> new CandidaturaCancelada();
        };
    }
}
