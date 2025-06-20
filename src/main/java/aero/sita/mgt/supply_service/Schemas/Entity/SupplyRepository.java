package aero.sita.mgt.supply_service.Schemas.Entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplyRepository extends JpaRepository<SupplyEntity, Long> {

    Optional<SupplyEntity> findBySupplyName(String supplyName);

}
