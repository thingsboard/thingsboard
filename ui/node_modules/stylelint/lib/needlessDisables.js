'use strict';

const _ = require('lodash');

/** @typedef {import('stylelint').RangeType} RangeType */
/** @typedef {import('stylelint').UnusedRange} UnusedRange */
/** @typedef {import('stylelint').StylelintDisableOptionsReport} StylelintDisableOptionsReport */

/**
 * @param {import('stylelint').StylelintResult[]} results
 * @returns {StylelintDisableOptionsReport}
 */
module.exports = function(results) {
	/** @type {StylelintDisableOptionsReport} */
	const report = [];

	results.forEach((result) => {
		// File with `CssSyntaxError` have not `_postcssResult`
		if (!result._postcssResult) {
			return;
		}

		/** @type {{ranges: UnusedRange[], source: string}} */
		const unused = { source: result.source || '', ranges: [] };

		/** @type {{[ruleName: string]: Array<RangeType>}} */
		const rangeData = _.cloneDeep(result._postcssResult.stylelint.disabledRanges);

		if (!rangeData) {
			return;
		}

		result.warnings.forEach((warning) => {
			const rule = warning.rule;

			const ruleRanges = rangeData[rule];

			if (ruleRanges) {
				// Back to front so we get the *last* range that applies to the warning
				for (const range of ruleRanges.reverse()) {
					if (isWarningInRange(warning, range)) {
						range.used = true;

						return;
					}
				}
			}

			for (const range of rangeData.all.reverse()) {
				if (isWarningInRange(warning, range)) {
					range.used = true;

					return;
				}
			}
		});

		Object.keys(rangeData).forEach((rule) => {
			rangeData[rule].forEach((range) => {
				// Is an equivalent range already marked as unused?
				const alreadyMarkedUnused = unused.ranges.find((unusedRange) => {
					return unusedRange.start === range.start && unusedRange.end === range.end;
				});

				// If this range is unused and no equivalent is marked,
				// mark this range as unused
				if (!range.used && !alreadyMarkedUnused) {
					unused.ranges.push({
						start: range.start,
						end: range.end,
						unusedRule: rule,
					});
				}

				// If this range is used but an equivalent has been marked as unused,
				// remove that equivalent. This can happen because of the duplication
				// of ranges in rule-specific range sets and the "all" range set
				if (range.used && alreadyMarkedUnused) {
					_.remove(unused.ranges, alreadyMarkedUnused);
				}
			});
		});

		unused.ranges = _.sortBy(unused.ranges, ['start', 'end']);

		report.push(unused);
	});

	return report;
};

/**
 * @param {import('stylelint').StylelintWarning} warning
 * @param {RangeType} range
 * @return {boolean}
 */
function isWarningInRange(warning, range) {
	const line = warning.line;

	// Need to check if range.end exist, because line number type cannot be compared to undefined
	return (
		range.start <= line &&
		((range.end !== undefined && range.end >= line) || range.end === undefined)
	);
}
