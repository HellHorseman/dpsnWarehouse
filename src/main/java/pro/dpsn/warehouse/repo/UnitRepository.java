package pro.dpsn.warehouse.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pro.dpsn.warehouse.model.Unit;
import java.util.List;
import java.util.Optional;

public interface UnitRepository extends JpaRepository<Unit, Long> {
    Optional<Unit> findByQrCode(String qrCode);
    Optional<Unit> findBySerialNumber(String serialNumber);
    List<Unit> findByStatus(String status);
    long countByCurrentBoxId(Long boxId);

    @Query("SELECT u FROM Unit u WHERE " +
            "(:status IS NULL OR u.status = :status) AND " +
            "(:modelName IS NULL OR LOWER(u.model.name) LIKE LOWER(CONCAT('%', :modelName, '%')))")
    List<Unit> findWithFilters(@Param("status") String status, @Param("modelName") String modelName);
}
