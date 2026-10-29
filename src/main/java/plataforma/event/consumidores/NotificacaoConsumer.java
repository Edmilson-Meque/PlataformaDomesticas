package plataforma.event.consumidores;

import plataforma.event.EventListener;
import plataforma.event.EventoDominio;
import plataforma.model.Candidatura;
import plataforma.model.Contrato;
import plataforma.model.Reuniao;
import plataforma.model.Trabalhadora;
import plataforma.service.NotificacaoService;

// CONSUMIDOR: envia as notificações dentro da aplicação.
public class NotificacaoConsumer implements EventListener<Object> {

    private final NotificacaoService notificacaoService;

    public NotificacaoConsumer(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @Override
    public void onEvent(EventoDominio<Object> evento) {
        switch (evento.getTipo()) {
            case CANDIDATURA_SUBMETIDA -> {
                Candidatura c = (Candidatura) evento.getDados();
                notificacaoService.enviar(c.getVaga().getContratante(), "Nova candidatura de "
                        + c.getTrabalhadora().getNome() + " para a vaga '" + c.getVaga().getTitulo() + "'.");
            }
            case CANDIDATURA_ACEITE -> {
                Candidatura c = (Candidatura) evento.getDados();
                notificacaoService.enviar(c.getTrabalhadora(), "Parabéns! A tua candidatura à vaga '"
                        + c.getVaga().getTitulo() + "' foi ACEITE por " + c.getVaga().getContratante().getNome() + ".");
            }
            case CANDIDATURA_RECUSADA -> {
                Candidatura c = (Candidatura) evento.getDados();
                notificacaoService.enviar(c.getTrabalhadora(),
                        "A tua candidatura à vaga '" + c.getVaga().getTitulo() + "' não foi selecionada.");
            }
            case CANDIDATURA_CANCELADA -> {
                Candidatura c = (Candidatura) evento.getDados();
                notificacaoService.enviar(c.getTrabalhadora(), "A tua candidatura à vaga '" + c.getVaga().getTitulo()
                        + "' foi cancelada porque a reunião não avançou. Podes candidatar-te a outras vagas.");
            }
            case REUNIAO_AGENDADA -> {
                Reuniao r = (Reuniao) evento.getDados();
                notificacaoService.enviar(r.getTrabalhadora(), "Convite de reunião de " + r.getContratante().getNome()
                        + (r.getCandidatura() != null ? " (vaga '" + r.getCandidatura().getVaga().getTitulo() + "')" : "")
                        + ". Responda no menu: 3 - Ver convites pendentes.");
                notificacaoService.enviar(r.getContratante(), "Convite de reunião enviado a " + r.getTrabalhadora().getNome()
                        + ". Aguarde a resposta dela.");
            }
            case REUNIAO_ACEITE -> {
                Reuniao r = (Reuniao) evento.getDados();
                notificacaoService.enviar(r.getContratante(), r.getTrabalhadora().getNome()
                        + " aceitou o convite de reunião. Já pode confirmar a contratação.");
            }
            case REUNIAO_RECUSADA -> {
                Reuniao r = (Reuniao) evento.getDados();
                notificacaoService.enviar(r.getContratante(), r.getTrabalhadora().getNome() + " recusou o convite de reunião.");
            }
            case REUNIAO_CANCELADA -> {
                Reuniao r = (Reuniao) evento.getDados();
                notificacaoService.enviar(r.getTrabalhadora(), "A reunião com " + r.getContratante().getNome() + " foi cancelada.");
            }
            case REUNIAO_EXPIRADA -> {
                Reuniao r = (Reuniao) evento.getDados();
                notificacaoService.enviar(r.getTrabalhadora(), "A reunião com " + r.getContratante().getNome()
                        + " expirou (ficou demasiado tempo sem avançar).");
                notificacaoService.enviar(r.getContratante(), "A reunião com " + r.getTrabalhadora().getNome()
                        + " expirou (ficou demasiado tempo sem avançar).");
            }
            case CONTRATO_CRIADO -> {
                Contrato c = (Contrato) evento.getDados();
                notificacaoService.enviar(c.getTrabalhadora(), "Foste contratada oficialmente por " + c.getContratante().getNome() + ".");
                notificacaoService.enviar(c.getContratante(), "Contrato " + c.getId() + " com " + c.getTrabalhadora().getNome()
                        + " criado. O pedido de pagamento foi enviado.");
            }
            case PAGAMENTO_CONFIRMADO -> {
                Contrato c = (Contrato) evento.getDados();
                notificacaoService.enviar(c.getContratante(), "Pagamento do contrato " + c.getId() + " confirmado.");
            }
            case PERFIL_VALIDADO -> notificacaoService.enviar((Trabalhadora) evento.getDados(),
                    "O teu perfil foi validado e já aparece nas pesquisas.");
            default -> {
            }
        }
    }
}
