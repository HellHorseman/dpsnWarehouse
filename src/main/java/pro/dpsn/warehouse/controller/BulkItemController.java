package pro.dpsn.warehouse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.dpsn.warehouse.dto.BulkItemResponse;
import pro.dpsn.warehouse.repo.BulkItemRepository;
import pro.dpsn.warehouse.repo.BulkStockRepository;

import java.util.List;

@RestController
@RequestMapping("/api/bulk-items")
@RequiredArgsConstructor
public class BulkItemController {

    private final BulkItemRepository bulkItemRepository;
    private final BulkStockRepository bulkStockRepository;

    @GetMapping
    public List<BulkItemResponse> getAllBulkItems() {
        return bulkItemRepository.findAll().stream().map(item -> {
            BulkItemResponse dto = new BulkItemResponse();
            dto.setId(item.getId());
            dto.setName(item.getName());
            // Ищем остаток
            bulkStockRepository.findById(item.getId()).ifPresentOrElse(
                    stock -> dto.setQtyOnHand(stock.getQtyOnHand()),
                    () -> dto.setQtyOnHand(0)
            );
            return dto;
        }).toList();
    }
}