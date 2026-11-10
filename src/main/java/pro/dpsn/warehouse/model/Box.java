package pro.dpsn.warehouse.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.ZonedDateTime;

@Entity
@Table(name = "boxes")
@Getter
@Setter
public class Box {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "qr_code", nullable = false, unique = true)
    private String qrCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoxType type;

    @Column(name = "max_cells")
    private Integer maxCells; // Только для сумок (FIXED_BAG, FIXED_BAG_OPTIONAL)

    @Column(nullable = false)
    private String status = "on_stock"; // on_stock, in_transit, at_client

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now();
}