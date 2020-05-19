# no-dollar-variables

Disallow dollar variables within a stylesheet.

```scss
    $a: 1;
/** ↑
 * These dollar variables */
```

## Options

### `true`

The following patterns are considered violations:

```scss
$a: 1;
```

```scss
$a: 1;
$b: 2;
```

```scss
.b {
  $a: 1;
}
```

The following patterns are *not* considered violations:

```scss
a {
  color: blue;
}
```
