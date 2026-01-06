-- SQLite configuration for optimal performance and data integrity
PRAGMA foreign_keys = ON;           -- Enable referential integrity
PRAGMA journal_mode = WAL;          -- Write-Ahead Logging for better concurrency
PRAGMA synchronous = NORMAL;        -- Balance between safety and performance
PRAGMA cache_size = -64000;         -- 64MB cache (negative value = kibibytes)
PRAGMA temp_store = MEMORY;         -- Temporary tables in memory

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'USER', -- 'USER' or 'ADMIN'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Comic files metadata
CREATE TABLE IF NOT EXISTS comic_files (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    filename TEXT NOT NULL,
    original_filename TEXT NOT NULL,
    file_path TEXT NOT NULL,
    file_type TEXT NOT NULL, -- 'CBR' or 'CBZ'
    file_size INTEGER NOT NULL,
    page_count INTEGER NOT NULL,
    cover_image_path TEXT,
    uploaded_by INTEGER REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User reading progress (last page only)
CREATE TABLE IF NOT EXISTS reading_progress (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id),
    file_id INTEGER NOT NULL REFERENCES comic_files(id),
    current_page INTEGER NOT NULL DEFAULT 1,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, file_id)
);

-- User ratings
CREATE TABLE IF NOT EXISTS ratings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id),
    file_id INTEGER NOT NULL REFERENCES comic_files(id),
    rating INTEGER NOT NULL CHECK(rating >= 1 AND rating <= 5),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, file_id)
);

-- TTS processing jobs
CREATE TABLE IF NOT EXISTS tts_jobs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_id INTEGER NOT NULL REFERENCES comic_files(id),
    requested_by INTEGER NOT NULL REFERENCES users(id),
    status TEXT NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED
    progress INTEGER DEFAULT 0, -- 0-100
    current_page INTEGER DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- TTS results stored as JSON in SQLite
CREATE TABLE IF NOT EXISTS tts_results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    file_id INTEGER NOT NULL REFERENCES comic_files(id),
    page_number INTEGER NOT NULL,
    ocr_text TEXT, -- Raw extracted text
    speakers_json TEXT, -- JSON: [{"speaker": "narrator", "text": "...", "audio_path": "..."}]
    audio_file_path TEXT, -- Combined audio for the page
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(file_id, page_number)
);

-- LLM Configuration (admin-managed)
CREATE TABLE IF NOT EXISTS llm_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    provider TEXT NOT NULL, -- 'openai', 'claude', 'minimax'
    api_key_encrypted TEXT NOT NULL,
    model_name TEXT,
    is_active INTEGER DEFAULT 1,
    priority INTEGER DEFAULT 1, -- For fallback ordering
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Audit logs
CREATE TABLE IF NOT EXISTS audit_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER REFERENCES users(id),
    action TEXT NOT NULL,
    entity_type TEXT,
    entity_id INTEGER,
    details TEXT, -- JSON
    ip_address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- System logs
CREATE TABLE IF NOT EXISTS system_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    level TEXT NOT NULL, -- INFO, WARN, ERROR
    message TEXT NOT NULL,
    stack_trace TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Feature flags for runtime configuration
CREATE TABLE IF NOT EXISTS feature_flags (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    feature_name TEXT UNIQUE NOT NULL,
    is_enabled INTEGER DEFAULT 0, -- 0=disabled, 1=enabled
    description TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for query optimization
-- User lookup by credentials
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- File browsing and filtering
CREATE INDEX IF NOT EXISTS idx_comic_files_uploaded_by ON comic_files(uploaded_by);
CREATE INDEX IF NOT EXISTS idx_comic_files_created_at ON comic_files(created_at DESC);

-- Reading progress lookup
CREATE INDEX IF NOT EXISTS idx_reading_progress_user_file ON reading_progress(user_id, file_id);

-- Rating aggregation
CREATE INDEX IF NOT EXISTS idx_ratings_file_id ON ratings(file_id);

-- TTS job status filtering
CREATE INDEX IF NOT EXISTS idx_tts_jobs_status ON tts_jobs(status);
CREATE INDEX IF NOT EXISTS idx_tts_jobs_file_id ON tts_jobs(file_id);

-- TTS result page lookup
CREATE INDEX IF NOT EXISTS idx_tts_results_file_page ON tts_results(file_id, page_number);

-- Audit log filtering
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs(action);

-- System log filtering
CREATE INDEX IF NOT EXISTS idx_system_logs_level ON system_logs(level);
CREATE INDEX IF NOT EXISTS idx_system_logs_created_at ON system_logs(created_at DESC);

-- LLM config lookup
CREATE INDEX IF NOT EXISTS idx_llm_config_provider ON llm_config(provider);
CREATE INDEX IF NOT EXISTS idx_llm_config_priority ON llm_config(priority ASC, is_active DESC);
