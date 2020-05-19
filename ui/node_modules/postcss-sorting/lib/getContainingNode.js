module.exports = function getContainingNode(node) {
	// For styled-components declarations are children of Root node
	if (
		node.type !== 'rule' &&
		node.type !== 'atrule' &&
		node.parent.document &&
		node.parent.document.nodes &&
		node.parent.document.nodes.some(item => item.type === 'root')
	) {
		return node.parent;
	}

	return node;
};
