-- Add new columns to url_mappings table for user ownership and management
ALTER TABLE url_mappings
    ADD COLUMN owner_id BIGINT REFERENCES users(id),
ADD COLUMN expires_at TIMESTAMP,
ADD COLUMN click_count BIGINT DEFAULT 0 NOT NULL,
ADD COLUMN is_active BOOLEAN DEFAULT true NOT NULL;

-- Create indexes for performance
CREATE INDEX idx_url_mappings_owner_id ON url_mappings(owner_id);
CREATE INDEX idx_url_mappings_expires_at ON url_mappings(expires_at);
CREATE INDEX idx_url_mappings_is_active ON url_mappings(is_active);

-- Update existing records to have default values
UPDATE url_mappings
SET click_count = 0, is_active = true
WHERE click_count IS NULL OR is_active IS NULL;

-- Add foreign key constraint
ALTER TABLE url_mappings
    ADD CONSTRAINT fk_url_mappings_owner
        FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL;