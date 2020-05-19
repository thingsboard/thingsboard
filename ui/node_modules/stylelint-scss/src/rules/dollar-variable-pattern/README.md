# dollar-variable-pattern

Specify a pattern for Sass-like variables.

```scss
a { $foo: 1px; }
/** ↑
 * The pattern of this */
```

## Options

`regex` or `string`

A string will be translated into a RegExp like so `new RegExp(yourString)` — so be sure to escape properly.

### E.g. `/foo-.+/`

The following patterns are considered warnings:

```scss
a { $boo-bar: 0; }
```

The following patterns are *not* considered warnings:

```scss
a { $foo-bar: 0; }
```

## Optional Options

### `ignore: "local"|"global"`

#### `"local"`

Makes this rule ignore local variables (variables defined inside a rule/mixin/function, etc.).

For example, with `/^foo-/`:

The following patterns are *not* considered warnings:

```scss
$foo-name00: 10px;
```

```scss
a {
  $bar-name01: 10px;
}
```

#### `"global"`

Makes this rule ignore global variables (variables defined in the stylesheet root).

For example, with `/^foo-/`:

The following patterns are *not* considered warnings:

```scss
$bar-name01: 10px;
```

```scss
a {
  $foo-name02: 10px;
}
```
