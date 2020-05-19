const _ = require('lodash');

module.exports = function validatePrimaryOption(actualOptions) {
	// Begin checking array options
	if (!Array.isArray(actualOptions)) {
		return false;
	}

	// Every item in the array must be a string or an object
	// with a "properties" property
	if (
		!actualOptions.every(item => {
			if (_.isString(item)) {
				return true;
			}

			return _.isPlainObject(item) && !_.isUndefined(item.properties);
		})
	) {
		return false;
	}

	const objectItems = actualOptions.filter(_.isPlainObject);

	// Every object-item's "properties" should be an array with no items, or with strings
	if (
		!objectItems.every(item => {
			if (!Array.isArray(item.properties)) {
				return false;
			}

			return item.properties.every(property => _.isString(property));
		})
	) {
		return false;
	}

	// Every object-item's "emptyLineBefore" must be "always" or "never"
	if (
		!objectItems.every(item => {
			if (_.isUndefined(item.emptyLineBefore)) {
				return true;
			}

			return ['always', 'never', 'threshold'].includes(item.emptyLineBefore);
		})
	) {
		return false;
	}

	// Every object-item's "noEmptyLineBetween" must be a boolean
	if (
		!objectItems.every(item => {
			if (_.isUndefined(item.noEmptyLineBetween)) {
				return true;
			}

			return _.isBoolean(item.noEmptyLineBetween);
		})
	) {
		return false;
	}

	return true;
};
