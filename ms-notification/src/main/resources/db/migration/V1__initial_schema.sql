CREATE TABLE subscriber (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE subscriber_preference (
    id BIGSERIAL PRIMARY KEY,
    subscriber_id BIGINT NOT NULL,
    category VARCHAR(100) NOT NULL,
    CONSTRAINT fk_subscriber FOREIGN KEY (subscriber_id) REFERENCES subscriber(id) ON DELETE CASCADE
);
CREATE INDEX idx_sub_pref_category ON subscriber_preference(category);

CREATE TABLE store_contact (
    store_id VARCHAR(100) PRIMARY KEY,
    email VARCHAR(255) NOT NULL
);

CREATE TABLE notification_log (
    id BIGSERIAL PRIMARY KEY,
    recipient_email VARCHAR(255) NOT NULL,
    promotion_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    dispatched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
