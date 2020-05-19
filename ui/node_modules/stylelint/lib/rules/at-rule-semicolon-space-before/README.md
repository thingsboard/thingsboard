# at-rule-semicolon-space-before

Require a single space or disallow whitespace before the semicolons of at-rules.

```css
@import "components/buttons";
/**                         ↑
 * The space before this semicolon */
```

## Options

`string`: `"always"|"never"`

### `"always"`

There *must always* be a single space before the semicolons.

The following pattern is considered a violation:

```css
@import "components/buttons";
```

The following pattern is *not* considered a violation:

```css
@import "components/buttons" ;
```

### `"never"`

There *must never* be a single space before the semicolons.

The following pattern is considered a violation:

```css
@import "components/buttons" ;
```

The following pattern is *not* considered a violation:

```css
@import "components/buttons";
```
