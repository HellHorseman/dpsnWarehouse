package pro.dpsn.warehouse.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.dpsn.warehouse.model.Unit;
import java.util.List;
import java.util.Optional;

public interface UnitRepository extends JpaRepository<Unit, Long> {
    // Поиск по QR-коду (для сканера)
    Optional<Unit> findByQrCode(String qrCode);

    // Поиск по серийнику
    Optional<Unit> findBySerialNumber(String serialNumber);

    // Список по статусу (например, все, что на складе)
    List<Unit> findByStatus(String status);
}