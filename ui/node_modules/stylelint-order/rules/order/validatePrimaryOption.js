const _ = require('lodash');

module.exports = function validatePrimaryOption(actualOptions) {
	// Otherwise, begin checking array options
	if (!Array.isArray(actualOptions)) {
		return false;
	}

	// Every item in the array must be a certain string or an object
	// with a "type" property
	if (
		!actualOptions.every(item => {
			if (_.isString(item)) {
				return [
					'custom-properties',
					'dollar-variables',
					'at-variables',
					'declarations',
					'rules',
					'at-rules',
					'less-mixins',
				].includes(item);
			}

			return _.isPlainObject(item) && !_.isUndefined(item.type);
		})
	) {
		return false;
	}

	const objectItems = actualOptions.filter(_.isPlainObject);

	if (
		!objectItems.every(item => {
			let result = true;

			if (item.type !== 'at-rule' && item.type !== 'rule') {
				return false;
			}

			if (item.type === 'at-rule') {
				// if parameter is specified, name should be specified also
				if (!_.isUndefined(item.parameter) && _.isUndefined(item.name)) {
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

			return result;
		})
	) {
		return false;
	}

	return true;
};
