CREATE TABLE promotion (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    original_price DECIMAL(10,2),
    category VARCHAR(100) NOT NULL,
    store_id VARCHAR(100) NOT NULL,
    url TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    valid_until TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_promotion_status ON promotion(status);
CREATE INDEX idx_promotion_category ON promotion(category);

CREATE TABLE promotion_metrics (
    promotion_id BIGINT PRIMARY KEY,
    upvotes INT DEFAULT 0,
    heat_score DECIMAL(8,4) DEFAULT 0.0000,
    last_calculated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_metrics_heat_score ON promotion_metrics(heat_score DESC);
