# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](https://semver.org/).

## 5.0.1
* Fixed: `properties-order: "alphabetical"` now puts shorthands before their longhand forms even if that isn't alphabetical to avoid broken CSS. E. g. `border-color` will be before `border-bottom-color`.

## 5.0.0
* Dropped Node.js 6 support. Node.js 8.7.0 or greater is now required.

## 4.1.0
* Added: _Experimental_ support for HTML style tag and attribute (when used with [postcss-html](https://github.com/gucong3000/postcss-html) syntax).
* Added: _Experimental_ support for CSS-in-JS (when used with [postcss-jsx](https://github.com/gucong3000/postcss-jsx) syntax).

## 4.0.1
* Fixed: Incorrect sorting in Node.js 11, due recent change to `Array.sort()` in V8.
* Fixed: Logic for `at-variables` keyword for `order` now follows `postcss-less@3` parser.

## 4.0.0
* Breaking change: Dropped Node.js 4 support. Node.js 6.14.3 is the minimum supported version.
* Could be a breaking change: Plugin won't sort inside some at-rule (mostly Sass specific directives). Read more about [ignored at-rules](https://github.com/hudochenkov/postcss-sorting#ignored-at-rules).
* Added: `at-variables` keyword for `order`.

## 3.1.0
* Added: `throw-validate-errors` option.

## 3.0.2
* Changed: Show more helpful error messages for invalid config.

## 3.0.1
* Fixed: Inconsistent sorting for the same name properties in `properties-order`.

## 3.0.0
This is a cleanup release. Removed everything not related to ordering stylesheets. I recommend to use combination [stylelint 7.11+](https://stylelint.io/) with `--fix` option and [stylelint-order 0.5+](https://github.com/hudochenkov/stylelint-order) plugin instead of this plugin. Using combination above you'll receive linting and autofixing using only one tool!

* Removed options:
	* `at-rule-nested-empty-line-before`
	* `clean-empty-lines`
	* `comment-empty-line-before`
	* `custom-property-empty-line-before`
	* `declaration-empty-line-before`
	* `dollar-variable-empty-line-before`
	* `rule-nested-empty-line-before`
* Changes to `properties-order`:
	* Removed `emptyLineBefore` option.
	* Removed objects support in configuration. Use plain arrays instead.
* Updated to PostCSS 6.

## 2.1.0
* Added: `order` supports new `rule` extended object, which has new `selector` option. Rules in `order` can be specified by their selector.
* Fixed: Inconsistency with shared line comments.
* Fixed: Incorrect behaviour if `hasBlock` was set to `false` in extended at-rule object in `order`.

## 2.0.1
* Accept `null` for all options.

## 2.0.0
This release completely incompatible with the previous API. There is a lot new options. Please read the documentation.

[A migration guide](https://github.com/hudochenkov/postcss-sorting#migration-from-1x) is available.

### Added
* `sort-order` split into `order` and `properties-order`.
* Alphabetical order.
* At-rules can be checked if they have a block. E.g., `@include icon;` has no block.
* Custom properties and $-variables can be grouped separately.
* Empty lines for different node types:
	* `rule-nested-empty-line-before`
	* `at-rule-nested-empty-line-before`
	* `declaration-empty-line-before`
	* `custom-property-empty-line-before`
	* `dollar-variable-empty-line-before`
	* `comment-empty-line-before`
* `clean-empty-lines`: Remove all empty lines.

### Changed
* By default all options are disabled, and the plugin does nothing.
* Empty lines don't delete anymore if only “order” options are enabled.
* Dropped support for Node <4.

### Removed
* Predefined configs.
* Command comments `/* postcss-sorting: on/off */`
* `preserve-empty-lines-between-children-rules`
* `empty-lines-between-children-rules`
* `empty-lines-between-media-rules`
* `empty-lines-before-comment`
* `empty-lines-after-comment`

## 1.7.0
* Added `smacss` and `alphabetical` predefined configs.
* Under the hood refactoring.

## 1.6.1
* Fixed a regression in 1.6.0. Sort order with item like `@include media` didn't found rules like `@include media(">=desk") {}`.

## 1.6.0
* Add special comments to disable processing for some part in style sheet
* Support custom properties as $variable #27
* Fix an issue when there is a lot of comments in the end of a rule #24
* At-rule parameter now supports parentheses. For example, `@include mwp(1)`. (thanks, @Redknife) #29

## 1.5.0
* Add `empty-lines-before-comment` and `empty-lines-after-comment`, which add empty lines before and after a comment or a group of comments.

## 1.4.1
* Fix issue with a rule content starting with a comment and follow by a rule. Error happens if config has any option except `sort-order`. #21

## 1.4.0
* Added `preserve-empty-lines-between-children-rules`, which preserve empty lines between children rules and preserve empty lines for comments between children rules. #20

## 1.3.1
* Fix adding additional empty line if both `empty-lines-between-children-rules` and `empty-lines-between-media-rules` are not 0. #19

## 1.3.0
* Added `empty-lines-between-media-rules` option which set a number of empty lines between nested media rules. #16

## 1.2.3
* Fixed removing last comments in the rule.
* Fixed adding empty lines between children rules if there are comments between them.

## 1.2.2
* Fixed removing comments in rule if they are only children.
* Fixed removing of the first comment in the rule if it's not on separate line.

## 1.2.1
* Fixed comments wrong ordering and added better tests for it.

## 1.2.0
* Added `empty-lines-between-children-rules` option which set a number of empty lines between nested children rules. (thanks, @scalder27) #9

## 1.1.0
* [Sort prefixed properties](https://github.com/hudochenkov/postcss-sorting#prefixed-properties) without explicit specifying in config.
* Support for SCSS files if [postcss-scss](https://github.com/postcss/postcss-scss) used for parsing.

## 1.0.1
* Change .npmignore to not deliver unneeded files.

## 1.0.0
* Initial release.
