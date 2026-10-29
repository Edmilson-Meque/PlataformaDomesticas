package plataforma.event.consumidores;

import plataforma.event.EventListener;
import plataforma.event.EventoDominio;
import plataforma.offline.Telemovel;
import plataforma.util.Util;

// CONSUMIDOR do evento de SYSTEM "REDE_RESTABELECIDA": sincroniza em background.
public class SincronizacaoConsumer implements EventListener<Telemovel> {

    @Override
    public void onEvent(EventoDominio<Telemovel> evento) {
        Telemovel t = evento.getDados();
        Util.log("[Offline-First] Internet de " + t.getDona().getNome() + " voltou -> a enviar "
                + t.pendentes() + " acção(ões) guardada(s)");
        t.sincronizar();
    }
}
