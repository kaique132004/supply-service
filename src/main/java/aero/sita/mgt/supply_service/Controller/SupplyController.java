package aero.sita.mgt.supply_service.Controller;


import aero.sita.mgt.supply_service.Schemas.DTO.*;
import aero.sita.mgt.supply_service.Services.SupplyService;
import aero.sita.mgt.supply_service.Services.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/supply")
@RequiredArgsConstructor
public class SupplyController {

    private final SupplyService supplyService;

    private final TransactionService transactionService;

    @PostMapping("/consumptions")
    public ResponseEntity<TransactionResponse> createTransaction(@RequestBody TransactionRequest request) {
        TransactionResponse saved = transactionService.saveTransaction(request);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/consumptions/list")
    public ResponseEntity<List<SupplyResponse>> getAllSupply() {
        List<SupplyResponse> list = supplyService.getAllSupply();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/list/{id}")
    public ResponseEntity<SupplyResponse> getSupplyById(@PathVariable("id") Long id) {
        SupplyResponse response = supplyService.getSupplyById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/edit/{id}")
    public ResponseEntity<SupplyResponse> updateSupply(@PathVariable Long id, @RequestBody SupplyRequest request) {
        SupplyResponse updated = supplyService.updateSupply(id, request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/create")
    @PreAuthorize("!hasRole('USER') and !hasRole('GUEST')")
    public ResponseEntity<SupplyResponse> createSupply(@RequestBody SupplyRequest request) {
        SupplyResponse created = supplyService.saveSupply(request);
        return ResponseEntity.status(201).body(created);
    }

    @PostMapping("/finder")
    public ResponseEntity<?> getBySupplyFilter(@RequestBody TransactionFilter request) {
        return transactionService.getUseFilters(request);
    }

    @PostMapping("/consumptions/export")
    public ResponseEntity<byte[]> exportConsumptions(@RequestBody TransactionFilter request, @RequestParam(defaultValue = "csv") String format) {
        byte[] data = transactionService.getExportUseFilters(request, format);

        String filename = "filtro_consumo." + format;

        MediaType mediaType = switch (format.toLowerCase()) {
            case "csv" -> MediaType.parseMediaType("text/csv");
            case "xlsx" ->
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            default -> throw new IllegalArgumentException("Formato inválido.");
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(data);
    }

    @Transactional
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("!hasRole('USER') and !hasRole('GUEST')")
    public ResponseEntity<SupplyResponse> deleteSupply(@PathVariable Long id) {
        return supplyService.deleteSupply(id);
    }
}
