# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).


## [1.1.0] - 2017-08-16
Publication on Github


## [1.0.2] - 2017-08-16
### Changed
- the `groupId` & the main class name


## [1.0.2] - 2017-08-09
### Added
- this file
- `createInstance`/`deleteInstance`
- a new logo

### Changed
- moving all the pipelines API into `vars/hesperides.groovy`
- `HesperidesUtils` renamed into `Hesperides` + splitted the 500 lines class into `traits`
- no more hardcoded default credentials, now using `withCredentials` + a new `auth` parameter
- fully gradle-based packaging & publishing
- enforcing `Map args` parameters with `_required` validation
- systematically using `moduleName` instead of `module` when the variable is a `String`
