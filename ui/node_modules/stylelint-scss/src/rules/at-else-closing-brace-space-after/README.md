# at-else-closing-brace-space-after

Require a single space or disallow whitespace after the closing brace of `@else` statements.

```scss
@if ($a == 0) { }
@else if ($x == 2) { }
                     ↑
/**                  ↑
 * The space after this brace */
```

The `--fix` option on the [command line](https://github.com/stylelint/stylelint/blob/master/docs/user-guide/cli.md#autofixing-errors) can automatically fix all of the problems reported by this rule.

This rule might have conflicts with stylelint's core [`block-closing-brace-space-after`](https://stylelint.io/user-guide/rules/block-closing-brace-space-after) rule if the latter is set up in your `.stylelintrc` config file.

## Options

`string`: `"always-intermediate"|"never-intermediate"`

### `"always-intermediate"`

There *must always* be a single space after the closing brace of `@else` that is not the last statement in a conditional statement chain (i.e. does have another `@else` right after it).

The following patterns are considered warnings:

```scss
@if ($x == 1) {
  // ...
} @else if ($x == 2) {
  // ...
}@else {}

@if ($x == 1) {
  // ...
} @else if ($x == 2) {
  // ...
}
@else { }

// `@else if` has a space and a newline after the closing brace
@if ($x == 1) {
  // ...
} @else if ($x == 2) {
  // ...
} 
@else { }

@if ($x == 1) {
  // ...
} @else if ($x == 2) {
  // ...
}  @else { } // Two spaces
```

The following patterns are *not* considered warnings:

```scss
@if ($x == 1) {
  // ...
} @else if ($x == 2) {
  // ...
} @else {}
      
a {
  @if ($x == 1) {
    // ...
  } @else ($x == 2) {
    // ...
  }
  width: 10px;
}

@if ($x == 1) { } @else if ($x == 2) { 
  // ...
} @include x;

@if ($x == 1) {
  // ...
} @else if ($x == 2) {
  // ...
} @include x;
```

### `"never-intermediate"`

There *must never* be a whitespace after the closing brace of `@else` that is not the last statement in a conditional statement chain (i.e. does have another `@else` right after it).

The following patterns are considered warnings:

```scss
@if ($x == 1) {
  // ...
} @else if ($x == 2) {
  // ...
} @else {}

@if ($x == 1) {
  // ...
} @else if ($x == 2) {
  // ...
}
@else { }
```

The following patterns are *not* considered warnings:

```scss
@if ($x == 1) {
  // ...
}@else {}

a {
  @if ($x == 1) {
    // ...
  } @else if ($x == 2) {
    // ...
  }
  width: 10px;
}

@if ($x == 1) { } @else if ($x == 2) {
  // ...
}@include x;

@if ($x == 1) {
  // ...
} @else if ($x == 2) {
  // ...
} @include x;
```
