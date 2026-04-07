# Repository Guidelines

## Project Structure & Module Organization
This repository contains two independent Java codebases:
- `java-agent/zero-intrusion-monitor/`: Java Agent multi-module project (`agent`, `demo-app`) for zero-intrusion exception capture and async reporting.
- `analyze-platform/platform-server/`: Spring Boot analysis service (REST + JPA + H2 + OpenAPI).

Use standard Maven layout in each module:
- `src/main/java`: production code
- `src/main/resources`: runtime config (for example `application.yml`)
- `src/test/java`: tests
- `target/`: build outputs (do not edit manually)

## Build, Test, and Development Commands
Run commands from the corresponding project directory.

- Agent project build: `cd java-agent/zero-intrusion-monitor && mvn clean package -DskipTests`
- Agent project tests: `cd java-agent/zero-intrusion-monitor && mvn clean test`
- Build only agent module: `mvn -pl agent -am package`
- Run demo with agent:
  `java -javaagent:agent/target/agent-1.0-SNAPSHOT.jar=appName=Demo;packages=com.github.monitor.demo -jar demo-app/target/demo-app-1.0-SNAPSHOT.jar`
- Platform server run: `cd analyze-platform/platform-server && mvn spring-boot:run`
- Platform server tests/package: `mvn clean test` / `mvn clean package`

Windows helpers are available in `java-agent/*.bat` (`build.bat`, `run-demo.bat`, `verify-demo.bat`).

## Coding Style & Naming Conventions
- Encoding: UTF-8 (no BOM).
- Indentation: 4 spaces, no tabs.
- Naming: packages `lowercase`, classes `PascalCase`, methods/fields `camelCase`, constants `UPPER_SNAKE_CASE`.
- Keep methods small and focused (SRP); avoid speculative abstractions (YAGNI).
- Language baselines: Agent modules use Java 8; platform-server uses Java 17.

## Testing Guidelines
- Prefer JUnit-based tests under `src/test/java`.
- Naming: unit tests `*Test`; integration tests `*IT`.
- For agent changes, cover: no-report on success path, report on exception path, serializer guardrails (depth/cycle/size limits).
- For platform-server changes, cover controller-service-repository flow and request validation.

## Commit & Pull Request Guidelines
Git history is not available in this workspace snapshot, so follow Conventional Commits:
- `feat(agent): add trace context filter`
- `fix(platform-server): handle empty fingerprint`

PRs should include: scope, motivation, impacted modules, verification commands, and sample logs/API responses for behavior changes.

## Security & Configuration Tips
- Never commit secrets, tokens, or real endpoint credentials.
- Keep desensitization fields configured (for example `password,token`).
- Validate sampling, queue, and batch settings before production rollout.
