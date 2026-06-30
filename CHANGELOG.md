## v1.3.1 (2026-06-30)

### Fix

- **observability-aws**: gate AMP and X-Ray exporters at runtime

## v1.3.0 (2026-06-29)

### Feat

- **observability**: add AWS observability and SigV4 HTTP modules

## v1.2.1 (2026-06-29)

### Fix

- **ci**: run OWASP aggregate across reactor and trim suppressions

## v1.2.0 (2026-06-29)

### Feat

- **logging**: add forge-logging module and forge-logging-api

### Fix

- **ci**: restore full Quarkus BOM OWASP suppressions for forge-kit
- **ci**: restore Micrometer 1.16.5 OWASP suppression
- **ci**: trim OWASP suppressions for Quarkus 3.36.1 in forge-kit
- **ci**: exclude AWS key test fixture from Trufflehog scan
- **logging**: avoid Trufflehog false positive on AWS key test fixture
- **logging**: align forge-logging parent POM version with 1.1.1

## v1.1.1 (2026-06-20)

### Fix

- **throttle**: guard null Redis responses and run filter on worker pool

## v1.1.0 (2026-06-20)

### Feat

- **cache**: redis impl

### Fix

- **cz**: bump tag issue

### Refactor

- **github**: repository org transfer

## v1.0.9 (2026-04-02)

### Fix

- **deps**: update aws sdk v2 monorepo to v2.42.27 (#92)
- **deps**: update quarkus platform updates to v3.34.1 (#90)

## v1.0.8 (2026-04-02)

### Fix

- **deps**: update aws sdk v2 monorepo to v2.42.15 (#87)
- **deps**: update aws sdk v2 monorepo to v2.42.14 (#86)
- **deps**: update aws sdk v2 monorepo to v2.42.13 (#85)
- **deps**: update aws sdk v2 monorepo to v2.42.12 (#84)
- **deps**: update dependency org.projectlombok:lombok to v1.18.44 (#82)
- **deps**: update quarkus platform updates to v3.32.3 (#80)
- **deps**: update aws sdk v2 monorepo to v2.42.11 (#81)
- **deps**: update dependency com.bucket4j:bucket4j_jdk17-core to v8.17.0 (#79)
- **deps**: update aws sdk v2 monorepo to v2.42.9 (#78)

## v1.0.7 (2026-03-12)

### Fix

- **deps**: update aws sdk v2 monorepo to v2.42.8 (#76)
- **deps**: update aws sdk v2 monorepo to v2.42.7 (#75)
- **deps**: update dependency io.quarkus.platform:quarkus-bom to v3.32.2 (#74)
- **deps**: update aws sdk v2 monorepo to v2.42.6 (#73)
- **deps**: update aws sdk v2 monorepo to v2.42.5 (#71)
- **deps**: update aws sdk v2 monorepo to v2.42.4 (#66)
- **deps**: update aws sdk v2 monorepo to v2.42.3 (#65)
- **deps**: update aws sdk v2 monorepo to v2.42.2 (#64)
- **deps**: update dependency io.quarkus.platform:quarkus-bom to v3.32.1 (#63)
- **deps**: update aws sdk v2 monorepo to v2.42.1 (#62)
- **deps**: update aws sdk v2 monorepo to v2.42.0 (#60)
- **deps**: update aws sdk v2 monorepo to v2.41.34 (#59)

## v1.0.6 (2026-02-20)

### Fix

- **deps**: update aws sdk v2 monorepo to v2.41.33 (#54)
- **deps**: update quarkus platform updates (#53)
- **deps**: update aws sdk v2 monorepo to v2.41.32 (#52)
- **deps**: update aws sdk v2 monorepo to v2.41.31 (#51)
- **deps**: update aws sdk v2 monorepo to v2.41.30 (#50)

## v1.0.5 (2026-02-16)

### Fix

- **bump**: ditching the overwriting of gpg signature
- **bump**: commitizen version issue - now it's pinned there's a change and we need to provide git commit message
- **deploy**: skipping tests during deploy phase
- **deps**: update aws sdk v2 monorepo to v2.41.29 (#47)
- **deps**: update aws sdk v2 monorepo to v2.41.28 (#46)
- **deps**: update dependency io.quarkus.platform:quarkus-bom to v3.31.3 (#45)
- **deps**: update aws sdk v2 monorepo to v2.41.27 (#44)
- **deps**: update aws sdk v2 monorepo to v2.41.26 (#43)
- **deps**: update aws sdk v2 monorepo to v2.41.25 (#42)
- **deps**: update aws sdk v2 monorepo to v2.41.24 (#40)
- **deps**: update aws sdk v2 monorepo to v2.41.23 (#39)
- **deps**: update dependency io.quarkus.platform:quarkus-bom to v3.31.2 (#38)
- **deps**: update aws sdk v2 monorepo to v2.41.22 (#37)
- **deps**: update aws sdk v2 monorepo to v2.41.21 (#35)
- **deps**: update dependency com.bucket4j:bucket4j_jdk17-core to v8.16.1 (#34)
- **deps**: update aws sdk v2 monorepo to v2.41.20 (#33)

## v1.0.4 (2026-02-01)

### Fix

- **deps**: update aws sdk v2 monorepo to v2.41.19 (#28)

## v1.0.3 (2026-02-01)

### Fix

- **deps**: update quarkus platform updates to v3.31.1 (#26)
- **deps**: update aws sdk v2 monorepo to v2.41.17 (#25)
- **deps**: update aws sdk v2 monorepo to v2.41.16 (#24)
- **deps**: update aws sdk v2 monorepo to v2.41.15 (#23)

### Refactor

- **reflection**: bunch of refactoring to support duplicate reflection routines in both forge-kit and -platform

## v1.0.2 (2026-01-26)

### Fix

- **config**: minor config change to suppress default interceptor messages
- **deps**: update quarkus platform updates (#19)
- **deps**: update aws sdk v2 monorepo to v2.41.14 (#20)

## v1.0.1 (2026-01-25)

### Fix

- **deps**: update aws sdk v2 monorepo to v2.41.13 (#16)
- **deps**: update quarkus platform updates (#15)
- **deps**: update aws sdk v2 monorepo to v2.41.12 (#14)
- **deps**: update aws sdk v2 monorepo to v2.41.11 (#13)

## v1.0.0 (2026-01-20)

### Feat

- **bump**: First public release of forge-kit, providing infrastructure building blocks for Java/Quarkus services
    - forge-common
    - forge-health-aws
    - forge-metrics
    - forge-security
    - forge-throttle
