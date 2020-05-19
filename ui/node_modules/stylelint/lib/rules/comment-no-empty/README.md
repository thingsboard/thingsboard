# comment-no-empty

Disallow empty comments.

```css
    /* */
/** ↑
 * Comments like this */
```

This rule ignores SCSS-like comments.

**Caveat:** Comments within *selector and value lists* are currently ignored.

## Options

### `true`

The following patterns are considered violations:

```css
/**/
```

```css
/* */
```

```css
/*

 */
```

The following patterns are *not* considered violations:

```css
/* comment */
```

```css
/*
 * Multi-line Comment
**/
```
