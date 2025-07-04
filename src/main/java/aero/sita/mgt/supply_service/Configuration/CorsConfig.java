package aero.sita.mgt.supply_service.Configuration;

import aero.sita.mgt.supply_service.Schemas.DTO.CorsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final CorsProperties corsProperties;


    @Bean
    public CorsFilter corsFilter() {
        List<String> allowedOrigins = new ArrayList<>();
        for (String host : corsProperties.getFrontend()) {
            for (Integer port : corsProperties.getFrontendPorts()) {
                allowedOrigins.add("http://" + host + ":" + port);
                allowedOrigins.add("https://" + host + ":" + port);
            }
        }

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
