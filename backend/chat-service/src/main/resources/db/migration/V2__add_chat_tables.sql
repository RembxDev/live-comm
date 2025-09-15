CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS chat.chat_message (
                                                 id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id  UUID         NOT NULL,
    sender      VARCHAR(120) NOT NULL,
    type        VARCHAR(32)  NOT NULL,
    content     TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_chat_message_session   ON chat.chat_message(session_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_created_at ON chat.chat_message(created_at);
