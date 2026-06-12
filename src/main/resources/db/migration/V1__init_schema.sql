-- ============================================================
-- NOTIVIO - PostgreSQL Database Schema
-- V1: Initial Schema
-- ============================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- USERS TABLE
-- ============================================================
CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    google_id           VARCHAR(255) UNIQUE NOT NULL,
    email               VARCHAR(255) UNIQUE NOT NULL,
    name                VARCHAR(255),
    profile_picture_url TEXT,
    timezone            VARCHAR(100) DEFAULT 'UTC',
    is_active           BOOLEAN DEFAULT TRUE,
    gmail_connected     BOOLEAN DEFAULT FALSE,
    calendar_connected  BOOLEAN DEFAULT FALSE,
    digest_enabled      BOOLEAN DEFAULT TRUE,
    digest_time         TIME DEFAULT '08:00:00',
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_google_id ON users(google_id);

-- ============================================================
-- GMAIL TOKENS TABLE (encrypted storage)
-- ============================================================
CREATE TABLE gmail_tokens (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    access_token        TEXT NOT NULL,         -- AES-256 encrypted
    refresh_token       TEXT,                  -- AES-256 encrypted
    token_type          VARCHAR(50) DEFAULT 'Bearer',
    scope               TEXT,
    expires_at          TIMESTAMP WITH TIME ZONE,
    last_synced_at      TIMESTAMP WITH TIME ZONE,
    last_sync_history_id VARCHAR(255),         -- Gmail history ID for incremental sync
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uq_gmail_tokens_user UNIQUE (user_id)
);

CREATE INDEX idx_gmail_tokens_user_id ON gmail_tokens(user_id);
CREATE INDEX idx_gmail_tokens_expires_at ON gmail_tokens(expires_at);

-- ============================================================
-- EMAILS TABLE
-- ============================================================
CREATE TABLE emails (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    gmail_message_id    VARCHAR(255) NOT NULL,
    gmail_thread_id     VARCHAR(255),
    subject             TEXT,
    sender              VARCHAR(500),
    sender_email        VARCHAR(255),
    snippet             TEXT,
    body_plain          TEXT,
    body_html           TEXT,
    received_at         TIMESTAMP WITH TIME ZONE,
    labels              TEXT[],                -- Gmail labels array
    is_processed        BOOLEAN DEFAULT FALSE,
    is_relevant         BOOLEAN DEFAULT FALSE,
    processing_error    TEXT,
    processed_at        TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uq_emails_user_message UNIQUE (user_id, gmail_message_id)
);

CREATE INDEX idx_emails_user_id ON emails(user_id);
CREATE INDEX idx_emails_gmail_message_id ON emails(gmail_message_id);
CREATE INDEX idx_emails_is_processed ON emails(is_processed);
CREATE INDEX idx_emails_received_at ON emails(received_at DESC);
CREATE INDEX idx_emails_user_processed ON emails(user_id, is_processed);

-- ============================================================
-- EXTRACTED TASKS TABLE
-- ============================================================
CREATE TYPE task_type AS ENUM (
    'ASSIGNMENT', 'EXAM', 'INTERVIEW', 'MEETING',
    'EVENT', 'INTERNSHIP', 'PLACEMENT', 'SUBMISSION',
    'DEADLINE', 'GENERAL_REMINDER', 'OTHER'
);

CREATE TYPE task_priority AS ENUM ('HIGH', 'MEDIUM', 'LOW');

CREATE TYPE task_status AS ENUM (
    'PENDING', 'IN_PROGRESS', 'COMPLETED', 'OVERDUE', 'CANCELLED'
);

CREATE TABLE extracted_tasks (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email_id            UUID REFERENCES emails(id) ON DELETE SET NULL,
    title               VARCHAR(500) NOT NULL,
    description         TEXT,
    task_type           task_type DEFAULT 'OTHER',
    priority            task_priority DEFAULT 'MEDIUM',
    status              task_status DEFAULT 'PENDING',
    deadline            TIMESTAMP WITH TIME ZONE,
    event_date          TIMESTAMP WITH TIME ZONE,
    location            VARCHAR(500),
    organizer           VARCHAR(255),
    course_name         VARCHAR(255),
    source_email_sender VARCHAR(255),
    ai_confidence       DECIMAL(5,2),           -- 0.00 to 100.00
    ai_summary          TEXT,
    calendar_event_id   VARCHAR(255),           -- Google Calendar event ID
    is_reminder_created BOOLEAN DEFAULT FALSE,
    is_duplicate        BOOLEAN DEFAULT FALSE,
    raw_ai_response     TEXT,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_tasks_user_id ON extracted_tasks(user_id);
CREATE INDEX idx_tasks_deadline ON extracted_tasks(deadline);
CREATE INDEX idx_tasks_status ON extracted_tasks(status);
CREATE INDEX idx_tasks_priority ON extracted_tasks(priority);
CREATE INDEX idx_tasks_user_status ON extracted_tasks(user_id, status);
CREATE INDEX idx_tasks_user_deadline ON extracted_tasks(user_id, deadline);
CREATE INDEX idx_tasks_type ON extracted_tasks(task_type);

-- ============================================================
-- REMINDERS TABLE
-- ============================================================
CREATE TYPE reminder_status AS ENUM (
    'SCHEDULED', 'SENT', 'FAILED', 'CANCELLED', 'SKIPPED'
);

CREATE TABLE reminders (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    task_id             UUID NOT NULL REFERENCES extracted_tasks(id) ON DELETE CASCADE,
    remind_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    interval_label      VARCHAR(50),            -- e.g. "1 day before", "1 hour before"
    status              reminder_status DEFAULT 'SCHEDULED',
    sent_at             TIMESTAMP WITH TIME ZONE,
    error_message       TEXT,
    retry_count         INTEGER DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_reminders_user_id ON reminders(user_id);
CREATE INDEX idx_reminders_task_id ON reminders(task_id);
CREATE INDEX idx_reminders_remind_at ON reminders(remind_at);
CREATE INDEX idx_reminders_status ON reminders(status);
CREATE INDEX idx_reminders_due ON reminders(remind_at, status) WHERE status = 'SCHEDULED';

-- ============================================================
-- NOTIFICATIONS TABLE
-- ============================================================
CREATE TYPE notification_type AS ENUM (
    'REMINDER', 'TASK_CREATED', 'DEADLINE_ALERT',
    'DIGEST', 'SYSTEM', 'QUOTA_ALERT'
);

CREATE TYPE notification_status AS ENUM (
    'PENDING', 'SENT', 'FAILED', 'READ'
);

CREATE TABLE notifications (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    task_id             UUID REFERENCES extracted_tasks(id) ON DELETE SET NULL,
    reminder_id         UUID REFERENCES reminders(id) ON DELETE SET NULL,
    type                notification_type DEFAULT 'REMINDER',
    title               VARCHAR(500) NOT NULL,
    body                TEXT,
    data                JSONB,                  -- extra data payload
    status              notification_status DEFAULT 'PENDING',
    sent_at             TIMESTAMP WITH TIME ZONE,
    read_at             TIMESTAMP WITH TIME ZONE,
    error_message       TEXT,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);

-- ============================================================
-- DEVICE TOKENS TABLE (FCM / Expo)
-- ============================================================
CREATE TYPE device_platform AS ENUM ('ANDROID', 'IOS', 'WEB');

CREATE TABLE device_tokens (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token               TEXT NOT NULL,
    platform            device_platform DEFAULT 'ANDROID',
    device_name         VARCHAR(255),
    is_active           BOOLEAN DEFAULT TRUE,
    last_used_at        TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uq_device_tokens UNIQUE (user_id, token)
);

CREATE INDEX idx_device_tokens_user_id ON device_tokens(user_id);
CREATE INDEX idx_device_tokens_active ON device_tokens(user_id, is_active);

-- ============================================================
-- AI LOGS TABLE
-- ============================================================
CREATE TYPE ai_provider AS ENUM ('GROQ', 'OPENROUTER');

CREATE TABLE ai_logs (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID REFERENCES users(id) ON DELETE SET NULL,
    email_id            UUID REFERENCES emails(id) ON DELETE SET NULL,
    provider            ai_provider NOT NULL,
    model               VARCHAR(255),
    prompt_tokens       INTEGER DEFAULT 0,
    completion_tokens   INTEGER DEFAULT 0,
    total_tokens        INTEGER DEFAULT 0,
    latency_ms          INTEGER,
    success             BOOLEAN DEFAULT TRUE,
    error_message       TEXT,
    request_payload     TEXT,
    response_payload    TEXT,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_ai_logs_user_id ON ai_logs(user_id);
CREATE INDEX idx_ai_logs_provider ON ai_logs(provider);
CREATE INDEX idx_ai_logs_created_at ON ai_logs(created_at DESC);

-- ============================================================
-- API KEYS TABLE
-- ============================================================
CREATE TYPE api_service AS ENUM (
    'GROQ', 'OPENROUTER', 'GMAIL', 'GOOGLE_CALENDAR', 'FIREBASE'
);

CREATE TABLE api_keys (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service             api_service NOT NULL,
    key_name            VARCHAR(255),
    api_key_encrypted   TEXT,                   -- Encrypted API key
    is_active           BOOLEAN DEFAULT TRUE,
    daily_limit         INTEGER,
    monthly_limit       INTEGER,
    total_requests      BIGINT DEFAULT 0,
    total_tokens        BIGINT DEFAULT 0,
    last_used_at        TIMESTAMP WITH TIME ZONE,
    expires_at          TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_api_keys_service ON api_keys(service);
CREATE INDEX idx_api_keys_active ON api_keys(is_active);

-- ============================================================
-- API USAGE LOGS TABLE
-- ============================================================
CREATE TABLE api_usage_logs (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    api_key_id          UUID REFERENCES api_keys(id) ON DELETE SET NULL,
    service             api_service NOT NULL,
    endpoint            VARCHAR(500),
    tokens_used         INTEGER DEFAULT 0,
    request_count       INTEGER DEFAULT 1,
    response_code       INTEGER,
    success             BOOLEAN DEFAULT TRUE,
    error_message       TEXT,
    cost_estimate       DECIMAL(10, 6) DEFAULT 0,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_api_usage_service ON api_usage_logs(service);
CREATE INDEX idx_api_usage_created_at ON api_usage_logs(created_at DESC);
CREATE INDEX idx_api_usage_api_key ON api_usage_logs(api_key_id);

-- ============================================================
-- API ALERTS TABLE
-- ============================================================
CREATE TYPE alert_level AS ENUM ('WARNING', 'CRITICAL', 'INFO');
CREATE TYPE alert_status AS ENUM ('ACTIVE', 'ACKNOWLEDGED', 'RESOLVED');

CREATE TABLE api_alerts (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service             api_service NOT NULL,
    alert_level         alert_level NOT NULL,
    status              alert_status DEFAULT 'ACTIVE',
    message             TEXT NOT NULL,
    usage_percentage    DECIMAL(5,2),
    tokens_used         BIGINT,
    tokens_limit        BIGINT,
    acknowledged_at     TIMESTAMP WITH TIME ZONE,
    resolved_at         TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_api_alerts_service ON api_alerts(service);
CREATE INDEX idx_api_alerts_status ON api_alerts(status);
CREATE INDEX idx_api_alerts_created_at ON api_alerts(created_at DESC);

-- ============================================================
-- SCHEDULER LOCKS TABLE (prevent duplicate job execution)
-- ============================================================
CREATE TABLE scheduler_locks (
    lock_name           VARCHAR(255) PRIMARY KEY,
    locked_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_by           VARCHAR(255),
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

-- ============================================================
-- AUTO-UPDATE updated_at TRIGGER
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_gmail_tokens_updated_at
    BEFORE UPDATE ON gmail_tokens
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_extracted_tasks_updated_at
    BEFORE UPDATE ON extracted_tasks
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_reminders_updated_at
    BEFORE UPDATE ON reminders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_device_tokens_updated_at
    BEFORE UPDATE ON device_tokens
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_api_keys_updated_at
    BEFORE UPDATE ON api_keys
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
