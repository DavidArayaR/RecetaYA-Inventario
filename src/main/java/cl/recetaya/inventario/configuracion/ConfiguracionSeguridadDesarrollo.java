package cl.recetaya.inventario.configuracion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("desarrollo")
public class ConfiguracionSeguridadDesarrollo {
    @Bean
    SecurityFilterChain cadenaSeguridadDesarrollo(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(autorizacion -> autorizacion
                        .requestMatchers(HttpMethod.GET, "/api/v1/inventario", "/api/v1/inventario/**").permitAll()
                        .requestMatchers("/api/v1/inventario/**").hasRole("FARMACEUTICO")
                        .requestMatchers("/consola-h2/**", "/actuator/health").permitAll()
                        .anyRequest().denyAll())
                .csrf(AbstractHttpConfigurer::disable)
                .headers(cabeceras -> cabeceras.frameOptions(marco -> marco.sameOrigin()))
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(recurso -> recurso.jwt(jwt -> jwt.jwtAuthenticationConverter(
                        ConfiguracionJwtInventario.convertidorAutenticacion())));
        return http.build();
    }
}
