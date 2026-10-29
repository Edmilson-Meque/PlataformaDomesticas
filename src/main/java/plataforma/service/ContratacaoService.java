package plataforma.service;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import plataforma.event.EventBus;
import plataforma.event.TipoEvento;
import plataforma.logica.MotorRegras;
import plataforma.model.Contrato;
import plataforma.model.EstadoReuniao;
import plataforma.model.EstadoTrabalhadora;
import plataforma.model.Reuniao;
import plataforma.model.Trabalhadora;
import plataforma.util.Util;

// P02 - Contratação: LOCK + PARADIGMA LÓGICO.
// Depois do contrato, o consumidor EncerramentoContratoConsumer cancela as outras reuniões
// e a fila de espera; o pagamento é pedido através do broker (PedidoPagamentoConsumer).
public class ContratacaoService {

    private final List<Contrato> contratos;
    private final EventBus eventBus;
    private final AgendamentoService agendamentoService;
    private final MotorRegras motor;

    public ContratacaoService(List<Contrato> contratos, EventBus eventBus, AgendamentoService agendamentoService,
                              MotorRegras motor) {
        this.contratos = contratos;
        this.eventBus = eventBus;
        this.agendamentoService = agendamentoService;
        this.motor = motor;
    }

    public Contrato contratar(Reuniao reuniao) {
        Trabalhadora trabalhadora = reuniao.getTrabalhadora();
        Contrato contrato;
        ReentrantLock lock = agendamentoService.lockDe(trabalhadora);
        lock.lock();
        try {
            if (trabalhadora.getEstado() == EstadoTrabalhadora.CONTRATADA) {
                Util.log("[Rejeitado] " + trabalhadora.getNome() + " já está contratada.");
                return null;
            }
            if (reuniao.getEstado() != EstadoReuniao.ACEITE) {
                Util.log("[Rejeitado] Só se pode contratar após uma reunião aceite.");
                return null;
            }
            // PARADIGMA LÓGICO: o motor deduz se a contratação deve ser barrada
            if (motor.deduz(MotorRegras.BLOQUEAR_CONTRATACAO, trabalhadora)) {
                System.out.println("   [Paradigma Lógico] Contratação BLOQUEADA porque:");
                System.out.print(motor.explicar(MotorRegras.BLOQUEAR_CONTRATACAO, trabalhadora));
                return null;
            }
            synchronized (contratos) { // id + guardar numa só operação (atomicidade)
                contrato = new Contrato(Util.proximoId(contratos, Contrato::getId), reuniao.getContratante(),
                        trabalhadora, reuniao);
                contratos.add(contrato);
            }
            reuniao.setEstado(EstadoReuniao.CONCLUIDA); // a reunião cumpriu o seu papel
            trabalhadora.setEstado(EstadoTrabalhadora.CONTRATADA);
            System.out.println("   [Contratada] " + contrato);
        } finally {
            lock.unlock();
        }
        eventBus.publicar(TipoEvento.CONTRATO_CRIADO, contrato, contrato.getContratante().getNome());
        return contrato;
    }
}
