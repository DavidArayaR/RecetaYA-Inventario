package cl.recetaya.inventario.integracion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Component
@ConditionalOnProperty(prefix = "recetaya.integracion.sqs", name = "habilitada", havingValue = "true")
public class ConsumidorSqs {
    private static final Logger REGISTRO = LoggerFactory.getLogger(ConsumidorSqs.class);
    private final SqsClient cliente;
    private final ProcesadorEventosInventario procesador;
    private final String colaSolicitudes;

    public ConsumidorSqs(SqsClient cliente, ProcesadorEventosInventario procesador,
                        @Value("${recetaya.integracion.sqs.solicitudes}") String colaSolicitudes) {
        if (colaSolicitudes == null || colaSolicitudes.isBlank()) {
            throw new IllegalArgumentException("Debe configurarse la cola SQS de solicitudes.");
        }
        this.cliente = cliente;
        this.procesador = procesador;
        this.colaSolicitudes = colaSolicitudes;
    }

    @Scheduled(fixedDelay = 1000)
    public void recibir() {
        try {
            var mensajes = cliente.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(colaSolicitudes).maxNumberOfMessages(10).waitTimeSeconds(10).build()).messages();
            for (Message mensaje : mensajes) {
                try {
                    procesador.procesar(mensaje.body());
                    cliente.deleteMessage(DeleteMessageRequest.builder()
                            .queueUrl(colaSolicitudes).receiptHandle(mensaje.receiptHandle()).build());
                } catch (RuntimeException excepcion) {
                    REGISTRO.warn("No se confirmó una solicitud SQS; quedará disponible para reintento: {}",
                            mensaje.messageId());
                }
            }
        } catch (RuntimeException excepcion) {
            REGISTRO.warn("No se pudo consultar la cola de solicitudes SQS.");
        }
    }
}
