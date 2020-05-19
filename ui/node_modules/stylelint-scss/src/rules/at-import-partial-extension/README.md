# at-import-partial-extension

Require or disallow extension in `@import` commands.

```scss
@import "file.scss";
/**           ↑
 * This extension */
```

The rule ignores [cases](https://sass-lang.com/documentation/at-rules/import) when Sass considers an `@import` command just a plain CSS import:

- If the file’s extension is `.css`.
- If the filename begins with `http://` (or any other protocol).
- If the filename is a `url()`.
- If the `@import` has any media queries.

## Options

`string`: `"always"|"never"`

### `"always"`

The following patterns are considered warnings:

```scss
@import "foo";
```

```scss
@import "path/fff";
```

```scss
@import "path\\fff";
```

```scss
@import "df/fff", "1.SCSS";
```

The following patterns are _not_ considered warnings:

```scss
@import "fff.scss";
```

```scss
@import "path/fff.scss";
```

```scss
@import url("path/_file.css"); /* has url(), so doesn't count as a partial @import */
```

```scss
@import "file.css"; /* Has ".css" extension, so doesn't count as a partial @import */
```

```scss
/* Both are URIs, so don't count as partial @imports */
@import "http://_file.scss";
@import "//_file.scss";
```

```scss
@import "file.scss" screen; /* Has a media query, so doesn't count as a partial @import */
```

### `"never"`

The following patterns are considered warnings:

```scss
@import "foo.scss";
```

```scss
@import "path/fff.less";
```

```scss
@import "path\\fff.ruthless";
```

```scss
@import "df/fff", "1.SCSS";
```

The following patterns are _not_ considered warnings:

```scss
@import "foo";
```

```scss
@import "path/fff";
```

```scss
@import url("path/_file.css"); /* has url(), so doesn't count as a partial @import */
```

```scss
@import "file.css"; /* Has ".css" extension, so doesn't count as a partial @import */
```

```scss
/* Both are URIs, so don't count as partial @imports */
@import "http://_file.scss";
@import "//_file.scss";
```

```scss
@import "file.scss" screen; /* Has a media query, so doesn't count as a partial @import */
```
