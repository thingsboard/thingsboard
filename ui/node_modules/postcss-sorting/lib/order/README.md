# order

Specify the order of content within declaration blocks.

`array`: `["array", "of", "keywords", "or", "expanded", "at-rule", "or", "rule" "objects"]`

Within an order array, you can include:

- keywords:
	- `custom-properties` — Custom properties (e. g., `--property: 10px;`)
	- `dollar-variables` — Dollar variables (e. g., `$variable`)
	- `at-variables` — At-variables (e. g., `@variable`)
	- `declarations` — CSS declarations (e. g., `display: block`)
	- `rules` — Nested rules (e. g., `span {}` in `a { span {} }`)
	- `at-rules` — Nested at-rules (e. g., `@media () {}` in `div { @media () {} }`)
- extended at-rule objects:

	```js
	{
		type: 'at-rule',
		name: 'include',
		parameter: 'hello',
		hasBlock: true
	}
	```

- extended rule objects:

	```js
	{
		type: 'rule',
		selector: 'div'
	}
	```

**Unlisted elements will be placed after all listed elements.** So if you specify an array and do not include `declarations`, that means that all declarations will be placed after any other element.

## Extended at-rule objects

Extended at-rule objects have different parameters and variations.

Object parameters:

* `type`: always `"at-rule"`
* `name`: `string`. E. g., `name: "include"` for `@include`
* `parameter`: `string`|`regex`. A string will be translated into a RegExp — `new RegExp(yourString)` — so _be sure to escape properly_. E. g., `parameter: "icon"` for `@include icon(20px);`
* `hasBlock`: `boolean`. E. g., `hasBlock: true` for `@include icon { color: red; }` and not for `@include icon;`

Always specify `name` if `parameter` is specified.

Matches all at-rules:

```js
{
	type: 'at-rule'
}
```

Or keyword `at-rules`.

Matches all at-rules, which have nested elements:

```js
{
	type: 'at-rule',
	hasBlock: true
}
```

Matches all at-rules with specific name:

```js
{
	type: 'at-rule',
	name: 'media'
}
```

Matches all at-rules with specific name, which have nested elements:

```js
{
	type: 'at-rule',
	name: 'media',
	hasBlock: true
}
```

Matches all at-rules with specific name and parameter:

```js
{
	type: 'at-rule',
	name: 'include',
	parameter: 'icon'
}
```

Matches all at-rules with specific name and parameter, which have nested elements:

```js
{
	type: 'at-rule',
	name: 'include',
	parameter: 'icon',
	hasBlock: true
}
```

Each described above variant has more priority than its previous variant. For example, `{ type: 'at-rule', name: 'media' }` will be applied to an element if both `{ type: 'at-rule', name: 'media' }` and `{ type: 'at-rule', hasBlock: true }` can be applied to an element.

## Extended rule objects

Object parameters:

* `type`: always `"rule"`
* `selector`: `string`|`regex`. Selector pattern. A string will be translated into a RegExp — `new RegExp(yourString)` — so _be sure to escape properly_. Examples:
	* `selector: /^&:[\w-]+$/` matches simple pseudo-classes. E. g., `&:hover`, `&:first-child`. Doesn't match complex pseudo-classes, e. g. `&:not(.is-visible)`.
	* `selector: /^&::[\w-]+$/` matches pseudo-elements. E. g. `&::before`, `&::placeholder`.

Matches all rules:

```js
{
	type: 'rule'
}
```

Or keyword `rules`.

Matches all rules with selector matching pattern:

```js
{
	type: 'rule',
	selector: 'div'
}
```

```js
{
	type: 'rule',
	selector: /^&:\w+$/
}
```

If more than one pattern can be applied to a node, and these patterns have equal “power” than the first matched pattern will be applied.

## Examples

Given:

```js
["custom-properties", "dollar-variables", "declarations", "rules", "at-rules"]
```

Before:

```css
a {
	top: 0;
	--height: 10px;
	color: pink;
}
a {
	@media (min-width: 100px) {}
	display: none;
}
a {
	--width: 10px;
	@media (min-width: 100px) {}
	display: none;
	$height: 20px;
	span {}
}
```

After:

```css
a {
	--height: 10px;
	top: 0;
	color: pink;
}
a {
	display: none;
	@media (min-width: 100px) {}
}
a {
	--width: 10px;
	$height: 20px;
	display: none;
	span {}
	@media (min-width: 100px) {}
}
```

---

Given:

```js
[
	{
		type: 'at-rule',
		name: 'include',
	},
	{
		type: 'at-rule',
		name: 'include',
		hasBlock: true
	},
	{
		type: 'at-rule',
		hasBlock: true
	},
	{
		type: 'at-rule',
	}
]
```

Before:

```scss
a {
	@include hello {
		display: block;
	}
	@include hello;
}

a {
	@extend .something;
	@media (min-width: 10px) {
		display: none;
	}
}

a {
	@include hello {
		display: block;
	}
	@include hello;
	@media (min-width: 10px) {
		display: none;
	}
	@extend .something;
}
```

After:

```scss
a {
	@include hello;
	@include hello {
		display: block;
	}
}

a {
	@media (min-width: 10px) {
		display: none;
	}
	@extend .something;
}

a {
	@include hello;
	@include hello {
		display: block;
	}
	@media (min-width: 10px) {
		display: none;
	}
	@extend .something;
}
```

---

Given:

```js
[
	{
		type: 'at-rule',
		name: 'include',
		hasBlock: true
	},
	{
		type: 'at-rule',
		name: 'include',
		parameter: 'icon',
		hasBlock: true
	},
	{
		type: 'at-rule',
		name: 'include',
		parameter: 'icon'
	}
]
```

Before:

```scss
a {
	@include icon {
		display: block;
	}
	@include hello {
		display: none;
	}
	@include icon;
}

a {
	@include icon;
	@include icon {
		display: block;
	}
}
```

After:

```scss
a {
	@include icon {
		display: block;
	}
	@include hello {
		display: none;
	}
	@include icon;
}

a {
	@include icon;
	@include icon {
		display: block;
	}
}
```

---

Given:

```js
[
	{
		type: 'rule',
		selector: '^a'
	},
	{
		type: 'rule',
		selector: /^&/
	},
	'rules'
]
```

Before:

```scss
a {
	a {}
	&:hover {}
	abbr {}
	span {}
}

a {
	span {}
	&:hover {}
}

a {
	span {}
	abbr {}
}
```

After:

```scss
a {
	a {}
	abbr {}
	&:hover {}
	span {}
}

a {
	&:hover {}
	span {}
}

a {
	abbr {}
	span {}
}
```

---

Given:

```js
[
	{
		type: 'rule',
		selector: /^&/
	},
	{
		type: 'rule',
		selector: /^&:\w/
	}
]
```

This code won't change, because selector patterns have equal “power” for `&:hover` selector, so the first matching is used (`/^&/`):

```scss
a {
	&:hover {}
	& b {}
}

a {
	& b {}
	&:hover {}
}
```

---

Given:

```js
[
	{
		type: 'rule',
		selector: /^&:\w/
	},
	{
		type: 'rule',
		selector: /^&/
	}
]
```

Before:

```scss
a {
	& b {}
	&:hover {}
}
```

After:

```scss
a {
	&:hover {}
	& b {}
}
```

---

Given:

```js
[
	'custom-properties',
	{
		type: 'at-rule',
		hasBlock: true,
	},
	'declarations'
]
```

Before:

```css
a {
	@media (min-width: 10px) {
		display: none;
	}
	--height: 10px;
	width: 20px;
}

a {
	width: 20px;
	@media (min-width: 10px) {
		display: none;
	}
	--height: 10px;
}

a {
	width: 20px;
	@media (min-width: 10px) {
		display: none;
	}
}
```

After:

```css
a {
	--height: 10px;
	@media (min-width: 10px) {
		display: none;
	}
	width: 20px;
}

a {
	--height: 10px;
	@media (min-width: 10px) {
		display: none;
	}
	width: 20px;
}

a {
	@media (min-width: 10px) {
		display: none;
	}
	width: 20px;
}
```
