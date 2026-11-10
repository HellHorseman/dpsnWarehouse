package pro.dpsn.warehouse.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.dpsn.warehouse.model.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
}