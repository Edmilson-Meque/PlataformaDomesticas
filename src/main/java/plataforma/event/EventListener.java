package plataforma.event;

// CONSUMIDOR (ouvinte / handler): só trabalha quando um evento chega.
// Toda a regra de negócio da REACÇÃO fica dentro do método onEvent.
public interface EventListener<T> extends java.util.EventListener {
    void onEvent(EventoDominio<T> evento);
}
