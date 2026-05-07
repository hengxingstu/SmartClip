# Changelog

## v1.0.0

SmartClip v1.0.0 is the first public major release.

### Highlights

- Local-first Windows clipboard text history manager.
- Automatic clipboard text collection, de-duplication, copy counting, and copy timestamps.
- Type detection for URL, JSON, SQL, command, Java exception logs, file paths, code snippets, and plain text.
- Desktop UI with history, favorites, frequent clips, ignored clips, search, type filters, tag filters, detail view, and copy-back support.
- Tag editing for individual clipboard records.
- Settings for listener status, polling interval, minimum text length, and sensitive-content ignore behavior.
- SQLite storage with Flyway migrations.
- Electron desktop packaging with bundled Spring Boot backend and slim JRE runtime.

### Distribution

- Source code is published under the MIT License.
- Windows installer is attached to the GitHub Release as `SmartClip-Setup-1.0.0.exe`.
- SHA256 checksum is attached as `SmartClip-Setup-1.0.0.exe.sha256`.

### Known Limitations

- Windows only.
- The installer is not code-signed, so Windows may show a security warning.
- Clipboard history is stored locally and is not synced across devices.
- User data is stored under the current Windows user profile and is not automatically removed by uninstalling the application.
