package aero.sita.mgt.supply_service.Services;

import aero.sita.mgt.supply_service.Schemas.DTO.*;
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
import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final SupplyRepository supplyRepository;
    private final TransactionRepository transactionRepository;
    private final SupplyMapper supplyMapper;
    private final CSVExportService csvExportService;
    private final ExcelExportService excelExportService;

    public TransactionResponse saveTransaction(TransactionRequest request) {
        SupplyEntity supply = supplyRepository.findById(request.getSupplyId())
                .orElseThrow(() -> new RuntimeException("Supply not found"));

        RegionControlEntity regionControl = getRegion(supply, request.getRegionCode());

        int currencyQuantity = Optional.ofNullable(regionControl.getQuantity()).orElse(0);
        double priceUnit = Optional.ofNullable(regionControl.getPrice()).orElse(0.0);

        int delta = calcularDelta(request.getTypeEntry(), request.getQuantityAmended());
        int quantityAfter = currencyQuantity + delta;

        if (quantityAfter < 0) throw new IllegalStateException("Estoque não pode ficar negativo.");

        double totalPrice = request.getTypeEntry().equalsIgnoreCase("OUT") ? currencyQuantity * priceUnit : 0.0;

        LocalDateTime createdAt = Optional.ofNullable(request.getCreated()).orElse(LocalDateTime.now());

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
        return supplyMapper.toTransactionResponse(saved);
    }

    public ResponseEntity<?> getUseFilters(TransactionFilter request) {
        List<TransactionEntity> rawResults;
        boolean onlyDates = request.getStartDate() != null && request.getEndDate() != null &&
                request.getUser() == null && request.getTypeEntry() == null &&
                request.getDateDays() == null && request.getQuantitySupply() == null &&
                request.getNameSupply() == null &&
                (request.getRegionCodes() == null || request.getRegionCodes().isEmpty());

        if (onlyDates) {
            LocalDateTime start = request.getStartDate().atStartOfDay();
            LocalDateTime end = request.getEndDate().atTime(LocalTime.MAX);
            rawResults = transactionRepository.findByCreatedAtBetween(start, end);
        } else {
            rawResults = transactionRepository.findAll();
            Stream<TransactionEntity> stream = rawResults.stream();

            if (request.getUser() != null) stream = stream.filter(t -> request.getUser().equalsIgnoreCase(t.getUserName()));
            if (request.getTypeEntry() != null) stream = stream.filter(t ->
                    t.getTypeEntry() == TransactionEntity.TransactionType.valueOf(request.getTypeEntry().toUpperCase()));
            if (request.getDateDays() != null) {
                LocalDateTime cutoff = LocalDateTime.now().minusDays(request.getDateDays());
                stream = stream.filter(t -> t.getCreatedAt().isAfter(cutoff));
            }
            if (request.getStartDate() != null && request.getEndDate() != null) {
                LocalDateTime start = request.getStartDate().atStartOfDay();
                LocalDateTime end = request.getEndDate().atTime(LocalTime.MAX);
                stream = stream.filter(t -> !t.getCreatedAt().isBefore(start) && !t.getCreatedAt().isAfter(end));
            }
            if (request.getRegionCodes() != null && !request.getRegionCodes().isEmpty()) {
                stream = stream.filter(t -> request.getRegionCodes().contains(t.getRegionCode()));
            }
            if (request.getNameSupply() != null) {
                Optional<SupplyEntity> s = supplyRepository.findBySupplyName(request.getNameSupply());
                if (s.isEmpty()) return ResponseEntity.badRequest().body("Supply não encontrado.");
                stream = stream.filter(t -> t.getSupplyId().equals(s.get().getId()));
            }
            if (request.getQuantitySupply() != null) {
                stream = stream.filter(t -> t.getQuantity().equals(request.getQuantitySupply()));
            }

            rawResults = stream.toList();
        }

        List<TransactionResponse> responseList = rawResults.stream().map(tx -> {
            TransactionResponse response = supplyMapper.toTransactionResponse(tx);
            response.setSupplyName(supplyRepository.findById(tx.getSupplyId())
                    .map(SupplyEntity::getSupplyName).orElse("Supply não encontrado"));
            return response;
        }).toList();

        return ResponseEntity.ok(responseList);
    }

    public ResponseEntity<?> getAllConsumptions() {
        return ResponseEntity.ok(transactionRepository.findAll());
    }

    public byte[] getExportUseFilters(TransactionFilter request, String format) {
        ResponseEntity<?> response = getUseFilters(request);
        if (!response.getStatusCode().is2xxSuccessful() || !(response.getBody() instanceof List<?> list))
            throw new RuntimeException("Erro ao gerar exportação.");
        List<TransactionResponse> historyList = (List<TransactionResponse>) list;
        return switch (format.toLowerCase()) {
            case "csv" -> csvExportService.exportHistoryToCsv(historyList);
            case "xlsx" -> excelExportService.exportHistoryToExcel(historyList);
            default -> throw new IllegalArgumentException("Formato inválido: " + format);
        };
    }

    @Transactional
    public TransactionResponse updateTransactionAndRecalculate(Long id, TransactionRequest newRequest) {
        TransactionEntity original = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada."));
        if (!original.getSupplyId().equals(newRequest.getSupplyId()) ||
                !original.getRegionCode().equalsIgnoreCase(newRequest.getRegionCode())) {
            throw new IllegalArgumentException("SupplyId ou RegionCode não podem ser alterados.");
        }

        original.setQuantity(newRequest.getQuantityAmended());
        original.setCreatedAt(Optional.ofNullable(newRequest.getCreated()).orElse(original.getCreatedAt()));
        original.setTypeEntry(TransactionEntity.TransactionType.valueOf(newRequest.getTypeEntry().toUpperCase()));

        Optional<TransactionEntity> anterior = transactionRepository
                .findTopBySupplyIdAndRegionCodeAndCreatedAtBeforeOrderByCreatedAtDesc(
                        original.getSupplyId(), original.getRegionCode(), original.getCreatedAt());

        int quantityBefore = anterior.map(TransactionEntity::getQuantityAfter).orElse(0);
        int delta = calcularDelta(original.getTypeEntry(), original.getQuantity());
        int quantityAfter = quantityBefore + delta;

        if (quantityAfter < 0) throw new IllegalStateException("Estoque não pode ficar negativo.");

        original.setQuantityBefore(quantityBefore);
        original.setQuantityAfter(quantityAfter);
        transactionRepository.save(original);

        List<TransactionEntity> posteriores = transactionRepository
                .findBySupplyIdAndRegionCodeAndCreatedAtAfterOrderByCreatedAtAsc(
                        original.getSupplyId(), original.getRegionCode(), original.getCreatedAt());

        int cursor = quantityAfter;
        for (TransactionEntity tx : posteriores) {
            tx.setQuantityBefore(cursor);
            int novoAfter = cursor + calcularDelta(tx.getTypeEntry(), tx.getQuantity());
            tx.setQuantityAfter(novoAfter);
            cursor = novoAfter;
        }
        transactionRepository.saveAll(posteriores);

        atualizarEstoqueFinal(original.getSupplyId(), original.getRegionCode(), cursor);

        return supplyMapper.toTransactionResponse(original);
    }

    @Transactional
    public void deleteAndRecalculate(Long transactionId) {
        TransactionEntity toDelete = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada"));
        Long supplyId = toDelete.getSupplyId();
        String regionCode = toDelete.getRegionCode();
        LocalDateTime createdAt = toDelete.getCreatedAt();

        Optional<TransactionEntity> anterior = transactionRepository
                .findTopBySupplyIdAndRegionCodeAndCreatedAtBeforeOrderByCreatedAtDesc(supplyId, regionCode, createdAt);
        int baseQuantity = anterior.map(TransactionEntity::getQuantityAfter).orElse(0);

        List<TransactionEntity> posteriores = transactionRepository
                .findBySupplyIdAndRegionCodeAndCreatedAtAfterOrderByCreatedAtAsc(supplyId, regionCode, createdAt);

        transactionRepository.delete(toDelete);

        int cursor = baseQuantity;
        for (TransactionEntity tx : posteriores) {
            tx.setQuantityBefore(cursor);
            int after = cursor + calcularDelta(tx.getTypeEntry(), tx.getQuantity());
            tx.setQuantityAfter(after);
            cursor = after;
        }

        transactionRepository.saveAll(posteriores);
        atualizarEstoqueFinal(supplyId, regionCode, cursor);
    }

    public ResponseEntity<TransactionResponse> getConsumptionDetails(Long id) {
        Optional<TransactionEntity> optTx = transactionRepository.findById(id);

        if (optTx.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TransactionEntity tx = optTx.get();
        TransactionResponse response = supplyMapper.toTransactionResponse(tx);

        // Preenche o nome do supply manualmente
        String supplyName = supplyRepository.findById(tx.getSupplyId())
                .map(SupplyEntity::getSupplyName)
                .orElse("Supply não encontrado");

        response.setSupplyName(supplyName);
        response.setSupplyId(tx.getSupplyId());

        return ResponseEntity.ok(response);
    }


    private RegionControlEntity getRegion(SupplyEntity supply, String regionCode) {
        return supply.getRegionalPrices().stream()
                .filter(r -> r.getRegionCode().equalsIgnoreCase(regionCode))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Região não encontrada"));
    }

    private int calcularDelta(TransactionEntity.TransactionType type, int quantity) {
        return type == TransactionEntity.TransactionType.IN ? quantity : -quantity;
    }

    private int calcularDelta(String type, int quantity) {
        return "IN".equalsIgnoreCase(type) ? quantity : -quantity;
    }

    private void atualizarEstoqueFinal(Long supplyId, String regionCode, int quantityFinal) {
        SupplyEntity supply = supplyRepository.findById(supplyId)
                .orElseThrow(() -> new RuntimeException("Supply não encontrado"));
        RegionControlEntity region = getRegion(supply, regionCode);
        region.setQuantity(quantityFinal);
        supply.setUpdatedAt(LocalDateTime.now());
        supplyRepository.save(supply);
    }
}
