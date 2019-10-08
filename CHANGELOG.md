# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased] - 2019-XX-YY

## [1.1.18] - 2019-10-08
### Fixed
- remove buggy uses of `.empty` in favour of `.isEmpty`, _cf._ [JENKINS-50863](https://issues.jenkins-ci.org/browse/JENKINS-50863)
Impacted methods: `getModulePropertiesForPlatform` & `updateProperties`.

## [1.1.17] - 2019-09-23
### Fixed
- `upsertFromDescriptor` compatibility with [Hesperides API v2019-09-20](https://github.com/voyages-sncf-technologies/hesperides/blob/2019-09-20/CHANGELOG.md#2019-09-20) : https://github.com/voyages-sncf-technologies/hesperides-jenkins-lib/pull/29
- bugfix for JenkinsHTTRequester: considering all 2XX HTTP status OK 
### Added
- `getTemplates` & `deleteTemplate`
### Changed
- `getTemplate` now requires a `moduleType` argument

## [1.1.16] - 2019-07-22
### Fixed
- Bug de MAJ des propriétés d'instance : add @NonCPS on method `selectModule` and `selectModules` - fix #27 - thanks @brunoMod !

## [1.1.15] - 2019-06-28
### Fixed
- Calling a log method that doesn't exist, now using `steps.echo`

## [1.1.14] - 2019-06-28
### Added
- added logs for version ID changes & `Set-Cookie` header - thanks @thomaslhostis !

## [1.1.13] - 2019-06-06
### Added
- we try to build a FQDN based on the Jenkins executor own domain name to set the default API URL

## [1.1.12] - 2019-02-06
### Changed
- Consistency case change on `isWorkingcopy` argument (`createModule`, `setPlatformModuleVersion`, `setPlatformModulesVersion`), now `isWorkingCopy`: #17 - thanks @benjaminrene !
- Replaced all the calls to buggy .empty: #20 - thanks @yann-soliman !
- Bugfix: replacing existing iterable properties during platform update: #22 - thanks @A---- !

## [1.1.11] - 2018-06-14
### Changed
- replaced `userRights` / `groupRights` optional args by `filePerms`

## [1.1.10] - 2018-06-14
### Added
- Generalizing optional `copyPropertiesForUpgradedModules` arg to `createPlatform` / `setPlatformVersion` / `setPlatformModulesVersion`
- Adding optional args `userRights` & `groupRights` to `createTemplate` & `updateTemplate`

### Fixed
- allowing `copyPropertiesForUpgradedModules` to be `false` in `setPlatformModuleVersion` & `updatePlatform` - thanks @mareths !

## [1.1.9] - 2018-06-14
### Added
- Adding `copyPropertiesForUpgradedModules` optional arg in `setPlatformModuleVersion` - thanks @mareths !

## [1.1.8] - 2018-06-04
### Added
- Adding `updatePropertiesForPlatform` method to update properties same way as you get it from `getModulePropertiesForPlatform` - thanks @dedalusium !

## [1.1.7] - 2018-04-17
### Added
- Adding `iterables_properties` support for the path-specific properties - thanks @yann-soliman !

## [1.1.6] - 2018-01-22
### Added
- Allow updating all modules having the same name inside a platform - thanks @victorsalaun & @GeoffreyMc !

## [1.1.5] - 2018-01-15
### Added
- `updateTemplate` - update a template for a given module working copy - thanks @pepcitron & @emartin !
- `upsertTemplate` - upsert (create or update) a template for a given module working copy - thanks @pepcitron & @emartin !
- `getTemplate` - retrieve a template by filename (or title) - thanks @pepcitron & @emartin !
- `upsertFromDescriptor` - upsert (create or update) module(s) and template(s) from a JSON descriptor file - thanks @pepcitron & @emartin !

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
