# at-mixin-argumentless-call-parentheses

Require or disallow parentheses in argumentless `@mixin` calls.

```scss
@include mixin-name() {
/**                ↑
 *                 Such mixin calls */
```

The `--fix` option on the [command line](https://github.com/stylelint/stylelint/blob/master/docs/user-guide/cli.md#autofixing-errors) can automatically fix all of the problems reported by this rule.

## Options

`string`: `"always"|"never"`

### `"always"`

There *must always* be parentheses in mixin calls, even if no arguments are passed.

The following patterns are considered warnings:

```scss
@include mixin-name;
```

The following patterns are *not* considered warnings:

```scss
@include mixin-name();
```

### `"never"`

There *must never* be parentheses when calling a mixin without arguments.

The following patterns are considered warnings:

```scss
@include mixin-name();
```

The following patterns are *not* considered warnings:

```scss
@include mixin-name;
```
