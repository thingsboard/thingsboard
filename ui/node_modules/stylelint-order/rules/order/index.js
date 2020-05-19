const stylelint = require('stylelint');
const _ = require('lodash');
const postcssSorting = require('postcss-sorting');
const { namespace, getContainingNode, isRuleWithNodes } = require('../../utils');
const checkNode = require('./checkNode');
const createOrderInfo = require('./createOrderInfo');
const validatePrimaryOption = require('./validatePrimaryOption');

const ruleName = namespace('order');

const messages = stylelint.utils.ruleMessages(ruleName, {
	expected: (first, second) => `Expected ${first} to come before ${second}`,
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
					unspecified: ['top', 'bottom', 'ignore'],
					disableFix: _.isBoolean,
				},
				optional: true,
			}
		);

		if (!validOptions) {
			return;
		}

		let disableFix = options.disableFix || false;
		let isFixEnabled = context.fix && !disableFix;

		// Contains information which will be shared in many files and this info would be mutated to share state between them
		let sharedInfo = {
			orderInfo: createOrderInfo(primaryOption),
			unspecified: options.unspecified || 'ignore',
			messages,
			ruleName,
			result,
			isFixEnabled,
			shouldFix: false,
		};

		let processedParents = [];

		// Check all rules and at-rules recursively
		root.walk(function processRulesAndAtrules(input) {
			// return early if we know there is a violation and auto fix should be applied
			if (isFixEnabled && sharedInfo.shouldFix) {
				return;
			}

			let node = getContainingNode(input);

			// Avoid warnings duplication, caused by interfering in `root.walk()` algorigthm with `getContainingNode()`
			if (processedParents.includes(node)) {
				return;
			}

			processedParents.push(node);

			if (isRuleWithNodes(node)) {
				checkNode(node, sharedInfo);
			}
		});

		if (sharedInfo.shouldFix) {
			postcssSorting({ order: primaryOption })(root);
		}
	};
}

rule.ruleName = ruleName;
rule.messages = messages;
rule.primaryOptionArray = true;

module.exports = rule;
