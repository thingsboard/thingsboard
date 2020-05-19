# dimension-no-non-numeric-values

Interpolating a value with a unit (e.g. `#{$value}px`) results in a
_string_ value, not as numeric value. This value then cannot be used in
numerical operations.  It is better to use arithmetic to apply a unit to a
number (e.g. `$value * 1px`).

This rule requires that all interpolation for values should be in the format `$value * 1<unit>` instead of `#{value}<unit>`

```scss
$value: 4;

p {
  padding: #{value}px;
//         ↑         ↑
//  should be $value * 1px
}
```

## Options

### `true`

The following patterns are considered violations:

```scss
$value: 4;

p {
  padding: #{value}px;
}
```

The following patterns are _not_ considered violations:

```scss
$value: 4;

p {
  padding: $value * 1px;
}
```

## List of units
Font-relative lengths ([link](https://www.w3.org/TR/css-values-4/#font-relative-lengths))
* em
* ex
* cap
* ch
* ic
* rem
* lh
* rlh

Viewport-relative lengths ([link](https://www.w3.org/TR/css-values-4/#viewport-relative-lengths))
* vw
* vh
* vi
* vb
* vmin
* vmax

Absolute lengths ([link](https://www.w3.org/TR/css-values-4/#absolute-lengths))
* cm
* mm
* Q
* in
* pc
* pt
* px

Angle units ([link](https://www.w3.org/TR/css-values-4/#angles))
* deg
* grad
* rad
* turn

Duration units ([link](https://www.w3.org/TR/css-values-4/#time))
* s
* ms

Frequency units ([link](https://www.w3.org/TR/css-values-4/#frequency))
* Hz
* kHz

Resolution units ([link](https://www.w3.org/TR/css-values-4/#resolution))
* dpi
* dpcm
* dppx
* x

Flexible lengths ([link](https://www.w3.org/TR/css-grid-1/#fr-unit))
* fr
