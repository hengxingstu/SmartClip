CREATE TABLE IF NOT EXISTS tag (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    normalized_name TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tag_normalized_name
    ON tag (normalized_name);

CREATE TABLE IF NOT EXISTS clip_item_tag (
    clip_item_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,
    created_at TEXT NOT NULL,
    PRIMARY KEY (clip_item_id, tag_id),
    FOREIGN KEY (clip_item_id) REFERENCES clip_item(id),
    FOREIGN KEY (tag_id) REFERENCES tag(id)
);

CREATE INDEX IF NOT EXISTS idx_clip_item_tag_tag_id
    ON clip_item_tag (tag_id);

CREATE INDEX IF NOT EXISTS idx_clip_item_tag_clip_item_id
    ON clip_item_tag (clip_item_id);

CREATE INDEX IF NOT EXISTS idx_clip_item_type_sub_type
    ON clip_item (type, sub_type);

CREATE INDEX IF NOT EXISTS idx_clip_item_ignored_last_copied_at
    ON clip_item (is_ignored, last_copied_at);

CREATE INDEX IF NOT EXISTS idx_clip_item_ignored_favorite_last_copied_at
    ON clip_item (is_ignored, is_favorite, last_copied_at);

CREATE INDEX IF NOT EXISTS idx_clip_item_ignored_copy_count_last_copied_at
    ON clip_item (is_ignored, copy_count, last_copied_at);
