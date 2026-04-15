CREATE INDEX IF NOT EXISTS idx_clip_item_is_favorite
    ON clip_item (is_favorite);

CREATE INDEX IF NOT EXISTS idx_clip_item_copy_count
    ON clip_item (copy_count);
