# Changelog

All notable changes to this project will be documented in this file. See [standard-version](https://github.com/conventional-changelog/standard-version) for commit guidelines.

## [2.2.0](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v2.1.3...v2.2.0) (2019-07-31)


### Features

* generate higher quality SourceMaps ([#408](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/408)) ([b1df135](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/b1df135))

### [2.1.3](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v2.1.2...v2.1.3) (2019-05-15)


### Bug Fixes

* disable parallel on WSL ([#403](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/403)) ([7619736](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/7619736))
* fallback for cache directory ([#401](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/401)) ([d117827](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/d117827))



<a name="2.1.2"></a>
## [2.1.2](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v2.1.1...v2.1.2) (2019-02-25)


### Bug Fixes

* invalidate cache after changing node version ([#396](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/396)) ([380bb54](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/380bb54))



<a name="2.1.1"></a>
## [2.1.1](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v2.1.0...v2.1.1) (2018-12-27)


### Bug Fixes

* don't crash when no extracted comments ([#387](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/387)) ([68ad71c](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/68ad71c))



<a name="2.1.0"></a>
# [2.1.0](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v2.0.1...v2.1.0) (2018-12-22)


### Bug Fixes

* `chunks` is a `Set` in webpack@5 ([#365](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/365)) ([bf36e21](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/bf36e21))
* catch `worker-farm` errors ([#380](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/380)) ([aa7a9fc](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/aa7a9fc))
* dedupe extracted comments ([#383](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/383)) ([beaf1ad](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/beaf1ad))
* more consistent cache ([#385](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/385)) ([f61439d](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/f61439d))
* regenerate `contenthash` when assets was minified ([#386](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/386)) ([65a30da](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/65a30da))


### Features

* `chunkFilter` option for filtering chunks ([#382](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/382)) ([1e58c99](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/1e58c99))



<a name="2.0.1"></a>
## [2.0.1](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v2.0.0...v2.0.1) (2018-09-18)


### Bug Fixes

* decrease minimum required `uglify-js` version ([#357](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/357)) ([dc9d038](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/dc9d038))



<a name="2.0.0"></a>
# [2.0.0](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.3.0...v2.0.0) (2018-09-14)


### Bug Fixes

* default extract comment condition is case insensitivity

### Features

* full coverage schema options validation
* enable inline optimization by default

### BREAKING CHANGES

* Switch back to [uglify-js](https://github.com/mishoo/UglifyJS2) (`uglify-es` is abandoned, if you need uglify ES6 code please use [terser-webpack-plugin](https://github.com/webpack-contrib/terser-webpack-plugin)).
* Changed function signature for `warningsFilter` option (now first argument is `warning`, second is `source`).
* Enforces `peerDependencies` of `"webpack": "^4.3.0`.
* Enforces `engines` of `"node": ">= 6.9.0 <7.0.0 || >= 8.9.0`



<a name="1.3.0"></a>
# [1.3.0](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.2.7...v1.3.0) (2018-08-14)


### Bug Fixes

* all nodejs globals available inside `minify` function ([#332](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/332)) ([24fe22b](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/24fe22b))
* **uglify/Runner:** `cpus` length in a chroot environment (`os.cpus()`) ([#341](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/341)) ([6d9c36f](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/6d9c36f))


### Features

* `cacheKeys` option ([#320](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/320)) ([249eef3](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/249eef3))
* support `minify` function option ([#325](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/325)) ([0174605](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/0174605))



<a name="1.2.7"></a>
## [1.2.7](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.2.6...v1.2.7) (2018-06-25)


### Bug Fixes

* no warning when `warningsFilter` return false ([#317](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/317)) ([cabee6c](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/cabee6c))



<a name="1.2.6"></a>
## [1.2.6](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.2.5...v1.2.6) (2018-06-20)


### Bug Fixes

* disable inline optimization by default ([#308](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/308)) ([6ab0918](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/6ab0918))
* don't use a worker farm unless more than one process is required ([#280](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/280)) ([3f0767b](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/3f0767b))
* better handle invalid source map ([#284](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/284)) ([dbebb3b](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/dbebb3b))
* perf regression with memory usage for source maps ([#305](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/305)) ([3ca5eaf](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/3ca5eaf))
* return missing important information for uglify warnings ([#306](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/306)) ([4f50156](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/4f50156))



<a name="1.2.5"></a>
## [1.2.5](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.2.4...v1.2.5) (2018-04-18)


### Bug Fixes

* don't handle invalid source map ([#268](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/268)) ([693e45e](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/693e45e))


### Performance Improvements

* **sourcemaps:** reduce memory usage ([#276](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/276)) ([0b11134](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/0b11134))



<a name="1.2.4"></a>
## [1.2.4](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.2.3...v1.2.4) (2018-03-16)


### Bug Fixes

* **index:** correct default asset name `{RegExp}` (`options.test`) ([#259](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/259)) ([f9e55fe](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/f9e55fe))
* **index:** don't remove legal comments by default (`options.extractComments`) ([#250](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/250)) ([b31b574](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/b31b574))



<a name="1.2.3"></a>
## [1.2.3](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.2.2...v1.2.3) (2018-03-10)


### Bug Fixes

* **index:** correct `[name].js.LICENSE` file path (`options.extractComments`) ([#249](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/249)) ([430111c](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/430111c))
* **index:** relax default asset name `{RegExp}` (`options.test`) ([#251](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/251)) ([d27e822](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/d27e822))
* **package:** update `schema-utils` v0.4.3...0.4.5 (`dependencies`) ([#255](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/255)) ([4c0feb8](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/4c0feb8))
* **package:** update`cacache` v10.0.1...10.0.4 (`dependencies`) ([#253](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/253)) ([8b0c0cd](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/8b0c0cd))


### Performance Improvements

* **index:** switch to `md4` for content hashing ([#243](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/243)) ([14d2c4c](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/14d2c4c))



<a name="1.2.2"></a>
## [1.2.2](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.2.1...v1.2.2) (2018-02-24)


### Bug Fixes

* **index:** typo `buildMobule` => `buildModule` ([#242](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/242)) ([259adad](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/259adad))
* **package:** add `webpack >= v4.0.0` (`peerDependencies`) ([#239](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/239)) ([4c7f80a](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/4c7f80a))



<a name="1.2.1"></a>
## [1.2.1](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.2.0...v1.2.1) (2018-02-23)


### Bug Fixes

* **index:** `tapable` deprecation warnings (`webpack >= v4.0.0`) ([#238](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/238)) ([23b1b38](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/23b1b38))



<a name="1.2.0"></a>
# [1.2.0](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.1.8...v1.2.0) (2018-02-16)


### Features

* **uglify:** add missing `toplevel`, `nameCache`, `keep_classnames`, `keep_fnames` and `safari10` options (`uglifyOptions`) ([#229](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/229)) ([990de2a](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/990de2a))



<a name="1.1.8"></a>
## [1.1.8](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.1.7...v1.1.8) (2018-01-29)


### Bug Fixes

* **package:** use `prepare` instead of `prepublish` for release ([43ffa48](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/43ffa48))


### Reverts

* **README:** `mangle.props` instead of `mangle.properties` ([44b77e3](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/44b77e3))



<a name="1.1.7"></a>
## [1.1.7](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.1.6...v1.1.7) (2018-01-29)


### Bug Fixes

* **index:** reduce memory consumption (`cacheKey.hash`) ([#215](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/215)) ([2343274](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/2343274))


<a name="1.1.6"></a>
## [1.1.6](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.1.5...v1.1.6) (2018-01-05)


### Bug Fixes

* **index:** don't pass `compilation` as an argument to `UglifyJsPlugin.buildError` ([#201](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/201)) ([339ca10](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/339ca10))


### Reverts

* **package:** use exact `uglify-es` version (`dependencies`) ([#199](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/199)) ([#202](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/202)) ([426bafd](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/426bafd))



<a name="1.1.5"></a>
## [1.1.5](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.1.4...v1.1.5) (2017-12-27)


### Bug Fixes

* **package:** use exact `uglify-es` version (`dependencies`) ([#199](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/199)) ([2e2ed36](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/2e2ed36))



<a name="1.1.4"></a>
## [1.1.4](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.1.3...v1.1.4) (2017-12-14)


### Chores

* update `uglify-es` v3.2.0...v3.2.1 ([#190](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/190)) ([b356f74](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/b356f74))



<a name="1.1.3"></a>
## [1.1.3](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.1.2...v1.1.3) (2017-12-14)


### Refactoring

* use `serialize-javascript` package instead own implementatin ([#183](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/183)) ([8d7ebcd](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/8d7ebcd))



<a name="1.1.2"></a>
## [1.1.2](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.1.1...v1.1.2) (2017-11-30)


### Chores

* update `uglify-es` v3.1.3...3.2.0 ([#176](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/176)) ([3be7f62](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/3be7f62))



<a name="1.1.1"></a>
## [1.1.1](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.1.0...v1.1.1) (2017-11-23)


### Bug Fixes

* comments behavior (`options.uglifyOptions.comments/options.extractComments`) ([#174](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/174)) ([321c9f6](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/321c9f6))
* **index:** ensure to nullify `inputSourceMap` if `sourcemap` is unavailable (`devtool`) ([#169](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/169)) ([062ec72](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/062ec72))
* **index:** set `maxConcurrentCallsPerWorker` to 1 (`options.parallel`) ([#173](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/173)) ([07c57c1](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/07c57c1))



<a name="1.1.0"></a>
# [1.1.0](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.0.1...v1.1.0) (2017-11-19)


### Bug Fixes

* **index:** `exclude` is ignored (`options.exclude`)  ([#164](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/164)) ([ad17690](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/ad17690))
* pass of `{Boolean|Function}` parameters being ignored (`options.extractComments`) ([#168](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/168)) ([2c06f46](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/2c06f46))


### Features

* `include` option ([#167](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/167)) ([bbe0ef5](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/bbe0ef5))



<a name="1.0.1"></a>
## [1.0.1](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.0.0...v1.0.1) (2017-10-24)


### Bug Fixes

* **minify:** `nameCache` assignment (`uglifyOptions.nameCache`) ([#147](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/147)) ([af11e8e](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/af11e8e))



<a name="1.0.0"></a>
# [1.0.0](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.0.0-rc.0...v1.0.0) (2017-10-23)


### Features

* update to `uglify-es` ([#63](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/63)) ([1d62560](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/1d62560))

* add support for `parallelization` && `caching` (`options.parallel`) ([#77](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/77)) ([ee16639](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/ee16639))
* **index:** add `options` validation (`schema-utils`) ([#80](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/80)) ([f19b2de](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/f19b2de))


### Bug Fixes

* **deps:** cacache@10 with ISC licence ([#145](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/145)) ([9331034](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/9331034))
* typo "filterd" -> "filtered" ([#37](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/37)) ([238c373](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/238c373))
* **package:** mv uglify2 to `dependencies` && update `peerDependencies` ([#45](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/45)) ([93b0cd2](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/93b0cd2))
* **uglify:** use Compress API not ast.transform ([990f2e2](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/990f2e2))


### Code Refactoring

* apply `webpack-defaults` ([#35](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/35)) ([f6c5aa9](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/f6c5aa9))


### BREAKING CHANGES

* Enforces `peerDependencies` of `"webpack": ">= 3.0.0-rc.0 || ^3.0.0"`.
* Enforces `engines` of `"node": ">=4.3.0 < 5.0.0 || >= 5.10`



<a name="1.0.0-rc.0"></a>
# [1.0.0-rc.0](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.0.0-beta.2...v1.0.0-rc.0) (2017-10-23)



<a name="1.0.0-beta.3"></a>
# [1.0.0-beta.3](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.0.0-beta.2...v1.0.0-beta.3) (2017-09-29)



<a name="1.0.0-beta.2"></a>
# [1.0.0-beta.2](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.0.0-beta.1...v1.0.0-beta.2) (2017-07-21)


### Features

* add support for `parallelization` && `caching` (`options.parallel`) ([#77](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/77)) ([ee16639](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/ee16639))
* **index:** add `options` validation (`schema-utils`) ([#80](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/80)) ([f19b2de](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/f19b2de))



<a name="1.0.0-beta.1"></a>
# [1.0.0-beta.1](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v1.0.0-beta.0...v1.0.0-beta.1) (2017-07-06)


### Features

* update to `uglify-es` ([#63](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/63)) ([1d62560](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/1d62560))



<a name="1.0.0-beta.0"></a>
# [1.0.0-beta.0](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v0.1.4...v1.0.0-beta.0) (2017-06-29)


### Bug Fixes

* typo "filterd" -> "filtered" ([#37](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/37)) ([238c373](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/238c373))
* **package:** mv uglify2 to `dependencies` && update `peerDependencies` ([#45](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/45)) ([93b0cd2](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/93b0cd2))
* **uglify:** use Compress API not ast.transform ([990f2e2](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/990f2e2))


### Code Refactoring

* apply `webpack-defaults` ([#35](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/35)) ([f6c5aa9](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/f6c5aa9))


### BREAKING CHANGES

* Enforces `peerDependencies` of `"webpack": ">= 3.0.0-rc.0 || ^3.0.0"`.
* Enforces `engines` of `"node": ">=4.3.0 < 5.0.0 || >= 5.10`



<a name="0.4.6"></a>
## [0.4.6](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/compare/v0.1.4...v0.4.6) (2017-06-29)


### Bug Fixes

* typo "filterd" -> "filtered" ([#37](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/37)) ([238c373](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/238c373))
* **package:** mv uglify2 to `dependencies` && update `peerDependencies` ([#45](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/issues/45)) ([93b0cd2](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/93b0cd2))
* **uglify:** use Compress API not ast.transform ([990f2e2](https://github.com/webpack-contrib/uglifyjs-webpack-plugin/commit/990f2e2))
