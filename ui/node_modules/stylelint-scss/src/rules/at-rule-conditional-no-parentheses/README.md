# at-rule-conditional-no-parentheses

Disallow parentheses in conditional @ rules (if, elsif, while)

```css
    @if (true) {}
/**     ↑    ↑
 * Get rid of parentheses like this. */
```



## Options

### `true`

The following patterns are considered warnings:

```scss
@if(true)
```

```scss
@else if(true)
```

```scss
@while(true)
```

The following patterns are *not* considered warnings:

```scss
@if true
```

```scss
@else if true
```

```scss
@while true
```
