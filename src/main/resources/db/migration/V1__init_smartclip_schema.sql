CREATE TABLE IF NOT EXISTS clip_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    content TEXT NOT NULL,
    content_hash TEXT NOT NULL,
    type TEXT NOT NULL,
    sub_type TEXT,
    title TEXT,
    preview_text TEXT,
    copy_count INTEGER NOT NULL DEFAULT 1,
    first_copied_at TEXT NOT NULL,
    last_copied_at TEXT NOT NULL,
    is_favorite INTEGER NOT NULL DEFAULT 0,
    is_ignored INTEGER NOT NULL DEFAULT 0,
    sensitivity_level TEXT NOT NULL DEFAULT 'NORMAL',
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_clip_item_content_hash
    ON clip_item (content_hash);

CREATE INDEX IF NOT EXISTS idx_clip_item_last_copied_at
    ON clip_item (last_copied_at);

CREATE INDEX IF NOT EXISTS idx_clip_item_type
    ON clip_item (type);

CREATE INDEX IF NOT EXISTS idx_clip_item_is_ignored
    ON clip_item (is_ignored);

CREATE INDEX IF NOT EXISTS idx_clip_item_search
    ON clip_item (title, preview_text);

CREATE TABLE IF NOT EXISTS clip_event (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    clip_item_id INTEGER NOT NULL,
    copied_at TEXT NOT NULL,
    raw_preview TEXT,
    FOREIGN KEY (clip_item_id) REFERENCES clip_item(id)
);

CREATE INDEX IF NOT EXISTS idx_clip_event_clip_item_id
    ON clip_event (clip_item_id);

CREATE INDEX IF NOT EXISTS idx_clip_event_copied_at
    ON clip_event (copied_at);

CREATE TABLE IF NOT EXISTS app_setting (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    setting_key TEXT NOT NULL,
    setting_value TEXT NOT NULL,
    value_type TEXT NOT NULL,
    description TEXT,
    updated_at TEXT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_app_setting_key
    ON app_setting (setting_key);
