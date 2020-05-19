const createExpectedOrder = require('../createExpectedOrder');
const isRuleWithNodes = require('../isRuleWithNodes');
const processLastComments = require('../processLastComments');
const processMostNodes = require('../processMostNodes');
const getContainingNode = require('../getContainingNode');
const sorting = require('../sorting');

module.exports = function(css, opts) {
	const expectedOrder = createExpectedOrder(opts.order);

	css.walk(function(input) {
		const node = getContainingNode(input);

		if (isRuleWithNodes(node)) {
			// Nodes for sorting
			let processed = [];

			// Add indexes to nodes
			node.each(function(childNode, index) {
				processed = processMostNodes(childNode, index, expectedOrder, processed);
			});

			// Add last comments in the rule. Need this because last comments are not belonging to anything
			node.each(function(childNode, index) {
				processed = processLastComments(childNode, index, processed);
			});

			// Sort declarations saved for sorting
			processed.sort(sorting.sortByIndexes);

			// Enforce semicolon for the last node
			node.raws.semicolon = true;

			// Replace rule content with sorted one
			node.removeAll();
			node.append(processed);
		}
	});
};
