package pro.dpsn.warehouse.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "box_template_items")
@Getter
@Setter
public class BoxTemplateItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "box_id", nullable = false)
    private Box box;

    @Column(name = "unit_id", nullable = false)
    private Long unitId;

    @Column(name = "is_optional", nullable = false)
    private Boolean isOptional = false; // false = обязательное, true = можно добавить
}