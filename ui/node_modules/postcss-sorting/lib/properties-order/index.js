const postcss = require('postcss');

const createExpectedPropertiesOrder = require('../createExpectedPropertiesOrder');
const getComments = require('../getComments');
const getPropertiesOrderData = require('../getPropertiesOrderData');
const getContainingNode = require('../getContainingNode');
const isCustomProperty = require('../isCustomProperty');
const isRuleWithNodes = require('../isRuleWithNodes');
const isStandardSyntaxProperty = require('../isStandardSyntaxProperty');
const sorting = require('../sorting');

module.exports = function(css, opts) {
	const isAlphabetical = opts['properties-order'] === 'alphabetical';
	const expectedOrder = isAlphabetical
		? null
		: createExpectedPropertiesOrder(opts['properties-order']);
	let unspecifiedPropertiesPosition = opts['unspecified-properties-position'];

	if (!unspecifiedPropertiesPosition) {
		unspecifiedPropertiesPosition = 'bottom';
	}

	css.walk(function(input) {
		const node = getContainingNode(input);

		if (isRuleWithNodes(node)) {
			const allRuleNodes = [];
			let declarations = [];

			node.each(function(childNode, index) {
				if (
					childNode.type === 'decl' &&
					isStandardSyntaxProperty(childNode.prop) &&
					!isCustomProperty(childNode.prop)
				) {
					let unprefixedPropName = postcss.vendor.unprefixed(childNode.prop);

					// Hack to allow -moz-osx-font-smoothing to be understood
					// just like -webkit-font-smoothing
					if (unprefixedPropName.indexOf('osx-') === 0) {
						unprefixedPropName = unprefixedPropName.slice(4);
					}

					const propData = {
						name: childNode.prop,
						unprefixedName: unprefixedPropName,
						orderData: isAlphabetical
							? null
							: getPropertiesOrderData(expectedOrder, unprefixedPropName),
						node: childNode,
						initialIndex: index,
						unspecifiedPropertiesPosition,
					};

					// add a marker
					childNode.sortProperty = true;

					// If comment on separate line before node, use node's indexes for comment
					const commentsBefore = getComments.beforeDeclaration([], childNode.prev(), propData);

					// If comment on same line with the node and node, use node's indexes for comment
					const commentsAfter = getComments.afterDeclaration([], childNode.next(), propData);

					declarations = declarations.concat(commentsBefore, propData, commentsAfter);
				}
			});

			if (isAlphabetical) {
				declarations.sort(sorting.sortDeclarationsAlphabetically);
			} else {
				declarations.sort(sorting.sortDeclarations);
			}

			let foundDeclarations = false;

			node.each(function(childNode) {
				if (childNode.sortProperty) {
					if (!foundDeclarations) {
						foundDeclarations = true;

						declarations.forEach(item => {
							allRuleNodes.push(item.node);
						});
					}
				} else {
					allRuleNodes.push(childNode);
				}
			});

			node.removeAll();
			node.append(allRuleNodes);
		}
	});
};
