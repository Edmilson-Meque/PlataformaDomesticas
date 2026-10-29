package plataforma.broker;

import java.util.List;

import plataforma.dados.BaseDados;
import plataforma.event.EventBus;
import plataforma.event.TipoEvento;
import plataforma.model.Contrato;
import plataforma.util.Util;

// SERVIÇO DE PAGAMENTOS: corre na sua PRÓPRIA THREAD e VEM BUSCAR os pedidos de pagamento
// que ficaram na fila do broker. Se estiver em baixo, os pedidos esperam na fila (não se perdem).
public class ServicoPagamentos {

    public static final String TOPICO = "pagamentos";

    private final Broker broker;
    private final List<Contrato> contratos;
    private final EventBus eventBus;
    private final BaseDados bd;
    private volatile boolean activo; // volatile: a thread vê logo quando o main o desliga

    public ServicoPagamentos(Broker broker, List<Contrato> contratos, EventBus eventBus, BaseDados bd) {
        this.broker = broker;
        this.contratos = contratos;
        this.eventBus = eventBus;
        this.bd = bd;
    }

    public void ligar() {
        if (activo) {
            return;
        }
        activo = true;
        Thread t = new Thread(this::ciclo, "pagamentos");
        t.setDaemon(true);
        t.start();
        System.out.println("   [Pagamentos] Serviço LIGADO (" + broker.pendentes(TOPICO) + " pedido(s) na fila)");
    }

    public void desligar() {
        activo = false;
        System.out.println("   [Pagamentos] Serviço DESLIGADO. Os pedidos ficam à espera na fila do broker.");
    }

    public boolean isActivo() {
        return activo;
    }

    private void ciclo() {
        try {
            while (activo) {
                Broker.Mensagem m = broker.receber(TOPICO, 300);
                if (m == null) {
                    continue;
                }
                if (!"application/json".equals(m.contentType())) {
                    Util.log("[Pagamentos] Formato não suportado (" + m.contentType() + "). Só aceito application/json");
                    continue;
                }
                processar(m.corpo());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Payload esperado: {"contrato": 1, "valor": 250}
    private void processar(String json) {
        int id = Integer.parseInt(json.replaceAll(".*\"contrato\":\\s*(\\d+).*", "$1"));
        Util.log("[Pagamentos] Pedido recebido: " + json + " -> a cobrar via M-Pesa...");
        Util.dormir(500);
        for (Contrato c : contratos) {
            if (c.getId() == id) {
                c.setPago(true);
                bd.guardarContrato(c);
                Util.log("[Pagamentos] Contrato " + id + " PAGO.");
                eventBus.publicar(TipoEvento.PAGAMENTO_CONFIRMADO, c, c.getContratante().getNome());
            }
        }
    }
}
