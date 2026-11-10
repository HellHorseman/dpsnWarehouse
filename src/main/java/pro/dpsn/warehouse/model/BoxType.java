package pro.dpsn.warehouse.model;

public enum BoxType {
    BULK_CONTAINER,    // Ящик под мелочь (много ячеек, произвольный состав)
    STATIC_KIT,        // Статический комплект (обязательное + опциональное)
    FIXED_BAG,         // Сумка с фиксированным числом ячеек (без добора)
    FIXED_BAG_OPTIONAL // Сумка с фиксированным числом ячеек (с добором)
}