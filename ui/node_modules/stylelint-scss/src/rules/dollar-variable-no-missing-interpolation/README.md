# dollar-variable-no-missing-interpolation

Disallow Sass variables that are used without interpolation with CSS features that use custom identifiers.

```scss
.class {
  $var: "my-anim";
  animation-name: $var;
//                ↑
// This variable needs to be interpolated
// because its value is a string
}
```

Sass variables that contain a custom identifier as a string always require interpolation when used. Some CSS [at-rules](https://css-tricks.com/the-at-rules-of-css/) require variable interpolation even when the custom identifier value is not a string.

For example, your CSS animation could look like this:

```scss
animation: myAnim 5s;
```

When you store your custom identifier as string in a Sass variable...

```scss
$myVar: "myAnim";
```

...then you need to make sure that the variable is interpolated when it gets used:

```scss
animation: #{$myVar} 5s;
```

If you do not interpolate the variable, Sass will compile your animation name to a string, producing invalid CSS:

```scss
animation: "myAnim" 5s;
```

This rule can only check for variables that are defined inside the same file where they are used.

The following patterns are considered warnings:

```scss
$var: my-anim;

@keyframes $var {}
```

```scss
$var: "circled-digits";

@counter-style $var {
  system: fixed;
  symbols: ➀ ➁ ➂;
  suffix: ' ';
  speak-as: numbers;
}
```

```scss
$var: "my-counter";

body {
  counter-reset: $var;
}
```

```scss
$var: "my-anim";

@supports (animation-name: $var) {
  @keyframes {}
}
```

The following patterns are *not* considered warnings:

```scss
$var: my-anim;

@keyframes #{$var} {}
```

```scss
$var: circled-digits;

@counter-style #{$var} {
  system: fixed;
  symbols: ➀ ➁ ➂;
  suffix: ' ';
  speak-as: numbers;
}
```

```scss
$var: my-counter;

body {
  counter-reset: $var;
}
```

```scss
$var: my-anim;

@supports (animation-name: $var) {
  @keyframes {}
}
```
