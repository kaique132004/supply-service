package aero.sita.mgt.supply_service.Schemas.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "supply_region_table")
public class RegionControlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 3, name = "region_code")
    private String regionCode;

    @Column(nullable = false, length = 3, name = "currency")
    private String currency;

    @Column(nullable = false, length = 30)
    private String supplier;

    private Double price;

    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supply_id", nullable = false)
    private SupplyEntity supply;
}
