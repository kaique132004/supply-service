package aero.sita.mgt.supply_service.Schemas.Entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegionRepository extends JpaRepository<RegionControlEntity, Long> {
    Double findPriceByRegionCode(String regionCode);

    Integer findQuantityByRegionCode(String regionCode);
}
