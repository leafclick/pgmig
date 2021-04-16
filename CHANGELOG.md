# Change Log
All notable changes to this project will be documented in this file.
This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [0.6.0] - 2021-04-16
### Added
- Allow more reflective calls in clj migrations

### Changes
- Update logging to Timbre 5
- Updated major dependencies including Clojure and Sci
- Fixed exported symbols of next.jdbc.prepare

## [0.6.0] - 2021-02-16
### Added
- More complete support for next.jdbc
- Improved json and jsonb support by including PGObject jdbc extension

### Changes
- Updated to GraalVM 21.0.2.
- Updated Clojure and dependencies
- Docker image is based on the new graalvm/graalvm-ce base

## [0.5.0] - 2020-11-09
### Added
- Support native-image programmatic migrations via [Small Clojure Interpreter (sci)](https://github.com/borkdude/sci)
- Add cli option for the migration format: sql (default), clj, edn
- Support EDN migrations (uberjar only)
- Print version command

## [0.4.0] - 2020-10-08
### Breaking changes
- Command line options have the highest priority (overriding environment variables)
- Environment variables have been changed to match `psql` environment
    * `PGHOST` instead of `SERVER_HOST`
    * `PGPORT` instead of `SERVER_PORT`
    * `PGDATABASE` instead of `DATABASE_NAME`
    * `PGUSER` instead of `DBUSER`
    * `PGPASSWORD` instead of `DBPASS`
- Command line options have been changed to match `psql` cli as well
    * `--host` instead of `--server-host`
    * `--port` instead of `--server-port`
    * `--dbname` instead of `--database-name`
    * `--username` instead of `--dbuser` and `-U` instead of `-u`
    * `--password` instead of `--dbpass`
- Changed defaults
    * `--resource-dir` defaults to `resources/migrations` instead of `db/migrations`

### Changes
- Updated to GraalVM 20.2.0.
- No verbose logging defaults.
- Bumped deps.

### Added
- Native image capability to connect via TLS.
- More command line options checks.

## [0.3.0] - 2020-08-11
### Changed
- Updated to GraalVM 20.1.0.
- Based on the official GraalVM CE docker image.
- Documentation updates.

## [0.2.1] - 2020-03-09
### Changed
- Made compatible with Java 11 and Clojure 10.0.2-alpha1.
- Updated to GraalVM 19.3.1.
- Bumped deps.

## [0.2.0] - 2019-12-26
The first stable release based on GraalVM 19.3.0.

## [0.2.0] - private version

Initial code drop.