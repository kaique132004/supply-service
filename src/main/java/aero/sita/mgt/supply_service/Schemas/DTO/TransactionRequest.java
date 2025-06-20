package aero.sita.mgt.supply_service.Schemas.DTO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TransactionRequest {

    private Long supplyId;
    private Integer quantityAmended;
    private LocalDateTime created;
    private String regionCode;
    private Double priceUnit;
    private Double totalPrice;
    private String typeEntry;
}
