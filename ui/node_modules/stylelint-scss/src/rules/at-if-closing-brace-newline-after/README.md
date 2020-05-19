# at-if-closing-brace-newline-after

Require or disallow a newline after the closing brace of `@if` statements.

```scss
@if ($a == 0) { }
                ↑
/**             ↑
 * The newline after this brace */
```

The `--fix` option on the [command line](https://github.com/stylelint/stylelint/blob/master/docs/user-guide/cli.md#autofixing-errors) can automatically fix all of the problems reported by this rule.

This rule might have conflicts with stylelint's core rule [`block-closing-brace-newline-after`](https://stylelint.io/user-guide/rules/block-closing-brace-newline-after) if it doesn't have `"ignoreAtRules": ["if"]` in a `.stylelintrc` config file. That's because an `@if { ... }` statement can be successfully parsed as an at-rule with a block. You might also want to set `"ignoreAtRules": ["else"]` for another stylelint's core rule - [`at-rule-empty-line-before`](https://stylelint.io/user-guide/rules/at-rule-empty-line-before) that could be forcing empty lines before at-rules (including `@else`s that follow `@if`s or other `@else`s).

This rule doesn't have usual `"always"` and `"never"` main option values, because if you don't need special behavior for `@if` and `@else` you could just use [`block-closing-brace-newline-after`](https://stylelint.io/user-guide/rules/block-closing-brace-newline-after) set to `"always"` or any other value.

## Options

`string`: `"always-last-in-chain"`

### `"always-last-in-chain"`

There *must always* be a newline after the closing brace of `@if` that is the last statement in a conditional statement chain (i.e. has no `@else` right after it). If it's not, there *must not* be a newline.

The following patterns are considered warnings:

```scss
a {
  @if ($x == 1) {
    // ...
  } width: 10px; // No @else - should have a newline
}

@if ($x == 1) {
  // ...
} // Has @else - shouldn't have a newline
@else { }
```

The following patterns are *not* considered warnings:

```scss
a {
  @if ($x == 1) {}
  width: 10px;
}

@if ($x == 1) {
  // ...
} @else {} // Has @else, so no newline needed

@if ($x == 1) { }@else { }
```

## Optional secondary options

### `disableFix: true`

Disables autofixing for this rule.
