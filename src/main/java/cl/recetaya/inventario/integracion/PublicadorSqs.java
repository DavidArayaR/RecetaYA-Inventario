package cl.recetaya.inventario.integracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
@ConditionalOnProperty(prefix = "recetaya.integracion.sqs", name = "habilitada", havingValue = "true")
public class PublicadorSqs {
    private static final Logger REGISTRO = LoggerFactory.getLogger(PublicadorSqs.class);
    private final SqsClient cliente;
    private final ServicioBandejaSalida bandeja;
    private final String colaResultados;

    public PublicadorSqs(SqsClient cliente, ServicioBandejaSalida bandeja,
                        @Value("${recetaya.integracion.sqs.resultados}") String colaResultados) {
        if (colaResultados == null || colaResultados.isBlank()) {
            throw new IllegalArgumentException("Debe configurarse la cola SQS de resultados.");
        }
        this.cliente = cliente;
        this.bandeja = bandeja;
        this.colaResultados = colaResultados;
    }

    @Scheduled(fixedDelay = 1000)
    public void publicar() {
        for (MensajePendiente mensaje : bandeja.pendientes()) {
            try {
                SendMessageRequest.Builder solicitud = SendMessageRequest.builder()
                        .queueUrl(colaResultados).messageBody(mensaje.contenido());
                if (colaResultados.endsWith(".fifo")) {
                    solicitud.messageGroupId("inventario").messageDeduplicationId(mensaje.id().toString());
                }
                cliente.sendMessage(solicitud.build());
                bandeja.marcarPublicado(mensaje.id());
            } catch (RuntimeException excepcion) {
                bandeja.registrarFallo(mensaje.id());
                REGISTRO.warn("Falló la publicación SQS del resultado {}.", mensaje.id());
            }
        }
    }
}
