ALTER TABLE session.guest_session
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ;

UPDATE session.guest_session
SET expires_at = created_at + INTERVAL '15 minutes'
WHERE expires_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_guest_session_expires_at
    ON session.guest_session(expires_at);
