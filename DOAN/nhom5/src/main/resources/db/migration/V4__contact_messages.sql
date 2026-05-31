-- Bảng tin nhắn liên hệ
CREATE TABLE IF NOT EXISTS contact_messages (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  sender_uid    VARCHAR(128),    -- null nếu chưa login
  sender_name   VARCHAR(100) NOT NULL,
  sender_email  VARCHAR(150) NOT NULL,
  sender_phone  VARCHAR(15),
  subject       VARCHAR(200) NOT NULL,
  message       TEXT NOT NULL,
  status        VARCHAR(20) DEFAULT 'UNREAD',
  reply_content TEXT,
  replied_at    TIMESTAMP,
  replied_by    VARCHAR(128),    -- admin uid
  created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index tăng tốc query
CREATE INDEX IF NOT EXISTS idx_contact_status ON contact_messages(status);
CREATE INDEX IF NOT EXISTS idx_contact_sender ON contact_messages(sender_uid);
CREATE INDEX IF NOT EXISTS idx_contact_created ON contact_messages(created_at);
