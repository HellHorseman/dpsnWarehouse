package pro.dpsn.warehouse.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.dpsn.warehouse.model.Box;

public interface BoxRepository extends JpaRepository<Box, Long> {
}