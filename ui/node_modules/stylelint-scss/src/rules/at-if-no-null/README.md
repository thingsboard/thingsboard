# at-if-no-null

Check for equality to null is unnecessarily explicit since `null` is falsey in Sass.

```scss
a {
    @if $x == null {}
/**         ↑     ↑
 * == or != null is unncessary */
}
```

## Options

true

### `true`

The following patterns are considered warnings:
```scss
a {
    @if $x == null {}
}
```

```scss
a {
    @if $x != null {}
}
```

The following patterns are *not* considered warnings:

```scss
a {
    @if $x {}
}
```

```scss
a {
    @if not $x {}
}
```
