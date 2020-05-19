/**
 * Check whether a property is a Less mixin
 */

module.exports = function isLessMixin(node) {
	return node.type === 'atrule' && node.mixin;
};
