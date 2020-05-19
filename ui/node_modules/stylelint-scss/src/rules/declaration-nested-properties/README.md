# declaration-nested-properties

Require or disallow properties with `-` in their names to be in a form of a nested group.

```scss
/* This is properties nesting: */  
font: {
  size: 16px;
  weight: 700;
}
```

[Sass official docs on nested properties](https://sass-lang.com/documentation/style-rules/declarations#nesting).

## Options

`string`: `"always"|"never"`

### `"always"`

*Every property* with a `-` in its name *must be* inside a nested property group.

Property names with browser prefixes are ignored with `always`.

The following patterns are considered warnings:

```scss
a {
  margin-left: 10px;
}
```

```scss
a {
  font: {
    size: 10px;
  }
  font-weight: 400; // This one should be nested either
}
```

The following patterns are *not* considered warnings:

```scss
a {
  margin: {
    left: 10px;
  }
}
```

```scss
a {
  font: 10px/1.1 Arial {
    weight: bold;
  }
}
```

```scss
a {
  -webkit-box-sizing: border-box;
  -webkit-box-shadow: 1px 0 0 red;
}
```

### `"never"`

Nested properties are not allowed.

The following patterns are considered warnings:

```scss
a {
  margin: {
    left: 10px;
  }
}
```

```scss
a {
  background: red {
    repeat: no-repeat;
  }
}
```

The following patterns are *not* considered warnings:

```scss
a {
  background-color: red;
  background-repeat: no-repeat;
}
```

```scss
a {
  background:red {
    color: blue;
  }
}

/* There is no space after the colon in `background:red` so it is considered A SELECTOR and is compiled into: 

a background:red {
  color: blue;
}

*/
```

## Optional options

### `except: ["only-of-namespace"]`

*Works only* with `"always"` and reverses its effect for a property if it is the only one with the corresponding "namespace" (e.g. `margin` in `margin-top`) in a ruleset.

The following patterns are considered warnings:

```scss
a {
  font-family: Arial, sans-serif;
  font-size: 10px;
}
```

```scss
a {
  font: {
    size: 10px; // Only one rule nested (reverse "always")
  }
}
```

```scss
a {
  font: {
    size: 10px; // Prop 1, ...
  }
  font-weight: 400; // ... prop 2, so must be nested as well
}
```

The following patterns are *not* considered warnings:

```scss
a {
  position: absolute;
  background-color: red; // no other `background-` properties in a ruleset
}
```
