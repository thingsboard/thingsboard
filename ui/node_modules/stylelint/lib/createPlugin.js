'use strict';

/**
 * @param {string} ruleName
 * @param {Function} rule
 * @returns {{ruleName: string, rule: Function}}
 */
module.exports = function(ruleName, rule) {
	return {
		ruleName,
		rule,
	};
};
