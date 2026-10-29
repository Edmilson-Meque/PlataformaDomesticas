package plataforma.concorrencia;

// synchronized + wait() + notifyAll()
// A thread de NOTIFICAÇÕES espera (wait) até a thread de PAGAMENTOS terminar e a avisar (notifyAll).
public class MonitorPagamento {

    private boolean pago = false;

    public synchronized void confirmarPagamento() {
        pago = true;
        notifyAll(); // acorda quem está à espera
    }

    public synchronized void esperarPagamento() throws InterruptedException {
        while (!pago) {  // "while" e não "if": volta a verificar depois de acordar
            wait();      // larga o monitor e dorme sem gastar CPU
        }
    }
}
