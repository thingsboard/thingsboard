# function-comma-newline-before

Require a newline or disallow whitespace before the commas of functions.

```css
  a { transform: translate(1
    , 1) }
/** ↑
 * This comma */
```

The `--fix` option on the [command line](../../../docs/user-guide/cli.md#autofixing-errors) can automatically fix all of the problems reported by this rule.

## Options

`string`: `"always"|"always-multi-line"|"never-multi-line"`

### `"always"`

There *must always* be a newline before the commas.

The following patterns are considered violations:

```css
a { transform: translate(1,1) }
```

```css
a { transform: translate(1 ,1) }
```

```css
a { transform: translate(1,
  1) }
```

The following patterns are *not* considered violations:

```css
a {
  transform: translate(1
    ,1)
}
```

```css
a {
  transform: translate(1
    , 1)
}
```

### `"always-multi-line"`

There *must always* be a newline before the commas in multi-line functions.

The following patterns are considered violations:

```css
a { transform: translate(1,
  1) }
```

The following patterns are *not* considered violations:

```css
a { transform: translate(1,1) }
```

```css
a { transform: translate(1 ,1) }
```

```css
a {
  transform: translate(1
    ,1)
}
```

```css
a {
  transform: translate(1
    , 1)
}
```

### `"never-multi-line"`

There *must never* be whitespace before the commas in multi-line functions.

The following patterns are considered violations:

```css
a { transform: translate(1 ,
  1) }
```

The following patterns are *not* considered violations:

```css
a { transform: translate(1 ,1) }
```

```css
a { transform: translate(1 , 1) }
```

```css
a {
  transform: translate(1,
    1)
}
```
