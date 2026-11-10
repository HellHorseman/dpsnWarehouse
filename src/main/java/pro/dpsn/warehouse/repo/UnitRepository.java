package pro.dpsn.warehouse.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.dpsn.warehouse.model.Unit;
import java.util.List;
import java.util.Optional;

public interface UnitRepository extends JpaRepository<Unit, Long> {
    Optional<Unit> findByQrCode(String qrCode);
    Optional<Unit> findBySerialNumber(String serialNumber);
    List<Unit> findByStatus(String status);
    long countByCurrentBoxId(Long boxId);
}