'use strict';

const _ = require('lodash');
const isCustomProperty = require('../../utils/isCustomProperty');
const isStandardSyntaxDeclaration = require('../../utils/isStandardSyntaxDeclaration');
const isStandardSyntaxProperty = require('../../utils/isStandardSyntaxProperty');
const optionsMatches = require('../../utils/optionsMatches');
const postcss = require('postcss');
const properties = require('known-css-properties').all;
const report = require('../../utils/report');
const ruleMessages = require('../../utils/ruleMessages');
const validateOptions = require('../../utils/validateOptions');

const ruleName = 'property-no-unknown';

const messages = ruleMessages(ruleName, {
	rejected: (property) => `Unexpected unknown property "${property}"`,
});

function rule(actual, options) {
	const allValidProperties = new Set(properties);

	return (root, result) => {
		const validOptions = validateOptions(
			result,
			ruleName,
			{ actual },
			{
				actual: options,
				possible: {
					ignoreProperties: [_.isString, _.isRegExp],
					checkPrefixed: _.isBoolean,
					ignoreSelectors: [_.isString, _.isRegExp],
				},
				optional: true,
			},
		);

		if (!validOptions) {
			return;
		}

		const shouldCheckPrefixed = _.get(options, 'checkPrefixed');

		root.walkDecls((decl) => {
			const prop = decl.prop;

			if (!isStandardSyntaxProperty(prop)) {
				return;
			}

			if (!isStandardSyntaxDeclaration(decl)) {
				return;
			}

			if (isCustomProperty(prop)) {
				return;
			}

			if (!shouldCheckPrefixed && postcss.vendor.prefix(prop)) {
				return;
			}

			if (optionsMatches(options, 'ignoreProperties', prop)) {
				return;
			}

			const { selector } = decl.parent;

			if (selector && optionsMatches(options, 'ignoreSelectors', selector)) {
				return;
			}

			if (allValidProperties.has(prop.toLowerCase())) {
				return;
			}

			report({
				message: messages.rejected(prop),
				node: decl,
				result,
				ruleName,
			});
		});
	};
}

rule.ruleName = ruleName;
rule.messages = messages;
module.exports = rule;
