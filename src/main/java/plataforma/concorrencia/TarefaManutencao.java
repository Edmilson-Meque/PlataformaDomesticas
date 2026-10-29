package plataforma.concorrencia;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import plataforma.dados.BaseDados;
import plataforma.model.Trabalhadora;
import plataforma.service.AgendamentoService;
import plataforma.util.Util;

// TASKS de manutenção em tempo real: correm sozinhas todos os dias às 02:00, numa thread
// própria ("scheduler"), sem desligar o sistema. Os 4 elementos pedidos pelo docente:
//   1) SCHEDULER  -> ScheduledExecutorService
//   2) HORA       -> 02:00
//   3) QUERY      -> o UPDATE em SQL que normaliza os números de BI
//   4) EXECUÇÃO   -> o método executar()
// Task 1: normalizar os números de BI directamente na base de dados (tira espaços/traços, letra maiúscula)
// Task 2: expirar reuniões paradas há mais de 7 dias (a trabalhadora deixa de ficar presa em EM_REUNIAO)
public class TarefaManutencao {

    public static final String QUERY_BI =
            "UPDATE trabalhadora SET bi = UPPER(REPLACE(REPLACE(bi, ' ', ''), '-', '')) WHERE bi <> UPPER(REPLACE(REPLACE(bi, ' ', ''), '-', ''))";
    public static final int DIAS_PARA_EXPIRAR = 7;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "scheduler");
        t.setDaemon(true);
        return t;
    });
    private final BaseDados bd;
    private final List<Trabalhadora> trabalhadoras;
    private final AgendamentoService agendamentoService;

    public TarefaManutencao(BaseDados bd, List<Trabalhadora> trabalhadoras, AgendamentoService agendamentoService) {
        this.bd = bd;
        this.trabalhadoras = trabalhadoras;
        this.agendamentoService = agendamentoService;
    }

    public void agendar(LocalTime hora) {
        LocalDateTime proxima = LocalDateTime.now().toLocalDate().atTime(hora);
        if (proxima.isBefore(LocalDateTime.now())) {
            proxima = proxima.plusDays(1);
        }
        long atraso = Duration.between(LocalDateTime.now(), proxima).toMillis();
        scheduler.scheduleAtFixedRate(() -> executar(DIAS_PARA_EXPIRAR), atraso, TimeUnit.DAYS.toMillis(1),
                TimeUnit.MILLISECONDS);
        System.out.println("   [Scheduler] Tasks de manutenção agendadas para as " + hora + " (todos os dias)");
    }

    public void executar(int diasParaExpirar) {
        normalizarBis();
        int expiradas = agendamentoService.expirarReunioes(diasParaExpirar);
        Util.log("[Task 2] " + expiradas + " reunião(ões) parada(s) há mais de " + diasParaExpirar + " dia(s) expirada(s).");
    }

    private void normalizarBis() {
        Util.log("[Task 1] SQL> " + QUERY_BI);
        // A transacção bloqueia (LOCK) as linhas alteradas até ao commit
        try (Connection c = bd.ligar(); Statement s = c.createStatement()) {
            c.setAutoCommit(false);
            int linhas = s.executeUpdate(QUERY_BI);
            c.commit();
            Util.log("[Task 1] " + linhas + " número(s) de BI normalizado(s) na base de dados.");
            // a aplicação volta a ler os BIs da base de dados
            try (ResultSet rs = s.executeQuery("SELECT id, bi FROM trabalhadora")) {
                while (rs.next()) {
                    for (Trabalhadora t : trabalhadoras) {
                        if (t.getId() == rs.getInt("id")) {
                            t.setBi(rs.getString("bi"));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            Util.log("[Task 1] Erro: " + e.getMessage());
        }
    }
}
