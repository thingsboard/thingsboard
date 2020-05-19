# declaration-nested-properties-no-divided-groups

Disallow nested properties of the same "namespace" be divided into multiple groups.

```scss
/* Such groups: */
font: { /* `font` is a "namespace" */
  size: 16px;
  weight: 700;
}
```

A "namespace" is everything before the first `-` in property names, e.g. `margin` in `margin-bottom`. It is the "namespace" that is used as a root identifier for a nested properties group (`font` in the example above).

[Sass official docs on nested properties](https://sass-lang.com/documentation/style-rules/declarations#nesting).

The following patterns are considered warnings:

```scss
a {
  background: url(img.png) center {
    size: auto;
  }
  background: {
    repeat: no-repeat;
  }
}
```

The following patterns are *not* considered warnings:

```scss
a {
  background: url(img.png) center {
    size: auto;
  }
  background-repeat: no-repeat;
}
```

```scss
a {
  background: url(img.png) center no-repeat {
    color: red;
  }
  margin: 10px {
    left: 1px;
  }
}
```

```scss
a {
  background: url(img.png) center {
    size: auto;
  }
  background :red {
    repeat: no-repeat;
  }
}
/* There is no space after the colon in `background :red` so it is considered A SELECTOR and is compiled into: 

a background :red {
  color: blue;
}

*/
```
