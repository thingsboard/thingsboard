'use strict';

/** @typedef {import('stylelint').RangeType} RangeType */
/** @typedef {import('stylelint').UnusedRange} UnusedRange */
/** @typedef {import('stylelint').StylelintDisableOptionsReport} StylelintDisableOptionsReport */

/**
 * @param {import('stylelint').StylelintResult[]} results
 * @param {import('stylelint').StylelintConfig|undefined} config
 * @returns {StylelintDisableOptionsReport}
 */
module.exports = function(results, config = {}) {
	/** @type {StylelintDisableOptionsReport} */
	const report = [];
	const usedRules = new Set(Object.keys(config.rules || {}));

	usedRules.add('all');

	results.forEach((result) => {
		// File with `CssSyntaxError` have not `_postcssResult`
		if (!result._postcssResult) {
			return;
		}

		/** @type {import('stylelint').StylelintDisableReportEntry} */
		const sourceReport = { source: result.source, ranges: [] };
		const rangeData = result._postcssResult.stylelint.disabledRanges;
		const disabledRules = Object.keys(rangeData);

		disabledRules.forEach((rule) => {
			if (usedRules.has(rule)) {
				return;
			}

			rangeData[rule].forEach((range) => {
				if (!range.strictStart && !range.strictEnd) {
					return;
				}

				sourceReport.ranges.push({
					unusedRule: rule,
					start: range.start,
					end: range.end,
				});
			});
		});

		if (sourceReport.ranges.length > 0) {
			report.push(sourceReport);
		}
	});

	return report;
};
