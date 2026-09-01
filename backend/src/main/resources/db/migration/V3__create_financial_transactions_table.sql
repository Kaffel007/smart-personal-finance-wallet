CREATE TABLE financial_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    transaction_date DATE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_financial_transactions PRIMARY KEY (id),
    CONSTRAINT fk_financial_transactions_user FOREIGN KEY (user_id) REFERENCES app_users(id),
    CONSTRAINT fk_financial_transactions_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT chk_financial_transactions_type CHECK (type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT chk_financial_transactions_amount CHECK (amount > 0)
);

CREATE INDEX idx_financial_transactions_user_date ON financial_transactions(user_id, transaction_date);
CREATE INDEX idx_financial_transactions_user_type_date ON financial_transactions(user_id, type, transaction_date);
CREATE INDEX idx_financial_transactions_category ON financial_transactions(category_id);
