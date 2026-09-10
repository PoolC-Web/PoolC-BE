CREATE SEQUENCE IF NOT EXISTS official_activity_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS official_activity (
    id BIGINT PRIMARY KEY,
    activity_date DATE NOT NULL,
    title VARCHAR(255) NOT NULL,
    recognized_hours NUMERIC(6, 1) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

ALTER TABLE official_activity ADD COLUMN IF NOT EXISTS activity_date DATE;
UPDATE official_activity SET activity_date = created_at::date WHERE activity_date IS NULL;
ALTER TABLE official_activity ALTER COLUMN activity_date SET NOT NULL;

CREATE TABLE IF NOT EXISTS official_activity_members (
    activity_id BIGINT NOT NULL REFERENCES official_activity(id) ON DELETE CASCADE,
    member_login_id VARCHAR(40) NOT NULL,
    PRIMARY KEY (activity_id, member_login_id)
);

CREATE INDEX IF NOT EXISTS idx_official_activity_members_login_id
    ON official_activity_members (member_login_id);

CREATE SEQUENCE IF NOT EXISTS official_activity_qr_attendance_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE IF NOT EXISTS official_activity_qr_attendance (
    id BIGINT PRIMARY KEY,
    activity_id BIGINT NOT NULL REFERENCES official_activity(id) ON DELETE CASCADE,
    member_login_id VARCHAR(40) NOT NULL,
    checked_in_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_official_activity_qr_attendance UNIQUE (activity_id, member_login_id)
);

CREATE TABLE IF NOT EXISTS official_activity_qr_token (
    token VARCHAR(64) PRIMARY KEY,
    activity_id BIGINT NOT NULL REFERENCES official_activity(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL
);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'official_activity' AND column_name = 'member_uuid'
    ) THEN
        INSERT INTO official_activity_members (activity_id, member_login_id)
        SELECT official_activity.id, member.login_id
        FROM official_activity
        JOIN member ON official_activity.member_uuid = member.uuid
        ON CONFLICT (activity_id, member_login_id) DO NOTHING;

        ALTER TABLE official_activity DROP COLUMN member_uuid;
    END IF;
END $$;
