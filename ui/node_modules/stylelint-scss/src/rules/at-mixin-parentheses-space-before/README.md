# at-mixin-parentheses-space-before

Require or disallow a space before `@mixin` parentheses.

```scss
@mixin foo ($arg) { }
/**        ↑
 * The space before this parenthesis */
```

The `--fix` option on the [command line](https://github.com/stylelint/stylelint/blob/master/docs/user-guide/cli.md#autofixing-errors) can automatically fix all of the problems reported by this rule.

## Options

`string`: `"always"|"never"`

### `"always"`

There *must always* be exactly one space between the mixin name and the opening parenthesis. 

*Note: This rule does not enforce parentheses to be present in a declaration without arguments.*

The following patterns are considered warnings:

```scss
@mixin foo($arg) { }
```
```scss
@mixin foo  ($arg) { }
```

The following patterns are *not* considered warnings:

```scss
@mixin foo ($arg) { }
```
```scss
@mixin foo { }
```

### `"never"`

There *must never* be any whitespace between the mixin name and the opening parenthesis.

The following patterns are considered warnings:

```scss
@mixin foo ($arg) { }
```

The following patterns are *not* considered warnings:

```scss
@mixin foo($arg) { }
```
```scss
@mixin foo { }
```
