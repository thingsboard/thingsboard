# double-slash-comment-empty-line-before

Require or disallow an empty line before `//`-comments.

```scss
a {}
           /* ← */
// comment /* ↑ */
/**           ↑
*     This line */
```

The `--fix` option on the [command line](https://github.com/stylelint/stylelint/blob/master/docs/user-guide/cli.md#autofixing-errors) can automatically fix all of the problems reported by this rule.

This rule only works with SCSS-like [single-line comments](https://sass-lang.com/documentation/syntax/comments) and ignores:
* comments that are the very first nodes in a file;
* CSS comments (`/* */`);
* comments that are on the same line as some non-comment code (inline comments).

## Options

`string`: `"always"|"never"`

### `"always"`

There *must always* be an empty line before `//`-comments.

The following patterns are considered warnings:

```scss
a {}
// comment
```

The following patterns are *not* considered warnings:

```scss
a {}

// comment
```

```scss
a {} // comment
```

### `"never"`

There *must never* be an empty line before `//`-comments.

The following patterns are considered warnings:

```scss
a {}

// comment
```

The following patterns are *not* considered warnings:

```scss
a {}
// comment
```

```scss
a {} // comment
```

## Optional options

### `except: ["first-nested"]`

Reverse the primary option for `//`-comments that are nested and the first child of their parent node.

For example, with `"always"`:

The following patterns are considered warnings:

```scss
a {

  // comment
  color: pink;
}
```

The following patterns are *not* considered warnings:

```scss
a {
  // comment
  color: pink;
}
```

### `ignore: ["between-comments", "stylelint-commands"]`

#### `"between-comments"`

Don't require an empty line before `//`-comments that are placed after other `//`-comments or CSS comments.

For example, with `"always"`:

The following patterns are *not* considered warnings:

```scss
a {
  background: pink;

  // comment
  // comment
  color: #eee;
}
```

```scss
a {
  background: pink;

  /* comment */
  // comment
  color: #eee;
}
```

#### `"stylelint-commands"`

Ignore `//`-comments that deliver commands to stylelint, e.g. `// stylelint-disable color-no-hex`.

For example, with `"always"`:

The following patterns are considered warnings:

```scss
a {
  background: pink;
  // not a stylelint command
  color: #eee;
}
```

The following patterns are *not* considered warnings:

```scss
a {
  background: pink;
  // stylelint-disable color-no-hex
  color: pink;
}
```
