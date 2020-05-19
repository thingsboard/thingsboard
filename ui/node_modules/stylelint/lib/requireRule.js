'use strict';

const importLazy = require('import-lazy')(require);
const rules = require('./rules');

/**
 * @param {string} ruleName
 * @returns {false|any}
 */
module.exports = function(ruleName) {
	if (rules.includes(ruleName)) {
		return importLazy(`./rules/${ruleName}`);
	}

	return false;
};
