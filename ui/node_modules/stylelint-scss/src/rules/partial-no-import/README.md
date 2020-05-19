# partial-no-import

Disallow non-CSS `@import`s in partial files.

```scss
// path/to/_file.scss:
/*         ↑ in partial files */

  @import "path/to/file.scss"
/*↑ Disallow imports */
```

The rule skips CSS files (doesn't report any `@import`s in them).

The rule also ignores [cases](https://sass-lang.com/documentation/at-rules/import) when Sass considers an `@import` command just a plain CSS import:

* If the file’s extension is `.css`.
* If the filename begins with `http://` (or any other protocol).
* If the filename is a `url()`.
* If the `@import` has any media queries.

The following patterns are considered warnings:

```scss
// path/to/_file.scss:

@import "foo.scss";
```

```scss
// path/to/_file.less:
@import "path/fff.less";
```

```scss
// path/to/_file.scss:
@import "path\\fff.supa";
```

The following patterns are *not* considered warnings:

```scss
// path/to/file.scss:
@import "path/fff";

/* @import in a file that is not a partial */
```

```scss
// path/to/_file.scss:
@import url("path/_file.css"); /* has url(), so doesn't count as a partial @import */
```

```scss
// path/to/_file.scss:
@import "file.css"; /* Has ".css" extension, so doesn't count as a partial @import */
```

```scss
// path/to/_file.scss:
@import "http://_file.scss";
@import "//_file.scss";
/* Both are URIs, so don't count as partial @imports */
```

```scss
// path/to/_file.scss:
@import "file.scss" screen; /* Has a media query, so doesn't count as a partial @import */
```
