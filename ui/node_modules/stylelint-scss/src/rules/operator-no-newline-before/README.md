# operator-no-newline-before

Disallow linebreaks before Sass operators.

```scss
a { width: 10px
    + $n; }
/** ↑
 * Linebreaks before this */
```

This rule checks math operators (`+`, `-`, `/`, `*`, `%`) and comparison operators (`>`, `<`, `!=`, `==`, `>=`, `<=`).

Not all symbols that correspond to math operators are actually considered operators by Sass. Some of the exceptions are:

* `+` and `-` as signs before values;
* `+` and `-` as signs in [space-delimited lists](https://sass-lang.com/documentation/operators/string);
* `-` as part of [a string](https://sass-lang.com/documentation/operators/string) or [a Sass identifier](https://sass-lang.com/documentation/operators/numeric#unary-operators), e.g. a variable;
* `/` as a CSS delimiter in property values like `font: 10px/1.2 Arial;` ([read more](https://sass-lang.com/documentation/operators/numeric#slash-separated-values)).

For more details refer to [Sass official documentation](https://sass-lang.com/documentation). An online Sass compiler - [Sassmeister](https://www.sassmeister.com/) - could also come in handy.

The following patterns are considered warnings:

```scss
a { width: 10
+ 1; }
```

```scss
a {
  width: 10
    + 1;
}
```

The following patterns are *not* considered warnings:

```scss
a {
  width: 10px
    -1; // not a math operator, ignored
}
```

```scss
a { width: 10px     -    1; }
```

```scss
a {
  width: 100px +
    $var * 0.5625; // the newline is not right before the operator
}
```
