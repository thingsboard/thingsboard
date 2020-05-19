const stylelint = require('stylelint');
const checkOrder = require('./checkOrder');
const getOrderData = require('./getOrderData');

module.exports = function checkNode(node, sharedInfo) {
	let allNodesData = [];

	node.each(function processEveryNode(child) {
		// Skip comments
		if (child.type === 'comment') {
			return;
		}

		// Receive node description and expectedPosition
		let nodeOrderData = getOrderData(sharedInfo.orderInfo, child);

		let nodeData = {
			node: child,
			description: nodeOrderData.description,
			expectedPosition: nodeOrderData.expectedPosition,
		};

		allNodesData.push(nodeData);

		let previousNodeData = allNodesData[allNodesData.length - 2];

		// Skip first node
		if (!previousNodeData) {
			return;
		}

		let isCorrectOrder = checkOrder(previousNodeData, nodeData, allNodesData, sharedInfo);

		if (isCorrectOrder) {
			return;
		}

		if (sharedInfo.isFixEnabled) {
			sharedInfo.shouldFix = true;

			// Don't go further, fix will be applied
			return;
		}

		stylelint.utils.report({
			message: sharedInfo.messages.expected(nodeData.description, previousNodeData.description),
			node: child,
			result: sharedInfo.result,
			ruleName: sharedInfo.ruleName,
		});
	});
};
