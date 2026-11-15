package pro.dpsn.warehouse.service;

import pro.dpsn.warehouse.dto.BoxResponse;

import java.util.List;

public interface BoxService {
    void receiveBox(Long boxId);
    List<BoxResponse> getAllBoxes();
    BoxResponse getBoxById(Long id);
}
