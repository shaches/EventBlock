# Contributing to EventBlock

Thanks for your interest in improving EventBlock. This guide covers the current
project layout, local tooling, and verification commands expected before a pull
request is merged.

## License

EventBlock is licensed under the GNU Affero General Public License v3.0
(AGPLv3). All contributions must be compatible with this license. By submitting
a pull request, you agree that your contribution is licensed under AGPLv3.

The original OneBlock codebase by MrMarL was licensed under MIT. That original
copyright and license text are preserved in `LICENSE-upstream`. All
modifications and the independent EventBlock distribution are under AGPLv3.

## Project layout

```text
src/main/java/oneblock/        Java source tree
src/main/resources/            plugin.yml and shipped YAML resources
src/test/java/oneblock/        JUnit 5 test suite
docs/                          Supporting development and test documentation
```

Keep Java sources under `src/main/java`, shipped plugin resources under
`src/main/resources`, and tests under `src/test/java`.

## Tooling requirements

| Tool | Version |
| --- | --- |
| JDK | 21 |
| Maven | 3.9+ |
| Git | 2.40+ |

Any system-installed Maven 3.9+ works. `JAVA_HOME` must point to a JDK 21
installation.

## Common commands

```bash
# Run unit tests
mvn -B test

# Check Java formatting
mvn -B spotless:check

# Produce the shaded plugin jar under target/
mvn -B -DskipTests clean package

# Run the CI gate used by GitHub Actions
mvn -B -Pci verify
```

Optional slow or environment-sensitive gates:

```bash
# OWASP dependency-check; requires network access and benefits from an NVD API key/cache
mvn -B -DskipTests -Psecurity-scan verify

# PIT mutation testing
mvn -B -Pmutation-test verify
```

Optional Docker-backed server integration tests are documented in
[`docs/server-integration-tests.md`](docs/server-integration-tests.md):

```bash
mvn -B -Pserver-tests verify
```

## Pull request expectations

- Keep the existing test suite green.
- Add or update tests for changed business logic.
- Keep user-facing configuration and permission keys backward compatible unless
  the PR explicitly documents a breaking change.
- Do not commit local build outputs, downloaded tools, logs, temporary worktrees,
  or generated artifacts.
- Document user-visible changes in `CHANGELOG.md`.

## Release notes

Release-specific change summaries belong in `CHANGELOG.md`, the pull request
body, and the commit message. `CONTRIBUTING.md` should stay focused on durable
contributor workflow.