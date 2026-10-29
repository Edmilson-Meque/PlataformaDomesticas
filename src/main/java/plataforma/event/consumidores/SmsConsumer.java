package plataforma.event.consumidores;

import plataforma.event.EventListener;
import plataforma.event.EventoDominio;
import plataforma.model.Candidatura;
import plataforma.model.Reuniao;
import plataforma.util.Util;

// CONSUMIDOR ASSÍNCRONO: SMS (muitas trabalhadoras nas províncias não têm dados móveis).
// Demora (rede), por isso corre numa thread do pool e não atrasa o utilizador.
public class SmsConsumer implements EventListener<Object> {

    @Override
    public void onEvent(EventoDominio<Object> evento) {
        Util.dormir(300); // simula o envio pela rede
        if (evento.getDados() instanceof Candidatura c) {
            Util.log("[ASSÍNCRONO] [SMS -> " + c.getTrabalhadora().getNome() + "] A sua candidatura foi aceite.");
        } else if (evento.getDados() instanceof Reuniao r) {
            Util.log("[ASSÍNCRONO] [SMS -> " + r.getTrabalhadora().getNome() + "] Tem um convite de reunião de "
                    + r.getContratante().getNome() + ".");
        }
    }
}
