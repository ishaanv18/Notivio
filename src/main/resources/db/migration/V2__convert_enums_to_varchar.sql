-- ============================================================
-- V2: Convert PostgreSQL native ENUMs to VARCHAR
-- Root fix: Drop partial indexes with enum WHERE clauses
--           before altering column types, then recreate them.
--           Also drop DEFAULT values before altering types.
-- ============================================================

-- ── extracted_tasks ──────────────────────────────────────────
ALTER TABLE extracted_tasks ALTER COLUMN task_type DROP DEFAULT;
ALTER TABLE extracted_tasks ALTER COLUMN task_type TYPE VARCHAR(50) USING task_type::text;
ALTER TABLE extracted_tasks ALTER COLUMN task_type SET DEFAULT 'OTHER';

ALTER TABLE extracted_tasks ALTER COLUMN priority DROP DEFAULT;
ALTER TABLE extracted_tasks ALTER COLUMN priority TYPE VARCHAR(20) USING priority::text;
ALTER TABLE extracted_tasks ALTER COLUMN priority SET DEFAULT 'MEDIUM';

ALTER TABLE extracted_tasks ALTER COLUMN status DROP DEFAULT;
ALTER TABLE extracted_tasks ALTER COLUMN status TYPE VARCHAR(30) USING status::text;
ALTER TABLE extracted_tasks ALTER COLUMN status SET DEFAULT 'PENDING';

-- ── reminders ────────────────────────────────────────────────
-- Must drop partial index first: its WHERE clause stores
-- status = 'SCHEDULED'::reminder_status which blocks ALTER TYPE
DROP INDEX IF EXISTS idx_reminders_due;

ALTER TABLE reminders ALTER COLUMN status DROP DEFAULT;
ALTER TABLE reminders ALTER COLUMN status TYPE VARCHAR(30) USING status::text;
ALTER TABLE reminders ALTER COLUMN status SET DEFAULT 'SCHEDULED';

-- Recreate the partial index using plain VARCHAR comparison
CREATE INDEX idx_reminders_due ON reminders(remind_at, status) WHERE status = 'SCHEDULED';

-- ── notifications ─────────────────────────────────────────────
ALTER TABLE notifications ALTER COLUMN type DROP DEFAULT;
ALTER TABLE notifications ALTER COLUMN type TYPE VARCHAR(50) USING type::text;
ALTER TABLE notifications ALTER COLUMN type SET DEFAULT 'REMINDER';

ALTER TABLE notifications ALTER COLUMN status DROP DEFAULT;
ALTER TABLE notifications ALTER COLUMN status TYPE VARCHAR(30) USING status::text;
ALTER TABLE notifications ALTER COLUMN status SET DEFAULT 'PENDING';

-- ── device_tokens ─────────────────────────────────────────────
ALTER TABLE device_tokens ALTER COLUMN platform DROP DEFAULT;
ALTER TABLE device_tokens ALTER COLUMN platform TYPE VARCHAR(20) USING platform::text;
ALTER TABLE device_tokens ALTER COLUMN platform SET DEFAULT 'ANDROID';

-- ── ai_logs ───────────────────────────────────────────────────
ALTER TABLE ai_logs ALTER COLUMN provider TYPE VARCHAR(30) USING provider::text;

-- ── api_keys ──────────────────────────────────────────────────
ALTER TABLE api_keys ALTER COLUMN service TYPE VARCHAR(50) USING service::text;

-- ── api_usage_logs ────────────────────────────────────────────
ALTER TABLE api_usage_logs ALTER COLUMN service TYPE VARCHAR(50) USING service::text;

-- ── api_alerts ────────────────────────────────────────────────
ALTER TABLE api_alerts ALTER COLUMN service TYPE VARCHAR(50) USING service::text;
ALTER TABLE api_alerts ALTER COLUMN alert_level TYPE VARCHAR(20) USING alert_level::text;

ALTER TABLE api_alerts ALTER COLUMN status DROP DEFAULT;
ALTER TABLE api_alerts ALTER COLUMN status TYPE VARCHAR(30) USING status::text;
ALTER TABLE api_alerts ALTER COLUMN status SET DEFAULT 'ACTIVE';

-- Drop old PostgreSQL enum types (no longer needed)
DROP TYPE IF EXISTS task_type CASCADE;
DROP TYPE IF EXISTS task_priority CASCADE;
DROP TYPE IF EXISTS task_status CASCADE;
DROP TYPE IF EXISTS reminder_status CASCADE;
DROP TYPE IF EXISTS notification_type CASCADE;
DROP TYPE IF EXISTS notification_status CASCADE;
DROP TYPE IF EXISTS device_platform CASCADE;
DROP TYPE IF EXISTS ai_provider CASCADE;
DROP TYPE IF EXISTS api_service CASCADE;
DROP TYPE IF EXISTS alert_level CASCADE;
DROP TYPE IF EXISTS alert_status CASCADE;
