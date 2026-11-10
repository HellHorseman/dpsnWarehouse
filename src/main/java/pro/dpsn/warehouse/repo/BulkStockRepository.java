package pro.dpsn.warehouse.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.dpsn.warehouse.model.BulkStock;

public interface BulkStockRepository extends JpaRepository<BulkStock, Long> {
}