package pro.dpsn.warehouse.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.dpsn.warehouse.model.Invoice;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findAllByOrderByIdDesc();
}