package plataforma.event.consumidores;

import plataforma.event.EventListener;
import plataforma.event.EventoDominio;
import plataforma.logica.MotorRegras;
import plataforma.model.Denuncia;
import plataforma.model.Trabalhadora;
import plataforma.util.Util;

// CONSUMIDOR que liga EVENTOS ao PARADIGMA LÓGICO: a cada denúncia o motor reavalia o perfil
// em tempo real, sem esperar que o administrador tenha tempo de analisar.
public class MotorLogicoConsumer implements EventListener<Denuncia> {

    private final MotorRegras motor;

    public MotorLogicoConsumer(MotorRegras motor) {
        this.motor = motor;
    }

    @Override
    public void onEvent(EventoDominio<Denuncia> evento) {
        Trabalhadora t = evento.getDados().getAlvo();
        if (motor.deduz(MotorRegras.BLOQUEAR_CONTRATACAO, t)) {
            Util.log("[SÍNCRONO] [Motor Lógico] " + t.getNome() + " tem denúncias de " + t.contarDenunciantesDistintos()
                    + " contratantes diferentes -> perfil BLOQUEADO. Regras que provaram:");
            System.out.print(motor.explicar(MotorRegras.BLOQUEAR_CONTRATACAO, t));
        }
    }
}
