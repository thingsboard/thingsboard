'use strict';

/**
 * Check whether a node is logical combination (`:not`, `:has`, `:matches`)
 *
 * @param {import('postcss-selector-parser').Pseudo} node postcss-selector-parser node (of type pseudo)
 * @return {boolean} If `true`, the combination is logical
 */
module.exports = function isLogicalCombination(node) {
	if (node.type === 'pseudo') {
		switch (node.value) {
			case ':not':
			case ':has':
			case ':matches':
				return true;
			default:
				return false;
		}
	}

	return false;
};
