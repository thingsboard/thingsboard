const stylelint = require('stylelint');
const _ = require('lodash');
const hasEmptyLineBefore = require('./hasEmptyLineBefore');
const removeEmptyLinesBefore = require('./removeEmptyLinesBefore');

module.exports = function checkEmptyLineBeforeFirstProp(propData, sharedInfo) {
	let emptyLineBefore = false;

	if (propData.orderData) {
		// Get an array of just the property groups, remove any solo properties
		let groups = _.reject(sharedInfo.primaryOption, _.isString);

		emptyLineBefore = _.get(groups[propData.orderData.separatedGroup - 2], 'emptyLineBefore');
	} else if (sharedInfo.emptyLineBeforeUnspecified) {
		emptyLineBefore = true;
	}

	if (emptyLineBefore && hasEmptyLineBefore(propData.node)) {
		if (sharedInfo.isFixEnabled) {
			removeEmptyLinesBefore(propData.node, sharedInfo.context.newline);
		} else {
			stylelint.utils.report({
				message: sharedInfo.messages.rejectedEmptyLineBefore(propData.name),
				node: propData.node,
				result: sharedInfo.result,
				ruleName: sharedInfo.ruleName,
			});
		}
	}
};
