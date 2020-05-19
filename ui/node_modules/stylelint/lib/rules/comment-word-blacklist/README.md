# comment-word-blacklist

Specify a blacklist of disallowed words within comments.

```css
 /* words within comments */
/** ↑     ↑      ↑
 * These three words */
```

**Caveat:** Comments within *selector and value lists* are currently ignored.

## Options

`array|string|regexp`: `["array", "of", "words", /or/, "/regex/"]|"word"|"/regex/"`

If a string is surrounded with `"/"` (e.g. `"/^TODO:/"`), it is interpreted as a regular expression.

Given:

```js
["/^TODO:/", "badword"]
```

The following patterns are considered violations:

```css
/* TODO: */
```

```css
/* TODO: add fallback */
```

```css
/* some badword */
```

The following patterns are *not* considered violations:

```css
/* comment */
```
