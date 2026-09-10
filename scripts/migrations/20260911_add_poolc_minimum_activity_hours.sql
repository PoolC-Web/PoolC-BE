ALTER TABLE poolc ADD COLUMN IF NOT EXISTS minimum_activity_hours INTEGER;
UPDATE poolc SET minimum_activity_hours = 10 WHERE minimum_activity_hours IS NULL;
ALTER TABLE poolc ALTER COLUMN minimum_activity_hours SET DEFAULT 10;
ALTER TABLE poolc ALTER COLUMN minimum_activity_hours SET NOT NULL;
