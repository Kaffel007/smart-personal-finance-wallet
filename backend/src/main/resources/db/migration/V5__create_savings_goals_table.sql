CREATE TABLE savings_goals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    target_amount DECIMAL(19,4) NOT NULL,
    saved_amount DECIMAL(19,4) NOT NULL,
    target_date DATE NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_savings_goals PRIMARY KEY (id),
    CONSTRAINT fk_savings_goals_user FOREIGN KEY (user_id) REFERENCES app_users(id),
    CONSTRAINT chk_savings_goals_target_amount CHECK (target_amount > 0),
    CONSTRAINT chk_savings_goals_saved_amount CHECK (saved_amount >= 0)
);

CREATE INDEX idx_savings_goals_user ON savings_goals(user_id);
