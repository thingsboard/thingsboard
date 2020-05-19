'use strict';

const _ = require('lodash');
const findAtRuleContext = require('../../utils/findAtRuleContext');
const isKeyframeRule = require('../../utils/isKeyframeRule');
const nodeContextLookup = require('../../utils/nodeContextLookup');
const normalizeSelector = require('normalize-selector');
const report = require('../../utils/report');
const resolvedNestedSelector = require('postcss-resolve-nested-selector');
const ruleMessages = require('../../utils/ruleMessages');
const validateOptions = require('../../utils/validateOptions');

const ruleName = 'no-duplicate-selectors';

const messages = ruleMessages(ruleName, {
	rejected: (selector, firstDuplicateLine) =>
		`Unexpected duplicate selector "${selector}", first used at line ${firstDuplicateLine}`,
});

function rule(actual, options) {
	return (root, result) => {
		const validOptions = validateOptions(
			result,
			ruleName,
			{ actual },
			{
				actual: options,
				possible: {
					disallowInList: _.isBoolean,
				},
				optional: true,
			},
		);

		if (!validOptions) {
			return;
		}

		// The top level of this map will be rule sources.
		// Each source maps to another map, which maps rule parents to a set of selectors.
		// This ensures that selectors are only checked against selectors
		// from other rules that share the same parent and the same source.
		const selectorContextLookup = nodeContextLookup();

		root.walkRules((rule) => {
			if (isKeyframeRule(rule)) {
				return;
			}

			const contextSelectorSet = selectorContextLookup.getContext(rule, findAtRuleContext(rule));
			const resolvedSelectors = rule.selectors.reduce((result, selector) => {
				return _.union(result, resolvedNestedSelector(selector, rule));
			}, []);

			const normalizedSelectorList = resolvedSelectors.map(normalizeSelector);
			const selectorLine = rule.source.start.line;

			// Complain if the same selector list occurs twice

			// Sort the selectors list so that the order of the constituents
			// doesn't matter
			const sortedSelectorList = normalizedSelectorList
				.slice()
				.sort()
				.join(',');

			const checkPreviousDuplicationPosition = (
				selectorList,
				{ shouldDisallowDuplicateInList },
			) => {
				let duplicationPosition = null;

				if (shouldDisallowDuplicateInList) {
					// iterate throw Map for checking, was used this selector in a group selector
					contextSelectorSet.forEach((selectorLine, selector) => {
						if (selector.includes(selectorList)) {
							duplicationPosition = selectorLine;
						}
					});
				} else {
					duplicationPosition = contextSelectorSet.get(selectorList);
				}

				return duplicationPosition;
			};

			const previousDuplicatePosition = checkPreviousDuplicationPosition(sortedSelectorList, {
				shouldDisallowDuplicateInList: _.get(options, 'disallowInList'),
			});

			if (previousDuplicatePosition) {
				// If the selector isn't nested we can use its raw value; otherwise,
				// we have to approximate something for the message -- which is close enough
				const isNestedSelector = resolvedSelectors.join(',') !== rule.selectors.join(',');
				const selectorForMessage = isNestedSelector ? resolvedSelectors.join(', ') : rule.selector;

				return report({
					result,
					ruleName,
					node: rule,
					message: messages.rejected(selectorForMessage, previousDuplicatePosition),
				});
			}

			const presentedSelectors = new Set();
			const reportedSelectors = new Set();

			// Or complain if one selector list contains the same selector more than one
			rule.selectors.forEach((selector) => {
				const normalized = normalizeSelector(selector);

				if (presentedSelectors.has(normalized)) {
					if (reportedSelectors.has(normalized)) {
						return;
					}

					report({
						result,
						ruleName,
						node: rule,
						message: messages.rejected(selector, selectorLine),
					});
					reportedSelectors.add(normalized);
				} else {
					presentedSelectors.add(normalized);
				}
			});

			contextSelectorSet.set(sortedSelectorList, selectorLine);
		});
	};
}

rule.ruleName = ruleName;
rule.messages = messages;
module.exports = rule;
