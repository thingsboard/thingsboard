# dollar-variable-colon-newline-after

Require a newline after the colon in `$`-variable declarations.

```scss
$box-shadow:
  0 0 0 1px #5b9dd9,
  0 0 2px 1px rgba(30, 140, 190, 0.8);
        /* ↑ */
/**        ↑
 * The newline after this colon */
```

The `--fix` option on the [command line](https://github.com/stylelint/stylelint/blob/master/docs/user-guide/cli.md#autofixing-errors) can automatically fix all of the problems reported by this rule.

## Options

`string`: `"always"|"always-multi-line"`

### `"always"`

There *must always* be a newline after the colon.

The following patterns are considered warnings:

```scss
$var:100px;
```

```scss
a { $var:100px; }
```

```scss
$var: 100px;
```

The following patterns are *not* considered warnings:

```scss
$var:
  100px;
```

```scss
a {
  $var:
    100px;
}
```

### `"always-multi-line"`

There *must always* be a newline after the colon *if the variable value is multi-line*.

The following patterns are considered warnings:

```scss
$box-shadow: 0 0 0 1px #5b9dd9,
  0 0 2px 1px rgba(30, 140, 190, 0.8);
```

The following patterns are *not* considered warnings:

```scss
$box-shadow:
  0 0 0 1px #5b9dd9,
  0 0 2px 1px rgba(30, 140, 190, 0.8);
```

```scss
$box-shadow:
  0 0 0 1px #5b9dd9, 0 0 2px 1px rgba(30, 140, 190, 0.8);
// The VALUE is single-line, so a newline after the colon is ignored by this rule.
```

```scss
$var: 100px;
```

## Optional secondary options

### `disableFix: true`

Disables autofixing for this rule.
