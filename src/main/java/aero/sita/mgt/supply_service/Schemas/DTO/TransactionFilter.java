package aero.sita.mgt.supply_service.Schemas.DTO;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class TransactionFilter {
    private Integer dateDays;
    private String nameSupply;
    private String typeEntry;
    private List<String> regionCodes;
    private Integer quantitySupply;
    private String user;
    private LocalDate startDate;
    private LocalDate endDate;
}