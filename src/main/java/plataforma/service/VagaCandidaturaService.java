package plataforma.service;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import plataforma.event.EventBus;
import plataforma.event.TipoEvento;
import plataforma.logica.MotorRegras;
import plataforma.model.Candidatura;
import plataforma.model.Contratante;
import plataforma.model.EstadoCandidatura;
import plataforma.model.EstadoTrabalhadora;
import plataforma.model.Trabalhadora;
import plataforma.model.Vaga;
import plataforma.util.Util;

public class VagaCandidaturaService {

    private final List<Vaga> vagas;
    private final List<Candidatura> candidaturas;
    private final EventBus eventBus;
    private final MotorRegras motor;

    public VagaCandidaturaService(List<Vaga> vagas, List<Candidatura> candidaturas, EventBus eventBus, MotorRegras motor) {
        this.vagas = vagas;
        this.candidaturas = candidaturas;
        this.eventBus = eventBus;
        this.motor = motor;
    }

    public synchronized Vaga publicarVaga(Contratante contratante, String titulo, String provincia, String competencia, double salario) {
        Vaga vaga = new Vaga(Util.proximoId(vagas, Vaga::getId), contratante, titulo, provincia, competencia, salario);
        vagas.add(vaga);
        System.out.println("   [Vaga Publicada] " + vaga);
        return vaga;
    }

    // synchronized: várias trabalhadoras a candidatar-se ao mesmo tempo não se atropelam
    public synchronized Candidatura candidatar(Trabalhadora trabalhadora, Vaga vaga) {
        if (!vaga.isAberta()) {
            System.out.println("   [Rejeitado] Esta vaga já está fechada.");
            return null;
        }
        // PARADIGMA LÓGICO: um perfil oculto (não validado ou bloqueado) não se pode candidatar
        if (motor.deduz(MotorRegras.OCULTAR_DAS_PESQUISAS, trabalhadora)) {
            System.out.println("   [Rejeitado] O teu perfil ainda não foi validado pelo administrador"
                    + " (ou está bloqueado). Não podes candidatar-te.");
            return null;
        }
        // PADRÃO STATE (RF08): cada candidatura da trabalhadora decide se permite uma nova.
        // O estado ACEITE lança excepção e bloqueia.
        try {
            candidaturas.stream()
                    .filter(c -> c.getTrabalhadora().equals(trabalhadora))
                    .forEach(Candidatura::verificarNovaCandidatura);
        } catch (IllegalStateException e) {
            System.out.println("   [Rejeitado] " + e.getMessage());
            return null;
        }
        if (trabalhadora.getEstado() == EstadoTrabalhadora.CONTRATADA) {
            System.out.println("   [Rejeitado] Trabalhadora já está contratada.");
            return null;
        }

        boolean jaCandidatou = candidaturas.stream()
                .anyMatch(c -> c.getVaga().equals(vaga) && c.getTrabalhadora().equals(trabalhadora)
                        && (c.getEstado() == EstadoCandidatura.PENDENTE || c.getEstado() == EstadoCandidatura.ACEITE));
        if (jaCandidatou) {
            System.out.println("   [Aviso] Já submeteste candidatura a esta vaga.");
            return null;
        }

        Candidatura candidatura = new Candidatura(Util.proximoId(candidaturas, Candidatura::getId), vaga, trabalhadora);
        candidaturas.add(candidatura);
        System.out.println("   [Candidatura Submetida] " + candidatura);

        // Dispara evento de submissão
        eventBus.publicar(TipoEvento.CANDIDATURA_SUBMETIDA, candidatura, trabalhadora.getNome());
        return candidatura;
    }

    // Aceita ou recusa candidatura e dispara eventos (ex.: CANDIDATURA_ACEITE)
    // As transições são feitas pelos objectos de estado (Padrão State).
    public synchronized void avaliarCandidatura(Candidatura candidatura, boolean aceitar) {
        if (candidatura.getEstado() != EstadoCandidatura.PENDENTE) {
            System.out.println("   [Aviso] Esta candidatura já foi avaliada.");
            return;
        }
        String autor = candidatura.getVaga().getContratante().getNome();
        if (aceitar) {
            candidatura.aceitar();
            candidatura.getVaga().setAberta(false);

            // Recusa funcionalmente as outras candidaturas pendentes da mesma vaga
            candidaturas.stream()
                    .filter(c -> c.getVaga().equals(candidatura.getVaga()))
                    .filter(c -> !c.equals(candidatura))
                    .filter(c -> c.getEstado() == EstadoCandidatura.PENDENTE)
                    .forEach(c -> {
                        c.recusar();
                        eventBus.publicar(TipoEvento.CANDIDATURA_RECUSADA, c, autor);
                    });

            // Dispara o evento central CANDIDATURA_ACEITE
            eventBus.publicar(TipoEvento.CANDIDATURA_ACEITE, candidatura, autor);
        } else {
            candidatura.recusar();
            eventBus.publicar(TipoEvento.CANDIDATURA_RECUSADA, candidatura, autor);
        }
    }

    // Depois de um contrato: as candidaturas pendentes da trabalhadora são recusadas
    public synchronized int recusarPendentesDe(Trabalhadora trabalhadora) {
        List<Candidatura> pendentes = candidaturas.stream()
                .filter(c -> c.getTrabalhadora() == trabalhadora)
                .filter(c -> c.getEstado() == EstadoCandidatura.PENDENTE)
                .collect(Collectors.toList());
        for (Candidatura c : pendentes) {
            c.recusar();
            eventBus.publicar(TipoEvento.CANDIDATURA_RECUSADA, c, "Sistema");
        }
        return pendentes.size();
    }

    // Operações Funcionais: filter + sorted (ordenar vagas abertas por salário decrescente)
    public List<Vaga> listarVagasAbertasOrdenadasPorSalario() {
        return vagas.stream()
                .filter(Vaga::isAberta)
                .sorted(Comparator.comparingDouble(Vaga::getSalarioOferecido).reversed())
                .collect(Collectors.toList());
    }

    // Operações Funcionais: filter com Predicate dinâmico + sorted
    public List<Vaga> filtrarVagas(Predicate<Vaga> criterio, Comparator<Vaga> ordenacao) {
        return vagas.stream()
                .filter(Vaga::isAberta)
                .filter(criterio)
                .sorted(ordenacao)
                .collect(Collectors.toList());
    }

    // Operações Funcionais: filter + map (extrair resumos formatados de vagas)
    public List<String> mapearResumosDeVagasAbertas() {
        return vagas.stream()
                .filter(Vaga::isAberta)
                .sorted(Comparator.comparingDouble(Vaga::getSalarioOferecido).reversed())
                .map(v -> String.format("[#%d] %s em %s - %.2f MT (Req: %s)",
                        v.getId(), v.getTitulo(), v.getProvincia(), v.getSalarioOferecido(), v.getCompetenciaRequerida()))
                .collect(Collectors.toList());
    }

    // Operações Funcionais sobre candidaturas: filter + sorted
    public List<Candidatura> listarCandidaturasPendentesDoContratante(Contratante contratante) {
        return candidaturas.stream()
                .filter(c -> c.getVaga().getContratante().equals(contratante))
                .filter(c -> c.getEstado() == EstadoCandidatura.PENDENTE)
                .sorted(Comparator.comparing(Candidatura::getSubmetidaEm))
                .collect(Collectors.toList());
    }

    // Operações Funcionais: filter + map (extrair nomes das candidatas a uma vaga)
    public List<String> mapearNomesCandidatasPorVaga(Vaga vaga) {
        return candidaturas.stream()
                .filter(c -> c.getVaga().equals(vaga))
                .map(c -> c.getTrabalhadora().getNome() + " (" + c.getEstado() + ")")
                .collect(Collectors.toList());
    }

    // Operações Funcionais: filter + sorted para histórico da trabalhadora
    public List<Candidatura> listarCandidaturasDaTrabalhadora(Trabalhadora trabalhadora) {
        return candidaturas.stream()
                .filter(c -> c.getTrabalhadora().equals(trabalhadora))
                .sorted(Comparator.comparing(Candidatura::getSubmetidaEm).reversed())
                .collect(Collectors.toList());
    }
}