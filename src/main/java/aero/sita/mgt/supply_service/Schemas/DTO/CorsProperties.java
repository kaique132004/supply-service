package aero.sita.mgt.supply_service.Schemas.DTO;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "cors.server")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CorsProperties {
    private List<String> frontend;
    private List<Integer> frontendPorts;

}
