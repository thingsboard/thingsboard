'use strict';

const isPlainObject = require('is-plain-obj');
const arrify = require('arrify');

const push = (obj, prop, value) => {
	if (!obj[prop]) {
		obj[prop] = [];
	}

	obj[prop].push(value);
};

const insert = (obj, prop, key, value) => {
	if (!obj[prop]) {
		obj[prop] = {};
	}

	obj[prop][key] = value;
};

const passthroughOptions = ['stopEarly', 'unknown', '--'];
const availableTypes = ['string', 'boolean', 'number', 'array'];

const buildOptions = options => {
	options = options || {};

	const result = {};

	passthroughOptions.forEach(key => {
		if (options[key]) {
			result[key] = options[key];
		}
	});

	Object.keys(options).forEach(key => {
		let value = options[key];

		if (key === 'arguments') {
			key = '_';
		}

		// If short form is used
		// convert it to long form
		// e.g. { 'name': 'string' }
		if (typeof value === 'string') {
			value = {type: value};
		}

		if (isPlainObject(value)) {
			const props = value;
			const {type} = props;

			if (type) {
				if (!availableTypes.includes(type)) {
					throw new TypeError(`Expected "${key}" to be one of ["string", "boolean", "number", "array"], got ${type}`);
				}

				push(result, type, key);
			}

			const aliases = arrify(props.alias);

			aliases.forEach(alias => {
				insert(result, 'alias', alias, key);
			});

			if ({}.hasOwnProperty.call(props, 'default')) {
				if (type === 'array' && !Array.isArray(props.default)) {
					throw new TypeError(`Expected "${key}" default value to be array, got ${typeof props.default}`);
				}

				if (type && type !== 'array' && typeof props.default !== type) {
					throw new TypeError(`Expected "${key}" default value to be ${type}, got ${typeof props.default}`);
				}

				insert(result, 'default', key, props.default);
			}
		}
	});

	return result;
};

module.exports = buildOptions;
module.exports.default = buildOptions;
