package cl.recetaya.inventario.configuracion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile({"desarrollo", "prueba"})
public class ConfiguracionSeguridadLocal {
    @Bean
    SecurityFilterChain cadenaSeguridadLocal(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(autorizacion -> autorizacion
                        .requestMatchers("/api/v1/inventario", "/api/v1/inventario/**").permitAll()
                        .requestMatchers("/consola-h2/**").permitAll()
                        .anyRequest().denyAll())
                .csrf(proteccion -> proteccion.ignoringRequestMatchers("/api/v1/inventario/**", "/consola-h2/**"))
                .headers(cabeceras -> cabeceras.frameOptions(marco -> marco.sameOrigin()));
        return http.build();
    }
}
