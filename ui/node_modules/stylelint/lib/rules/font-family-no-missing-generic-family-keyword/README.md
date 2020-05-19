# font-family-no-missing-generic-family-keyword

Disallow missing generic families in lists of font family names.

```css
a { font-family: Arial, sans-serif; }
/**                     ↑
 * An example of generic family name */
```

The generic font family can be:

-   placed anywhere in the font family list
-   omitted if a keyword related to property inheritance or a system font is used

This rule checks the `font` and `font-family` properties.

## Options

### `true`

The following patterns are considered violations:

```css
a { font-family: Helvetica, Arial, Verdana, Tahoma; }
```

```css
a { font: 1em/1.3 Times; }
```

The following patterns are *not* considered violations:

```css
a { font-family: Helvetica, Arial, Verdana, Tahoma, sans-serif; }
```

```css
a { font: 1em/1.3 Times, serif, Apple Color Emoji; }
```

```css
a { font: inherit; }
```

```css
a { font: caption; }
```
