package pro.dpsn.warehouse.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bulk_stock")
@Getter
@Setter
public class BulkStock {
    @Id
    @Column(name = "bulk_item_id")
    private Long bulkItemId;

    @Column(name = "qty_on_hand", nullable = false)
    private Integer qtyOnHand = 0;
}