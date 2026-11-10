-- ============================================
-- V1: Инициализация схемы складского учёта
-- ============================================

-- Пользователи
CREATE TABLE users (
                       id              BIGSERIAL PRIMARY KEY,
                       phone           VARCHAR(20) UNIQUE NOT NULL,
                       full_name       VARCHAR(200) NOT NULL,
                       pin_hash        VARCHAR(100) NOT NULL,
                       role            VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN','MANAGER','STOREKEEPER')),
                       active          BOOLEAN NOT NULL DEFAULT true,
                       created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_users_phone ON users(phone);

-- Модели (номенклатура)
CREATE TABLE models (
                        id              BIGSERIAL PRIMARY KEY,
                        name            VARCHAR(200) NOT NULL,
                        category        VARCHAR(100),
                        created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_models_name ON models(name);

-- Единицы оборудования с QR
CREATE TABLE units (
                       id              BIGSERIAL PRIMARY KEY,
                       model_id        BIGINT NOT NULL REFERENCES models(id),
                       serial_number   VARCHAR(100) UNIQUE NOT NULL,
                       qr_code         VARCHAR(100) UNIQUE NOT NULL,
                       status          VARCHAR(20) NOT NULL DEFAULT 'warehouse'
                           CHECK (status IN ('warehouse','rented','service','client','written_off')),
                       current_box_id  BIGINT NULL,  -- FK добавим после создания boxes
                       created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                       updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_units_model ON units(model_id);
CREATE INDEX idx_units_status ON units(status);
CREATE INDEX idx_units_qr ON units(qr_code);
CREATE INDEX idx_units_box ON units(current_box_id);

-- Мелкое оборудование без QR
CREATE TABLE bulk_items (
                            id              BIGSERIAL PRIMARY KEY,
                            name            VARCHAR(200) NOT NULL,
                            unit_of_measure VARCHAR(20) NOT NULL DEFAULT 'шт',
                            created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Остатки мелкого на складе
CREATE TABLE bulk_stock (
                            bulk_item_id    BIGINT PRIMARY KEY REFERENCES bulk_items(id) ON DELETE CASCADE,
                            qty_on_hand     INT NOT NULL DEFAULT 0 CHECK (qty_on_hand >= 0)
);

-- Ящики
CREATE TABLE boxes (
                       id              BIGSERIAL PRIMARY KEY,
                       code            VARCHAR(50) UNIQUE NOT NULL,
                       qr_code         VARCHAR(100) UNIQUE NOT NULL,
                       is_template     BOOLEAN NOT NULL DEFAULT false,  -- true = статический комплект
                       status          VARCHAR(20) NOT NULL DEFAULT 'on_stock'
                           CHECK (status IN ('on_stock','in_transit','at_client')),
                       created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_boxes_code ON boxes(code);
CREATE INDEX idx_boxes_qr ON boxes(qr_code);

-- Теперь FK для units.current_box_id
ALTER TABLE units
    ADD CONSTRAINT fk_units_box
        FOREIGN KEY (current_box_id) REFERENCES boxes(id) ON DELETE SET NULL;

-- Шаблон статического ящика (только QR-оборудование)
CREATE TABLE box_template_items (
                                    id              BIGSERIAL PRIMARY KEY,
                                    box_id          BIGINT NOT NULL REFERENCES boxes(id) ON DELETE CASCADE,
                                    unit_id         BIGINT NOT NULL REFERENCES units(id) ON DELETE CASCADE,
                                    is_optional     BOOLEAN NOT NULL DEFAULT false,
                                    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                                    UNIQUE (box_id, unit_id)
);
CREATE INDEX idx_template_box ON box_template_items(box_id);

-- Накладные
CREATE TABLE invoices (
                          id              BIGSERIAL PRIMARY KEY,
                          customer        VARCHAR(200) NOT NULL,
                          responsible     VARCHAR(200) NOT NULL,
                          shipped_by      VARCHAR(200),
                          status          VARCHAR(30) NOT NULL DEFAULT 'draft'
                              CHECK (status IN ('draft','shipped','partially_returned','returned')),
                          locked_by       BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
                          locked_at       TIMESTAMPTZ NULL,
                          version         INT NOT NULL DEFAULT 0,
                          created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                          updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_locked ON invoices(locked_by);

-- Строки накладной
CREATE TABLE invoice_items (
                               id              BIGSERIAL PRIMARY KEY,
                               invoice_id      BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
                               unit_id         BIGINT NULL REFERENCES units(id),
                               bulk_item_id    BIGINT NULL REFERENCES bulk_items(id),
                               box_id          BIGINT NULL REFERENCES boxes(id),
                               qty_planned     INT NOT NULL DEFAULT 1 CHECK (qty_planned >= 0),
                               qty_shipped     INT NOT NULL DEFAULT 0 CHECK (qty_shipped >= 0),
                               qty_returned    INT NOT NULL DEFAULT 0 CHECK (qty_returned >= 0),
                               created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                               CHECK (unit_id IS NOT NULL OR bulk_item_id IS NOT NULL)
);
CREATE INDEX idx_inv_items_invoice ON invoice_items(invoice_id);
CREATE INDEX idx_inv_items_unit ON invoice_items(unit_id);
CREATE INDEX idx_inv_items_box ON invoice_items(box_id);

-- Аудит перемещений
CREATE TABLE movements (
                           id              BIGSERIAL PRIMARY KEY,
                           unit_id         BIGINT NULL REFERENCES units(id),
                           bulk_item_id    BIGINT NULL REFERENCES bulk_items(id),
                           invoice_id      BIGINT NULL REFERENCES invoices(id),
                           from_status     VARCHAR(30),
                           to_status       VARCHAR(30),
                           qty             INT NOT NULL DEFAULT 1,
                           comment         TEXT,
                           created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_movements_unit ON movements(unit_id);
CREATE INDEX idx_movements_invoice ON movements(invoice_id);
CREATE INDEX idx_movements_created ON movements(created_at);

-- Очередь синхронизации (на случай офлайна)
CREATE TABLE sync_operations (
                                 op_id           UUID PRIMARY KEY,
                                 entity          VARCHAR(50) NOT NULL,
                                 entity_id       BIGINT NOT NULL,
                                 operation       VARCHAR(20) NOT NULL,
                                 payload         JSONB NOT NULL,
                                 applied_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_sync_entity ON sync_operations(entity, entity_id);