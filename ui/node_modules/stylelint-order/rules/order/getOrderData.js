const utils = require('../../utils');
const calcAtRulePatternPriority = require('./calcAtRulePatternPriority');
const calcRulePatternPriority = require('./calcRulePatternPriority');
const getDescription = require('./getDescription');

module.exports = function getOrderData(orderInfo, node) {
	let nodeType;

	if (utils.isAtVariable(node)) {
		nodeType = 'at-variables';
	} else if (utils.isLessMixin(node)) {
		nodeType = 'less-mixins';
	} else if (node.type === 'decl') {
		if (utils.isCustomProperty(node.prop)) {
			nodeType = 'custom-properties';
		} else if (utils.isDollarVariable(node.prop)) {
			nodeType = 'dollar-variables';
		} else if (utils.isStandardSyntaxProperty(node.prop)) {
			nodeType = 'declarations';
		}
	} else if (node.type === 'rule') {
		nodeType = {
			type: 'rule',
			selector: node.selector,
		};

		const rules = orderInfo.rule;

		// Looking for most specified pattern, because it can match many patterns
		if (rules && rules.length) {
			let prioritizedPattern;
			let max = 0;

			rules.forEach(function(pattern) {
				const priority = calcRulePatternPriority(pattern, nodeType);

				if (priority > max) {
					max = priority;
					prioritizedPattern = pattern;
				}
			});

			if (max) {
				return prioritizedPattern;
			}
		}
	} else if (node.type === 'atrule') {
		nodeType = {
			type: 'at-rule',
			name: node.name,
			hasBlock: false,
		};

		if (node.nodes && node.nodes.length) {
			nodeType.hasBlock = true;
		}

		if (node.params && node.params.length) {
			nodeType.parameter = node.params;
		}

		const atRules = orderInfo['at-rule'];

		// Looking for most specified pattern, because it can match many patterns
		if (atRules && atRules.length) {
			let prioritizedPattern;
			let max = 0;

			atRules.forEach(function(pattern) {
				const priority = calcAtRulePatternPriority(pattern, nodeType);

				if (priority > max) {
					max = priority;
					prioritizedPattern = pattern;
				}
			});

			if (max) {
				return prioritizedPattern;
			}
		}
	}

	if (orderInfo[nodeType]) {
		return orderInfo[nodeType];
	}

	// Return only description if there no patterns for that node
	return {
		description: getDescription(nodeType),
	};
};
