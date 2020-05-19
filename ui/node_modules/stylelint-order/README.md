# stylelint-order [![Build Status][ci-img]][ci] [![npm version][npm-version-img]][npm] [![npm downloads last month][npm-downloads-img]][npm]

A plugin pack of order related linting rules for [stylelint]. Every rule support autofixing (`stylelint --fix`).

## Installation

First, install [stylelint]:

```
npm install stylelint --save-dev
```

Then install plugin:

```
npm install stylelint-order --save-dev
```

## Usage

Add `stylelint-order` to your stylelint config plugins array, then add rules you need to the rules list. All rules from stylelint-order need to be namespaced with `order`.

Like so:

```js
// .stylelintrc
{
	"plugins": [
		"stylelint-order"
	],
	"rules": {
		// ...
		"order/order": [
			"custom-properties",
			"declarations"
		],
		"order/properties-alphabetical-order": true
		// ...
	}
}
```

## List of rules

* [`order`](./rules/order/README.md): Specify the order of content within declaration blocks.
* [`properties-order`](./rules/properties-order/README.md): Specify the order of properties within declaration blocks.
* [`properties-alphabetical-order`](./rules/properties-alphabetical-order/README.md): Specify the alphabetical order of properties within declaration blocks.

## Autofixing

Every rule support autofixing (`stylelint --fix`). [postcss-sorting] is using internally for order autofixing.

Automatic sortings has some limitation, which are described for every rule if any. Please, take a look at [how comments are handled](https://github.com/hudochenkov/postcss-sorting#handling-comments) by postcss-sorting.

CSS-in-JS styles with template interpolation [could be ignored by autofixing](https://github.com/hudochenkov/postcss-sorting#css-in-js) to avoid styles corruption.

Autofixing is enabled by default if it's enabled in stylelint configuration. Autofixing can be disabled on per rule basis using `disableFix: true` secondary option. E. g.:

```json
{
	"rules": {
		"order/order": [
			[
				"custom-properties",
				"declarations"
			],
			{
				"disableFix": true
			}
		]
	}
}
```

Less isn't supported. It might work, but haven't tested.

## Thanks

`properties-order` and `properties-alphabetical-order` code and readme are based on `declaration-block-properties-order` rule which was a stylelint's core rule prior stylelint 8.0.0.

[ci-img]: https://travis-ci.org/hudochenkov/stylelint-order.svg
[ci]: https://travis-ci.org/hudochenkov/stylelint-order
[npm-version-img]: https://img.shields.io/npm/v/stylelint-order.svg
[npm-downloads-img]: https://img.shields.io/npm/dm/stylelint-order.svg
[npm]: https://www.npmjs.com/package/stylelint-order

[stylelint]: https://stylelint.io/
[postcss-sorting]: https://github.com/hudochenkov/postcss-sorting
