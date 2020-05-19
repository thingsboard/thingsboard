# at-function-named-arguments

Require named parameters in SCSS function call rule.

```scss
animation: animation($duration: 250ms) {
//                   ↑
// Require or disallow this
```

## Options

`string`: `"always"|"never"`

### `always`

The following patterns are considered warnings:

```scss
.foo {
  animation: animation(250ms, 100ms, infinite);
}
```

```scss
.foo {
  animation: animation(250ms);
}
```

```scss
.foo {
  border: reset($value: 20, "bar", $color: #fff);
}
```

The following patterns are _not_ considered warnings:

```scss
.foo {
  animation: animation($duration: 250ms);
}
```

```scss
.foo {
  animation: animation($duration: 250ms, $delay: 100ms, $direction: infinite);
}
```

### `never`

The following patterns are considered warnings:

```scss
.foo {
  border: reset($value: 20);
}
```

```scss
.foo {
  animation: animation($duration: 250ms, $delay: 100ms, $direction: infinite);
}
```

```scss
.foo {
  border: reset($value: 20, "bar", $color: #fff);
}
```

The following patterns are _not_ considered warnings:

```scss
.foo {
  animation: animation(250ms, 100ms, infinite);
}
```

## Optional secondary options

### `"ignore": ["single-argument"]`

Given:

```json
{ "ignore": ["single-argument"] }
```

The following patterns are _not_ considered warnings:

```scss
.foo {
  @include animation($duration: 250ms);
}
```

```scss
.foo {
  @include reset(20);
}
```

### `ignoreFunctions: ["/regex/", "string"]`

Given:

```js
["always", { ignoreFunctions: ["/^my-/i", "custom"] }];
```

The following patterns are _not_ considered warnings:

```scss
.foo {
  border: custom(20, 30);
}
```

```scss
.foo {
  border: my-func(20, 30);
}
```

```scss
.foo {
  border: MY-FUNC(20, 30);
}
```
