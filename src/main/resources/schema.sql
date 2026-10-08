CREATE TABLE IF NOT EXISTS items (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    img_path    VARCHAR(255),
    price       BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS cart_items (
    id      BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL UNIQUE REFERENCES items(id) ON DELETE CASCADE,
    count   INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS order_items (
    id       BIGSERIAL PRIMARY KEY,
    order_id BIGINT REFERENCES orders(id) ON DELETE CASCADE,
    item_id  BIGINT,
    title    VARCHAR(255),
    price    BIGINT,
    count    INTEGER
);
