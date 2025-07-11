package aero.sita.mgt.supply_service.Controller;


import aero.sita.mgt.supply_service.Schemas.DTO.*;
import aero.sita.mgt.supply_service.Services.SupplyService;
import aero.sita.mgt.supply_service.Services.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v2/supply")
@RequiredArgsConstructor
@Tag(name = "Supply", description = "Management of supplies and consumptions")
public class SupplyController {

    private final SupplyService supplyService;

    private final TransactionService transactionService;

    @Operation(summary = "Get all supplies of database")
    @GetMapping("/list")
    public ResponseEntity<List<SupplyResponse>> list() {
        List<SupplyResponse> list = supplyService.getAllSupply();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Register a supply consumption transaction")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transação registrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro de validação ou parâmetros inválidos")
    })
    @PostMapping("/consumptions")
    public ResponseEntity<TransactionResponse> createTransaction(@RequestBody TransactionRequest request) {
        TransactionResponse saved = transactionService.saveTransaction(request);
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "List all available supplies")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of movements", content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
    })
    @GetMapping("/consumptions/list")
    public ResponseEntity<?> getAllConsumptions() {
        return transactionService.getAllConsumptions();
    }

    @Operation(summary = "Get supply details by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Supply found successfully"),
            @ApiResponse(responseCode = "404", description = "Supply not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/list/{id}")
    public ResponseEntity<SupplyResponse> getSupplyById(
            @Parameter(description = "ID od supply") @PathVariable("id") Long id) {
        SupplyResponse response = supplyService.getSupplyById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update an existing supply")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Supply updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid update data"),
            @ApiResponse(responseCode = "404", description = "Supply not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/edit/{id}")
    public ResponseEntity<SupplyResponse> updateSupply(@PathVariable Long id, @RequestBody SupplyRequest request) {
        SupplyResponse updated = supplyService.updateSupply(id, request);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Create a new supply")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Supply created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "403", description = "Forbidden – insufficient permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("!hasRole('USER') and !hasRole('GUEST')")
    @PostMapping("/create")
    public ResponseEntity<SupplyResponse> createSupply(@RequestBody SupplyRequest request) {
        SupplyResponse created = supplyService.saveSupply(request);
        return ResponseEntity.status(201).body(created);
    }

    @Operation(summary = "Filter consumption transactions based on given criteria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filtered results returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/finder")
    public ResponseEntity<?> getBySupplyFilter(@RequestBody TransactionFilter request) {
        return transactionService.getUseFilters(request);
    }

    @Operation(summary = "Export filtered consumption transactions as CSV or XLSX")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Export generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid filter parameters or format"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/consumptions/export")
    public ResponseEntity<byte[]> exportConsumptions(
            @RequestBody TransactionFilter request,
            @RequestParam(defaultValue = "csv") String format) {

        byte[] data = transactionService.getExportUseFilters(request, format);
        String filename = "filter-consumption." + format;

        MediaType mediaType = switch (format.toLowerCase()) {
            case "csv" -> MediaType.parseMediaType("text/csv");
            case "xlsx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            default -> throw new IllegalArgumentException("Invalid format.");
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(data);
    }


    @Operation(summary = "Delete a supply by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Supply deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Supply not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden – insufficient permissions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("!hasRole('USER') and !hasRole('GUEST')")
    @Transactional
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteSupply(@PathVariable Long id) {
        return supplyService.deleteSupply(id);
    }


    @PutMapping("/consumptions/{id}/edit")
    public ResponseEntity<TransactionResponse> editTransaction(
            @PathVariable Long id,
            @RequestBody TransactionRequest request
    ) {
        return ResponseEntity.ok(transactionService.updateTransactionAndRecalculate(id, request));
    }

    @DeleteMapping("/consumptions/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteAndRecalculate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/consumptions/{id}")
    public ResponseEntity<TransactionResponse> getConsumptionDetails(@PathVariable Long id) {
        return transactionService.getConsumptionDetails(id);
    }

}
