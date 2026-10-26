# Contributing

Thanks for your interest in contributing.

## Reporting bugs and requesting features

Use [GitHub Issues](https://github.com/thingsboard/thingsboard/issues) for bug reports and
feature requests. Search existing issues first to avoid duplicates.

## Reporting security vulnerabilities

Do **not** open a public issue for a security vulnerability. Follow the process in
[security.md](./security.md) instead.

## Prerequisites

- JDK 25
- Maven (a recent 3.9.x release)
- Docker, for the tests that use [Testcontainers](https://www.testcontainers.org/)

Node.js and Yarn are provisioned automatically by the Maven build (`frontend-maven-plugin` in
`ui-ngx`); you don't need to install them yourself unless you're iterating on the UI directly
with `yarn`.

## Repository layout

This is a multi-module Maven reactor. The top-level modules:

| Module        | Contents                                                              |
|---------------|------------------------------------------------------------------------|
| `common`      | Shared server code: data models, messaging, utilities                |
| `dao`         | Persistence layer (SQL DAOs, entity services)                        |
| `rule-engine` | Rule engine core and built-in rule nodes                             |
| `transport`   | Device transports (MQTT, HTTP, CoAP, LwM2M, SNMP)                    |
| `integration` | Third-party platform integrations and the integration executor       |
| `edqs`        | Entity Data Query Service                                            |
| `application` | The main server application, assembling the modules above            |
| `ui-ngx`      | The Angular web UI                                                    |
| `msa`         | Microservices packaging: per-service Docker images and the black-box test suite |
| `rest-client` | Java REST client for the platform API                                |
| `netty-mqtt`  | Standalone Netty-based MQTT client library                           |
| `tools`       | Developer and CI tooling                                             |
| `packaging`   | Gradle plugin, run from Maven, backing the deb/rpm/zip package builds |
| `monitoring`  | Standalone monitoring service                                        |
| `migrator`    | Standalone data migration application                                |
| `report`      | Report generation service                                            |

## Building

```bash
mvn clean install -DskipTests -Dpkg.skip=true
```

`-Dpkg.skip=true` skips building the deb/rpm/zip distribution artifacts, which you don't need
for day-to-day development. It's a shorthand for four finer-grained flags:

| Flag                       | Skips                                        | Safe for the unit and integration tests?                      |
|-----------------------------|-----------------------------------------------|-----------------------------------------------------------------|
| `-Dpkg.skip=true`           | All of the below (boot jar + deb + rpm + zip) | Yes                                                              |
| `-Dpkg.skip.bootjar=true`   | `spring-boot:repackage` (`*-boot.jar`)        | Yes — but the boot jar is Gradle `buildDeb`'s input, so this drops the `.deb` too |
| `-Dpkg.skip.deb=true`       | Gradle `buildDeb` + Maven `attach-artifact`   | Yes — the `msa/*` copy of the DEB is bound to the same phase and is skipped with it |
| `-Dpkg.skip.rpm=true`       | Gradle `buildRpm`                             | Yes — no test depends on the RPM                                 |
| `-Dpkg.skip.zip=true`       | `maven-assembly-plugin` Windows ZIP           | Yes — no test depends on the ZIP                                 |

None of them are safe when you are going to build the `msa/*` Docker images or run the black-box
suite: those need the `.deb`, and a build that skipped it fails on the missing artifact.

Each source file needs the project's license header. Apply it automatically before committing:

```bash
mvn license:format
```

Building the UI on its own, from `ui-ngx`:

```bash
yarn install
yarn start      # dev server with live reload
yarn build:prod # production build
yarn lint
```

## Running tests

For a single module, scope the build with `-pl`/`-am` instead of rebuilding the whole reactor:

```bash
mvn -pl <module> -am -DskipTests -Dpkg.skip=true test-compile
```

To run the full backend test suite in parallel with bounded memory usage:

```bash
export MAVEN_OPTS="-Xmx1024m"
export NODE_OPTIONS="--max_old_space_size=4096"
export SUREFIRE_JAVA_OPTS="-Xmx1200m -Xss256k -XX:+ExitOnOutOfMemoryError"

# Compile and install all modules, skipping packaging artifacts not needed for tests
mvn clean install -T6 -DskipTests -Dpkg.skip=true

mvn test -pl='!application,!dao,!ui-ngx,!msa/js-executor,!msa/web-ui' -T4
mvn test -pl='msa/js-executor'
mvn test -pl dao -Dparallel=packages -DforkCount=4

mvn test -pl application -Dtest='!**/nosql/**,org.thingsboard.server.controller.**'      -DforkCount=6 -Dparallel=classes  -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5
mvn test -pl application -Dtest='!**/nosql/**,org.thingsboard.server.edge.**'            -DforkCount=4 -Dparallel=packages -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5
mvn test -pl application -Dtest='!**/nosql/**,org.thingsboard.server.service.**'         -DforkCount=6 -Dparallel=packages -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5
mvn test -pl application -Dtest='!**/nosql/**,org.thingsboard.server.transport.mqtt.**'  -DforkCount=6 -Dparallel=classes  -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5
mvn test -pl application -Dtest='!**/nosql/**,org.thingsboard.server.transport.coap.**'  -DforkCount=6 -Dparallel=classes  -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5
mvn test -pl application -Dtest='!**/nosql/**,org.thingsboard.server.transport.lwm2m.**' -DforkCount=6 -Dparallel=packages -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5
mvn test -pl application -Dtest='**/*TestSuite.java'                                     -DforkCount=4 -Dparallel=classes  -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5

# the rest of the application tests
mvn test -pl application -Dtest='
!**/nosql/**,
!org.thingsboard.server.controller.**,
!org.thingsboard.server.edge.**,
!org.thingsboard.server.service.**,
!org.thingsboard.server.transport.mqtt.**,
!org.thingsboard.server.transport.coap.**,
!org.thingsboard.server.transport.lwm2m.**,
!**/*TestSuite.java
' -DforkCount=6 -Dparallel=packages -Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5
```

None of this is required for a small, focused change — scope your `mvn test` to the module and
package you touched. This is here for running the full suite quickly when you need to.

### Testcontainers and the Docker API version

Several `dao` and `application` tests start real databases and brokers via Testcontainers. If
they fail to start with an "unsupported Docker API version" error, your Docker Engine's API is
newer than what the Testcontainers version in this repo expects. Work around it by pinning a
minimum API version in Docker's daemon config (`/etc/docker/daemon.json`, or via Docker Desktop's
UI on macOS) and restarting Docker:

```json
{
  "min-api-version": "1.32"
}
```

If Testcontainers can't find Docker at all, try removing its cached properties file so it gets
recreated on the next run:

```bash
rm ~/.testcontainers.properties
```

## Submitting changes

Open a pull request against `master`. Fill out the pull request template, and make sure:

- New or changed behavior has test coverage.
- `mvn license:format` has been run, so new files carry the correct license header. Do not add
  your own copyright or author lines to file headers.
- The change builds cleanly: `mvn -pl <module> -am -DskipTests -Dpkg.skip=true test-compile` at minimum for the modules you touched.
- You have signed the Contributor License Agreement (see below).

### Pull request titles

The release notes are generated from pull request titles, word for word. Write the title for the
people who read them — users and customers, not reviewers:

- **Describe the change in behavior, as a user sees it.** Name the feature or screen and what now
  works differently. Leave out class names, exceptions, library versions and other internals a
  user can't relate to; they belong in the description.
- **Use the past tense, starting with a verb** such as `Fixed`, `Added`, `Updated`, `Improved`,
  `Removed`, `Renamed` or `Made`. Not `Fix`, `Add` or `Update`.
- **Leave out code-scope prefixes and suffixes** such as `fix(ui):`, ticket keys or branch names
  (`(lts-4.3)`). A product-area prefix a user recognizes is fine, followed by the verb:
  `IoT Hub: added filtering of the catalogue by the creator's verification status`.

The section an entry appears under (`UI`, `Bug: UI`, `Edge`, …) comes from the pull request's
labels, not its title, so don't repeat the area or the change type in the title. Changes users
don't see — CI, build, docs, refactoring, a second backport of the same fix — get the
`Ignore for release` label and stay out of the release notes.

| Instead of                                                     | Write                                                                                          |
|----------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| `fix(ui): update moment-timezone to 0.6.5 (tzdata 2026e)`      | `Updated UI time zone data to reflect 2026 daylight saving time changes in Canada, Morocco and Moldova` |
| `Warn that updating a solution template recreates everything it installed` | `Added a warning that updating a solution template recreates everything it installed` |
| `Fix empty Provisioning step when navigating the device install wizard via the stepper header` | `Fixed empty Provisioning step when navigating the device install wizard via the stepper header` |

## Contributor License Agreement

Before we can merge your pull request, you need to sign the ThingsBoard
[Contributor License Agreement](https://cla-assistant.io/thingsboard/thingsboard) (CLA). On your
first pull request a bot posts a link: sign in with GitHub and accept the agreement. You sign once,
and it covers all your contributions to this repository, including those made before you signed.

You keep the copyright in your contributions. The CLA grants ThingsBoard, Inc. a license to use and
distribute them, including under this project's license (see [LICENSE](./LICENSE)) and under
commercial licenses.

If you contribute on behalf of your employer, your employer may need to sign the Corporate CLA
instead. Contact info@thingsboard.io.
