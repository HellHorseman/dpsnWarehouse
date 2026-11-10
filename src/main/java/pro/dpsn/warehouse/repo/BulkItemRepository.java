package pro.dpsn.warehouse.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.dpsn.warehouse.model.BulkItem;

public interface BulkItemRepository extends JpaRepository<BulkItem, Long> {
}