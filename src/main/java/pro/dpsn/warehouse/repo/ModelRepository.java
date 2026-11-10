package pro.dpsn.warehouse.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.dpsn.warehouse.model.Model;

public interface ModelRepository extends JpaRepository<Model, Long> {
}