const getOrderData = require('./getOrderData');
const getComments = require('./getComments');

module.exports = function processMostNodes(node, index, order, processedNodes) {
	if (node.type === 'comment') {
		return processedNodes;
	}

	const nodeOrderData = getOrderData(order, node);

	node.position = nodeOrderData && nodeOrderData.position ? nodeOrderData.position : Infinity;
	node.initialIndex = index;

	// If comment on separate line before node, use node's indexes for comment
	const commentsBefore = getComments.beforeNode([], node.prev(), node);

	// If comment on same line with the node and node, use node's indexes for comment
	const commentsAfter = getComments.afterNode([], node.next(), node);

	return processedNodes.concat(commentsBefore, node, commentsAfter);
};
