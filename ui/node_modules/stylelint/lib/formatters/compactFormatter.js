'use strict';

const _ = require('lodash');

/**
 * @param {import('stylelint').StylelintResult[]} results
 * @returns {string}
 */
const formatter = (results) =>
	_.flatMap(results, (result) =>
		result.warnings.map(
			(warning) =>
				`${result.source}: ` +
				`line ${warning.line}, ` +
				`col ${warning.column}, ` +
				`${warning.severity} - ` +
				`${warning.text}`,
		),
	).join('\n');

module.exports = formatter;
