# Code Quality

Forge Kit enforces strict quality gates in CI to ensure production readiness:

- Static analysis (PMD, SpotBugs, OWASP Dependency Check)
- Test coverage via [OpenClover](https://openclover.org/)
- Deterministic [unit and integration tests](https://github.com/get-forge/forge-kit/actions/workflows/01-build-test.yml)
- Conventional commits and semantic versioning via [Commitizen](https://commitizen-tools.github.io/commitizen/)

All checks must pass before release artifacts are published.

## Code Coverage

[![codecov](https://codecov.io/github/get-forge/forge-kit/graph/badge.svg?token=RP8Z2NWG9L)](https://codecov.io/github/get-forge/forge-kit)

Forge Kit maintains high test coverage to ensure reliability and maintainability. Coverage metrics are automatically generated and tracked for all modules.

![Code Coverage Sunburst](https://codecov.io/github/get-forge/forge-kit/graphs/sunburst.svg?token=RP8Z2NWG9L)

### Coverage Reports

- **Live Dashboard**: View detailed coverage reports, trends, and module breakdowns at [Codecov](https://app.codecov.io/github/get-forge/forge-kit)
- **Automated Generation**: Coverage is calculated weekly via the [code coverage workflow](.github/workflows/51-code-coverage.yml) using Clover instrumentation
- **Module-Level Tracking**: Coverage is tracked per module, allowing targeted improvements where needed

Coverage reports help maintain code quality standards and identify areas that may need additional test coverage.
