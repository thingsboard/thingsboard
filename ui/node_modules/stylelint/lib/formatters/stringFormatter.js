'use strict';

const _ = require('lodash');
const chalk = require('chalk');
const path = require('path');
const stringWidth = require('string-width');
const symbols = require('log-symbols');
const utils = require('postcss-reporter/lib/util');
/** @type {import('table')} */
let table;

const MARGIN_WIDTHS = 9;

const levelColors = {
	info: 'blue',
	warning: 'yellow',
	error: 'red',
	success: undefined,
};

/**
 * @param {import('stylelint').StylelintResult[]} results
 * @returns {string}
 */
function deprecationsFormatter(results) {
	const allDeprecationWarnings = _.flatMap(results, 'deprecations');
	const uniqueDeprecationWarnings = _.uniqBy(allDeprecationWarnings, 'text');

	if (!uniqueDeprecationWarnings || !uniqueDeprecationWarnings.length) {
		return '';
	}

	return uniqueDeprecationWarnings.reduce((output, warning) => {
		output += chalk.yellow('Deprecation Warning: ');
		output += warning.text;

		if (warning.reference) {
			output += chalk.dim(' See: ');
			output += chalk.dim.underline(warning.reference);
		}

		return `${output}\n`;
	}, '\n');
}

/**
 * @param {import('stylelint').StylelintResult[]} results
 * @return {string}
 */
function invalidOptionsFormatter(results) {
	const allInvalidOptionWarnings = _.flatMap(results, (r) =>
		r.invalidOptionWarnings.map((w) => w.text),
	);
	const uniqueInvalidOptionWarnings = [...new Set(allInvalidOptionWarnings)];

	return uniqueInvalidOptionWarnings.reduce((output, warning) => {
		output += chalk.red('Invalid Option: ');
		output += warning;

		return `${output}\n`;
	}, '\n');
}

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
 * @param {{[k: number]: number}} columnWidths
 * @return {number}
 */
function getMessageWidth(columnWidths) {
	if (!process.stdout.isTTY) {
		return columnWidths[3];
	}

	const availableWidth = process.stdout.columns < 80 ? 80 : process.stdout.columns;
	const fullWidth = Object.values(columnWidths).reduce((a, b) => a + b);

	// If there is no reason to wrap the text, we won't align the last column to the right
	if (availableWidth > fullWidth + MARGIN_WIDTHS) {
		return columnWidths[3];
	}

	return availableWidth - (fullWidth - columnWidths[3] + MARGIN_WIDTHS);
}

/**
 * @param {import('stylelint').StylelintWarning[]} messages
 * @param {string} source
 * @return {string}
 */
function formatter(messages, source) {
	if (!messages.length) return '';

	const orderedMessages = _.sortBy(
		messages,
		(m) => (m.line ? 2 : 1), // positionless first
		(m) => m.line,
		(m) => m.column,
	);

	/**
	 * Create a list of column widths, needed to calculate
	 * the size of the message column and if needed wrap it.
	 * @type {{[k: string]: number}}
	 */
	const columnWidths = { 0: 1, 1: 1, 2: 1, 3: 1, 4: 1 };

	/**
	 * @param {[string, string, string, string, string]} columns
	 * @return {[string, string, string, string, string]}
	 */
	function calculateWidths(columns) {
		for (const [key, value] of Object.entries(columns)) {
			const normalisedValue = value ? value.toString() : value;

			columnWidths[key] = Math.max(columnWidths[key], stringWidth(normalisedValue));
		}

		return columns;
	}

	let output = '\n';

	if (source) {
		output += `${chalk.underline(logFrom(source))}\n`;
	}

	const cleanedMessages = orderedMessages.map((message) => {
		const location = utils.getLocation(message);
		const severity = /** @type {keyof import('log-symbols')} */ (message.severity);
		/**
		 * @type {[string, string, string, string, string]}
		 */
		const row = [
			location.line ? location.line.toString() : '',
			location.column ? location.column.toString() : '',
			symbols[severity]
				? chalk[/** @type {'blue' | 'red' | 'yellow'} */ (levelColors[severity])](symbols[severity])
				: severity,
			message.text
				// Remove all control characters (newline, tab and etc)
				.replace(/[\x01-\x1A]+/g, ' ') // eslint-disable-line no-control-regex
				.replace(/\.$/, '')
				// eslint-disable-next-line prefer-template
				.replace(new RegExp(_.escapeRegExp('(' + message.rule + ')') + '$'), ''),
			chalk.dim(message.rule || ''),
		];

		calculateWidths(row);

		return row;
	});

	if (!table) {
		table = require('table');
	}

	output += table
		.table(cleanedMessages, {
			border: table.getBorderCharacters('void'),
			columns: {
				0: { alignment: 'right', width: columnWidths[0], paddingRight: 0 },
				1: { alignment: 'left', width: columnWidths[1] },
				2: { alignment: 'center', width: columnWidths[2] },
				3: {
					alignment: 'left',
					width: getMessageWidth(columnWidths),
					wrapWord: getMessageWidth(columnWidths) > 1,
				},
				4: { alignment: 'left', width: columnWidths[4], paddingRight: 0 },
			},
			drawHorizontalLine: () => false,
		})
		.split('\n')
		.map(
			/**
			 * @param {string} el
			 * @returns {string}
			 */
			(el) => el.replace(/(\d+)\s+(\d+)/, (m, p1, p2) => chalk.dim(`${p1}:${p2}`)),
		)
		.join('\n');

	return output;
}

/**
 * @param {import('stylelint').StylelintResult[]} results
 * @returns {string}
 */
module.exports = function(results) {
	let output = invalidOptionsFormatter(results);

	output += deprecationsFormatter(results);

	output = results.reduce((output, result) => {
		// Treat parseErrors as warnings
		if (result.parseErrors) {
			result.parseErrors.forEach((error) =>
				result.warnings.push({
					line: error.line,
					column: error.column,
					rule: error.stylelintType,
					severity: 'error',
					text: `${error.text} (${error.stylelintType})`,
				}),
			);
		}

		output += formatter(result.warnings, result.source || '');

		return output;
	}, output);

	// Ensure consistent padding
	output = output.trim();

	if (output !== '') {
		output = `\n${output}\n\n`;
	}

	return output;
};
