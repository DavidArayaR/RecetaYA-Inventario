package cl.recetaya.inventario.configuracion;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "recetaya.integracion.sqs", name = "habilitada", havingValue = "true")
public class ConfiguracionSqs {
    @Bean(destroyMethod = "close")
    SqsClient clienteSqs() {
        return SqsClient.create();
    }
}
