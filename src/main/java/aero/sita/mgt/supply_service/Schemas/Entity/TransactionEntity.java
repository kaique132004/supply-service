package aero.sita.mgt.supply_service.Schemas.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "supply_usage")
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supply_id", nullable = false)
    private Long supplyId;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "quantity_amended")
    private Integer quantity;

    @Column(name = "quantity_before")
    private Integer quantityBefore;

    @Column(name = "quantity_after")
    private Integer quantityAfter;

    @Column(name = "change_date")
    private LocalDateTime createdAt;

    @Column(name = "region_code")
    private String regionCode;

    @Column(name = "price_unit")
    private Double priceUnit;

    @Column(name = "total_price")
    private Double totalPrice;

    @Column(name = "change_type")
    private TransactionType typeEntry;


    public enum TransactionType {
        IN, OUT
    }
}
