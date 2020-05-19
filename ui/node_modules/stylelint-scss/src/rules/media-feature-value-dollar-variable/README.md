# media-feature-value-dollar-variable

Require a media feature value be a `$`-variable or disallow `$`-variables in media feature values.

```scss
@media (max-width: $var) { a { color: red; } }
//                 ↑
// Require or disallow this
}
```

## Options

`string`: `"always"|"never"`

### `"always"`

A media feature value *must consist* of just a single `$`-variable (possibly with inteprolation).

The following patterns are considered warnings:

```scss
@media (max-width: 300px) { b { color: red; } }
```

```scss
@media (max-width: $var + 10px) { b { color: red; } }
```

```scss
@media screen and (max-width: $var), or (min-width: 100px){ b { color: red; } }
```

```scss
@media screen and (max-width: #{$val} + 10px) { a { display: none; } }
```

```scss
@media screen and (max-width: #{$val + $x} ) { a { display: none; } }
```

```scss
@media screen and (min-width: funcName($p)){ b { color: red; } }
```

The following patterns are *not* considered warnings:

```scss
@media ( max-width: $var ) {b { color: red; }}
```

```scss
@media ( max-width: #{$var}) {b { color: red; }}
```

### `"never"`

There *must never* be a `$`-variable in a media feature value. Even as a parameter to a function call.

The following patterns are considered warnings:

```scss
@media screen and (min-width: $var){ b { color: red; } }
```

```scss
@media screen and (min-width: 100px + $var){ b { color: red; } }
```

```scss
@media screen and (min-width: funcName($var)){ b { color: red; } }
```

The following patterns are *not* considered warnings:

```scss
@media screen and (min-width: 100px){ b { color: red; } }
```

```scss
@media screen and (min-width: 100px + 10px){ b { color: red; } }
```

```scss
@media screen and (min-width: funcName(10px)){ b { color: red; } }
```
