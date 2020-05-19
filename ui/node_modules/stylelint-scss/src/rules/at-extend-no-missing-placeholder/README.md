# at-extend-no-missing-placeholder

Disallow at-extends (`@extend`) with missing placeholders.

Using a class selector with the `@extend` directive usually results in more generated CSS than when using a placeholder selector. Furthermore, Sass specifically introduced placeholder selectors in order to be used with `@extend`.

See [Mastering Sass extends and placeholders](http://8gramgorilla.com/mastering-sass-extends-and-placeholders/).

```scss
.foo {
  @extend %bar
//        ↑
// This is a placeholder selector
}
```

The following patterns are considered warnings:

```scss
p {
  @extend .some-class;
}
```

```scss
p {
  @extend #some-identifer;
}
```

```scss
p {
  @extend .blah-#{$dynamically_generated_name};
}
```

The following patterns are *not* considered warnings:

```scss
p {
  @extend %placeholder;
}
```

```scss
p {
  @extend #{$dynamically_generated_placeholder_name};
}
```
