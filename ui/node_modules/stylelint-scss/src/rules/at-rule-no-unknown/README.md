# at-rule-no-unknown

Disallow unknown at-rules. Should be used **instead of** stylelint's [at-rule-no-unknown](https://stylelint.io/user-guide/rules/at-rule-no-unknown).

```css
    @unknown (max-width: 960px) {}
/** ↑
 * At-rules like this */
```

This rule is basically a wrapper around the mentioned core rule, but with added SCSS-specific `@`-directives. So if you use the core rule, `@if`, `@extend` and other Sass-y things will get warnings. You must to disable core rule to make this rule work:

```json
{
  "rules": {
    "at-rule-no-unknown": null,
    "scss/at-rule-no-unknown": true
  }
}
```

## Options

### `true`

The following patterns are considered warnings:

```css
@unknown {}
```

The following patterns are *not* considered warnings:

```css
@function foo () {}
```

```css
@while ($i == 1) {}
```

```css
@media (max-width: 960px) {}
```

```css
@if ($i) {} @else {}
```

## Optional secondary options

### `ignoreAtRules: ["/regex/", "string"]`

Given:

```js
["/^my-/i", "custom"]
```

The following patterns are *not* considered warnings:

```css
@my-at-rule "x.css";
```

```css
@my-other-at-rule {}
```

```css
@MY-OTHER-AT-RULE {}
```

```css
@custom {}
```
