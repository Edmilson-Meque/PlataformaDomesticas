package plataforma.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import plataforma.model.Contratante;
import plataforma.model.EstadoReuniao;
import plataforma.model.EstadoTrabalhadora;
import plataforma.model.Reuniao;
import plataforma.model.Trabalhadora;

// P01 - Agendamento de reuniões
public class AgendamentoService {

    private List<Reuniao> reunioes;
    private NotificacaoService notificacaoService;
    private Map<Trabalhadora, List<Contratante>> filaEspera = new HashMap<>();
    private int proximoId = 1;

    public AgendamentoService(List<Reuniao> reunioes, NotificacaoService notificacaoService) {
        this.reunioes = reunioes;
        this.notificacaoService = notificacaoService;
    }

    // O contratante pede para agendar uma reunião com a trabalhadora.
    // Devolve a Reuniao criada, ou null se não foi possível reservar.
    public Reuniao agendar(Contratante contratante, Trabalhadora trabalhadora) {
        EstadoTrabalhadora estado = trabalhadora.getEstado();

        if (estado == EstadoTrabalhadora.CONTRATADA) {
            System.out.println("   [Rejeitado] " + trabalhadora.getNome() + " já está contratada.");
            return null;
        }

        if (estado == EstadoTrabalhadora.EM_REUNIAO) {
            filaEspera.computeIfAbsent(trabalhadora, k -> new ArrayList<>()).add(contratante);
            System.out.println("   [Fila de espera] " + contratante.getNome()
                    + " ficou em espera por " + trabalhadora.getNome() + ".");
            return null;
        }

        // Está DISPONIVEL: reserva.
        Reuniao reuniao = new Reuniao(proximoId++, contratante, trabalhadora);
        reunioes.add(reuniao);
        trabalhadora.setEstado(EstadoTrabalhadora.EM_REUNIAO);
        System.out.println("   [Reservado] " + reuniao);
        notificacaoService.enviar(trabalhadora,
                "Convite de reunião de " + contratante.getNome());
        return reuniao;
    }

    // A trabalhadora aceita ou recusa o convite.
    public void responderConvite(Reuniao reuniao, boolean aceite) {
        if (reuniao.getEstado() != EstadoReuniao.PENDENTE) {
            System.out.println("   [Aviso] Este convite já foi respondido.");
            return;
        }

        Trabalhadora trabalhadora = reuniao.getTrabalhadora();
        Contratante contratante = reuniao.getContratante();

        if (aceite) {
            reuniao.setEstado(EstadoReuniao.ACEITE);
            notificacaoService.enviar(contratante,
                    trabalhadora.getNome() + " aceitou o convite.");
        } else {
            reuniao.setEstado(EstadoReuniao.RECUSADA);
            trabalhadora.setEstado(EstadoTrabalhadora.DISPONIVEL);
            notificacaoService.enviar(contratante,
                    trabalhadora.getNome() + " recusou o convite.");
            avisarFila(trabalhadora);
        }
    }

    // Avisa os contratantes em espera que a trabalhadora voltou a estar disponível.
    private void avisarFila(Trabalhadora trabalhadora) {
        List<Contratante> fila = filaEspera.get(trabalhadora);
        if (fila == null) {
            return;
        }
        for (Contratante c : fila) {
            notificacaoService.enviar(c,
                    trabalhadora.getNome() + " voltou a estar disponível.");
        }
        fila.clear();
    }
}