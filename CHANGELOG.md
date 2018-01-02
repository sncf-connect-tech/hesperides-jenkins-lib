# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).


## [1.1.5] -
### Added
- `updateTemplate` - update a template for a given module working copy
- `upsertTemplate` - upsert (create or update) a template for a given module working copy
- `getTemplate` - retrieve a template by filename (or title)
- `upsertFromDescriptor` - upsert (create or update) module(s) and template(s) from a JSON descriptor file

## [1.1.4] - 2017-11-04
### Added
- `getModuleVersions` - thanks @efouret !
- `updateInstanceProperties` private method - thanks @achoimet !
- `updateModuleProperties` private method - thanks @achoimet !
- `updatePathSpecificProperties` private method - thanks @achoimet !

### Fixed
- `updateProperties` the new `path:`-prefix introduced in 1.1.3 had a bug, cf. PR #4 - thanks @achoimet !
- `getAppInfo` method - thanks @efouret !

### Changed
- `updateProperties` now the method is easier to understand, she calls 3 others private methods - thanks @achoimet !


## [1.1.3] - 2017-10-11
### Added
- `updateProperties` now accepts properties paths to specify which module to update - thanks @achoimet !

### Fixed
- Using an application/json Content-Type with DELETE calls


## [1.1.2] - 2017-10-04
### Added
- Exposing `doesWorkingcopyExistForModuleVersion` & `doesReleaseExistForModuleVersion` methods - thanks @benjaminrene !

### Fixed
- Allowing `auth` to be null in `Hesperides` constructor
- Properly catching HTTP errors as exceptions with `JenkinsHTTRequester` - thanks @benjaminrene !


## [1.1.1] - 2017-09-14
### Added
- `deleteInstance` now supports a wildcard '*' value for its `instance` parameter


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
