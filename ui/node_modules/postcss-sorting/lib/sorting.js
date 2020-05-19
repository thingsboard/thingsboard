const postcss = require('postcss');
const _ = require('lodash');
const shorthandData = require('./shorthandData');

module.exports = {
	sortDeclarations,
	sortDeclarationsAlphabetically,
	sortByIndexes,
};

function sortDeclarations(a, b) {
	// If unprefixed prop names are the same, compare the prefixed versions
	if (a.node.type === 'decl' && b.node.type === 'decl' && a.unprefixedName === b.unprefixedName) {
		// If first property has no prefix and second property has prefix
		if (!postcss.vendor.prefix(a.name).length && postcss.vendor.prefix(b.name).length) {
			return 1;
		}

		if (postcss.vendor.prefix(a.name).length && !postcss.vendor.prefix(b.name).length) {
			return -1;
		}
	}

	if (!_.isUndefined(a.orderData) && !_.isUndefined(b.orderData)) {
		// If a and b have the same group index, and a's property index is
		// higher than b's property index, in a sorted list a appears after
		// b:
		if (a.orderData.propertyIndex !== b.orderData.propertyIndex) {
			return a.orderData.propertyIndex - b.orderData.propertyIndex;
		}
	}

	if (
		a.unspecifiedPropertiesPosition === 'bottom' ||
		a.unspecifiedPropertiesPosition === 'bottomAlphabetical' ||
		b.unspecifiedPropertiesPosition === 'bottomAlphabetical'
	) {
		if (!_.isUndefined(a.orderData) && _.isUndefined(b.orderData)) {
			return -1;
		}

		if (_.isUndefined(a.orderData) && !_.isUndefined(b.orderData)) {
			return 1;
		}
	}

	if (a.unspecifiedPropertiesPosition === 'top') {
		if (!_.isUndefined(a.orderData) && _.isUndefined(b.orderData)) {
			return 1;
		}

		if (_.isUndefined(a.orderData) && !_.isUndefined(b.orderData)) {
			return -1;
		}
	}

	if (a.unspecifiedPropertiesPosition === 'bottomAlphabetical') {
		if (_.isUndefined(a.orderData) && _.isUndefined(b.orderData)) {
			return sortDeclarationsAlphabetically(a, b);
		}
	}

	// If a and b have the same group index and the same property index,
	// in a sorted list they appear in the same order they were in
	// original array:
	return a.initialIndex - b.initialIndex;
}

function isShorthand(a, b) {
	const longhands = shorthandData[a] || [];

	return longhands.includes(b);
}

function sortDeclarationsAlphabetically(a, b) {
	if (isShorthand(a.unprefixedName, b.unprefixedName)) {
		return -1;
	}

	if (isShorthand(b.unprefixedName, a.unprefixedName)) {
		return 1;
	}

	if (a.unprefixedName === b.unprefixedName) {
		if (a.node.type === 'decl' && b.node.type === 'decl') {
			// If first property has no prefix and second property has prefix
			if (!postcss.vendor.prefix(a.name).length && postcss.vendor.prefix(b.name).length) {
				return 1;
			}

			if (postcss.vendor.prefix(a.name).length && !postcss.vendor.prefix(b.name).length) {
				return -1;
			}
		}

		return a.initialIndex - b.initialIndex;
	}

	return a.unprefixedName <= b.unprefixedName ? -1 : 1;
}

function sortByIndexes(a, b) {
	// If a and b have the same group index, and a's property index is
	// higher than b's property index, in a sorted list a appears after
	// b:
	if (a.position !== b.position) {
		return a.position - b.position;
	}

	// If a and b have the same group index and the same property index,
	// in a sorted list they appear in the same order they were in
	// original array:
	return a.initialIndex - b.initialIndex;
}
