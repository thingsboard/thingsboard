const postcss = require('postcss');
const _ = require('lodash');
const checkAlphabeticalOrder = require('../checkAlphabeticalOrder');

module.exports = function checkOrder({ firstPropData, secondPropData, allPropData, unspecified }) {
	function report(isCorrect, firstNode = firstPropData, secondNode = secondPropData) {
		return {
			isCorrect,
			firstNode,
			secondNode,
		};
	}

	if (firstPropData.unprefixedName === secondPropData.unprefixedName) {
		// If first property has no prefix and second property has prefix
		if (
			!postcss.vendor.prefix(firstPropData.name).length &&
			postcss.vendor.prefix(secondPropData.name).length
		) {
			return report(false);
		}

		return report(true);
	}

	const firstPropIsSpecified = Boolean(firstPropData.orderData);
	const secondPropIsSpecified = Boolean(secondPropData.orderData);

	// Check actual known properties
	if (firstPropIsSpecified && secondPropIsSpecified) {
		return report(
			firstPropData.orderData.expectedPosition <= secondPropData.orderData.expectedPosition
		);
	}

	if (!firstPropIsSpecified && secondPropIsSpecified) {
		// If first prop is unspecified, look for a specified prop before it to
		// compare to the current prop
		const priorSpecifiedPropData = _.findLast(allPropData.slice(0, -1), d => Boolean(d.orderData));

		if (
			priorSpecifiedPropData &&
			priorSpecifiedPropData.orderData &&
			priorSpecifiedPropData.orderData.expectedPosition > secondPropData.orderData.expectedPosition
		) {
			return report(false, priorSpecifiedPropData, secondPropData);
		}
	}

	// Now deal with unspecified props
	// Starting with bottomAlphabetical as it requires more specific conditionals
	if (unspecified === 'bottomAlphabetical' && firstPropIsSpecified && !secondPropIsSpecified) {
		return report(true);
	}

	if (unspecified === 'bottomAlphabetical' && !firstPropIsSpecified && !secondPropIsSpecified) {
		if (checkAlphabeticalOrder(firstPropData, secondPropData)) {
			return report(true);
		}

		return report(false);
	}

	if (unspecified === 'bottomAlphabetical' && !firstPropIsSpecified) {
		return report(false);
	}

	if (!firstPropIsSpecified && !secondPropIsSpecified) {
		return report(true);
	}

	if (unspecified === 'ignore' && (!firstPropIsSpecified || !secondPropIsSpecified)) {
		return report(true);
	}

	if (unspecified === 'top' && !firstPropIsSpecified) {
		return report(true);
	}

	if (unspecified === 'top' && !secondPropIsSpecified) {
		return report(false);
	}

	if (unspecified === 'bottom' && !secondPropIsSpecified) {
		return report(true);
	}

	if (unspecified === 'bottom' && !firstPropIsSpecified) {
		return report(false);
	}
};
