ALTER TABLE product ADD COLUMN ingredients TEXT;
ALTER TABLE product ADD COLUMN steps TEXT;
ALTER TABLE product MODIFY COLUMN ingredients TEXT;
ALTER TABLE product MODIFY COLUMN steps TEXT;
ALTER TABLE product ADD COLUMN created_by_email VARCHAR(320);
ALTER TABLE product MODIFY COLUMN category TEXT;
ALTER TABLE users ADD COLUMN avatar_url VARCHAR(1000);
ALTER TABLE users ADD COLUMN oauth_id VARCHAR(255);
CREATE TABLE IF NOT EXISTS user_favorites (
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, product_id)
);
