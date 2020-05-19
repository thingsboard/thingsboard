const stylelint = require('stylelint');
const _ = require('lodash');
const { namespace, getContainingNode, isRuleWithNodes } = require('../../utils');
const checkNode = require('./checkNode');
const createOrderInfo = require('./createOrderInfo');
const validatePrimaryOption = require('./validatePrimaryOption');

const ruleName = namespace('properties-order');

const messages = stylelint.utils.ruleMessages(ruleName, {
	expected: (first, second, groupName) =>
		`Expected "${first}" to come before "${second}"${groupName ? ` in group "${groupName}"` : ''}`,
	expectedEmptyLineBefore: property => `Expected an empty line before property "${property}"`,
	rejectedEmptyLineBefore: property => `Unexpected empty line before property "${property}"`,
});

function rule(primaryOption, options = {}, context = {}) {
	return function(root, result) {
		let validOptions = stylelint.utils.validateOptions(
			result,
			ruleName,
			{
				actual: primaryOption,
				possible: validatePrimaryOption,
			},
			{
				actual: options,
				possible: {
					unspecified: ['top', 'bottom', 'ignore', 'bottomAlphabetical'],
					emptyLineBeforeUnspecified: ['always', 'never', 'threshold'],
					disableFix: _.isBoolean,
					emptyLineMinimumPropertyThreshold: _.isNumber,
				},
				optional: true,
			}
		);

		if (!validOptions) {
			return;
		}

		let disableFix = options.disableFix || false;

		let sharedInfo = {
			context,
			emptyLineBeforeUnspecified: options.emptyLineBeforeUnspecified,
			emptyLineMinimumPropertyThreshold: options.emptyLineMinimumPropertyThreshold || 0,
			expectedOrder: createOrderInfo(primaryOption),
			isFixEnabled: context.fix && !disableFix,
			messages,
			primaryOption,
			result,
			ruleName,
			unspecified: options.unspecified || 'ignore',
		};

		let processedParents = [];

		// Check all rules and at-rules recursively
		root.walk(function processRulesAndAtrules(input) {
			let node = getContainingNode(input);

			// Avoid warnings duplication, caused by interfering in `root.walk()` algorigthm with `getContainingNode()`
			if (processedParents.includes(node)) {
				return;
			}

			processedParents.push(node);

			if (isRuleWithNodes(node)) {
				checkNode(node, sharedInfo, input);
			}
		});
	};
}

rule.primaryOptionArray = true;
rule.ruleName = ruleName;
rule.messages = messages;

module.exports = rule;
