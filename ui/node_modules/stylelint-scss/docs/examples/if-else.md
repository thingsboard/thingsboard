# `@if`/`@else`

A stylesheet author might want to treat `@if` and `@else` in a special manner, for example `@else` should be on the same line as its `@if`'s closing brace while all the other blocks and at-rules has to have newline after their closing brace. stylelint-scss has some rules for this, but from the core stylelint's point of view, `@if` and `@else` SCSS statements are pretty much regular at-rules, so they comply to corresponding `at-rule-...` and `block-...` rules. Below are some configurations that might help you achieve the needed patterns.

---

**Config 1**: `@else` is on the same line as the preceding `@if`/`@else`'s `}`, space between them. Empty line before all at-rules (except `@else`), space before `{`, newline after all `}` except `@if`'s and `@else`'s. 

```json
{
  "plugins": [
    "stylelint-scss"
  ],
  "rules": {
    "at-rule-empty-line-before": [
      "always", {
        "ignoreAtRules": [ "else" ]
      }
    ],
    "block-opening-brace-space-before": "always",
    "block-closing-brace-newline-after": [
      "always", {
        "ignoreAtRules": [ "if", "else" ]
      }
    ],
    "at-rule-name-space-after": "always",
    "rule-empty-line-before": "always",
    "scss/at-else-closing-brace-newline-after": "always-last-in-chain",
    "scss/at-else-closing-brace-space-after": "always-intermediate",
    "scss/at-else-empty-line-before": "never",
    "scss/at-if-closing-brace-newline-after": "always-last-in-chain",
    "scss/at-if-closing-brace-space-after": "always-intermediate"
  }
}
```

This code is considered **valid**
```scss
@if {
  // ...
}

a {}

@if {
  // ...
} @else {
  // ...
}

a {}

@if {
  // ...
} @else if {
  // ...
} @else {
  // ...
}

@if {
  // ...
} @else
if {
  // ...
} @else {
  // ...
}

a {}
```

These patterns are considered **non-valid**:

```scss
@if {
  // ...
} a {}
```
```scss
@if {
  // ...
}@else {
  // ...
}
```
```scss
@if {
  // ...
} @else if{
  // ...
} @else {
  // ...
}
```
```scss
@if {
  // ...
} @else if{
  // ...
} @else {
  // ...
}
```

---

**Config 2**: `@else` is on a newline, no empty line before it. 

```json
{
  "plugins": [
    "stylelint-scss"
  ],
  "rules": {
    "at-rule-empty-line-before": [
      "always", {
        "ignoreAtRules": [ "else" ]
      }
    ],
    "at-rule-name-space-after": "always",
    "block-opening-brace-space-before": "always",
    "block-closing-brace-newline-after": "always",
    "at-else-empty-line-before": "never"
  }
}
```

This code is considered **valid**:
```scss
@if {
  // ...
}
@else {
  // ...
}
```

This code is considered **non-valid**:
```scss
@if {
  // ...
}

@else {
  // ...
}
```
