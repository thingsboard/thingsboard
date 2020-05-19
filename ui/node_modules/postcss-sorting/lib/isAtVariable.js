/**
 * Check whether a property is a @-variable (Less)
 */

module.exports = function isAtVariable(node) {
	return node.type === 'atrule' && node.variable;
};
