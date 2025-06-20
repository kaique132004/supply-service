package aero.sita.mgt.supply_service.Schemas.DTO;

import lombok.Data;

@Data
public class RegionControlResponse {
    private Long id;
    private String regionCode;
    private String currency;
    private String supplier;
    private Double price;
    private Integer quantity;
}
