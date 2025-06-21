package aero.sita.mgt.supply_service.Services;

import aero.sita.mgt.supply_service.Components.ResourceNotFoundException;
import aero.sita.mgt.supply_service.Schemas.DTO.RegionControlRequest;
import aero.sita.mgt.supply_service.Schemas.DTO.SupplyRequest;
import aero.sita.mgt.supply_service.Schemas.DTO.SupplyResponse;
import aero.sita.mgt.supply_service.Schemas.Entity.RegionControlEntity;
import aero.sita.mgt.supply_service.Schemas.Entity.SupplyEntity;
import aero.sita.mgt.supply_service.Schemas.Entity.SupplyRepository;
import aero.sita.mgt.supply_service.Schemas.SupplyMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @Transactional
    public SupplyResponse updateSupply(Long id, SupplyRequest request) {
        SupplyEntity existing = supplyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supply not found with id " + id));

        existing.setSupplyName(request.getSupplyName());
        existing.setDescription(request.getDescription());
        existing.setIsActive(request.getIsActive());
        existing.setSupplyImage(request.getSupplyImages());
        existing.setUpdatedAt(LocalDateTime.now());

        if (request.getRegionalPrices() != null) {
            List<RegionControlEntity> updatedRegions = new ArrayList<>();
            for (RegionControlRequest dto : request.getRegionalPrices()) {
                if (dto.getId() != null) {
                    RegionControlEntity region = existing.getRegionalPrices().stream()
                            .filter(r -> r.getId().equals(dto.getId()))
                            .findFirst()
                            .orElseThrow(() -> new ResourceNotFoundException("Region not found with id " + dto.getId()));

                    region.setRegionCode(dto.getRegionCode());
                    region.setCurrency(dto.getCurrency());
                    region.setSupplier(dto.getSupplier());
                    region.setPrice(dto.getPrice());
                    region.setQuantity(dto.getQuantity());
                    updatedRegions.add(region);
                } else {
                    RegionControlEntity newRegion = new RegionControlEntity();
                    newRegion.setRegionCode(dto.getRegionCode());
                    newRegion.setCurrency(dto.getCurrency());
                    newRegion.setSupplier(dto.getSupplier());
                    newRegion.setPrice(dto.getPrice());
                    newRegion.setQuantity(dto.getQuantity());
                    newRegion.setSupply(existing);
                    updatedRegions.add(newRegion);
                }
            }
            existing.getRegionalPrices().clear();
            existing.getRegionalPrices().addAll(updatedRegions);
        }

        SupplyEntity saved = supplyRepository.save(existing);
        return supplyMapper.toSupplyResponse(saved);
    }


    @Transactional
    public ResponseEntity<Void> deleteSupply(Long id) {
        if (!supplyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Supply not found with id " + id);
        }
        supplyRepository.deleteById(id);
        return ResponseEntity.accepted().build();
    }
}
