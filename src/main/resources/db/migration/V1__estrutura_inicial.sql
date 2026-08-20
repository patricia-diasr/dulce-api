-- ============================================================================
-- V1__estrutura_inicial.sql
-- Estrutura inicial do banco de dados do Dulce, a partir do DER definido no
-- levantamento de requisitos. Todas as datas/horas de instante (created_at,
-- pickup_at, paid_at, sent_at etc.) são armazenadas em UTC (timestamptz).
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Tipos enumerados
-- ----------------------------------------------------------------------------
CREATE TYPE order_status AS ENUM ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELED', 'COMPLETED');
CREATE TYPE order_creation_channel AS ENUM ('CUSTOMER', 'ADMIN');
CREATE TYPE invoice_status AS ENUM ('PENDING', 'PARTIAL', 'PAID');
CREATE TYPE cake_finish AS ENUM ('white', 'dark');
CREATE TYPE schedule_block_type AS ENUM ('EVENTUAL', 'RECURRING');
CREATE TYPE schedule_recurrence AS ENUM ('DAILY', 'WEEKLY', 'MONTHLY');
CREATE TYPE audit_operation AS ENUM ('INSERT', 'UPDATE', 'DELETE');

-- ----------------------------------------------------------------------------
-- customer
-- ----------------------------------------------------------------------------
CREATE TABLE customer (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name   VARCHAR(150) NOT NULL,
    email  VARCHAR(255) NOT NULL UNIQUE,
    phone  VARCHAR(30),
    notes  TEXT
);

-- ----------------------------------------------------------------------------
-- administrator
-- ----------------------------------------------------------------------------
CREATE TABLE administrator (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name           VARCHAR(150) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL
);

-- ----------------------------------------------------------------------------
-- size
-- ----------------------------------------------------------------------------
CREATE TABLE size (
    id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name  VARCHAR(50) NOT NULL UNIQUE
);

-- ----------------------------------------------------------------------------
-- flavor
-- ----------------------------------------------------------------------------
CREATE TABLE flavor (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name                VARCHAR(100) NOT NULL UNIQUE,
    default_cake_base   cake_finish NOT NULL,
    default_topping     cake_finish NOT NULL,
    active              BOOLEAN NOT NULL DEFAULT TRUE
);

-- ----------------------------------------------------------------------------
-- flavor_size (matriz de preços: tamanho x recheio)
-- ----------------------------------------------------------------------------
CREATE TABLE flavor_size (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    flavor_id   BIGINT NOT NULL REFERENCES flavor(id),
    size_id     BIGINT NOT NULL REFERENCES size(id),
    cost_price  NUMERIC(10, 2) NOT NULL,
    sale_price  NUMERIC(10, 2) NOT NULL,
    UNIQUE (flavor_id, size_id)
);

-- ----------------------------------------------------------------------------
-- cake_order
-- ----------------------------------------------------------------------------
CREATE TABLE cake_order (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id       BIGINT NOT NULL REFERENCES customer(id),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    pickup_at         TIMESTAMPTZ NOT NULL,
    completed_at      TIMESTAMPTZ,
    notes             TEXT,
    status            order_status NOT NULL DEFAULT 'PENDING',
    creation_channel  order_creation_channel NOT NULL
);

-- ----------------------------------------------------------------------------
-- order_item (1 linha = 1 bolo dentro do pedido)
-- ----------------------------------------------------------------------------
CREATE TABLE order_item (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cake_order_id  BIGINT NOT NULL REFERENCES cake_order(id),
    flavor_id      BIGINT NOT NULL REFERENCES flavor(id),
    size_id        BIGINT NOT NULL REFERENCES size(id),
    cake_base      cake_finish NOT NULL,
    topping        cake_finish NOT NULL,
    unit_price     NUMERIC(10, 2) NOT NULL,
    message        VARCHAR(30),
    notes          TEXT
);

-- ----------------------------------------------------------------------------
-- invoice (1:1 com cake_order)
-- ----------------------------------------------------------------------------
CREATE TABLE invoice (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cake_order_id  BIGINT NOT NULL UNIQUE REFERENCES cake_order(id),
    gross_amount   NUMERIC(10, 2) NOT NULL,
    discount       NUMERIC(10, 2) NOT NULL DEFAULT 0,
    status         invoice_status NOT NULL DEFAULT 'PENDING'
);

-- ----------------------------------------------------------------------------
-- payment
-- ----------------------------------------------------------------------------
CREATE TABLE payment (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_id       BIGINT NOT NULL REFERENCES invoice(id),
    amount           NUMERIC(10, 2) NOT NULL,
    paid_at          TIMESTAMPTZ NOT NULL,
    payment_method   VARCHAR(50)
);

-- ----------------------------------------------------------------------------
-- schedule_block (bloqueios de agenda, eventuais ou recorrentes)
-- block_date é usado quando type = 'EVENTUAL'.
-- weekday é usado quando recurrence = 'WEEKLY'; month_day quando recurrence = 'MONTHLY'.
-- ----------------------------------------------------------------------------
CREATE TABLE schedule_block (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type         schedule_block_type NOT NULL,
    block_date   DATE,
    weekday      SMALLINT,
    month_day    SMALLINT,
    recurrence   schedule_recurrence,
    start_time   TIME NOT NULL,
    end_time     TIME NOT NULL,
    valid_from   DATE NOT NULL,
    valid_until  DATE,
    active       BOOLEAN NOT NULL DEFAULT TRUE
);

-- ----------------------------------------------------------------------------
-- notification_type (templates de notificação)
-- ----------------------------------------------------------------------------
CREATE TABLE notification_type (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    description       VARCHAR(150) NOT NULL,
    template_content  TEXT NOT NULL
);

-- ----------------------------------------------------------------------------
-- notification_history (registro de envios)
-- ----------------------------------------------------------------------------
CREATE TABLE notification_history (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    notification_type_id   BIGINT NOT NULL REFERENCES notification_type(id),
    cake_order_id          BIGINT REFERENCES cake_order(id),
    customer_id            BIGINT NOT NULL REFERENCES customer(id),
    email                   VARCHAR(255) NOT NULL,
    content                 TEXT NOT NULL,
    sent_at                 TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ----------------------------------------------------------------------------
-- audit_log
-- ----------------------------------------------------------------------------
CREATE TABLE audit_log (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    table_name   VARCHAR(100) NOT NULL,
    record_id    BIGINT NOT NULL,
    operation    audit_operation NOT NULL,
    old_data     JSONB,
    new_data     JSONB,
    changed_by   VARCHAR(150),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ----------------------------------------------------------------------------
-- login_verification_code
-- ----------------------------------------------------------------------------
CREATE TABLE login_verification_code (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id  BIGINT NOT NULL REFERENCES customer(id),
    code         VARCHAR(10) NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    used_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ----------------------------------------------------------------------------
-- Índices de apoio às consultas mais comuns (agenda, dashboard, histórico)
-- ----------------------------------------------------------------------------
CREATE INDEX idx_cake_order_customer_id ON cake_order (customer_id);
CREATE INDEX idx_cake_order_pickup_at ON cake_order (pickup_at);
CREATE INDEX idx_cake_order_status ON cake_order (status);
CREATE INDEX idx_order_item_cake_order_id ON order_item (cake_order_id);
CREATE INDEX idx_payment_invoice_id ON payment (invoice_id);
CREATE INDEX idx_notification_history_customer_id ON notification_history (customer_id);
CREATE INDEX idx_notification_history_cake_order_id ON notification_history (cake_order_id);
CREATE INDEX idx_schedule_block_block_date ON schedule_block (block_date);
CREATE INDEX idx_audit_log_table_record ON audit_log (table_name, record_id);
CREATE INDEX idx_login_verification_code_customer_id ON login_verification_code (customer_id);
