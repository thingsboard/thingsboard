const atRuleExcludes = ['function', 'if', 'else', 'for', 'each', 'while'];

module.exports = function isRuleWithNodes(node) {
	if (node.type === 'atrule' && atRuleExcludes.includes(node.name)) {
		return false;
	}

	return node.nodes && node.nodes.length && !node.nodes.some(item => item.type === 'literal');
};
