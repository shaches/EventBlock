# Server Integration Tests

This document describes how to run server integration tests for the Oneblock plugin using TestContainers.

## Overview

Server integration tests use TestContainers to spin up real Docker containers for testing plugin behavior in a realistic environment:

- **MySQL Container**: Tests database connectivity and persistence
- **Spigot Server Container**: Tests plugin lifecycle, event handling, and block generation (future)

These tests are slower than unit tests (30-60 seconds per container startup) and require Docker, so they are disabled by default.

## Prerequisites

### Docker Installation

You must have Docker Desktop or Docker daemon running on your machine:

- **Windows**: Install [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop)
- **macOS**: Install [Docker Desktop for Mac](https://www.docker.com/products/docker-desktop)
- **Linux**: Install Docker Engine following the [official documentation](https://docs.docker.com/engine/install/)

Verify Docker is running:
```bash
docker ps
```

## Running Server Integration Tests

### Run All Integration Tests (Fast)
```bash
mvn verify
```
This runs only the fast database integration tests (DatabaseManagerIT) using H2 in-memory database.

### Run Server Tests with TestContainers
```bash
mvn verify -Pserver-tests
```
This enables TestContainers-based server integration tests, including MySQL container tests. The project uses the official Testcontainers artifacts from Maven Central. Publishing should not vendor or replace Testcontainers.

### Run Only Server Tests (Skip Unit Tests)
```bash
mvn verify -Pserver-tests -DskipUnitTests
```

### Run with Verbose Output
```bash
mvn verify -Pserver-tests -X
```

## Test Files

### DatabaseManagerIT
Tests DatabaseManager with real H2 and MySQL databases:
- Database initialization
- Save/load operations
- Configuration validation
- Connection handling

### OneblockServerIT
Tests plugin with real server environment (currently basic):
- MySQL container startup verification
- Future: Plugin lifecycle, event handling, block generation

## Troubleshooting

### Docker Daemon Not Running
**Error**: `Cannot connect to the Docker daemon`

**Solution**: Start Docker Desktop or Docker daemon before running tests.

### Container Startup Timeout
**Error**: Tests timeout waiting for container startup

**Solution**:
- Increase Docker memory allocation (Docker Desktop settings)
- Check for conflicting ports
- Ensure sufficient disk space

### Port Conflicts
**Error**: Port already in use (25565 for Spigot, 3306 for MySQL)

**Solution**:
- Stop other Minecraft servers or MySQL instances
- TestContainers automatically handles port mapping, but conflicts can occur

### Windows Specific Issues
Testcontainers Docker detection can be unreliable on some Windows/WSL2 setups even when `docker ps`
works from the shell. If server tests fail locally on Windows, run the normal unit/build commands
without `-Pserver-tests` and use Linux/macOS or CI with native Docker for container verification.

**Error**: `java.nio.file.InvalidPathException`

**Solution**: Ensure your project path doesn't contain special characters that Docker doesn't support.

## Cleanup

TestContainers automatically cleans up containers after tests complete. However, if tests are interrupted:

```bash
# Remove all stopped containers
docker container prune

# Remove all unused images
docker image prune
```

## CI/CD Considerations

Server integration tests are **not** run in CI/CD pipelines by default because:
- GitHub Actions runners don't have Docker pre-installed
- Tests are too slow for CI (30-60 seconds per test)
- Requires additional CI configuration for Docker

To enable in CI, you would need:
1. GitHub Actions runner with Docker support
2. Increased timeout limits
3. Proper Docker-in-Docker configuration

## Development Workflow

When developing new server integration tests:

1. Write the test in `OneblockServerIT.java`
2. Use `@EnabledIf("testcontainersEnabled")` to disable by default
3. Run with `-Pserver-tests` to test locally
4. Verify cleanup happens in `@AfterEach`
5. Keep tests focused on critical paths (avoid testing everything)

## Future Enhancements

The current implementation provides a foundation for:
- Spigot server container with plugin loading
- RCON-based server control
- Event simulation and verification
- Block generation testing
- Full plugin lifecycle testing

These require significant additional implementation due to the complexity of:
- Building and copying plugin JAR into container
- Waiting for server startup
- RCON or socket-based communication
- Test data setup and cleanup
