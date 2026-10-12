package cl.recetaya.inventario.configuracion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

@Configuration
@Profile("!prueba")
public class ConfiguracionJwtInventario {
    @Bean
    JwtDecoder decodificadorJwtInventario(@Value("${recetaya.seguridad.jwt.secreto:}") String secreto,
                                         @Value("${recetaya.seguridad.jwt.emisor:recetaya-auth}") String emisor) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("RECETAYA_JWT_SECRETO debe tener al menos 32 caracteres UTF-8.");
        }
        SecretKey clave = new SecretKeySpec(bytes, "HmacSHA256");
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(clave)
                .macAlgorithm(MacAlgorithm.HS256).build();
        decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(emisor));
        return decodificador;
    }

    static Converter<Jwt, ? extends AbstractAuthenticationToken> convertidorAutenticacion() {
        return jwt -> {
            Object reclamacionRoles = jwt.getClaims().get("roles");
            Collection<SimpleGrantedAuthority> autoridades = reclamacionRoles instanceof Collection<?> roles
                    ? roles.stream().filter(String.class::isInstance).map(String.class::cast)
                    .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol)).toList()
                    : java.util.List.of();
            return new JwtAuthenticationToken(jwt, autoridades, jwt.getSubject());
        };
    }
}
