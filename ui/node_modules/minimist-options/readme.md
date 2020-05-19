# minimist-options [![Build Status](https://travis-ci.org/vadimdemedes/minimist-options.svg?branch=master)](https://travis-ci.org/vadimdemedes/minimist-options)

> Write options for [minimist](https://npmjs.org/package/minimist) in a comfortable way.
> Support string, boolean, number and array options.

## Installation

```
$ npm install --save minimist-options
```

## Usage

```js
const buildOptions = require('minimist-options');
const minimist = require('minimist');

const options = buildOptions({
	name: {
		type: 'string',
		alias: 'n',
		default: 'john'
	},

	force: {
		type: 'boolean',
		alias: ['f', 'o'],
		default: false
	},

	score: {
		type: 'number',
		alias: 's',
		default: 0
	},

	arr: {
		type: 'array',
		alias: 'a',
		default: []
	},

	published: 'boolean',

	// Special option for positional arguments (`_` in minimist)
	arguments: 'string'
});

const args = minimist(process.argv.slice(2), options);
```

instead of:

```js
const minimist = require('minimist');

const options = {
	string: ['name', '_'],
	number: ['score'],
	array: ['arr'],
	boolean: ['force', 'published'],
	alias: {
		n: 'name',
		f: 'force',
		s: 'score',
		a: 'arr'
	},
	default: {
		name: 'john',
		f: false,
		score: 0,
		arr: []
	}
};

const args = minimist(process.argv.slice(2), options);
```

## License

MIT © [Vadim Demedes](https://vadimdemedes.com)
