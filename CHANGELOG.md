# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.0.4] - 2026-06-04

### Added
- Maven-standard source, resource, and test layout (`src/main/java`, `src/main/resources`, `src/test/java`).
- Task-based level progression with branching level graph support and level-selection GUI.
- `PluginContext` for dependency access in code paths that previously depended on mutable global state.
- Gson-backed JSON persistence and a shared `PlayerDataStore` abstraction.
- Command rate limiting and level ID validation hardening.
- MockBukkit, parameterized tests, Testcontainers dependencies, JaCoCo, Spotless, SpotBugs, FindSecBugs, and opt-in dependency-check/PIT profiles.
- GitHub CI, release, CodeQL, Dependabot, issue templates, security policy, and server integration test documentation.

### Changed
- Bumped the plugin from the previous `0.0.3-SNAPSHOT` state to `0.0.4` for publishing.
- Updated the build to Java 21 and Maven 3.9+ with filtered `plugin.yml` metadata.
- Replaced json-simple persistence code with Gson serialization.
- Refactored large static/global systems into smaller command, storage, placement, task, config, and utility classes.
- Updated dependency coordinates for modern Spigot, PlaceholderAPI, XSeries, Oraxen, Nexo, HikariCP, MySQL, H2, and test libraries.
- Kept official Testcontainers artifacts from Maven Central for publish-safe builds; local Testcontainers forks are ignored.
- Kept network-bound security scans and slow mutation testing out of the default CI profile.
- Preserved AGPLv3 project licensing while retaining upstream MIT license notice.

### Fixed
- Corrected Testcontainers Maven coordinates and removed reliance on a locally modified Testcontainers build.
- Fixed unsafe Bukkit API usage from async scheduler threads during world initialization and border updates.
- Replaced broad plugin task cancellation with tracked cancellation of EventBlock's repeating main tasks.
- Made player cache iteration use stable entry snapshots to avoid player/coordinate races.
- Prevented live `BossBar` objects from being serialized into JSON player data.
- Hardened database loading so malformed owner/invite UUIDs are skipped instead of aborting startup.
- Fixed new-schema level color/style parsing and synchronized level multiplier configuration.
- Tightened level advancement so GUI choices can only advance to direct successor levels while awaiting selection.
- Corrected WorldGuard enablement semantics while keeping unavailable integrations disabled.
- Fixed release workflow checksum generation to produce deterministic checksum file names.

### Security
- Added command spam rate limiting.
- Added level ID validation before progression changes.
- Added SQL/UUID load robustness and opt-in dependency/security scanning configuration.
- Removed or isolated stale pre-modernization compatibility and local dependency worktree assumptions from publish paths.

### Testing
- Expanded regression coverage for config parsing, task progress, persistence, DB loading, cache snapshots, progression validation, decorated blocks, rewards, commands, coordinates, and migration behavior.
- Added server integration test profile documentation for optional Testcontainers-based verification.
- Verified release-prep build with `mvn -B test`, `mvn -B spotless:check`, `mvn -B -Pci verify`, `mvn -B dependency:tree "-Dincludes=org.testcontainers"`, and `mvn -B -DskipTests clean package`.

## [0.0.2] - Previous

### Added
- Initial OneBlock plugin implementation.
- JSON and database persistence.
- GUI system.
- Level progression system.