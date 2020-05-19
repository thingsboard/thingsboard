# selector-no-redundant-nesting-selector

Disallow redundant nesting selectors (`&`).

```scss
p {
  & a {}
//↑
// This type of selector
}
```

The following patterns are considered warnings:

```scss
p {
  & a {}
}
```

```scss
p {
  & > a {}
}
```

```scss
p {
  & .class {}
}
```

```scss
p {
  & + .foo {}
}
```

The following patterns are *not* considered warnings:

```scss
p {
  &.foo {}
}
```

```scss
p {
  .foo > & {}
}
```

```scss
p {
  &,
  .foo,
  .bar {
    margin: 0;
  }
}
```
