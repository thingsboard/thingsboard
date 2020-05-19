# at-each-key-value-single-line

This is a rule that checks for situations where users have:

- Done a loop using map-keys
- Grabbed the value for that key inside of the loop.

```scss
$font-weights: (
  "regular": 400,
  "medium": 500,
  "bold": 700
);
@each $key in map-keys($font-weights) {
  $value: map-get($font-weights, $key);
  /**        ↑
   * This call should be consolidated into the @each call.
   **/
}
```

## Options

### `true`

The following patterns are considered violations:

```scss
$font-weights: (
  "regular": 400,
  "medium": 500,
  "bold": 700
);
@each $key in map-keys($font-weights) {
  $value: map-get($font-weights, $key);
}
```

The following patterns are _not_ considered violations:

```scss
$font-weights: ("regular": 400, "medium": 500, "bold": 700);
@each $key, $value in $font-weights {...}
```

```scss
$font-weights: (
  "regular": 400,
  "medium": 500,
  "bold": 700
);
$other-weights: (
  "regular": 400,
  "medium": 500,
  "bold": 700
);

@each $key, $value in map-keys($font-weights) {
  $value: map-get($other-weights, $key);
}
```

```scss
$font-weights: ("regular": 400, "medium": 500, "bold": 700);

@each $key, $value in map-keys($font-weights) {...}
```
