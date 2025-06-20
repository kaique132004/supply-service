package aero.sita.mgt.supply_service.Schemas;

import aero.sita.mgt.supply_service.Schemas.DTO.*;
import aero.sita.mgt.supply_service.Schemas.Entity.RegionControlEntity;
import aero.sita.mgt.supply_service.Schemas.Entity.SupplyEntity;
import aero.sita.mgt.supply_service.Schemas.Entity.TransactionEntity;
import org.mapstruct.*;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface SupplyMapper {

    @Mapping(target = "id", ignore = true)
    RegionControlEntity createRegionControl(RegionControlRequest request);

    RegionControlRequest toRegionControl(RegionControlEntity entity);

    void updateRegionControl(RegionControlRequest request, @MappingTarget RegionControlEntity entity);

    void updateSupply(SupplyRequest request, @MappingTarget SupplyEntity entity);
    SupplyResponse toSupplyResponse(SupplyEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    SupplyEntity toSupplyEntity(SupplyRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateSupplyFromDto(SupplyRequest request, @MappingTarget SupplyEntity entity);
    @Mapping(target = "id", ignore = true)
    TransactionEntity toTransaction(TransactionRequest request);

    SupplyRequest toSupplyRequest(SupplyEntity entity);
    List<RegionControlEntity> toRegionEntities(List<RegionControlRequest> list);

    List<RegionControlResponse> toRegionResponses(List<RegionControlEntity> list);

    @AfterMapping
    default void linkSupply(@MappingTarget SupplyEntity entity) {
        if (entity.getRegionalPrices() != null) {
            entity.getRegionalPrices().forEach(r -> r.setSupply(entity));
        }
    }

    @Mapping(target = "supplyName", ignore = true) // Será preenchido manualmente após
    @Mapping(source = "createdAt", target = "created")
    @Mapping(source = "quantity", target = "quantityAmended")
    TransactionResponse toTransactionResponse(TransactionEntity entity);
}