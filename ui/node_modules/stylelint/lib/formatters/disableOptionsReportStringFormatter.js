'use strict';

const chalk = require('chalk');
const path = require('path');

/**
 * @param {string} fromValue
 * @return {string}
 */
function logFrom(fromValue) {
	if (fromValue.startsWith('<')) return fromValue;

	return path
		.relative(process.cwd(), fromValue)
		.split(path.sep)
		.join('/');
}

/**
 * @param {import('stylelint').StylelintDisableOptionsReport} report
 * @returns {string}
 */
module.exports = function(report) {
	if (!report) return '';

	let output = '';

	report.forEach((sourceReport) => {
		if (!sourceReport.ranges || sourceReport.ranges.length === 0) {
			return;
		}

		output += '\n';
		// eslint-disable-next-line prefer-template
		output += chalk.underline(logFrom(sourceReport.source || '')) + '\n';

		sourceReport.ranges.forEach((range) => {
			output += `unused rule: ${range.unusedRule}, start line: ${range.start}`;

			if (range.end !== undefined) {
				output += `, end line: ${range.end}`;
			}

			output += '\n';
		});
	});

	return output;
};
