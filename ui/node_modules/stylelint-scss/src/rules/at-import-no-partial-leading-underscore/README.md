# at-import-no-partial-leading-underscore

Disallow leading underscore in partial names in `@import`.

```scss
@import "path/to/_file"
/**              ↑
 *   Disallow this */
```

The rule ignores [cases](https://sass-lang.com/documentation/at-rules/import) when Sass considers an `@import` command just a plain CSS import:

* If the file’s extension is `.css`.
* If the filename begins with `http://` (or any other protocol).
* If the filename is a `url()`.
* If the `@import` has any media queries.


The following patterns are considered warnings:

```scss
@import "_foo";
```

```scss
@import "path/_fff";
```

```scss
@import "path\\_fff"; /* Windows delimiters */
```

```scss
@import "df/fff", '_1.scss';
```

The following patterns are *not* considered warnings:

```scss
@import "_path/fff"; /* underscore in a directory name, not in a partial name */
```

```scss
@import url("path/_file.css"); /* has url(), so doesn't count as a partial @import */
```

```scss
@import "_file.css"; /* Has ".css" extension, so doesn't count as a partial @import */
```

```scss
/* Both are URIs, so don't count as partial @imports */
@import "http://_file.scss";
@import "//_file.scss";
```

```scss
@import "_file.scss" screen; /* Has a media query, so doesn't count as a partial @import */
```
