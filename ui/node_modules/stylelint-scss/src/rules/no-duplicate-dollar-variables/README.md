# no-duplicate-dollar-variables

Disallow duplicate dollar variables within a stylesheet.

```scss
    $a: 1;
    $a: 2;
/** ↑
 * These are duplicates */
```

## Options

### `true`

The following patterns are considered violations:

```scss
$a: 1;
$a: 2;
```

```scss
$a: 1;
$b: 2;
$a: 3;
```

```scss
$a: 1;
.b {
  $a: 1;
}
```

```scss
$a: 1;
.b {
  .c {
    $a: 1;
  }
}
```

```scss
$a: 1;
@mixin b {
  $a: 1;
}
```

The following patterns are _not_ considered violations:

```scss
$a: 1;
$b: 2;
```

```scss
$a: 1;
.b {
  $b: 2;
}
```

### `ignoreInside: ["at-rule", "nested-at-rule"]`

#### `"at-rule"`

Ignores dollar variables that are inside both nested and non-nested at-rules (`@media`, `@mixin`, etc.).

Given:

```json
{ "ignoreInside": ["at-rule"] }
```

The following patterns are _not_ considered warnings:

```scss
$a: 1;
@mixin c {
  $a: 1;
}
```

```scss
$a: 1;
.b {
  @mixin c {
    $a: 1;
  }
}
```

#### `"nested-at-rule"`

Ignores dollar variables that are inside nested at-rules (`@media`, `@mixin`, etc.).

Given:

```json
{ "ignoreInside": ["nested-at-rule"] }
```

The following patterns are _not_ considered warnings:

```scss
$a: 1;
.b {
  @mixin c {
    $a: 1;
  }
}
```

### `ignoreInsideAtRules: ["array", "of", "at-rules"]`

Ignores all variables that are inside specified at-rules.

Given:

```json
{ "ignoreInsideAtRules": ["if", "mixin"] }
```

The following patterns are _not_ considered warnings:

```scss
$a: 1;

@mixin b {
  $a: 2;
}
```

```scss
$a: 1;

@if (true) {
  $a: 2;
}
```
