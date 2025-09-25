CREATE TABLE IF NOT EXISTS chat.chat_message (
    message_id  UUID PRIMARY KEY,
    room_id     TEXT         NOT NULL,
    session_id  UUID         NOT NULL,
    sender      VARCHAR(120) NOT NULL,
    type        VARCHAR(32)  NOT NULL,
    content     TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_chat_message_session     ON chat.chat_message(session_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_room_created ON chat.chat_message(room_id, created_at);