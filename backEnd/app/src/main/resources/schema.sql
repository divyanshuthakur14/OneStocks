CREATE DATABASE IF NOT EXISTS onestocks_db;
USE onestocks_db;

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    remember_me BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE stocks (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    symbol          VARCHAR(16)     NOT NULL,
    name            VARCHAR(128)    NOT NULL,
    sector          VARCHAR(64),
    description     TEXT,
    current_price   DECIMAL(19, 4)  NOT NULL,
    previous_close  DECIMAL(19, 4)  NOT NULL,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stocks_symbol (symbol)
);

CREATE INDEX idx_stocks_symbol ON stocks (symbol);

CREATE TABLE stock_price_history (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    stock_id     BIGINT          NOT NULL,
    price        DECIMAL(19, 4)  NOT NULL,
    recorded_at  TIMESTAMP       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sph_stock FOREIGN KEY (stock_id) REFERENCES stocks (id)
);

CREATE INDEX idx_sph_stock_id        ON stock_price_history (stock_id);
CREATE INDEX idx_sph_recorded_at     ON stock_price_history (recorded_at);
CREATE INDEX idx_sph_stock_recorded  ON stock_price_history (stock_id, recorded_at);

CREATE TABLE transactions (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    user_id          BIGINT          NOT NULL,
    stock_id         BIGINT          NOT NULL,
    type             ENUM('BUY', 'SELL')          NOT NULL,
    quantity         INT             NOT NULL,
    price_per_share  DECIMAL(19, 4)  NOT NULL,
    total_amount     DECIMAL(19, 4)  NOT NULL,
    status           ENUM('EXECUTED', 'FAILED')   NOT NULL,
    created_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_tx_user  FOREIGN KEY (user_id)  REFERENCES users  (id),
    CONSTRAINT fk_tx_stock FOREIGN KEY (stock_id) REFERENCES stocks (id)
);

CREATE INDEX idx_tx_user_id     ON transactions (user_id);
CREATE INDEX idx_tx_stock_id    ON transactions (stock_id);
CREATE INDEX idx_tx_created_at  ON transactions (created_at);

CREATE TABLE holdings (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    user_id             BIGINT          NOT NULL,
    stock_id            BIGINT          NOT NULL,
    quantity            INT             NOT NULL,
    average_buy_price   DECIMAL(19, 4)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_holdings_user_stock (user_id, stock_id),
    CONSTRAINT fk_hold_user  FOREIGN KEY (user_id)  REFERENCES users  (id),
    CONSTRAINT fk_hold_stock FOREIGN KEY (stock_id) REFERENCES stocks (id)
);

CREATE TABLE wallets (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    user_id     BIGINT          NOT NULL,
    balance     DECIMAL(19, 4)  NOT NULL,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallets_user (user_id),
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users (id)
);
