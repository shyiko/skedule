# Changelog
All notable changes to this project will be documented in this file.  
This project adheres to [Semantic Versioning](http://semver.org/).

## [0.3.0] - 2017-07-10

### Added
- `1 hours` = `hour`, `1 minutes` = `minute` aliases  
(in other words, `every minute ...` can be used instead of `every 1 minutes ...`).
- A shorthand for day/month intervals (e.g. `every mon-fri 09:00`, `1-7 of jun-aug 00:00`).

## [0.2.0] - 2017-07-10

### Changed
- [Schedule](src/main/java/com/github/shyiko/skedule/Schedule.java) interface to support nextOrSame iteration.

## 0.1.0 - 2017-07-09

[0.3.0]: https://github.com/shyiko/skedule/compare/0.2.0...0.3.0
[0.2.0]: https://github.com/shyiko/skedule/compare/0.1.0...0.2.0
