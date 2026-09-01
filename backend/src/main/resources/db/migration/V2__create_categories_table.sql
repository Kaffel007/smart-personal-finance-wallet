CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    normalized_name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES app_users(id),
    CONSTRAINT uk_categories_user_type_normalized_name
        UNIQUE (user_id, type, normalized_name),
    CONSTRAINT chk_categories_type CHECK (type IN ('INCOME', 'EXPENSE'))
);

CREATE INDEX idx_categories_user_type ON categories (user_id, type);
