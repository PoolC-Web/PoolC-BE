-- Existing environments may have had these tables created by Hibernate before
-- 20260911_add_official_activity.sql was applied. Repair their constraints
-- explicitly so activity deletion and concurrent QR check-in are safe.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM official_activity_members
        GROUP BY activity_id, member_login_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot add official activity member uniqueness: duplicate rows exist.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'official_activity_members'::regclass
          AND conname = 'uk_official_activity_members'
    ) THEN
        ALTER TABLE official_activity_members
            ADD CONSTRAINT uk_official_activity_members UNIQUE (activity_id, member_login_id);
    END IF;
END $$;

ALTER TABLE official_activity_members
    DROP CONSTRAINT IF EXISTS fkf47q0rcp8o57vl4g46bj8hn1u;
ALTER TABLE official_activity_members
    ADD CONSTRAINT fkf47q0rcp8o57vl4g46bj8hn1u
        FOREIGN KEY (activity_id) REFERENCES official_activity(id) ON DELETE CASCADE;

ALTER TABLE official_activity_qr_attendance
    DROP CONSTRAINT IF EXISTS fkmov16t8xnhmys1bk2lymrdcmn;
ALTER TABLE official_activity_qr_attendance
    ADD CONSTRAINT fkmov16t8xnhmys1bk2lymrdcmn
        FOREIGN KEY (activity_id) REFERENCES official_activity(id) ON DELETE CASCADE;

ALTER TABLE official_activity_qr_token
    DROP CONSTRAINT IF EXISTS fk2fdhl58x0vg0r83o3ai8kqage;
ALTER TABLE official_activity_qr_token
    ADD CONSTRAINT fk2fdhl58x0vg0r83o3ai8kqage
        FOREIGN KEY (activity_id) REFERENCES official_activity(id) ON DELETE CASCADE;
