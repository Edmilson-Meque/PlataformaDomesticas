package plataforma.broker;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

// BROKER de mensagens (a ideia do JMS / RabbitMQ / Kafka), em versão simples:
// uma FILA (queue) por tópico.
//  - o PRODUTOR só DEPOSITA a mensagem e continua o seu trabalho;
//  - o CONSUMIDOR EXTERNO VEM BUSCAR as mensagens quando puder (modelo do Banco de Moçambique:
//    ninguém abre VPN nem portas; se o consumidor estiver em baixo, as mensagens esperam na fila).
public class Broker {

    // PAYLOAD + MEDIA TYPE: o formato tem de ser o mesmo no produtor e no consumidor
    public record Mensagem(String contentType, String corpo) {
    }

    private final Map<String, BlockingQueue<Mensagem>> topicos = new ConcurrentHashMap<>();

    public void publicar(String topico, Mensagem mensagem) {
        fila(topico).add(mensagem);
    }

    // Espera no máximo "ms" milissegundos por uma mensagem (null se não houver)
    public Mensagem receber(String topico, long ms) throws InterruptedException {
        return fila(topico).poll(ms, TimeUnit.MILLISECONDS);
    }

    public int pendentes(String topico) {
        return fila(topico).size();
    }

    private BlockingQueue<Mensagem> fila(String topico) {
        return topicos.computeIfAbsent(topico, k -> new LinkedBlockingQueue<>());
    }
}
