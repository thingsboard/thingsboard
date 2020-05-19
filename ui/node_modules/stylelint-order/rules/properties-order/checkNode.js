const stylelint = require('stylelint');
const postcss = require('postcss');
const postcssSorting = require('postcss-sorting');
const { isProperty } = require('../../utils');
const checkEmptyLineBefore = require('./checkEmptyLineBefore');
const checkEmptyLineBeforeFirstProp = require('./checkEmptyLineBeforeFirstProp');
const checkOrder = require('./checkOrder');
const getNodeData = require('./getNodeData');
const createFlatOrder = require('./createFlatOrder');

module.exports = function checkNode(node, sharedInfo, originalNode) {
	// First, check order
	let allPropData = getAllPropData(node);

	if (!sharedInfo.isFixEnabled) {
		allPropData.forEach(checkEveryPropForOrder);
	}

	if (sharedInfo.isFixEnabled) {
		let shouldFixOrder = false;

		// Check if there order violation to avoid running re-ordering unnecessery
		allPropData.forEach(function checkEveryPropForOrder2(propData, index) {
			// Skip first decl
			if (index === 0) {
				return;
			}

			// return early if we know there is a violation and auto fix should be applied
			if (shouldFixOrder) {
				return;
			}

			let previousPropData = allPropData[index - 1];

			let checkedOrder = checkOrder({
				firstPropData: previousPropData,
				secondPropData: propData,
				unspecified: sharedInfo.unspecified,
				allPropData: allPropData.slice(0, index),
			});

			if (!checkedOrder.isCorrect) {
				shouldFixOrder = true;
			}
		});

		if (shouldFixOrder) {
			let sortingOptions = {
				'properties-order': createFlatOrder(sharedInfo.primaryOption),
				'unspecified-properties-position':
					sharedInfo.unspecified === 'ignore' ? 'bottom' : sharedInfo.unspecified,
			};

			// creating PostCSS Root node with current node as a child,
			// so PostCSS Sorting can process it
			let tempRoot = postcss.root({ nodes: [originalNode] });

			postcssSorting(sortingOptions)(tempRoot);

			let allPropData2 = getAllPropData(node);

			allPropData2.forEach(checkEveryPropForOrder);
		}
	}

	// Second, check emptyLineBefore
	sharedInfo.lastKnownSeparatedGroup = 1;

	let propsCount = node.nodes.filter(item => isProperty(item)).length;
	let allNodesData = node.nodes.map(function collectDataForEveryNode(child) {
		return getNodeData(child, sharedInfo.expectedOrder);
	});

	allNodesData.forEach(function checkEveryPropForEmptyLine(nodeData, index) {
		let previousNodeData = allNodesData[index - 1];

		// if previous node is shared-line comment, use second previous node
		if (
			previousNodeData &&
			previousNodeData.node.type === 'comment' &&
			!previousNodeData.node.raw('before').includes('\n')
		) {
			previousNodeData = allNodesData[index - 2];
		}

		// skip first decl
		if (!previousNodeData) {
			return;
		}

		// Nodes should be standard declarations
		if (!isProperty(previousNodeData.node) || !isProperty(nodeData.node)) {
			return;
		}

		checkEmptyLineBefore(previousNodeData, nodeData, sharedInfo, propsCount);
	});

	// Check if empty line before first prop should be removed
	if (isProperty(allNodesData[0].node)) {
		checkEmptyLineBeforeFirstProp(allNodesData[0], sharedInfo);
	}

	function checkEveryPropForOrder(propData, index, listOfProps) {
		// Skip first decl
		if (index === 0) {
			return;
		}

		const previousPropData = listOfProps[index - 1];

		const checkedOrder = checkOrder({
			firstPropData: previousPropData,
			secondPropData: propData,
			unspecified: sharedInfo.unspecified,
			allPropData: listOfProps.slice(0, index),
		});

		if (!checkedOrder.isCorrect) {
			const { orderData } = checkedOrder.secondNode;

			stylelint.utils.report({
				message: sharedInfo.messages.expected(
					checkedOrder.secondNode.name,
					checkedOrder.firstNode.name,
					orderData && orderData.groupName
				),
				node: checkedOrder.secondNode.node,
				result: sharedInfo.result,
				ruleName: sharedInfo.ruleName,
			});
		}
	}

	function getAllPropData(inputNode) {
		return inputNode.nodes
			.filter(item => isProperty(item))
			.map(item => getNodeData(item, sharedInfo.expectedOrder));
	}
};
