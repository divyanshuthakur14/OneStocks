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

-- Seed data: 12 top stocks with realistic INR prices.
-- INSERT IGNORE skips rows whose symbol already exists (uk_stocks_symbol),
-- so this file is safe to re-run after schema is created.
INSERT IGNORE INTO stocks (symbol, name, sector, description, current_price, previous_close, updated_at) VALUES
('RELIANCE',   'Reliance Industries Ltd.',       'Energy',          'Indian multinational conglomerate.',                                                                                                                    2935.5000, 2920.2500, NOW()),
('TCS',        'Tata Consultancy Services',      'Technology',      'Multinational IT services and consulting company.',                                                                                                    4012.8000, 4005.1000, NOW()),
('HDFCBANK',   'HDFC Bank Ltd.',                 'Financials',      'Largest private sector bank in India.',                                                                                                                1650.3500, 1642.9000, NOW()),
('INFY',       'Infosys Ltd.',                   'Technology',      'Global IT services corporation.',                                                                                                                      1812.6000, 1820.4500, NOW()),
('ITC',        'ITC Ltd.',                       'Consumer Goods',  'Conglomerate spanning FMCG, hotels, agri, paper.',                                                                                                      455.7500,  458.2000, NOW()),
('CTSH',       'Cognizant Technology Solutions', 'Technology',      'American multinational IT services and consulting company headquartered in Teaneck, New Jersey. Major presence in India with multiple delivery centers.', 6250.5000, 6215.3000, NOW()),
('SBIN',       'State Bank of India',            'Financials',      'Indian multinational public sector bank and financial services statutory body. Largest bank in India by assets.',                                       830.7500,  828.1500, NOW()),
('BHARTIARTL', 'Bharti Airtel Ltd.',             'Telecom',         'Indian multinational telecommunications services company. Third-largest mobile network operator in the world.',                                        1520.6000, 1510.4500, NOW()),
('LT',         'Larsen & Toubro Ltd.',           'Engineering',     'Indian multinational conglomerate with business interests in engineering, construction, manufacturing, technology, and financial services.',            3825.4000, 3840.2000, NOW()),
('HINDUNILVR', 'Hindustan Unilever Ltd.',        'Consumer Goods',  'British-owned Indian consumer goods company. Subsidiary of Unilever. Products include foods, beverages, cleaning agents, personal care.',              2750.8000, 2735.9000, NOW()),
('MARUTI',     'Maruti Suzuki India Ltd.',       'Automobile',      'Largest passenger car manufacturer in India. Subsidiary of Japanese automaker Suzuki Motor Corporation. Market leader with models like Swift, Baleno, Brezza.', 12845.5000, 12920.7500, NOW()),
('SUNPHARMA',  'Sun Pharmaceutical Industries',  'Pharmaceuticals', 'Largest pharmaceutical company in India and 4th largest specialty generic pharmaceutical company globally. Produces specialty and generic drugs across multiple therapeutic areas.', 1728.6000, 1715.4500, NOW());
