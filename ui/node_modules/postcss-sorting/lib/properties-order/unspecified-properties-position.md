# unspecified-properties-position

Specify position for properties not specified in [`properties-order`](./properties-order.md). This option only works if you've defined your own array of properties in `properties-order`.

`string`: `"top"|"bottom"|"bottomAlphabetical"`

Default value is `"bottom"`: unspecified properties will be placed *after* any specified properties.

With `"top"`, unspecified properties will be placed *before* any specified properties. With `"bottomAlphabetical"`, unspecified properties will be placed *after* any specified properties, and the unspecified properties will be in alphabetical order.

Given:

```js
{
	'properties-order': [
		'position',
		'z-index'
	],
	'unspecified-properties-position': 'bottom'
}
```

Before:

```css
a {
	leftover: yay;
	z-index: 1;
	position: absolute;
	bottom: 0;
}
b {
	top: 0;
	z-index: 2;
	leftover: yay;
	position: absolute;
}
```

After:

```css
a {
	position: absolute;
	z-index: 1;
	leftover: yay;
	bottom: 0;
}
b {
	position: absolute;
	z-index: 2;
	top: 0;
	leftover: yay;
}
```

---

Given:

```js
{
	'properties-order': [
		'position',
		'z-index'
	],
	'unspecified-properties-position': 'bottomAlphabetical'
}
```

Before:

```css
a {
	leftover: yay;
	z-index: 1;
	position: absolute;
	bottom: 0;
}
b {
	top: 0;
	z-index: 2;
	leftover: yay;
	position: absolute;
}
```

After:

```css
a {
	position: absolute;
	z-index: 1;
	bottom: 0;
	leftover: yay;
}
b {
	position: absolute;
	z-index: 2;
	leftover: yay;
	top: 0;
}
```

---

Given:

```js
{
	'properties-order': [
		'position',
		'z-index'
	],
	'unspecified-properties-position': 'top'
}
```

Before:

```css
a {
	leftover: yay;
	z-index: 1;
	position: absolute;
	bottom: 0;
}
b {
	top: 0;
	z-index: 2;
	leftover: yay;
	position: absolute;
}
```

After:

```css
a {
	leftover: yay;
	bottom: 0;
	position: absolute;
	z-index: 1;
}
b {
	top: 0;
	leftover: yay;
	position: absolute;
	z-index: 2;
}
```
