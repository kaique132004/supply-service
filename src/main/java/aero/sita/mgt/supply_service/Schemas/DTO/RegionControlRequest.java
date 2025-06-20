package aero.sita.mgt.supply_service.Schemas.DTO;

import lombok.Data;

@Data
public class RegionControlRequest {
    private String regionCode;
    private String currency;
    private String supplier;
    private Double price;
    private Integer quantity;
}
