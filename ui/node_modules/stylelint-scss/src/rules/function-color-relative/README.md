# function-color-relative

Encourage the use of the [scale-color](https://sass-lang.com/documentation/modules/color#scale-color) over:

* [darken](https://sass-lang.com/documentation/modules/color#darken)
* [desaturate](https://sass-lang.com/documentation/modules/color#desaturate)
* [fade-in](https://sass-lang.com/documentation/modules/color#fade-in)
* [fade-out](https://sass-lang.com/documentation/modules/color#fade-out)
* [lighten](https://sass-lang.com/documentation/modules/color#lighten)
* [opacify](https://sass-lang.com/documentation/modules/color#opacify)
* [saturate](https://sass-lang.com/documentation/modules/color#saturate)
* [transparentize](https://sass-lang.com/documentation/modules/color#transparentize)

```scss
p {
   color: saturate(blue, 20%);
  /**     ↑      ↑
   * This function should be scalar-color
   */
}
```

## Options

### `true`

The following patterns are considered violations:

```scss
p {
   color: saturate(blue, 20%);
}
```

```scss
p {
   color: desaturate(blue, 20%);
}
```

```scss
p {
   color: darken(blue, .2);
}
```

```scss
p {
   color: lighten(blue, .2);
}
```

```scss
p {
   color: opacify(blue, .2);
}
```

```scss
p {
   color: fade-in(blue, .2);
}
```

```scss
p {
   color: transparentize(blue, .2);
}
```

```scss
p {
   color: fade-out(blue, .2);
}
```

The following patterns are _not_ considered violations:

```scss
 p {
   color: scale-color(blue, $alpha: -40%);
 }
```
