# unicode-bom

Require or disallow the Unicode Byte Order Mark.

## Options

`string`: `"always"|"never"`

### `"always"`

The following pattern is considered a violation:

```css
a {}
```

The following pattern is *not* considered a violation:

```css
U+FEFF
a {}
```

### `"never"`

The following pattern is considered a violation:

```css
U+FEFF
a {}
```

The following pattern is *not* considered a violation:

```css
a {}
```
