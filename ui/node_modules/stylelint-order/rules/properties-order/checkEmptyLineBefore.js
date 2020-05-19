let stylelint = require('stylelint');
let _ = require('lodash');
let addEmptyLineBefore = require('./addEmptyLineBefore');
let hasEmptyLineBefore = require('./hasEmptyLineBefore');
let removeEmptyLinesBefore = require('./removeEmptyLinesBefore');

module.exports = function checkEmptyLineBefore(
	firstPropData,
	secondPropData,
	sharedInfo,
	propsCount
) {
	let firstPropIsSpecified = Boolean(firstPropData.orderData);
	let secondPropIsSpecified = Boolean(secondPropData.orderData);

	// Check newlines between groups
	let firstPropGroup = firstPropIsSpecified
		? firstPropData.orderData.separatedGroup
		: sharedInfo.lastKnownSeparatedGroup;
	let secondPropGroup = secondPropIsSpecified
		? secondPropData.orderData.separatedGroup
		: sharedInfo.lastKnownSeparatedGroup;

	sharedInfo.lastKnownSeparatedGroup = secondPropGroup;

	let startOfSpecifiedGroup = secondPropIsSpecified && firstPropGroup !== secondPropGroup;
	let startOfUnspecifiedGroup = firstPropIsSpecified && !secondPropIsSpecified;

	if (startOfSpecifiedGroup || startOfUnspecifiedGroup) {
		// Get an array of just the property groups, remove any solo properties
		let groups = _.reject(sharedInfo.primaryOption, _.isString);

		let emptyLineBefore = _.get(groups[secondPropGroup - 2], 'emptyLineBefore');

		if (startOfUnspecifiedGroup) {
			emptyLineBefore = sharedInfo.emptyLineBeforeUnspecified;
		}

		// Threshold logic
		let belowEmptyLineThreshold = propsCount < sharedInfo.emptyLineMinimumPropertyThreshold;
		let emptyLineThresholdInsertLines = emptyLineBefore === 'threshold' && !belowEmptyLineThreshold;
		let emptyLineThresholdRemoveLines = emptyLineBefore === 'threshold' && belowEmptyLineThreshold;

		if (
			(emptyLineBefore === 'always' || emptyLineThresholdInsertLines) &&
			!hasEmptyLineBefore(secondPropData.node)
		) {
			if (sharedInfo.isFixEnabled) {
				addEmptyLineBefore(secondPropData.node, sharedInfo.context.newline);
			} else {
				stylelint.utils.report({
					message: sharedInfo.messages.expectedEmptyLineBefore(secondPropData.name),
					node: secondPropData.node,
					result: sharedInfo.result,
					ruleName: sharedInfo.ruleName,
				});
			}
		} else if (
			(emptyLineBefore === 'never' || emptyLineThresholdRemoveLines) &&
			hasEmptyLineBefore(secondPropData.node)
		) {
			if (sharedInfo.isFixEnabled) {
				removeEmptyLinesBefore(secondPropData.node, sharedInfo.context.newline);
			} else {
				stylelint.utils.report({
					message: sharedInfo.messages.rejectedEmptyLineBefore(secondPropData.name),
					node: secondPropData.node,
					result: sharedInfo.result,
					ruleName: sharedInfo.ruleName,
				});
			}
		}
	}

	// Check newlines between properties inside a group
	if (
		firstPropIsSpecified &&
		secondPropIsSpecified &&
		firstPropData.orderData.groupPosition === secondPropData.orderData.groupPosition
	) {
		if (
			secondPropData.orderData.noEmptyLineBeforeInsideGroup &&
			hasEmptyLineBefore(secondPropData.node)
		) {
			if (sharedInfo.isFixEnabled) {
				removeEmptyLinesBefore(secondPropData.node, sharedInfo.context.newline);
			} else {
				stylelint.utils.report({
					message: sharedInfo.messages.rejectedEmptyLineBefore(secondPropData.name),
					node: secondPropData.node,
					result: sharedInfo.result,
					ruleName: sharedInfo.ruleName,
				});
			}
		}
	}
};
