# comment-no-loud

Disallow `/*`-comments.

```scss
/*  Comment */
//  ↑     ↑
// This line
```

This rule only works on CSS comments (`/* */`) and ignores all double-slash (`//`) comments.

## Options

### `true`

The following patterns are considered violations:

```scss
/* comment */
```

```scss
/*
 * comment
*/
```

The following patterns are *not* considered warnings:

```scss
// comment
```
