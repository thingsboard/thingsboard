# double-slash-comment-inline

Require or disallow `//`-comments to be inline comments.

```scss
a {
  width: 10px; // inline-comment
/*             ↑
 * Such comments */
```

An inline comment in terms of this rule is a comment that is placed on the same line with any other code, either before or after it.

This rule only works with SCSS-like [single-line comments](https://sass-lang.com/documentation/syntax/comments) and ignores CSS comments (`/* */`).

## Options

`string`: `"always"|"never"`

### `"always"`

`//`-comments *must always* be inline comments.

The following patterns are considered warnings:

```scss
// comment
a { width: 10px; }
```

```scss
a {
  // comment
  width: 10px;
}
```

The following patterns are *not* considered warnings:

```scss
a { // comment
  width: 10px;
}
```

```scss
a {
  width: 10px; // comment
}
```

```scss
a, // comment
b {
  width: 10px;
}
```

### `"never"`

`//`-comments *must never* be inline comments.

The following patterns are considered warnings:

```scss
a {
  width: 10px; // comment
}
```

```scss
a, // comment
b {
  width: 10px;
}
```

The following patterns are *not* considered warnings:

```scss
// comment
a { width: 10px; }
```

```scss
a {
  // comment
  width: 10px;
}
```

## Optional options

### `ignore: ["stylelint-commands"]`

#### `"stylelint-commands"`

Ignore `//`-comments that deliver commands to stylelint, e.g. `// stylelint-disable color-no-hex`.

For example, with `"always"`:

The following patterns are *not* considered warnings:

```scss
a {
  background: pink;
  // stylelint-disable color-no-hex
  color: pink;
}
```
