package plataforma.event.consumidores;

import java.net.InetAddress;

import plataforma.dados.BaseDados;
import plataforma.event.EventListener;
import plataforma.event.EventoDominio;

// CONSUMIDOR DE AUDITORIA (padrão e-SISTAFE): grava na tabela "auditoria" o IP da máquina,
// a hora, o utilizador e o que fez. Foi acrescentado SÓ com linhas no main(),
// sem alterar nenhuma regra de negócio.
public class AuditoriaConsumer implements EventListener<Object> {

    private final BaseDados bd;

    public AuditoriaConsumer(BaseDados bd) {
        this.bd = bd;
    }

    @Override
    public void onEvent(EventoDominio<Object> evento) {
        bd.registarAuditoria(ip(), evento.getUtilizador(), evento.getTipo().name(), evento.getTipo().getCategoria().name());
    }

    private static String ip() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
