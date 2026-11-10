package pro.dpsn.warehouse.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
@Getter
@Setter
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customer; // Заказчик

    @Column(nullable = false)
    private String responsible; // Ответственный

    @Column(name = "shipped_by")
    private String shippedBy; // Кто отгрузил

    @Column(nullable = false)
    private String status; // draft, shipped, partially_returned, returned

    @Column(name = "locked_by")
    private Long lockedBy; // ID пользователя, который редактирует

    @Column(name = "locked_at")
    private ZonedDateTime lockedAt;

    private Integer version = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    // Связь со строками накладной (OneToMany)
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();

    // Удобный метод для добавления строки
    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this);
    }
}