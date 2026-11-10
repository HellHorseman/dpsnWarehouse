package pro.dpsn.warehouse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.dpsn.warehouse.model.Box;
import pro.dpsn.warehouse.repo.BoxRepository;
import pro.dpsn.warehouse.service.BoxService;

@Service
@RequiredArgsConstructor
public class BoxServiceImpl implements BoxService {

    private final BoxRepository boxRepository;

    @Override
    @Transactional
    public void receiveBox(Long boxId) {
        Box box = boxRepository.findById(boxId)
                .orElseThrow(() -> new RuntimeException("Ящик не найден"));

        if (!"in_transit".equals(box.getStatus())) {
            throw new RuntimeException("Ящик не в пути (статус: " + box.getStatus() + ")");
        }

        box.setStatus("on_stock");
        boxRepository.save(box);
    }
}