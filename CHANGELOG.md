# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [2.4.0](https://github.com/networknt/yaml-rule-plugin/tree/2.4.0) (2026-10-07)

**Commits:**

- upgrade to version 2.4.0 before release in master branch ([37e8f54](https://github.com/networknt/yaml-rule-plugin/commit/37e8f542f65db7f0cccb5af152ad175fd59f3a41)) (by Steve Hu)
- upgrade maven-javadoc  to 3.12.0 ([b440236](https://github.com/networknt/yaml-rule-plugin/commit/b4402368f5816f6156d30160af72701b4e8dd4d1)) (by Steve Hu)
- upgrade slf4j to 2.0.20 from 2.0.19 ([cf0dc3f](https://github.com/networknt/yaml-rule-plugin/commit/cf0dc3f1b075dce2704b677d7fdec0f883193ddd)) (by Steve Hu)
- upgrade jose4j to 0.9.7 from 0.9.6 ([60dfa94](https://github.com/networknt/yaml-rule-plugin/commit/60dfa94193da51874f2ed610fc3c4033e0b36393)) (by Steve Hu)
- upgrade jackson to 2.22.3 from 2.22.1 ([aa918aa](https://github.com/networknt/yaml-rule-plugin/commit/aa918aacf4703178ef8fa4ce28b3fcd33e389d0f)) (by Steve Hu)
- upgrade to version 2.3.8-SNAPSHOT after release in master branch ([9a84508](https://github.com/networknt/yaml-rule-plugin/commit/9a84508b95f4a5662b0d2d93a9918cd6bdde39c8)) (by Steve Hu)
- upgrade slf4j to 2.0.19 from 2.0.17 ([6c22095](https://github.com/networknt/yaml-rule-plugin/commit/6c2209593b07458d61a6cfdf2ceeb7808e77a238)) (by Steve Hu)
- upgrade maven-surefire to 3.6.0 ([f864234](https://github.com/networknt/yaml-rule-plugin/commit/f86423492c971300f8aca6b7e1752bfec507a1c2)) (by Steve Hu)
- upgrade logback to 1.6.3 from 1.5.37 ([77ab1d6](https://github.com/networknt/yaml-rule-plugin/commit/77ab1d6995d48ff5f160b2b9f68e68305a7823ae)) (by Steve Hu)
- Remove obsolete javadoc-packagelist-maven-plugin workaround ([c870166](https://github.com/networknt/yaml-rule-plugin/commit/c870166a9872cb4b8923a5c5d65e7f3940910e10)) (by Steve Hu)
- upgrade central-publishing-maven to 0.11.0 from 0.7.0 ([c5d75c9](https://github.com/networknt/yaml-rule-plugin/commit/c5d75c9bb52d6e9a4aac3129edf8de4100750965)) (by Steve Hu)
- upgrade maven-jar to 3.5.1 from 3.1.2 ([832782d](https://github.com/networknt/yaml-rule-plugin/commit/832782d811609a08bb0e4400e62ba312fead029f)) (by Steve Hu)
- fixes #140 update version and light-4j version to 2.3.8-SNAPSHOT ([fdfbc77](https://github.com/networknt/yaml-rule-plugin/commit/fdfbc77ab29f792a89695b782bd9c916ce20e2a9)) (by Steve Hu)

## [Unreleased]

### Added

### Changed

## 3.0.1 - 2026-08-28

### Added
- Added an integration test proving that legacy 2.0.1 rule bodies and action values execute unchanged through the header-replace plugin ([#138](https://github.com/networknt/yaml-rule-plugin/issues/138)).

### Security
- Hardened SOAP UsernameToken generation with a cryptographically secure 32-byte nonce and configurable SHA-256, SHA-384, or SHA-512 password digests; SHA-256 is now the default ([#135](https://github.com/networknt/yaml-rule-plugin/issues/135)).
- Removed password values from debug logging and removed nonce output from standard output.
- Replaced scanner-sensitive sample passwords in configuration comments with explicit placeholders ([#136](https://github.com/networknt/yaml-rule-plugin/issues/136)).

### Changed
- Upgraded `yaml-rule` from 2.0.1 to 3.0.1, which restores the legacy Java rule model and `IAction` contract withdrawn by `yaml-rule` 3.0.0.
- Raised the build target from Java 21 to Java 25.
- Migrated the test suite from JUnit 4 to JUnit 5 and upgraded Surefire and Failsafe to 3.5.2 and JaCoCo to 0.8.14 ([#134](https://github.com/networknt/yaml-rule-plugin/issues/134)).
- Upgraded Jackson to 2.22.1, with `jackson-annotations` 2.22 ([#137](https://github.com/networknt/yaml-rule-plugin/issues/137)).
- Upgraded `http-client` to 1.0.18 and Logback to 1.5.37.

## 1.1.10 - 2026-02-20

### Added

### Changed
- upgrade to light-4j 2.3.3


## 1.1.9 - 2026-02-05

### Added

### Changed
- upgrade to http-client 1.0.17
- upgrade to light-4j 2.3.2
- update logback to 1.5.26

## 1.1.7 - 2026-01-23

### Added

### Changed
- fixes #130 add project name to each sub repo
- fixes #128 add maven publish plugin
- fixes #127 update oss repos and upgrade jacoco to 0.8.12
- update http-client to 1.0.16
- upgrade to java 21
- upgrade maven-gpg to 3.2.7
- update http-client to 1.0.14


## 1.1.6 - 2025-04-09

### Added
- fixes #125 upgrade to yaml-rule 2.0.1

## 1.1.4 - 2024-09-25

### Added
- add an error message with stacktrace
- fixes #116 update to snapshot version with more trace logging
- fixes #117 add more debug info for both conquest and token transformers

## 1.1.4 - 2024-09-25

### Added
- fixes #114 token-transformer plugin swallow the exception (#115)

## 1.1.3 - 2024-09-20

### Added
- fixes #113 upgrade to light-4j 2.1.37 release version

## 1.1.2 - 2024-09-19

### Added
- update token transformer to return false and error message (#112)
- fixes #111 update a test case to fix the assertion

## 1.1.1 - 2024-09-09

### Added
- Shared Variable Resolve Read Fix #110 @KalevGonvick

## 1.1.0 - 2024-09-04

### Added
- TTL time unit configuration (#108) @KalevGonvick
- Token Grace Period (#107) Thanks @KalevGonvick
- fixes #104 getBytes defaults to UTF-8 encoding (#105)

## 1.0.30 - 2024-08-23

### Added
- handle the HTTP_1_1 explicitly as http-client default to HTTP2 (#103)


## 1.0.29 - 2024-08-21

### Added
- Expiration Fix + Documentation (#99) Thanks @KalevGonvick
- fixes #98 update test case and readme.md for body-encoder

## 1.0.28 - 2024-08-07

### Added
- remove duplicated modules in pom.xml files (#97)
- 91 token transformer refactor (#95) Thanks @KalevGonvick

## 1.0.27 - 2024-07-25

### Added
- remove duplicated modules in pom.xml files (#94)
- refactored transformer plugin (#92) Thanks @KalevGonvick


## 1.0.26 - 2024-07-19

### Added
- Add a plugin to transform request or response body to utf-8 encoding (#90)
- Upgrade to light-4j 2.1.35-SNAPSHOT and resolve dependencies (#89)


## 1.0.6 - 2023-06-07

### Added
- fixes #11 upgrade version to 1.0.2 and dependencies
