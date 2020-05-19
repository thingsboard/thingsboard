# operator-no-unspaced

Disallow unspaced operators in Sass operations.

```scss
a { width: 10px*$n; }
/**            ↑
 * The space around this operator */
```

This rule checks math operators (`+`, `-`, `/`, `*`, `%`) and comparison operators (`>`, `<`, `!=`, `==`, `>=`, `<=`).

Not all symbols that correspond to math operators are actually considered operators by Sass. Some of the exceptions are:

* `+` and `-` as signs before values;
* `+` and `-` as signs in [space-delimited lists](https://sass-lang.com/documentation/operators/string);
* `-` as part of [a string](https://sass-lang.com/documentation/operators/string) or [a Sass identifier](https://sass-lang.com/documentation/operators/numeric#unary-operators), e.g. a variable;
* `/` as a CSS delimiter in property values like `font: 10px/1.2 Arial;` ([read more](https://sass-lang.com/documentation/operators/numeric#slash-separated-values)).

For more details refer to [Sass official documentation](https://sass-lang.com/documentation/file.SASS_REFERENCE.html). An online Sass compiler - [Sassmeister](https://www.sassmeister.com/) - could also come in handy.

The following patterns are considered warnings:

```scss
a { width: 10+1; }
```

```scss
a { width: 10+ 1; }
```

```scss
a { width: 10-1; }
```

```scss
a { width: 10px* 1.5; }
```

```scss
@if ($var==10) { ... }
```

```scss
a { width: 10px  * 1.5; } // More than one space
```

```scss
a { width: (10) /1; } // Has a value inside parens on one side, so is an operation
```

```scss
// Operations can be inside interpolation in selectors, property names, etc.
.class#{1 +1}name {
  color: red;
}

p {
  background-#{\"col\" +\"or\"}: red;
}
```

The following patterns are *not* considered warnings:

```scss
a { width: 10 * 1; }
```

```scss
a { width: 10 +1; } // A space-delimited Sass list
```

```scss
// A space-delimited Sass list, in "10px-" "10" is a number, "px-" is a unit
a { width: 10px- 1; }
```

```scss
a { width: 10px/1; } // Compiled as CSS, as in "font: 10px/1 ..."
```

```scss
a { width: (10) /#{1}; } // Has interpolation on one of the sides, so not an operation
```

```scss
a { width: $var-1; } // "$var-1" is a variable name
```

```scss
a { width: "10*1"; } // Inside a string, ignored
```

```scss
// Linebreak will do as a whitespace; indentation before "-" and trailing spaces after "1" are left to the corresponding stylelint rules
a {
  width: 1  
    - a;
}
```
