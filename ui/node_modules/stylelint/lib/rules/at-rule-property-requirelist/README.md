# at-rule-property-requirelist

Specify a requirelist of properties for an at-rule.

```css
    @font-face { font-display: swap; font-family: 'foo'; }
/**  ↑           ↑                   ↑ 
 *  At-rule and required property names */
```

## Options

`object`: `{
  "at-rule-name": ["array", "of", "properties"]
}`

Given:

```js
{
  "font-face": ["font-display", "font-family", "font-style"]
}
```

The following patterns are considered violations:

```css
@font-face {
    font-family: 'foo';
    src: url('./fonts/foo.woff2') format('woff2'); 
}
```

```css
@font-face {
    font-family: 'foo';
    font-style: normal;
    src: url('./fonts/foo.woff2') format('woff2'); 
}
```

The following patterns are *not* considered violations:

```css
@font-face {
    font-display: swap;
    font-family: 'foo';
    font-style: normal;
    src: url('./fonts/foo.woff2') format('woff2'); 
}
```
