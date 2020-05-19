# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](https://semver.org/).

## 4.0.0

* Breaking change: Dropped Node.js 8 support. Node.js 10 or greater is now required.
* Breaking change: Always remove empty line before the first property if this property has any `emptyLineBefore*` option targeting it in `properties-order`. Even if option set to `always` empty line before the first property will be removed.
* Fixed false positives for `emptyLineBeforeUnspecified`.

## 3.1.1

* Added `stylelint@11` as a peer dependency.

## 3.1.0

* Added `emptyLineBefore: "threshold"` option, and related options (`emptyLineMinimumPropertyThreshold`, `emptyLineBeforeUnspecified: "threshold"`) to `properties-order`.

## 3.0.1

* Fixed `properties-order` not report warnings, if autofix didn't fix them.
* Fixed `properties-alphabetical-order` now puts shorthands before their longhand forms even if that isn't alphabetical to avoid broken CSS. E. g. `border-color` will be before `border-bottom-color`.

## 3.0.0

* Dropped Node.js 6 support. Node.js 8.7.0 or greater is now required.
* Removed stylelint@9 as a peer dependency. stylelint 10 or greater is now required.
* Added `emptyLineBeforeUnspecified` option for `properties-order`.

## 2.2.1

* Fixed false negatives with `noEmptyLineBetween` in combination with the `order: "flexible"`.

## 2.2.0

* Added `noEmptyLineBetween` for groups in `properties-order`.
* Added `stylelint@10` as a peer dependency.

## 2.1.0

* Added _experimental_ support for HTML style tag and attribute.
* Added _experimental_ support for CSS-in-JS.

## 2.0.0

This is a major release, because this plugin requires stylelint@9.8.0+ to work correctly with Less files.

* Added optional groupName property for properties-order.
* Adopted `postcss-less@3` parser changes, which is dependency of `stylelint@9.7.0+`.
* Fixed incorrect fixing when properties order and empty lines should be changed at the same time.

## 1.0.0

* Removed `stylelint@8` as a peer dependency.

## 0.8.1

* Add `stylelint@9.0.0` as a peer dependency.

## 0.8.0

* Breaking change: Dropped Node.js 4 support. Use Node.js 6 or newer.
* Changed: `order` and `properties-order` will no longer autofix proactively. If there no violations would be reported with autofix disabled, then nothing will be changed with autofix enabled. Previously, there were changes to `flexible` properties order ([#49](https://github.com/hudochenkov/stylelint-order/issues/49)) or to the order of content within declaration blocks ([#51](https://github.com/hudochenkov/stylelint-order/issues/51)).

## 0.7.0

* Specified `stylelint` in `peerDependencies` rather in `dependencies`. Following [stylelint's plugin guide](https://github.com/stylelint/stylelint/blob/master/docs/developer-guide/plugins.md#peer-dependencies).

## 0.6.0

* Migrated to `stylelint@8.0.0`.

## 0.5.0
* Added autofixing for every rule! Please read docs before using this feature, because each rule has some caveats. stylelint 7.11+ is required for this feature.
* Removed SCSS nested properties support.
* Removed property shortcuts in `properties-order`. Before this version it was possible to define only e.g. `padding` and it would define position for all undefined `padding-*` properties. Now every property should be explicitly defined in a config.
* Removed deprecation warnings:
	* `declaration-block-order`
	* `declaration-block-properties-order`
	* `declaration-block-properties-alphabetical-order`
	* `declaration-block-properties-specified-order`
	* `declaration-block-property-groups-structure`

## 0.4.4
* Fixed false negative for blockless at-rules in `order`.

## 0.4.3
* Fixed regression in `properties-order` introduced in 0.4.2.

## 0.4.2
* Fixed: `order` and `properties-order` weren't recognize SCSS nested properties as declarations.

## 0.4.1
* Fixed `properties-order` bug, when non-standard declaration is following after a standard one

## 0.4.0
* Removed `declaration-block-properties-specified-order`. Instead use `properties-order` rule.
* Removed `declaration-block-property-groups-structure`. Instead use `properties-order` rule.
* Renamed `declaration-block-order` to `order`
* Renamed `declaration-block-properties-alphabetical-order` to `properties-alphabetical-order`
* Added `properties-order` rule. It combines removed `declaration-block-properties-specified-order`, `declaration-block-property-groups-structure`, and now support flexible order. Basically it's like [`declaration-block-properties-order` in stylelint 6.5.0](https://github.com/stylelint/stylelint/tree/6.5.0/src/rules/declaration-block-properties-order), but better :)

## 0.3.0
* Changed: Breaking! `declaration-block-property-groups-structure` now uses `declaration-block-properties-specified-order` rather stylelint's deprecated `declaration-block-properties-order`. Flexible group order isn't supported anymore
* Added: `declaration-block-order` support new `rule` extended object, which have new `selector` option. Rules in order can be specified by their selector
* Added: New keyword `at-variables` in `declaration-block-order`
* Added: New keyword `less-mixins` in `declaration-block-order`

## 0.2.2
* Fixed tests for `declaration-block-property-groups-structure` which were broken by previous fix ¯﻿\﻿_﻿(﻿ツ﻿)﻿_﻿/﻿¯

## 0.2.1
* Fixed incorrect severity level for `declaration-block-properties-order` which is called from `declaration-block-property-groups-structure`

## 0.2.0
* Breaking: Renamed `property-groups-structure` to `declaration-block-property-groups-structure`
* Added `declaration-block-properties-specified-order` rule
* Fixed unavailability of `declaration-block-properties-alphabetical-order` rule

## 0.1.0
* Initial release.
