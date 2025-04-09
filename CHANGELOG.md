# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

### Added

### Changed

## 1.1.6 - 2024-09-25

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
