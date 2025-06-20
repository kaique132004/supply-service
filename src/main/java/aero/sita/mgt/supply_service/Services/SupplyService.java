package aero.sita.mgt.supply_service.Services;

import aero.sita.mgt.supply_service.Schemas.DTO.SupplyRequest;
import aero.sita.mgt.supply_service.Schemas.DTO.SupplyResponse;
import aero.sita.mgt.supply_service.Schemas.Entity.SupplyEntity;
import aero.sita.mgt.supply_service.Schemas.Entity.SupplyRepository;
import aero.sita.mgt.supply_service.Schemas.SupplyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplyService {

    private final SupplyRepository supplyRepository;

    private final SupplyMapper supplyMapper;

    public SupplyResponse saveSupply(SupplyRequest request) {
        SupplyEntity entity = supplyMapper.toSupplyEntity(request);
        SupplyEntity saved = supplyRepository.save(entity);
        return supplyMapper.toSupplyResponse(saved);
    }

    public SupplyResponse getSupplyById(Long id) {
        SupplyEntity supply = supplyRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Supply not found"));
        return supplyMapper.toSupplyResponse(supply);
    }

    public List<SupplyResponse> getAllSupply() {
        List<SupplyEntity> supplies = supplyRepository.findAll();
        return supplies.stream()
                .map(supplyMapper::toSupplyResponse)
                .collect(Collectors.toList());
    }

    public SupplyResponse updateSupply(Long id, SupplyRequest request) {
        SupplyEntity existing = supplyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supply not found"));

        // Atualiza apenas os campos não nulos
        supplyMapper.updateSupplyFromDto(request, existing);

        if (existing.getRegionalPrices() != null) {
            existing.getRegionalPrices().forEach(price -> price.setSupply(existing));
        }

        existing.setUpdatedAt(LocalDateTime.now());

        supplyRepository.save(existing);
        return supplyMapper.toSupplyResponse(existing);
    }

    public ResponseEntity<SupplyResponse> deleteSupply(Long id) {
        supplyRepository.deleteById(id);
        return ResponseEntity.status(202).body(null);
    }
}
