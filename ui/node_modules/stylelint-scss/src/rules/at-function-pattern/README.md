# at-function-pattern

Specify a pattern for Sass/SCSS-like function names.

```scss
@function grid-width($n) {
/**       ↑
 * The pattern of this */
```

## Options

`regex` or `string`

A string will be translated into a RegExp like so `new RegExp(yourString)` — so be sure to escape properly.

### E.g. `/foo-.+/`

The following patterns are considered warnings:

```scss
@function boo-bar ($n) {
```

The following patterns are *not* considered warnings:

```scss
@function foo-bar ($n){
```
