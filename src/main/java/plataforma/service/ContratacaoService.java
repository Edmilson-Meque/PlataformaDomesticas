package plataforma.service;

import java.util.List;

import plataforma.model.Contrato;
import plataforma.model.EstadoReuniao;
import plataforma.model.EstadoTrabalhadora;
import plataforma.model.Reuniao;
import plataforma.model.Trabalhadora;

// P02 - Contratação
public class ContratacaoService {

    private List<Contrato> contratos;
    private NotificacaoService notificacaoService;
    private int proximoId = 1;

    public ContratacaoService(List<Contrato> contratos, NotificacaoService notificacaoService) {
        this.contratos = contratos;
        this.notificacaoService = notificacaoService;
    }

    // O contratante confirma a contratação depois da reunião.
    // Devolve o Contrato criado, ou null se não foi possível contratar.
    public Contrato contratar(Reuniao reuniao) {
        Trabalhadora trabalhadora = reuniao.getTrabalhadora();

        if (trabalhadora.getEstado() == EstadoTrabalhadora.CONTRATADA) {
            System.out.println("   [Rejeitado] " + trabalhadora.getNome() + " já está contratada.");
            return null;
        }

        if (reuniao.getEstado() != EstadoReuniao.ACEITE) {
            System.out.println("   [Rejeitado] Só se pode contratar após uma reunião aceite.");
            return null;
        }

        Contrato contrato = new Contrato(proximoId++, reuniao.getContratante(), trabalhadora, reuniao);
        contratos.add(contrato);
        trabalhadora.setEstado(EstadoTrabalhadora.CONTRATADA);
        System.out.println("   [Contratada] " + contrato);

        notificacaoService.enviar(trabalhadora,
                "Foste contratada por " + reuniao.getContratante().getNome() + ".");
        return contrato;
    }
}