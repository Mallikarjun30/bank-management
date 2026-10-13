
CREATE TABLE password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,

    CONSTRAINT pk_password_reset_tokens
        PRIMARY KEY (id),

    CONSTRAINT uk_password_reset_token_hash
        UNIQUE (token_hash),

    CONSTRAINT fk_password_reset_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees (id),

    INDEX idx_password_reset_employee (employee_id),
    INDEX idx_password_reset_expires (expires_at)
) ENGINE=InnoDB;

