# selector-nest-combinators

Require or disallow nesting of combinators in selectors

```scss
/* Examples of selectors without nesting of combinators */
.foo .bar {}

.foo.bar {}

.foo > .bar {}

.foo:hover {}

/* Corresponding selectors with combinators nested */
.foo {
  .bar {}
}

.foo {
  &.bar {}
}

.foo {
  & > .bar {}
}

.foo {
  &:hover {}
}
```

## Options

`string`: `"always"|"never"`

### `"always"`

*Every combinator* in a selector *must be* nested where possible without altering the existing resolved selector.

Sections of selectors preceding a parent selector are ignored with `always`.
e.g.

```scss
.foo {
  .bar.baz & {}
}
```

Sections of selectors within pseudo selectors are also ignored with `always`.
e.g.

```scss
.foo {
  &:not(.bar .baz) {}
}
```

while this could be refactored to:

```scss
.bar {
  .baz {
    .foo:not(&) {}
  }
}
```

There are variances in the way this is compiled between compilers, therefore for the purposes of this rule the selector sections within pseudo selectors are being ignored.

The following patterns are considered warnings:

```scss
.foo .bar {}
```

```scss
.foo.bar {}
```

```scss
.foo > .bar {}
```

```scss
.foo:hover {}
```

```scss
a[href] {}
```

```scss
* + li {}
```

```scss
:nth-child(2n - 1):last-child {}
```

The following patterns are *not* considered warnings:

```scss
.foo {
  .bar {}
}
```

```scss
.foo {
  &.bar {}
}
```

```scss
.foo {
  & > .bar {}
}
```

```scss
.foo {
  &:hover {}
}
```

```scss
a {
  &[href] {}
}
```

```scss
* {
  & + li {}
}
```

```scss
:nth-child(2n - 1) {
  &:last-child {}
}
```

### `"never"`

Nested of selectors are not allowed.

The following patterns are considered warnings:

```scss
.foo {
  .bar {}
}
```

```scss
.foo {
  &.bar {}
}
```

```scss
.foo {
  & > .bar {}
}
```

```scss
.foo {
  &:hover {}
}
```

```scss
a {
  &[href] {}
}
```

```scss
* {
  & + li {}
}
```

```scss
:nth-child(2n - 1) {
  &:last-child {}
}
```

The following patterns are *not* considered warnings:

```scss
.foo .bar {}
```

```scss
.foo.bar {}
```

```scss
.foo > .bar {}
```

```scss
.foo:hover {}
```

```scss
a[href] {}
```

```scss
* + li {}
```

```scss
:nth-child(2n - 1):last-child {}
```
