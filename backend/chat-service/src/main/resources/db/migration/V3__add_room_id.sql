ALTER TABLE chat.chat_message
    ADD COLUMN IF NOT EXISTS room_id TEXT;

UPDATE chat.chat_message
SET room_id = 'global'
WHERE room_id IS NULL;

ALTER TABLE chat.chat_message
    ALTER COLUMN room_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_chat_message_room_created
    ON chat.chat_message(room_id, created_at);
