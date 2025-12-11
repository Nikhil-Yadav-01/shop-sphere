CREATE TABLE notifications (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    title VARCHAR(1000) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    channel VARCHAR(50) NOT NULL,
    recipient_email VARCHAR(255),
    recipient_phone VARCHAR(20),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    read_at TIMESTAMP
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_user_id_created_at ON notifications(user_id, created_at);
