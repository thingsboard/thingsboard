# function-unquote-no-unquoted-strings-inside

Disallow unquoted strings inside the [unquote function](https://sass-lang.com/documentation/modules/string#unquote)

```scss
p {
  font-family: unquote(Helvetica);
  /**          ↑     ↑
   * This function call is unnecessary
   */
}
```

## Options

### `true`

The following patterns are considered violations:

```scss
a {
  font-family: unquote(Helvetica);
}
```

```scss
$font: Helvetica;
p {
  font-family: unquote($font);
}
```

The following patterns are _not_ considered violations:

```scss
a {
  color: unquote("blue");
}
```

```scss
$font: "Helvetica";
p {
  font-family: unquote($font);
}
```
