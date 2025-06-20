package aero.sita.mgt.supply_service.Schemas.DTO;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SupplyResponse {
    private Long id;
    private String supplyName;
    private String description;
    private List<RegionControlResponse> regionalPrices;
    private Boolean isActive = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> supplyImage;
}
