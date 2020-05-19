const _ = require('lodash');

module.exports = function validateOptions(options) {
	if (_.isUndefined(options) || _.isNull(options)) {
		return false;
	}

	if (!_.isPlainObject(options)) {
		return reportError('Options should be an object.');
	}

	if (!_.isUndefined(options.order) && !_.isNull(options.order)) {
		const validatedOrder = validateOrder(options.order);
		const { isValid, message } = validatedOrder;

		if (!isValid) {
			return reportInvalidOption('order', message);
		}
	}

	if (!_.isUndefined(options['properties-order']) && !_.isNull(options['properties-order'])) {
		const validatedPropertiesOrder = validatePropertiesOrder(options['properties-order']);
		const { isValid, message } = validatedPropertiesOrder;

		if (!isValid) {
			return reportInvalidOption('properties-order', message);
		}
	}

	if (
		!_.isUndefined(options['unspecified-properties-position']) &&
		!_.isNull(options['unspecified-properties-position'])
	) {
		const validatedUnspecifiedPropertiesPosition = validateUnspecifiedPropertiesPosition(
			options['unspecified-properties-position']
		);
		const { isValid, message } = validatedUnspecifiedPropertiesPosition;

		if (!isValid) {
			return reportInvalidOption('unspecified-properties-position', message);
		}
	}

	return true;
};

function reportError(errorMessage) {
	return `postcss-sorting: ${errorMessage}`;
}

function reportInvalidOption(optionName, optionError = 'Invalid value') {
	return reportError(`${optionName}: ${optionError}`);
}

function keywordsList(keywords) {
	return keywords.reduce(function(accumulator, value, index) {
		const comma = index === 0 ? '' : ', ';

		return accumulator + comma + value;
	}, '');
}

function validateOrder(options) {
	// Otherwise, begin checking array options
	if (!Array.isArray(options)) {
		return {
			isValid: false,
			message: 'Should be an array',
		};
	}

	const keywords = [
		'custom-properties',
		'dollar-variables',
		'at-variables',
		'declarations',
		'rules',
		'at-rules',
	];

	// Every item in the array must be a certain string or an object
	// with a "type" property
	if (
		!options.every(item => {
			if (_.isString(item)) {
				return _.includes(keywords, item);
			}

			return _.isPlainObject(item) && !_.isUndefined(item.type);
		})
	) {
		return {
			isValid: false,
			message: `Every item in the array must be an object with a "type" property, or one of keywords: ${keywordsList(
				keywords
			)}.`,
		};
	}

	const objectItems = options.filter(_.isPlainObject);
	let wrongObjectItem;

	if (
		!objectItems.every(item => {
			let result = true;

			if (item.type !== 'at-rule' && item.type !== 'rule') {
				wrongObjectItem = `"type" could be 'at-rule' or 'rule' only`;

				return false;
			}

			if (item.type === 'at-rule') {
				// if parameter is specified, name should be specified also
				if (!_.isUndefined(item.parameter) && _.isUndefined(item.name)) {
					wrongObjectItem = `"at-rule" with "parameter" should also has a "name"`;

					return false;
				}

				if (!_.isUndefined(item.hasBlock)) {
					result = item.hasBlock === true || item.hasBlock === false;
				}

				if (!_.isUndefined(item.name)) {
					result = _.isString(item.name) && item.name.length;
				}

				if (!_.isUndefined(item.parameter)) {
					result =
						(_.isString(item.parameter) && item.parameter.length) || _.isRegExp(item.parameter);
				}
			}

			if (item.type === 'rule') {
				if (!_.isUndefined(item.selector)) {
					result = (_.isString(item.selector) && item.selector.length) || _.isRegExp(item.selector);
				}
			}

			if (!result) {
				wrongObjectItem = `Following option is incorrect: ${JSON.stringify(item)}`;
			}

			return result;
		})
	) {
		return {
			isValid: false,
			message: wrongObjectItem,
		};
	}

	return {
		isValid: true,
	};
}

function validatePropertiesOrder(options) {
	// Return true early if alphabetical
	if (options === 'alphabetical') {
		return {
			isValid: true,
		};
	}

	// Otherwise, begin checking array options
	if (!Array.isArray(options)) {
		return {
			isValid: false,
			message: 'Should be an array',
		};
	}

	// Every item in the array must be a string
	if (!options.every(item => _.isString(item))) {
		return {
			isValid: false,
			message: 'Array should contain strings only',
		};
	}

	return {
		isValid: true,
	};
}

function validateUnspecifiedPropertiesPosition(options) {
	const keywords = ['top', 'bottom', 'bottomAlphabetical'];

	if (_.isString(options) && _.includes(keywords, options)) {
		return {
			isValid: true,
		};
	}

	return {
		isValid: false,
		message: `Option should be one of the following values: ${keywordsList(keywords)}.`,
	};
}
