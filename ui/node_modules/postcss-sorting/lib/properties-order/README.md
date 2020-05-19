# properties-order

Specify the order of properties within declaration blocks.

This rule ignore prefixes to determine properties order. E. g. `-moz-transform` is treated as `transform`. Shorthand properties *will always* precede their longhand forms (e.g. `border-style` will always be before `border-bottom-style`). Prefixed properties *will always* precede the unprefixed version (e. g. `-moz-transform` will be always before `transform`).

Recommended to use this rule only on source files, rather autoprefixed files. Some “non-standard” prefixes could be treated wrong. E. g. different flexbox implementations; `-ms-flex-align: center; align-items: center;` with alphabetical order will be sorted as `align-items: center; -ms-flex-align: center;` because alphabetically `flex-align` is after `align-item`.

This rule ignores variables (`$sass`, `@less`, `--custom-property`).

`string|array`: `"alphabetical"|["array", "of", "unprefixed", "property", "names"]`

## `"alphabetical"`

Properties will be ordered alphabetically.

Before:

```css
a {
	top: 0;
	color: pink;
}

a {
	-moz-transform: scale(1);
	transform: scale(1);
	-webkit-transform: scale(1);
}
```

After:

```css
a {
	color: pink;
	top: 0;
}

a {
	-moz-transform: scale(1);
	-webkit-transform: scale(1);
	transform: scale(1);
}
```

## `["array", "of", "unprefixed", "property", "names"]`

Within an order array, you should include unprefixed property names.

**By default, unlisted properties will be placed after all listed properties.** So if you specify an array and do not include `display`, that means that the `display` property can be included before or after all specified properties. *This can be changed with the [`unspecified-properties-position`](./unspecified-properties-position.md) option*.

Given:

```js
["transform", "top", "color"]
```

Before:

```css
a {
	color: pink;
	top: 0;
}

a {
	-moz-transform: scale(1);
	color: pink;
	transform: scale(1);
	-webkit-transform: scale(1);
}
```

After:

```css
a {
	top: 0;
	color: pink;
}

a {
	-moz-transform: scale(1);
	-webkit-transform: scale(1);
	transform: scale(1);
	color: pink;
}
```

---

Given:

```js
[
	"position",
	"top"
	"display",
	"z-index"
]
```

Before:

```css
a {
	z-index: 2;
	top: 0;

	position: absolute;
	display: block;
}
```

After:

```css
a {

	position: absolute;
	top: 0;
	display: block;
	z-index: 2;
}
```

Note: Empty line before `position` is preserved.
