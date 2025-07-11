package aero.sita.mgt.supply_service.Schemas.Entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    List<TransactionEntity> findBySupplyId(Long supplyId);

    List<TransactionEntity> findByRegionCode(String regionCode);

    List<TransactionEntity> findByRegionCodeIn(List<String> regionCodes);


    List<TransactionEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    Optional<TransactionEntity> findTopBySupplyIdAndRegionCodeAndCreatedAtBeforeOrderByCreatedAtDesc(
            Long supplyId, String regionCode, LocalDateTime createdAt);

    List<TransactionEntity> findBySupplyIdAndRegionCodeAndCreatedAtAfterOrderByCreatedAtAsc(
            Long supplyId, String regionCode, LocalDateTime createdAt);

}
