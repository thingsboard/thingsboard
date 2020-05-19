# at-else-if-parentheses-space-before

Require or disallow a space before `@else if` parentheses.

```scss
@else if ($condition) { }
/**      ↑
 * The space before this parenthesis */
```

The `--fix` option on the [command line](https://github.com/stylelint/stylelint/blob/master/docs/user-guide/cli.md#autofixing-errors) can automatically fix all of the problems reported by this rule.

## Options

`string`: `"always"|"never"`

### `"always"`

There *must always* be exactly one space between the `@else if` and the condition opening parenthesis. 

*Note: This rule does not enforce parentheses to be present.*

The following patterns are considered warnings:

```scss
@else if($condition) { }
```
```scss
@else if  ($condition) { }
```

The following patterns are *not* considered warnings:

```scss
@else if ($condition) { }
```
```scss
@else if $condition { }
```

### `"never"`

There *must never* be whitespace between the `@else if` and the condition opening parenthesis. 

The following patterns are considered warnings:

```scss
@else if ($condition) { }
```

The following patterns are *not* considered warnings:

```scss
@else if($condition) { }
```
```scss
@else if $condition { }
```
