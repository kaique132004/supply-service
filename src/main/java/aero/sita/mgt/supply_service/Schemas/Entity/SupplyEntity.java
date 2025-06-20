package aero.sita.mgt.supply_service.Schemas.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Table(name = "supply_table")
public class SupplyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_supply", nullable = false)
    private String supplyName;

    @Column(name = "supply_description", nullable = true)
    private String description;

    @OneToMany(mappedBy = "supply", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RegionControlEntity> regionalPrices;

    private Boolean isActive = true;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ElementCollection
    @CollectionTable(name = "supply_images", joinColumns = @JoinColumn(name = "supply_id"))
    @Column(name = "image_url", nullable = true)
    private List<String> supplyImage;
}
