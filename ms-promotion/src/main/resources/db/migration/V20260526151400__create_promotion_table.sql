CREATE TABLE promotion (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    store_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL
);
