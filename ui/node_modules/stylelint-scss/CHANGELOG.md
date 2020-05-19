# 3.13.0

- Added: support for stylelint version 12.
- Fixed: duplicate warnings in `at-rule-no-unknown` rule.

# 3.12.1

- Fixed: `at-rule-conditional-no-parentheses` don't warn for function calls.
- Fixed: `map-keys-quotes` ignore math operators inside map values.
- Fixed: `operator-no-unspaced` was looking for operators inside `@forward` and `@use`.

# 3.12.0

- Added: `no-duplicate-mixins` rule.
- Added: `at-rule-no-unknown` support for Sass' `@forward` and `@use`.

# 3.11.1

- Fixed: `selector-nest-combinators` warning for `@keyframes`.

# 3.11.0

- Added: support for stylelint version 11.

# 3.10.1

- Fixed: broken linting reporting for `dimension-no-non-numeric-values` rule.

# 3.10.0

- Added: `at-if-no-null`rule.
- Added: `at-import-partial-extension` rule.
- Added: `at-rule-conditional-no-parentheses` rule.
- Added: `dimension-no-non-numeric-values` rule.

# 3.9.4

- Fixed: `selector-nest-combinators` rule throwing an error when using nested props.

# 3.9.3

- Fixed: `map-keys-quotes` warning for unquoted numeric keys.

# 3.9.2

- Fixed: `selector-no-union-class-name` throwing an error when using nested `@`-rules.

# 3.9.1

- Fixed: `selector-no-union-class-name` false positives for id, attribute, and pseudo selectors.

# 3.9.0

- Added: `selector-no-union-class-name` rule.
- Added: `function-color-relative` rule.
- Added: `comment-no-loud` rule.
- Added: `map-keys-quotes` rule.
- Fixed: typo in error message for `at-else-empty-line-before` and `dollar-variable-empty-line-before` rules.

# 3.8.0

- Added: `function-unquote-no-unquoted-strings-inside` rule.
- Fixed: wrong message name for `at-each-key-value-single-line` rule.

# 3.7.0

- Added: `at-each-key-value-single-line` rule.
- Added: `function-quote-no-quoted-strings-inside` rule.

# 3.6.1

- Fixed: `double-slash-comment-empty-line-before` autofix not working in certain situations.

# 3.6.0

- Added: support for stylelint version 10.

# 3.5.4

- Fixed: `dollar-variable-colon-newline-after` no longer warns for multiline variables that use `!default`.

# 3.5.3

- Fixed: `operator-no-unspaced` no longer warns for valid absolute urls and urls with interpolation.

# 3.5.2

- Fixed: handle SCSS interpolation in `selector-nest-combinators` rule.

# 3.5.1

- Fixed: error messages for `selector-nest-combinators` rule.

# 3.5.0

- Added: `selector-nest-combinators` rule.

# 3.4.4

- Updated: `postcss-selector-parser` dependency to 5.0.0 (major version bump) with a memory leak fix and other bug fixes.

# 3.4.3

- Fixed: `double-slash-comment-whitespace-inside` fix error being thrown when using two backslashes inside a string.

# 3.4.2

- Fixed: `operator-no-unspaced` no longer warns for relative url paths.

# 3.4.1

- Fixed: `at-function-named-arguments` was not correctly handling functions inside Sass maps.

# 3.4.0

- Added: `at-mixin-argumentless-call-parentheses` autofix (#280).
- Fixed: `partial-no-import` correctly handle import parameters with urls (#283).

# 3.3.2

- Fixed: Account for hyphens in function names (`at-function-parentheses-space-before`) and mixin (`at-mixin-parentheses-space-before`) names.

# 3.3.1

- Fixed: `at-function-named-arguments` correctly parse function arguments with trailing commas.

# 3.3.0

- Added: `at-function-named-arguments` add `ignoreFunctions` option.
- Fixed: `at-function-named-arguments` correctly parse data uris as function parameters.

# 3.2.0

- Added: `no-dollar-variables` rule.
- Added: `ignoreInside` and `ignoreInsideAtRules` options to `no-duplicate-dollar-variables` rule.
- Fixed: `operator-no-unspaced` no longer warns for `unicode-range` property.

# 3.1.3

- Fixed: compatibility with non css syntaxes for `operator-no-newline-after` and `operator-no-newline-before` rules.

# 3.1.2

- Fixed: compatibility with non css syntaxes.

# 3.1.1

- Fixed: `operator-no-newline-before` and `operator-no-unspaced` rules were throwing an error when using CSS custom properties.

# 3.1.0

- Added: `no-duplicate-dollar-variables` rule.

# 3.0.1

- Fixed: `at-function-named-arguments` now ignores Sass maps.

# 3.0.0

- Removed: Node.JS 4.x support. Node.js 6.x or greater is now required (#213).
- Added: `at-else-empty-line-before` autofix (#221).
- Added: `at-else-if-parentheses-space-before` autofix (#222).
- Added: `at-function-parentheses-space-before` autofix (#223).
- Added: `at-mixin-parentheses-space-before` autofix (#224).
- Added: `dollar-variable-empty-line-before` autofix (#226).
- Added: `dollar-variable-colon-space-after` autofix (#227).
- Added: `dollar-variable-colon-space-before` autofix (#227).
- Added: `at-else-closing-brace-space-after` autofix (#228).
- Added: `at-if-else-closing-brace-space-after` autofix (#228).
- Added: `at-else-closing-brace-newline-after` autofix (#229).
- Added: `at-if-closing-brace-newline-after` autofix (#229).
- Added: `double-slash-comment-empty-line-before` autofix (#230).
- Added: `dollar-variable-colon-newline-after` autofix (#231).

# 2.5.0

- Added: `at-least-one-space` option to `dollar-variable-colon-space-after` rule.
- Fixed: `dollar-variable-colon-newline-after` now does not require a newline for Sass maps and multiline variables with parentheses when `always-multi-line` option is used.

# 2.4.0

- Added: support for stylelint version 9.
- Fixed: `dollar-variable-colon-newline-after` now allows multiline variables when `always` option is used.

# 2.3.0

- Added: `dollar-variable-default` rule.

# 2.2.0

- Added: `at-function-named-arguments` rule.
- Added: `at-mixin-named-arguments` rule.

# 2.1.0

- Added: `at-else-if-parentheses-space-before` rule.
- Added: `at-function-parentheses-space-before` rule.
- Added: `at-mixin-parentheses-space-before` rule.

# 2.0.1

- Fixed: `selector-no-redundant-nesting-selector` now handles multiple nested selectors.

# 2.0.0

This version updates stylelint to version 8 and removes 2 rules that were deprecated in earlier versions.

- Breaking changes:
  - Updated: stylelint dependency from version 7 to version 8.
  - Changed: stylelint is now listed in `peerDependencies` instead of `dependencies`. This means that you need to have `stylelint` installed in your project before using `stylelint-scss`.
  - Changed: white/blacklists and ignore\* options to be case sensitive by default. See https://github.com/stylelint/stylelint/pull/2709
  - Removed: 2 deprecated rules
    - `at-import-no-partial-extension`
    - `at-mixin-no-argumentless-call-parentheses`

# 1.5.2

- Fixed: `operator-no-unspaced` support escaped operators by handling them in `sassValueParser`.
- Fixed: `declaration-nested-properties` support escaped selectors by checking for escaped characters in `parseNestedPropRoot`.

# 1.5.1

- Fixed: `at-rule-no-unknown` add missing export to `ruleName`.
- Fixed: `at-rule-no-unknown` add options validation.

# 1.5.0

- Added: `at-rule-no-unknown` rule.

# 1.4.4

- Fixed: `at-if-closing-brace-newline-after`: support `@elseif`.

# 1.4.3

- Fixed: `at-mixin-no-argumentless-call-parentheses` messages

# 1.4.2:

- Fixed: false positives in inline comment detecting by `findCommentsInRaws` if a comment is the first/last in a file/line
- Fixed: `findCommentsInRaws` error in function detection

# 1.4.1

- Fixed: mixed import names for `at-else-closing-brace-space-after` and `at-else-empty-line-before` rules.
- Fixed: false positives for nested props rules (`:not()`-like selectors, strings, numbers).

# 1.4.0

- Added: `at-else-closing-brace-newline-after` rule.
- Added: `at-else-closing-brace-space-after` rule.
- Added: `at-if-closing-brace-newline-after` rule.
- Added: `at-if-closing-brace-space-after` rule.
- Added: `at-else-empty-line-before` rule.
- Added: `declaration-nested-properties` rule.
- Added: `declaration-nested-properties-no-divided-groups` rule.
- Added: `dollar-variable-empty-line-before` rule.
- Added: `ignore: "local"|"global"` to the `dollar-variable-pattern` rule.
- Added: `docs` folder to `npm` package.
- Removed: `src` folder from `npm` package.
- Removed: NodeJS 0.12.x support, stylelint-scss now requires NodeJS > 4.2.1 LTS or greater

# 1.3.4

- Fixed: parsing `-` and `+` at the operation start in `operator-` rules.
- Fixed: `findCommentsInRaws` false positives on comments inside strings (applicable to rules `double-slash-comment-inline`, `double-slash-comment-whitespace-inside`, `operator-no-unspaced`).

# 1.3.3

- Fixed: parsing `%` character by `operator-` rules.
- Fixed: false positives on `operator-` rules.

# 1.3.2

- Fixed: `findCommentsInRaws` fail on parsing selectors like `p:not(.not-p)` (applicable to rules `double-slash-comment-inline`, `double-slash-comment-whitespace-inside`, `operator-no-unspaced`).
- Fixed: 'double-slash-comment-whitespace-inside' false positives on empty comments (e.g. `//`).
- Fixed: `findCommentsInRaws` giving wrong column number (applicable to rules `double-slash-comment-inline`, `double-slash-comment-whitespace-inside`, `operator-no-unspaced`).

# 1.3.1

- Fixed: `findCommentsInRaws` for multiline CSS comments and text for //-comments (`double-slash-comment-` rules and `operator-no-unspaced` rule).

# 1.3.0

- Added: `at-mixin-argumentless-call-parentheses` rule (with "always"/"never" behavior as a replacement for `at-mixin-no-argumentless-call-parentheses`).
- Added: `dollar-variable-colon-newline-after` rule.
- Added: `dollar-variable-colon-space-after` rule.
- Added: `dollar-variable-colon-space-before` rule.
- Added: `double-slash-comment-empty-line-before` rule.
- Added: `double-slash-comment-inline` rule.
- Added: `double-slash-comment-whitespace-inside` rule.
- Added: `operator-no-newline-after` rule.
- Added: `operator-no-newline-before` rule.
- Added: `operator-no-unspaced` rule.
- Deprecated: `at-mixin-no-argumentless-call-parentheses`.
- Fixed: `partial-no-import` failing when linting a code string (not in an actual file, e.g. via stylelilnt Node API).
- Updated stylelint dependency to version 7.

# 1.2.1

- Fixed: `at-function-pattern`, `at-mixin-pattern` failing if there are parens inside a parameters list.

# 1.2.0

- Added: `partial-no-import` rule.
- Added: `media-feature-value-dollar-variable` rule.
- Added: `at-import-partial-extension-blacklist` rule.
- Added: `at-import-partial-extension-whitelist` rule.
- Deprecated: `at-import-no-partial-extension` rule.
- Fixed: `dollar-variable-no-missing-interpolation` was throwing an error on older Node.js versions.

# 1.1.1

- Fixed: newlines inside braces in `at-function-pattern`, `at-mixin-pattern`.
- Fixed: false positives and false negatives in `selector-no-redundant-nesting-selector`.

# 1.1.0

- Added: `at-mixin-no-argumentless-call-parentheses` rule.
- Added: `at-import-no-partial-leading-underscore` rule.
- Added: `at-import-no-partial-extension` rule.
- Added: `percent-placeholder-pattern` rule.
- Fixed: `selector-no-redundant-nesting-selector` no longer warns about BEM syntax.
- Fixed: bug causing rules to ignore severity levels `warning` / `error` and report `ignore` instead.

# 1.0.0

- Added: `at-extend-no-missing-placeholder` rule.
- Added: `at-function-pattern` rule.
- Added: `at-mixin-pattern` rule.
- Added: `dollar-variable-no-missing-interpolation` rule.
- Added: `dollar-variable-pattern` rule.
- Added: `selector-no-redundant-nesting-selector` rule.
