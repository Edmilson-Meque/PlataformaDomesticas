package plataforma.event.consumidores;

import plataforma.event.EventListener;
import plataforma.event.EventoDominio;
import plataforma.model.Candidatura;
import plataforma.service.AgendamentoService;
import plataforma.util.Util;

// CONSUMIDOR SÍNCRONO de CANDIDATURA_ACEITE: envia AUTOMATICAMENTE um convite de reunião.
// Usa o MESMO método agendar() do agendamento directo, por isso respeita o LOCK e a fila de espera:
//  - se a trabalhadora estiver livre  -> convite PENDENTE (é ela que decide se aceita);
//  - se já estiver reservada          -> a contratante entra na fila de espera.
public class AgendamentoAutomaticoConsumer implements EventListener<Candidatura> {

    private final AgendamentoService agendamentoService;

    public AgendamentoAutomaticoConsumer(AgendamentoService agendamentoService) {
        this.agendamentoService = agendamentoService;
    }

    @Override
    public void onEvent(EventoDominio<Candidatura> evento) {
        Candidatura c = evento.getDados();
        Util.log("[SÍNCRONO] [Evento CANDIDATURA_ACEITE] A enviar automaticamente o convite de reunião...");
        agendamentoService.agendar(c.getVaga().getContratante(), c.getTrabalhadora(), c);
    }
}
