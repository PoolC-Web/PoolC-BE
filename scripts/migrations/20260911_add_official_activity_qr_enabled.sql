ALTER TABLE official_activity
    ADD COLUMN IF NOT EXISTS qr_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE official_activity_qr_token
    ALTER COLUMN expires_at DROP NOT NULL;
