# function-quote-no-quoted-strings-inside

Disallow quoted strings inside the [quote function](https://sass-lang.com/documentation/modules/string#quote)

```scss
p {
  font-family: quote("Helvetica");
  /**                ↑         ↑
   * These quotes are unnecessary
   */
}
```

## Options

### `true`

The following patterns are considered violations:

```scss
a {
  font-family: quote("Helvetica");
}
```

```scss
$font: "Helvetica";
p {
  font-family: quote($font);
}
```

The following patterns are _not_ considered violations:

```scss
a {
  color: quote(blue);
}
```

```scss
$font: Helvetica;
p {
  font-family: quote($font);
}
```
