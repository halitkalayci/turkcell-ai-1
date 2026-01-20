-- Identity Service Schema
-- Optional: User profile cache table

CREATE TABLE IF NOT EXISTS user_profile (
    user_id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    email_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for email lookups
CREATE INDEX IF NOT EXISTS idx_user_profile_email ON user_profile(email);

-- Index for username lookups
CREATE INDEX IF NOT EXISTS idx_user_profile_username ON user_profile(username);
