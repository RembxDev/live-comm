
CREATE TABLE IF NOT EXISTS session.guest_session (
    session_id          UUID PRIMARY KEY,
    email               VARCHAR(255),
    verification_token  VARCHAR(255),
    verified            BOOLEAN NOT NULL DEFAULT FALSE,
    captcha_a           INTEGER,
    captcha_b           INTEGER,
    created_at          TIMESTAMPTZ NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_guest_session_created_at ON session.guest_session(created_at);
