# no-descending-specificity

Disallow selectors of lower specificity from coming after overriding selectors of higher specificity.

```css
    #container a { top: 10px; } a { top: 0; }
/** ↑                           ↑
 * The order of these selectors represents descending specificity */
```

Source order is important in CSS, and when two selectors have the *same* specificity, the one that occurs *last* will take priority. However, the situation is different when one of the selectors has a *higher* specificity. In that case, source order does *not* matter: the selector with higher specificity will win out even if it comes first.

The clashes of these two mechanisms for prioritization, source order and specificity, can cause some confusion when reading stylesheets. If a selector with higher specificity comes *before* the selector it overrides, we have to think harder to understand it, because it violates the source order expectation. **Stylesheets are most legible when overriding selectors always come *after* the selectors they override.** That way both mechanisms, source order and specificity, work together nicely.

This rule enforces that practice *as best it can*, reporting fewer errors than it should. It cannot catch every *actual* overriding selector, but it can catch certain common mistakes.

## How it works

**This rule looks at the last *compound selector* in every full selector, and then compares it with other selectors in the stylesheet that end in the same way.**

So `.foo .bar` (whose last compound selector is `.bar`) will be compared to `.bar` and `#baz .bar`, but not to `#baz .foo` or `.bar .foo`.

And `a > li#wag.pit` (whose last compound selector is `li#wag.pit`) will be compared to `div li#wag.pit` and `a > b > li + li#wag.pit`, but not to `li` or `li #wag`, etc.

Selectors targeting pseudo-elements are not considered comparable to similar selectors without the pseudo-element, because they target other elements on the rendered page. For example, `a::before {}` will not be compared to `a:hover {}`, because `a::before` targets a pseudo-element whereas `a:hover` targets the actual `<a>`.

This rule only compares rules that are within the same media context. So `a {} @media print { #baz a {} }` is fine.

This rule resolves nested selectors before calculating the specificity of the selectors.

## DOM Limitations

The linter can only check the CSS to check for specificity order. It does not have access to the HTML or DOM in order to interpret the use of the CSS.

This can lead to valid linting errors appearing to be invalid at first glance.

For example the following will cause an error:

```css
.component1 a {}
.component1 a:hover {}
.component2 a {}
```

This is a correct error because the `a:hover` on line 2 has a higher specificity than the `a` on line 3.

This may lead to confusion because "the two selectors will never match the same `a` in the DOM". However, since the linter does not have access to the DOM it can not evaluate this, and therefore correctly reports the error about descending specificity.

It may be possible to restructure your CSS to remove the error, otherwise it is recommended that you disable the rule for that line and leave a comment saying why the error should be ignored. Note that disabling the rule will cause additional valid errors from being reported.

## Options

### `true`

The following patterns are considered violations:

```css
b a {}
a {}
```

```css
a + a {}
a {}
```

```css
b > a[foo] {}
a[foo] {}
```

```css
a {
  & > b {}
}
b {}
```

```css
@media print {
  #c a {}
  a {}
}
```

The following patterns are *not* considered violations:

```css
a {}
b a {}
```

```css
a {}
a + a {}
```

```css
a[foo] {}
b > a[foo] {}
```

```css
b {}
a {
  & > b {}
}
```

```css
a::before {}
a:hover::before {}
a {}
a:hover {}
```

```css
@media print {
  a {}
  #c a {}
}
```

```css
a {}
@media print {
  #baz a {}
}
```

### `ignore: ["selectors-within-list"]`

Ignores selectors within list of selectors.

The following patterns are considered violations:

```css
b a {}
h1 {}
h2 {}
h3 {}
a {}
```

The following patterns are *not* considered violations:

```css
b a {}
h1, h2, h3, a {}
```
