# value-list-max-empty-lines

Limit the number of adjacent empty lines within value lists.

```css
a {
  box-shadow:
    inset 0 2px 0 #dcffa6,
                    /* ← */
    0 2px 5px #000; /* ↑ */
}                   /* ↑ */
/**                    ↑
 *       This empty line */
```

The `--fix` option on the [command line](../../../docs/user-guide/cli.md#autofixing-errors) can automatically fix all of the problems reported by this rule.

## Options

`int`: Maximum number of adjacent empty lines allowed.

For example, with `0`:

The following patterns are considered violations:

```css
a {
  padding: 10px

    10px
    10px
    10px
}
```

```css
a {
  padding:
    10px
    10px
    10px

    10px
}
```

```css
a {
  box-shadow: inset 0 2px 0 #dcffa6,

    0 2px 5px #000;
}
```

```css
a {
  box-shadow:
    inset 0 2px 0 #dcffa6,

    0 2px 5px #000;
}
```

The following patterns are *not* considered violations:

```css
a {
  padding: 10px 10px 10px 10px
}
```

```css
a {
  padding: 10px
    10px
    10px
    10px
}
```

```css
a {
  padding:
    10px
    10px
    10px
    10px
}
```

```css
a {
  box-shadow: inset 0 2px 0 #dcffa6, 0 2px 5px #000;
}
```

```css
a {
  box-shadow: inset 0 2px 0 #dcffa6,
    0 2px 5px #000;
}
```

```css
a {
  box-shadow:
    inset 0 2px 0 #dcffa6,
    0 2px 5px #000;
}
```
