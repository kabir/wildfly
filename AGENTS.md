# WildFly

WildFly is a Jakarta EE application server. This is a large multi-module Maven project (60+ modules) written in Java.

## Build

```sh
# Full build (skipping tests)
./mvnw install -DskipTests

# Build a single module
./mvnw install -pl <module-name> -DskipTests

# Build a module and its dependencies
./mvnw install -pl <module-name> -am -DskipTests
```

Java 17+ is required (compiler release target is 17). Maven 3.9+ is used via the Maven wrapper (`./mvnw`).

## Tests

```sh
# Run tests for a single module
./mvnw test -pl <module-name>

# Integration tests use the maven-failsafe-plugin
./mvnw verify -pl <module-name>
```

The `testsuite/` directory contains integration and domain tests that run against a built server distribution.

## Project structure

- Each top-level directory is generally a subsystem module (e.g. `ejb3/`, `jpa/`, `clustering/`, `jaxrs/`)
- `build/` and `dist/` — server distribution assembly
- `ee-feature-pack/`, `galleon-pack/` — Galleon feature pack definitions
- `testsuite/` — integration test suites (domain, integration, preview, layers)
- `boms/` — Bill of Materials POMs

## Code style

- Apache License 2.0 (SPDX: `Apache-2.0`)
- Checkstyle is enforced during the build
- IDE formatter configs are in the [wildfly-core repo](https://github.com/wildfly/wildfly-core/tree/main/ide-configs)

## Documentation

Before making architectural changes or configuring subsystems, consult the WildFly documentation index:
- [WildFly Documentation Index](https://raw.githubusercontent.com/kabir/wildfly/ai-index/docs/src/main/asciidoc/llms.txt)

Fetch specific `.adoc` files linked in that index only when required for your task.

## Issue tracking

Issues are tracked in Jira: https://redhat.atlassian.net/browse/WFLY
