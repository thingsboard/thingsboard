# double-slash-comment-whitespace-inside

Require or disallow whitespace after the `//` in `//`-comments

```scss
a {
  width: 10px; // inline-comment
/*               ↑
 * Such whitespace */
```

This rule only works with SCSS-like [single-line comments](https://sass-lang.com/documentation/syntax/comments) and ignores CSS comments (`/* */`).

Any number of slases are allowed at the beginning of the comment. So `/// comment` is treated the same way as `// comment`.

Note that a newline is not possible as a whitespace in terms of this rule as `//`-comments are intended to be single-line.

## Options

`string`: `"always"|"never"`

### `"always"`

There *must always* be whitespace after the `//` inside `//`-comments.

The following patterns are considered warnings:

```scss
//comment
```

The following patterns are *not* considered warnings:

```scss
// comment
```

```scss
///   comment
```

### `"never"`

There *must never* be whitespace after the `//` inside `//`-comments.

The following patterns are considered warnings:

```scss
// comment
```

The following patterns are *not* considered warnings:

```scss
//comment
```

```scss
///comment
```
