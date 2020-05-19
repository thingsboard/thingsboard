'use strict';

const isStandardSyntaxRule = require('../../utils/isStandardSyntaxRule');
const parseSelector = require('../../utils/parseSelector');
const punctuationSets = require('../../reference/punctuationSets');
const report = require('../../utils/report');
const ruleMessages = require('../../utils/ruleMessages');
const validateOptions = require('../../utils/validateOptions');

const ruleName = 'selector-descendant-combinator-no-non-space';

const messages = ruleMessages(ruleName, {
	rejected: (nonSpaceCharacter) => `Unexpected "${nonSpaceCharacter}"`,
});

function rule(expectation, options, context) {
	return (root, result) => {
		const validOptions = validateOptions(result, ruleName, {
			actual: expectation,
		});

		if (!validOptions) {
			return;
		}

		root.walkRules((rule) => {
			if (!isStandardSyntaxRule(rule)) {
				return;
			}

			let hasFixed = false;
			const selector = rule.raws.selector ? rule.raws.selector.raw : rule.selector;

			const fixedSelector = parseSelector(selector, result, rule, (fullSelector) => {
				fullSelector.walkCombinators((combinatorNode) => {
					if (!isActuallyCombinator(combinatorNode)) {
						return;
					}

					const value = combinatorNode.value;

					if (punctuationSets.nonSpaceCombinators.has(value)) {
						return;
					}

					if (
						value.includes('  ') ||
						value.includes('\t') ||
						value.includes('\n') ||
						value.includes('\r')
					) {
						if (context.fix && /^\s+$/.test(value)) {
							hasFixed = true;
							combinatorNode.value = ' ';

							return;
						}

						report({
							result,
							ruleName,
							message: messages.rejected(value),
							node: rule,
							index: combinatorNode.sourceIndex,
						});
					}
				});
			});

			if (hasFixed) {
				if (!rule.raws.selector) {
					rule.selector = fixedSelector;
				} else {
					rule.raws.selector.raw = fixedSelector;
				}
			}
		});
	};
}

rule.ruleName = ruleName;
rule.messages = messages;
module.exports = rule;

/**
 * Check whether is actually a combinator.
 * @param {Node} combinatorNode The combinator node
 * @returns {boolean} `true` if is actually a combinator.
 */
function isActuallyCombinator(combinatorNode) {
	// `.foo  /*comment*/, .bar`
	//      ^^
	// If include comments, this spaces is a combinator, but it is not combinators.
	if (!/^\s+$/.test(combinatorNode.value)) {
		return true;
	}

	let next = combinatorNode.next();

	while (skipTest(next)) {
		next = next.next();
	}

	if (isNonTarget(next)) {
		return false;
	}

	let prev = combinatorNode.prev();

	while (skipTest(prev)) {
		prev = prev.prev();
	}

	if (isNonTarget(prev)) {
		return false;
	}

	return true;

	function skipTest(node) {
		if (!node) {
			return false;
		}

		if (node.type === 'comment') {
			return true;
		}

		if (node.type === 'combinator' && /^\s+$/.test(node.value)) {
			return true;
		}

		return false;
	}

	function isNonTarget(node) {
		if (!node) {
			return true;
		}

		if (node.type === 'combinator' && !/^\s+$/.test(node.value)) {
			return true;
		}

		return false;
	}
}
