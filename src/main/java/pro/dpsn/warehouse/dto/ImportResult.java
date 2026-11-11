package pro.dpsn.warehouse.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ImportResult {
    private Long invoiceId;
    private int successCount;
    private List<String> errors = new ArrayList<>();
}