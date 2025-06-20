package aero.sita.mgt.supply_service.Schemas.DTO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TransactionResponse {
    private Long id;
    private String userName;
    private String supplyName;
    private Integer quantityAmended;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private LocalDateTime created;
    private String regionCode;
    private Double priceUnit;
    private Double totalPrice;
    private String typeEntry;
}
