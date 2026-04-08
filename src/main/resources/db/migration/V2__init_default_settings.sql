INSERT OR IGNORE INTO app_setting (setting_key, setting_value, value_type, description, updated_at)
VALUES
    ('clipboard.listener.enabled', 'true', 'BOOLEAN', 'Whether clipboard listener is enabled', strftime('%Y-%m-%dT%H:%M:%f', 'now', 'localtime')),
    ('clipboard.poll.interval.ms', '1000', 'INTEGER', 'Clipboard polling interval in milliseconds', strftime('%Y-%m-%dT%H:%M:%f', 'now', 'localtime')),
    ('clipboard.min.text.length', '3', 'INTEGER', 'Minimum trimmed text length to save', strftime('%Y-%m-%dT%H:%M:%f', 'now', 'localtime')),
    ('clipboard.ignore.sensitive.enabled', 'false', 'BOOLEAN', 'Whether sensitive content should be ignored', strftime('%Y-%m-%dT%H:%M:%f', 'now', 'localtime'));
