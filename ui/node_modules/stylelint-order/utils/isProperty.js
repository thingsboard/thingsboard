// Check whether a property is a CSS property
const isCustomProperty = require('./isCustomProperty');
const isStandardSyntaxProperty = require('./isStandardSyntaxProperty');

module.exports = function isProperty(node) {
	return (
		node.type === 'decl' && isStandardSyntaxProperty(node.prop) && !isCustomProperty(node.prop)
	);
};
