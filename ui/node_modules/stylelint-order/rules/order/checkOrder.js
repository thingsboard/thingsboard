let stylelint = require('stylelint');
let _ = require('lodash');

module.exports = function checkOrder(firstNodeData, secondNodeData, allNodesData, sharedInfo) {
	let firstNodeIsSpecified = Boolean(firstNodeData.expectedPosition);
	let secondNodeIsSpecified = Boolean(secondNodeData.expectedPosition);

	// If both nodes have their position
	if (firstNodeIsSpecified && secondNodeIsSpecified) {
		return firstNodeData.expectedPosition <= secondNodeData.expectedPosition;
	}

	if (!firstNodeIsSpecified && secondNodeIsSpecified) {
		// If first node is unspecified, look for a specified node before it
		// to compare to the current node
		let priorSpecifiedNodeData = _.findLast(allNodesData.slice(0, -1), d =>
			Boolean(d.expectedPosition)
		);

		if (
			priorSpecifiedNodeData &&
			priorSpecifiedNodeData.expectedPosition &&
			priorSpecifiedNodeData.expectedPosition > secondNodeData.expectedPosition
		) {
			if (sharedInfo.isFixEnabled) {
				sharedInfo.shouldFix = true;

				// Don't go further, fix will be applied
				return;
			}

			stylelint.utils.report({
				message: sharedInfo.messages.expected(
					secondNodeData.description,
					priorSpecifiedNodeData.description
				),
				node: secondNodeData.node,
				result: sharedInfo.result,
				ruleName: sharedInfo.ruleName,
			});

			return true; // avoid logging another warning
		}
	}

	if (!firstNodeIsSpecified && !secondNodeIsSpecified) {
		return true;
	}

	let { unspecified } = sharedInfo;

	if (unspecified === 'ignore' && (!firstNodeIsSpecified || !secondNodeIsSpecified)) {
		return true;
	}

	if (unspecified === 'top' && !firstNodeIsSpecified) {
		return true;
	}

	if (unspecified === 'top' && !secondNodeIsSpecified) {
		return false;
	}

	if (unspecified === 'bottom' && !secondNodeIsSpecified) {
		return true;
	}

	if (unspecified === 'bottom' && !firstNodeIsSpecified) {
		return false;
	}
};
