module.exports = function processLastComments(node, index, processedNodes) {
	if (node.type === 'comment' && !node.hasOwnProperty('position')) {
		node.position = Infinity;
		node.initialIndex = index;

		return processedNodes.concat(node);
	}

	return processedNodes;
};
