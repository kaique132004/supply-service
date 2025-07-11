package aero.sita.mgt.supply_service.Services;

import aero.sita.mgt.supply_service.Schemas.DTO.TransactionFilter;
import aero.sita.mgt.supply_service.Schemas.DTO.TransactionRequest;
import aero.sita.mgt.supply_service.Schemas.DTO.TransactionResponse;
import aero.sita.mgt.supply_service.Schemas.Entity.*;
import aero.sita.mgt.supply_service.Schemas.SupplyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final SupplyRepository supplyRepository;

    private final SupplyMapper supplyMapper;
    private final TransactionRepository transactionRepository;
    private final CSVExportService csvExportService;
    private final ExcelExportService excelExportService;

    public TransactionResponse saveTransaction(TransactionRequest request) {
        SupplyEntity supply = supplyRepository.findById(request.getSupplyId())
                .orElseThrow(() -> new RuntimeException("Supply not found"));

        RegionControlEntity regionControl = supply.getRegionalPrices().stream()
                .filter(r -> r.getRegionCode().equalsIgnoreCase(request.getRegionCode().toUpperCase()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Region not found"));

        int currencyQuantity = regionControl.getQuantity() != null ? regionControl.getQuantity() : 0;
        double priceUnit = regionControl.getPrice() != null ? regionControl.getPrice() : 0.0;

        int quantityAfter = request.getTypeEntry().equalsIgnoreCase("IN")
                ? currencyQuantity + request.getQuantityAmended()
                : currencyQuantity - request.getQuantityAmended();

        double totalPrice = request.getTypeEntry().equalsIgnoreCase("OUT")
                ? currencyQuantity * priceUnit : 0.0;

        LocalDateTime createdAt = request.getCreated() != null ? request.getCreated() : LocalDateTime.now();

        regionControl.setQuantity(quantityAfter);
        supply.setUpdatedAt(LocalDateTime.now());
        supplyRepository.save(supply);

        TransactionEntity entity = supplyMapper.toTransaction(request);
        entity.setUserName(SecurityContextHolder.getContext().getAuthentication().getName());
        entity.setQuantityBefore(currencyQuantity);
        entity.setQuantityAfter(quantityAfter);
        entity.setQuantity(request.getQuantityAmended());
        entity.setPriceUnit(priceUnit);
        entity.setTotalPrice(totalPrice);
        entity.setTypeEntry(TransactionEntity.TransactionType.valueOf(request.getTypeEntry().toUpperCase()));
        entity.setCreatedAt(createdAt);

        TransactionEntity saved = transactionRepository.save(entity);

        return supplyMapper.toTransactionResponse(saved); // <-- Correto!
    }

    public ResponseEntity<?> getUseFilters(TransactionFilter request) {
        String userFilter = request.getUser();
        String typeFilter = request.getTypeEntry();
        Integer daysFilter = request.getDateDays();
        List<String> regions = request.getRegionCodes();
        String nameFilter = request.getNameSupply();
        Integer qtnFilter = request.getQuantitySupply();
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        List<TransactionEntity> rawResults;
        boolean onlyDates = startDate != null
                && endDate != null
                && userFilter == null
                && typeFilter == null
                && daysFilter == null
                && qtnFilter == null
                && nameFilter == null
                && (regions == null || regions.isEmpty());

        if (onlyDates) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            rawResults = transactionRepository.findByCreatedAtBetween(start, end);
        } else {
            // 2) Senão, carrega tudo e faz filtros via Stream
            rawResults = transactionRepository.findAll();
            Stream<TransactionEntity> stream = rawResults.stream();

            if (userFilter != null && !userFilter.isBlank()) {
                stream = stream.filter(t -> userFilter.equalsIgnoreCase(t.getUserName()));
            }

            if (typeFilter != null) {
                stream = stream.filter(t ->
                        t.getTypeEntry() == TransactionEntity.TransactionType.valueOf(typeFilter.toUpperCase())
                );
            }
            if (daysFilter != null) {
                LocalDateTime cutoff = LocalDateTime.now().minusDays(daysFilter);
                stream = stream.filter(t -> t.getCreatedAt().isAfter(cutoff));
            }

            if (startDate != null && endDate != null) {
                LocalDateTime start = startDate.atStartOfDay();
                LocalDateTime end = endDate.atTime(LocalTime.MAX);
                stream = stream.filter(t ->
                        !t.getCreatedAt().isBefore(start) &&
                                !t.getCreatedAt().isAfter(end)
                );
            }

            if (regions != null && !regions.isEmpty()) {
                stream = stream.filter(t -> regions.contains(t.getRegionCode()));
            }

            if (nameFilter != null) {
                Optional<SupplyEntity> s = supplyRepository.findBySupplyName(nameFilter);
                if (s.isEmpty()) {
                    return ResponseEntity.badRequest().body("Supply não encontrado.");
                }
                Long supplyId = s.get().getId();
                stream = stream.filter(t -> t.getSupplyId().equals(supplyId));
            }

            if (qtnFilter != null) {
                stream = stream.filter(t -> t.getQuantity().equals(qtnFilter));
            }

            rawResults = stream.toList();
        }

        List<TransactionResponse> responseList = rawResults.stream()
                .map(tx -> {
                    TransactionResponse response = supplyMapper.toTransactionResponse(tx);

                    // Preenche o supplyName que não está direto na entidade
                    String supplyNameResolved = supplyRepository.findById(tx.getSupplyId())
                            .map(SupplyEntity::getSupplyName)
                            .orElse("Supply não encontrado");
                    response.setSupplyName(supplyNameResolved);

                    return response;
                })
                .toList();


        return ResponseEntity.ok(responseList);
    }

    public ResponseEntity<?> getAllConsumptions() {
        return ResponseEntity.ok(transactionRepository.findAll());
    }

    public byte[] getExportUseFilters(TransactionFilter request, String format) {
        ResponseEntity<?> response = getUseFilters(request);

        if (!response.getStatusCode().is2xxSuccessful() || !(response.getBody() instanceof List<?> list)) {
            throw new RuntimeException("Erro ao gerar exportação.");
        }

        List<TransactionResponse> historyList = (List<TransactionResponse>) list;

        return switch (format.toLowerCase()) {
            case "csv" -> csvExportService.exportHistoryToCsv(historyList);
            case "xlsx" -> excelExportService.exportHistoryToExcel(historyList);
            default -> throw new IllegalArgumentException("Formato inválido: " + format);
        };
    }

    @Transactional
    public TransactionResponse updateTransactionAndRecalculate(Long transactionId, TransactionRequest newRequest) {
        // 1. Busca a transação existente
        TransactionEntity original = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada."));

        // 2. Valida que o contexto (supply + região) não mudou
        if (!original.getSupplyId().equals(newRequest.getSupplyId()) ||
                !original.getRegionCode().equalsIgnoreCase(newRequest.getRegionCode())) {
            throw new IllegalArgumentException("SupplyId ou RegionCode não podem ser alterados.");
        }

        // 3. Atualiza os dados da transação original
        original.setQuantity(newRequest.getQuantityAmended());
        original.setCreatedAt(newRequest.getCreated() != null ? newRequest.getCreated() : original.getCreatedAt());
        original.setTypeEntry(TransactionEntity.TransactionType.valueOf(newRequest.getTypeEntry().toUpperCase()));

        // 4. Calcula os valores anteriores
        // Busca a última transação anterior à edição (para base de cálculo)
        Optional<TransactionEntity> anterior = transactionRepository
                .findTopBySupplyIdAndRegionCodeAndCreatedAtBeforeOrderByCreatedAtDesc(
                        original.getSupplyId(),
                        original.getRegionCode(),
                        original.getCreatedAt()
                );

        int quantityBefore = anterior.map(TransactionEntity::getQuantityAfter).orElse(0);
        int delta = original.getTypeEntry() == TransactionEntity.TransactionType.IN
                ? original.getQuantity()
                : -original.getQuantity();

        int quantityAfter = quantityBefore + delta;

        original.setQuantityBefore(quantityBefore);
        original.setQuantityAfter(quantityAfter);

        transactionRepository.save(original);

        // 5. Recalcula as transações posteriores
        List<TransactionEntity> posteriores = transactionRepository
                .findBySupplyIdAndRegionCodeAndCreatedAtAfterOrderByCreatedAtAsc(
                        original.getSupplyId(),
                        original.getRegionCode(),
                        original.getCreatedAt()
                );

        int cursor = quantityAfter;
        for (TransactionEntity tx : posteriores) {
            tx.setQuantityBefore(cursor);

            int diff = tx.getTypeEntry() == TransactionEntity.TransactionType.IN
                    ? tx.getQuantity()
                    : -tx.getQuantity();

            int novoAfter = cursor + diff;

            tx.setQuantityAfter(novoAfter);
            cursor = novoAfter;
        }

        transactionRepository.saveAll(posteriores);

        return supplyMapper.toTransactionResponse(original);
    }



    private void recalculateAfter(TransactionEntity fromTx) {
        List<TransactionEntity> affected = transactionRepository
                .findBySupplyIdAndRegionCodeAndCreatedAtAfterOrderByCreatedAtAsc(
                        fromTx.getSupplyId(),
                        fromTx.getRegionCode(),
                        fromTx.getCreatedAt()
                );

        // Começa a partir da transação alterada
        int runningQuantity = fromTx.getQuantityAfter();

        for (TransactionEntity tx : affected) {
            int before = runningQuantity;
            int delta = tx.getTypeEntry() == TransactionEntity.TransactionType.IN
                    ? tx.getQuantity()
                    : -tx.getQuantity();

            int after = before + delta;

            tx.setQuantityBefore(before);
            tx.setQuantityAfter(after);

            runningQuantity = after;
        }

        transactionRepository.saveAll(affected);
    }

}
