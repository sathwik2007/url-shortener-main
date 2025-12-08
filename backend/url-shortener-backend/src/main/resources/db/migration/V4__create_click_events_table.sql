-- V4__create_click_events_table.sql

CREATE TABLE click_events (
                              id BIGSERIAL PRIMARY KEY,
                              url_mapping_id BIGINT NOT NULL,
                              clicked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              ip_address_hash VARCHAR(64),
                              user_agent VARCHAR(500),
                              referrer VARCHAR(500),
                              device_type VARCHAR(50),
                              browser VARCHAR(100),
                              operating_system VARCHAR(100),
                              country VARCHAR(100),
                              city VARCHAR(100),
                              CONSTRAINT fk_click_events_url_mapping
                                  FOREIGN KEY (url_mapping_id)
                                      REFERENCES url_mappings(id)
                                      ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_click_events_url_mapping_id ON click_events(url_mapping_id);
CREATE INDEX idx_click_events_clicked_at ON click_events(clicked_at);
CREATE INDEX idx_click_events_url_mapping_clicked_at ON click_events(url_mapping_id, clicked_at);

-- Add comment for documentation
COMMENT ON TABLE click_events IS 'Stores click event data for URL analytics';
COMMENT ON COLUMN click_events.ip_address_hash IS 'Hashed IP address for privacy';
COMMENT ON COLUMN click_events.clicked_at IS 'Timestamp when the URL was clicked';
