package plataforma.util;


import java.util.List;
import java.util.function.ToIntFunction;

public final class Util {

    // Próximo id livre = maior id existente + 1 (funciona também com dados carregados da BD)
    public static <T> int proximoId(List<T> lista, ToIntFunction<T> id) {
        return lista.stream().mapToInt(id).max().orElse(0) + 1;
    }


    private Util() {
    }

    public static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Mostra o nome da thread que está a executar (para VER a concorrência na consola)
    public static void log(String texto) {
        System.out.println("   [" + Thread.currentThread().getName() + "] " + texto);
    }
}
