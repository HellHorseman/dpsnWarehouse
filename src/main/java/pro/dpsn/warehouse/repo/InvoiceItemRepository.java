package pro.dpsn.warehouse.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.dpsn.warehouse.model.InvoiceItem;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {
}