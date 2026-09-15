CREATE TABLE budgets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    budget_year INT NOT NULL,
    budget_month INT NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_budgets PRIMARY KEY (id),
    CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES app_users(id),
    CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT chk_budgets_amount CHECK (amount > 0),
    CONSTRAINT chk_budgets_month CHECK (budget_month BETWEEN 1 AND 12),
    CONSTRAINT chk_budgets_year CHECK (budget_year BETWEEN 2000 AND 2100),
    CONSTRAINT uk_budgets_user_category_period UNIQUE (user_id, category_id, budget_year, budget_month)
);

CREATE INDEX idx_budgets_user_period ON budgets(user_id, budget_year, budget_month);
CREATE INDEX idx_budgets_category ON budgets(category_id);
