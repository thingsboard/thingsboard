# dollar-variable-colon-space-after

Require or disallow whitespace after the colon in `$`-variable declarations.

```scss
$variable: 10px;
/**      ↑
 * The space after this colon */
```

The `--fix` option on the [command line](https://github.com/stylelint/stylelint/blob/master/docs/user-guide/cli.md#autofixing-errors) can automatically fix all of the problems reported by this rule.

## Options

`string`: `"always"|"never"|"always-single-line"|"at-least-one-space"`

### `"always"`

There *must always* be a single space after the colon.

The following patterns are considered warnings:

```scss
a { $var :10px }
```

```scss
$var:10px;
```

```scss
$var:
  10px;
// a newline is not a space
```

The following patterns are *not* considered warnings:

```scss
a { $var : 10px }
```

```scss
$var: 10px;
```

### `"never"`

There *must never* be whitespace after the colon.

The following patterns are considered warnings:

```scss
$var: 10px;
```

```scss
$var:
10px;
```

```scss
a { $var :10px }
```

The following patterns are *not* considered warnings:

```scss
$var :10px;
```

```scss
a { $var:10px }
```

### `"always-single-line"`

There *must always* be a single space after the colon *if the variable value is single-line*.

The following patterns are considered warnings:

```scss
$box-shadow:0 0 0 1px #5b9dd9, 0 0 2px 1px rgba(30, 140, 190, 0.8);
```

The following patterns are *not* considered warnings:

```scss
a {
  $box-shadow: 0 0 0 1px #5b9dd9, 0 0 2px 1px rgba(30, 140, 190, 0.8);
}
```

```scss
$box-shadow:
  0 0 0 1px #5b9dd9,
  0 0 2px 1px rgba(30, 140, 190, 0.8);
```

```scss
a {
  $box-shadow:0 0 0 1px #5b9dd9,
    0 0 2px 1px rgba(30, 140, 190, 0.8);
}
```

### `"at-least-one-space"`

There must always be *at least* a single space after the colon.

The following patterns are considered warnings:

```scss
a { $var :10px }
```

```scss
$var:10px;
```

```scss
$var:
  10px;
// a newline is not a space
```

The following patterns are *not* considered warnings:

```scss
a { $var : 10px }
```

```scss
$var: 10px;
```

```scss
$var:    10px;
```
