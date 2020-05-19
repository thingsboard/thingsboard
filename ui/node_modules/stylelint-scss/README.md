# stylelint-scss

[![NPM version](https://img.shields.io/npm/v/stylelint-scss.svg)](https://www.npmjs.com/package/stylelint-scss)
[![Build Status](https://github.com/kristerkari/stylelint-scss/workflows/Tests/badge.svg)](https://github.com/kristerkari/stylelint-scss/actions?workflow=Tests)
[![Coverage Status](https://img.shields.io/coveralls/github/kristerkari/stylelint-scss/master.svg)](https://coveralls.io/github/kristerkari/stylelint-scss?branch=master)
[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://egghead.io/courses/how-to-contribute-to-an-open-source-project-on-github)
[![Downloads per month](https://img.shields.io/npm/dm/stylelint-scss.svg)](https://npmcharts.com/compare/stylelint-scss)

A collection of SCSS specific linting rules for [stylelint](https://github.com/stylelint/stylelint) (in a form of a plugin).

## Purpose

stylelint by itself supports [SCSS syntax](https://stylelint.io/user-guide/css-processors#parsing-non-standard-syntax) very well (as well as other preprocessors' syntaxes). Moreover, it introduces some specific rules that can be used to lint SCSS, e.g. to limit [`nesting`](https://stylelint.io/user-guide/rules/max-nesting-depth), control the way [`@-rules`](https://stylelint.io/user-guide/rules#at-rule) are written. Yet stylelint is in general focused on standard CSS.

stylelint-scss introduces rules specific to SCSS syntax. That said, the rules from this plugin can be used with other syntaxes, like Less or some PostCSS syntaxes. That's why the rules' names are not tied to SCSS only (`at-function-pattern` instead of `scss-function-pattern`).

The plugin follows stylelint's guidelines (about [rule names](https://stylelint.io/user-guide/about-rules), testing and [so on](https://github.com/stylelint/stylelint/tree/master/docs/developer-guide)).

## Installation and usage

stylelint-scss is a plugin for [stylelint](https://stylelint.io/user-guide), so it's meant to be used with it.

**Node.js v6 or newer** is required. That's because stylelint itself [doesn't support Node.js versions below 6](https://github.com/stylelint/stylelint/issues/2996#issuecomment-352663375).

First, install stylelint-scss (and stylelint, if you haven't done so yet) via NPM:

```
npm install stylelint stylelint-scss
```

Create the `.stylelintrc.json` config file (or open the existing one), add `stylelint-scss` to the plugins array and the rules you need to the rules list. All rules from stylelint-scss need to be namespaced with `scss`.

```json
{
  "plugins": [
    "stylelint-scss"
  ],
  "rules": {
    "scss/dollar-variable-pattern": "^foo",
    "scss/selector-no-redundant-nesting-selector": true,
    ...
  }
}
```

Please refer to [stylelint docs](https://stylelint.io/user-guide) for the detailed info on using this linter.

## List of rules

Here are stylelint-scss' rules, grouped by the [_thing_](http://apps.workflower.fi/vocabs/css/en) they apply to (just like in [stylelint](https://stylelint.io/user-guide/about-rules)).

Please also see the [example configs](./docs/examples/) for special cases.

### `@`-each

- [`at-each-key-value-single-line`](./src/rules/at-each-key-value-single-line/README.md): This is a rule that checks for situations where users have done a loop using map-keys and grabbed the value for that key inside of the loop.

### `@`-else

- [`at-else-closing-brace-newline-after`](./src/rules/at-else-closing-brace-newline-after/README.md): Require or disallow a newline after the closing brace of `@else` statements (Autofixable).
- [`at-else-closing-brace-space-after`](./src/rules/at-else-closing-brace-space-after/README.md): Require a single space or disallow whitespace after the closing brace of `@else` statements (Autofixable).
- [`at-else-empty-line-before`](./src/rules/at-else-empty-line-before/README.md): Require an empty line or disallow empty lines before `@`-else (Autofixable).
- [`at-else-if-parentheses-space-before`](./src/rules/at-else-if-parentheses-space-before/README.md): Require or disallow a space before `@else if` parentheses (Autofixable).

### `@`-extend

- [`at-extend-no-missing-placeholder`](./src/rules/at-extend-no-missing-placeholder/README.md): Disallow at-extends (`@extend`) with missing placeholders.

### `@`-function

- [`at-function-named-arguments`](./src/rules/at-function-named-arguments/README.md): Require named parameters in SCSS function call rule.
- [`at-function-parentheses-space-before`](./src/rules/at-function-parentheses-space-before/README.md): Require or disallow a space before `@function` parentheses (Autofixable).
- [`at-function-pattern`](./src/rules/at-function-pattern/README.md): Specify a pattern for Sass/SCSS-like function names.

### `@`-if

- [`at-if-closing-brace-newline-after`](./src/rules/at-if-closing-brace-newline-after/README.md): Require or disallow a newline after the closing brace of `@if` statements (Autofixable).
- [`at-if-closing-brace-space-after`](./src/rules/at-if-closing-brace-space-after/README.md): Require a single space or disallow whitespace after the closing brace of `@if` statements (Autofixable).
- [`at-if-no-null`](./src/rules/at-if-no-null/README.md): Disallow `null` in `@if` statements.

### `@`-import

- [`at-import-no-partial-leading-underscore`](./src/rules/at-import-no-partial-leading-underscore/README.md): Disallow leading underscore in partial names in `@import`.
- [`at-import-partial-extension`](./src/rules/at-import-partial-extension/README.md): Require or disallow extension in `@import` commands.
- [`at-import-partial-extension-blacklist`](./src/rules/at-import-partial-extension-blacklist/README.md): Specify blacklist of disallowed file extensions for partial names in `@import` commands.
- [`at-import-partial-extension-whitelist`](./src/rules/at-import-partial-extension-whitelist/README.md): Specify whitelist of allowed file extensions for partial names in `@import` commands.

### `@`-mixin

- [`at-mixin-argumentless-call-parentheses`](./src/rules/at-mixin-argumentless-call-parentheses/README.md): Require or disallow parentheses in argumentless `@mixin` calls (Autofixable).
- [`at-mixin-named-arguments`](./src/rules/at-mixin-named-arguments/README.md): Require named parameters in at-mixin call rule.
- [`at-mixin-parentheses-space-before`](./src/rules/at-mixin-parentheses-space-before/README.md): Require or disallow a space before `@mixin` parentheses (Autofixable).
- [`at-mixin-pattern`](./src/rules/at-mixin-pattern/README.md): Specify a pattern for Sass/SCSS-like mixin names.

### `@`-rule

- [`at-rule-conditional-no-parentheses`](./src/rules/at-rule-conditional-no-parentheses/README.md): Disallow parentheses in conditional @ rules (if, elsif, while).
- [`at-rule-no-unknown`](./src/rules/at-rule-no-unknown/README.md): Disallow unknown at-rules. Should be used **instead of** stylelint's [at-rule-no-unknown](https://stylelint.io/user-guide/rules/at-rule-no-unknown).

### `$`-variable

- [`dollar-variable-colon-newline-after`](./src/rules/dollar-variable-colon-newline-after/README.md): Require a newline after the colon in `$`-variable declarations (Autofixable).
- [`dollar-variable-colon-space-after`](./src/rules/dollar-variable-colon-space-after/README.md): Require or disallow whitespace after the colon in `$`-variable declarations (Autofixable).
- [`dollar-variable-colon-space-before`](./src/rules/dollar-variable-colon-space-before/README.md): Require a single space or disallow whitespace before the colon in `$`-variable declarations (Autofixable).
- [`dollar-variable-default`](./src/rules/dollar-variable-default/README.md): Require `!default` flag for `$`-variable declarations.
- [`dollar-variable-empty-line-before`](./src/rules/dollar-variable-empty-line-before/README.md): Require a single empty line or disallow empty lines before `$`-variable declarations (Autofixable).
- [`dollar-variable-no-missing-interpolation`](./src/rules/dollar-variable-no-missing-interpolation/README.md): Disallow Sass variables that are used without interpolation with CSS features that use custom identifiers.
- [`dollar-variable-pattern`](./src/rules/dollar-variable-pattern/README.md): Specify a pattern for Sass-like variables.

### `%`-placeholder

- [`percent-placeholder-pattern`](./src/rules/percent-placeholder-pattern/README.md): Specify a pattern for `%`-placeholders.

### `//`-comment

- [`double-slash-comment-empty-line-before`](./src/rules/double-slash-comment-empty-line-before/README.md): Require or disallow an empty line before `//`-comments (Autofixable).
- [`double-slash-comment-inline`](./src/rules/double-slash-comment-inline/README.md): Require or disallow `//`-comments to be inline comments.
- [`double-slash-comment-whitespace-inside`](./src/rules/double-slash-comment-whitespace-inside/README.md): Require or disallow whitespace after the `//` in `//`-comments

### Comment

- [`comment-no-loud`](./src/rules/comment-no-loud/README.md): Disallow `/*`-comments.

### Declaration

- [`declaration-nested-properties`](./src/rules/declaration-nested-properties/README.md): Require or disallow properties with `-` in their names to be in a form of a nested group.
- [`declaration-nested-properties-no-divided-groups`](./src/rules/declaration-nested-properties-no-divided-groups/README.md): Disallow nested properties of the same "namespace" be divided into multiple groups.

### Dimension

- [`dimension-no-non-numeric-values`](./src/rules/dimension-no-non-numeric-values/README.md): Disallow non-numeric values when interpolating a value with a unit.

### Function

- [`function-color-relative`](./src/rules/function-color-relative/README.md): Encourage the use of the [scale-color](https://sass-lang.com/documentation/modules/color#scale-color) function over regular color functions.
- [`function-quote-no-quoted-strings-inside`](./src/rules/function-quote-no-quoted-strings-inside/README.md): Disallow quoted strings inside the [quote function](https://sass-lang.com/documentation/modules/string#quote) (Autofixable).
- [`function-unquote-no-unquoted-strings-inside`](./src/rules/function-unquote-no-unquoted-strings-inside/README.md): Disallow unquoted strings inside the [unquote function](https://sass-lang.com/documentation/modules/string#unquote) (Autofixable).

### Map

- [`map-keys-quotes`](./src/rules/map-keys-quotes/README.md): Require quoted keys in Sass maps.

### Media feature

- [`media-feature-value-dollar-variable`](./src/rules/media-feature-value-dollar-variable/README.md): Require a media feature value be a `$`-variable or disallow `$`-variables in media feature values.

### Operator

- [`operator-no-newline-after`](./src/rules/operator-no-newline-after/README.md): Disallow linebreaks after Sass operators.
- [`operator-no-newline-before`](./src/rules/operator-no-newline-before/README.md): Disallow linebreaks before Sass operators.
- [`operator-no-unspaced`](./src/rules/operator-no-unspaced/README.md): Disallow unspaced operators in Sass operations.

### Partial

- [`partial-no-import`](./src/rules/partial-no-import/README.md): Disallow non-CSS `@import`s in partial files.

### Selector

- [`selector-nest-combinators`](./src/rules/selector-nest-combinators/README.md): Require or disallow nesting of combinators in selectors.
- [`selector-no-redundant-nesting-selector`](./src/rules/selector-no-redundant-nesting-selector/README.md): Disallow redundant nesting selectors (`&`).
- [`selector-no-union-class-name`](./src/rules/selector-no-union-class-name/README.md): Disallow union class names with the parent selector (`&`).

### General / Sheet

- [`no-dollar-variables`](./src/rules/no-dollar-variables/README.md): Disallow dollar variables within a stylesheet.
- [`no-duplicate-dollar-variables`](./src/rules/no-duplicate-dollar-variables/README.md): Disallow duplicate dollar variables within a stylesheet.
- [`no-duplicate-mixins`](./src/rules/no-duplicate-mixins/README.md): Disallow duplicate mixins within a stylesheet.

## Help out

There work on the plugin's rules is still in progress, so if you feel like it, you're welcome to help out in any of these (the plugin follows stylelint guidelines so most part of this is based on its docs):

- Create, enhance, and debug rules (see stylelint's guide to "[Working on rules](https://github.com/stylelint/stylelint/blob/master/docs/developer-guide/rules.md)").
- Improve documentation.
- Chime in on any open issue or pull request.
- Open new issues about your ideas on new rules, or for how to improve the existing ones, and pull requests to show us how your idea works.
- Add new tests to absolutely anything.
- Work on improving performance of rules.
- Contribute to [stylelint](https://github.com/stylelint/stylelint)
- Spread the word.

We communicate via [issues](https://github.com/kristerkari/stylelint-scss/issues) and [pull requests](https://github.com/kristerkari/stylelint-scss/pulls).

There is also [stackoverflow](https://stackoverflow.com/questions/tagged/stylelint), which would be the preferred QA forum.

## Contributors

Thanks goes to these wonderful people:

<table>
<thead>
<tr>
<th style="text-align:center"><a href="https://github.com/kristerkari"><img alt="kristerkari" src="https://avatars0.githubusercontent.com/u/993108?v=4&s=80" width="80"></a></th>
<th style="text-align:center"><a href="https://github.com/dryoma"><img alt="dryoma" src="https://avatars2.githubusercontent.com/u/11942776?v=4&s=80" width="80"></a></th>
<th style="text-align:center"><a href="https://github.com/rambleraptor"><img alt="rambleraptor" src="https://avatars1.githubusercontent.com/u/1325798?v=4&s=80" width="80"></a></th>
<th style="text-align:center"><a href="https://github.com/pipopotamasu"><img alt="pipopotamasu" src="https://avatars0.githubusercontent.com/u/14048211?v=4&s=80" width="80"></a></th>
<th style="text-align:center"><a href="https://github.com/evilebottnawi"><img alt="evilebottnawi" src="https://avatars3.githubusercontent.com/u/4567934?v=4&s=80" width="80"></a></th>
<th style="text-align:center"><a href="https://github.com/OriR"><img alt="OriR" src="https://avatars3.githubusercontent.com/u/2384068?v=4&s=80" width="80"></a></th>
</tr>
</thead>
<tbody>
<tr>
<td style="text-align:center"><a href="https://github.com/kristerkari">kristerkari</a></td>
<td style="text-align:center"><a href="https://github.com/dryoma">dryoma</a></td>
<td style="text-align:center"><a href="https://github.com/rambleraptor">rambleraptor</a></td>
<td style="text-align:center"><a href="https://github.com/pipopotamasu">pipopotamasu</a></td>
<td style="text-align:center"><a href="https://github.com/evilebottnawi">evilebottnawi</a></td>
<td style="text-align:center"><a href="https://github.com/OriR">OriR</a></td>
</tr>
</tbody>
</table>
<table>
<thead>
<tr>
<th style="text-align:center"><a href="https://github.com/ntwb"><img alt="ntwb" src="https://avatars2.githubusercontent.com/u/1016458?v=4&s=80" width="80"></a></th>
<th style="text-align:center"><a href="https://github.com/ricardogobbosouza"><img alt="ricardogobbosouza" src="https://avatars3.githubusercontent.com/u/13064722?v=4&s=80" width="80"></a></th>
<th style="text-align:center"><a href="https://github.com/bjankord"><img alt="bjankord" src="https://avatars1.githubusercontent.com/u/633148?v=4&s=80" width="80"></a></th>
<th style="text-align:center"><a href="https://github.com/thibaudcolas"><img alt="thibaudcolas" src="https://avatars1.githubusercontent.com/u/877585?v=4&s=80" width="80"></a></th>
<th style="text-align:center"><a href="https://github.com/AndyOGo"><img alt="AndyOGo" src="https://avatars1.githubusercontent.com/u/914443?v=4&s=80" width="80"></a></th>
<th style="text-align:center"><a href="https://github.com/niksy"><img alt="niksy" src="https://avatars3.githubusercontent.com/u/389286?v=4&s=80" width="80"></a></th>
</tr>
</thead>
<tbody>
<tr>
<td style="text-align:center"><a href="https://github.com/ntwb">ntwb</a></td>
<td style="text-align:center"><a href="https://github.com/ricardogobbosouza">ricardogobbosouza</a></td>
<td style="text-align:center"><a href="https://github.com/bjankord">bjankord</a></td>
<td style="text-align:center"><a href="https://github.com/thibaudcolas">thibaudcolas</a></td>
<td style="text-align:center"><a href="https://github.com/AndyOGo">AndyOGo</a></td>
<td style="text-align:center"><a href="https://github.com/niksy">niksy</a></td>
</tr>
</tbody>
</table>
<table>
<thead>
<tr>
<th style="text-align:center"><a href="https://github.com/lxsymington"><img alt="lxsymington" src="https://avatars3.githubusercontent.com/u/15095115?v=4&s=80" width="80"></a></th>
<th style="text-align:center"><a href="https://github.com/manovotny"><img alt="manovotny" src="https://avatars2.githubusercontent.com/u/446260?v=4&s=80" width="80"></a></th>
<th style="text-align:center"><a href="https://github.com/Deimos"><img alt="Deimos" src="https://avatars0.githubusercontent.com/u/9033?v=4&s=80" width="80"></a></th>
<th style="text-align:center"><a href="https://github.com/jantimon"><img alt="jantimon" src="https://avatars2.githubusercontent.com/u/4113649?v=4&s=80" width="80"></a></th>
<th style="text-align:center"><a href="https://github.com/stormwarning"><img alt="stormwarning" src="https://avatars1.githubusercontent.com/u/999825?v=4&s=80" width="80"></a></th>
<th style="text-align:center"><a href="https://github.com/keegan-lillo"><img alt="keegan-lillo" src="https://avatars0.githubusercontent.com/u/3537963?v=4&s=80" width="80"></a></th>
</tr>
</thead>
<tbody>
<tr>
<td style="text-align:center"><a href="https://github.com/lxsymington">lxsymington</a></td>
<td style="text-align:center"><a href="https://github.com/manovotny">manovotny</a></td>
<td style="text-align:center"><a href="https://github.com/Deimos">Deimos</a></td>
<td style="text-align:center"><a href="https://github.com/jantimon">jantimon</a></td>
<td style="text-align:center"><a href="https://github.com/stormwarning">stormwarning</a></td>
<td style="text-align:center"><a href="https://github.com/keegan-lillo">keegan-lillo</a></td>
</tr>
</tbody>
</table>
<table>
<thead>
<tr>
<th style="text-align:center"><a href="https://github.com/diego-codes"><img alt="diego-codes" src="https://avatars0.githubusercontent.com/u/5973294?v=4&s=80" width="80"></a></th>
<th style="text-align:center"><a href="https://github.com/paulgv"><img alt="paulgv" src="https://avatars0.githubusercontent.com/u/4895885?v=4&s=80" width="80"></a></th>
<th style="text-align:center"><a href="https://github.com/YozhikM"><img alt="YozhikM" src="https://avatars0.githubusercontent.com/u/27273025?v=4&s=80" width="80"></a></th>
<th style="text-align:center"><a href="https://github.com/YodaDaCoda"><img alt="YodaDaCoda" src="https://avatars0.githubusercontent.com/u/365349?v=4&s=80" width="80"></a></th>
<th style="text-align:center"><a href="https://github.com/freezy-sk"><img alt="freezy-sk" src="https://avatars0.githubusercontent.com/u/661637?v=4&s=80" width="80"></a></th>
<th style="text-align:center"><a href="https://github.com/jeddy3"><img alt="jeddy3" src="https://avatars0.githubusercontent.com/u/808227?v=4&s=80" width="80"></a></th>
</tr>
</thead>
<tbody>
<tr>
<td style="text-align:center"><a href="https://github.com/diego-codes">diego-codes</a></td>
<td style="text-align:center"><a href="https://github.com/paulgv">paulgv</a></td>
<td style="text-align:center"><a href="https://github.com/YozhikM">YozhikM</a></td>
<td style="text-align:center"><a href="https://github.com/YodaDaCoda">YodaDaCoda</a></td>
<td style="text-align:center"><a href="https://github.com/freezy-sk">freezy-sk</a></td>
<td style="text-align:center"><a href="https://github.com/jeddy3">jeddy3</a></td>
</tr>
</tbody>
</table>

## Important documents

- [Changelog](./CHANGELOG.md)
- [Contributing](./CONTRIBUTING.md)
- [License](./LICENSE)
