package plataforma.event.consumidores;

import java.util.List;

import plataforma.event.EventListener;
import plataforma.event.EventoDominio;
import plataforma.model.Contrato;
import plataforma.model.Reuniao;
import plataforma.service.AgendamentoService;
import plataforma.service.VagaCandidaturaService;
import plataforma.util.Util;

// CONSUMIDOR SÍNCRONO de CONTRATO_CRIADO: a trabalhadora foi contratada, por isso
//  - as outras reuniões dela são CANCELADAS e a fila de espera acaba;
//  - as outras candidaturas pendentes dela são RECUSADAS.
// É síncrono porque tem de acontecer logo: ninguém pode marcar reunião com uma contratada.
public class EncerramentoContratoConsumer implements EventListener<Contrato> {

    private final AgendamentoService agendamentoService;
    private final VagaCandidaturaService vagaCandidaturaService;

    public EncerramentoContratoConsumer(AgendamentoService agendamentoService,
                                        VagaCandidaturaService vagaCandidaturaService) {
        this.agendamentoService = agendamentoService;
        this.vagaCandidaturaService = vagaCandidaturaService;
    }

    @Override
    public void onEvent(EventoDominio<Contrato> evento) {
        Contrato c = evento.getDados();
        List<Reuniao> canceladas = agendamentoService.encerrarPorContratacao(c.getTrabalhadora(), c.getReuniao());
        int recusadas = vagaCandidaturaService.recusarPendentesDe(c.getTrabalhadora());
        Util.log("[SÍNCRONO] [Contrato " + c.getId() + "] " + canceladas.size() + " outra(s) reunião(ões) cancelada(s), "
                + recusadas + " candidatura(s) pendente(s) recusada(s), fila de espera encerrada.");
    }
}
