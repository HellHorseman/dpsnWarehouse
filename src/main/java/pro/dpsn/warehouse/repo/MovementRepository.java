package pro.dpsn.warehouse.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.dpsn.warehouse.model.Movement;

public interface MovementRepository extends JpaRepository<Movement, Long> {
}