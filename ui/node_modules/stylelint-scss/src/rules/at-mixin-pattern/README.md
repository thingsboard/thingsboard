# at-mixin-pattern

Specify a pattern for Sass/SCSS-like mixin names.

```scss
@mixin complex-object ($items: 10) {
/**    ↑
 * The pattern of this */
```

## Options

`regex` or `string`

A string will be translated into a RegExp like so `new RegExp(yourString)` — so be sure to escape properly.

### E.g. `/foo-.+/`

The following patterns are considered warnings:

```scss
@mixin boo-bar {
```

The following patterns are *not* considered warnings:

```scss
@mixin foo-bar {
```
