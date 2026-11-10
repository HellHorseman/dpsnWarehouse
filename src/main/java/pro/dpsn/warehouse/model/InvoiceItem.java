package pro.dpsn.warehouse.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
public class InvoiceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    // Ссылка на конкретную единицу с серийником (может быть null, если это мелкое)
    @Column(name = "unit_id")
    private Long unitId;

    // Ссылка на мелкое оборудование (может быть null)
    @Column(name = "bulk_item_id")
    private Long bulkItemId;

    // В каком ящике уехало (если есть)
    @Column(name = "box_id")
    private Long boxId;

    @Column(name = "qty_planned", nullable = false)
    private Integer qtyPlanned = 1;

    @Column(name = "qty_shipped", nullable = false)
    private Integer qtyShipped = 0;

    @Column(name = "qty_returned", nullable = false)
    private Integer qtyReturned = 0;
}