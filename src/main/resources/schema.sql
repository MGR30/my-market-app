CREATE TABLE IF NOT EXISTS items (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(2000),
    img_path    VARCHAR(255),
    price       BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS cart_items (
    id       BIGSERIAL PRIMARY KEY,
    item_id  BIGINT NOT NULL UNIQUE REFERENCES items(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL CHECK (quantity > 0)
);

CREATE TABLE IF NOT EXISTS orders (
    id         BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS order_items (
    id       BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    item_id  BIGINT NOT NULL,
    title    VARCHAR(255) NOT NULL,
    price    BIGINT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0)
);