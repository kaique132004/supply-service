package aero.sita.mgt.supply_service.Schemas.DTO;

import lombok.Data;

import java.util.List;

@Data
public class SupplyRequest {
    private String supplyName;
    private String description;

    private List<RegionControlRequest> regionalPrices;

    private Boolean isActive;
    private List<String> supplyImages;
}
