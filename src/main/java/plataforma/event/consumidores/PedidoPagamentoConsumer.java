package plataforma.event.consumidores;

import plataforma.broker.Broker;
import plataforma.broker.ServicoPagamentos;
import plataforma.event.EventListener;
import plataforma.event.EventoDominio;
import plataforma.model.Contrato;
import plataforma.util.Util;

// CONSUMIDOR de CONTRATO_CRIADO: monta o PAYLOAD em JSON e DEPOSITA o pedido de pagamento
// na fila do broker. Quem cobra é o ServicoPagamentos, que vem buscar o pedido.
public class PedidoPagamentoConsumer implements EventListener<Contrato> {

    private final Broker broker;

    public PedidoPagamentoConsumer(Broker broker) {
        this.broker = broker;
    }

    @Override
    public void onEvent(EventoDominio<Contrato> evento) {
        Contrato c = evento.getDados();
        String json = "{\"contrato\": " + c.getId() + ", \"valor\": 250}";
        broker.publicar(ServicoPagamentos.TOPICO, new Broker.Mensagem("application/json", json));
        Util.log("[Broker] Pedido de pagamento do contrato " + c.getId() + " colocado na fila '" + ServicoPagamentos.TOPICO + "'");
    }
}
