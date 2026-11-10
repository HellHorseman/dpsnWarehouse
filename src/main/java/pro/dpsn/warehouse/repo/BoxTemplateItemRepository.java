package pro.dpsn.warehouse.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.dpsn.warehouse.model.BoxTemplateItem;
import java.util.List;

public interface BoxTemplateItemRepository extends JpaRepository<BoxTemplateItem, Long> {
    List<BoxTemplateItem> findByBoxId(Long boxId);
}