package pro.dpsn.warehouse.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bulk_items")
@Getter
@Setter
public class BulkItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // Например: "Провод HDMI"

    @Column(name = "unit_of_measure", nullable = false)
    private String unitOfMeasure = "шт";
}