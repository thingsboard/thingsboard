module.exports = {
	beforeNode,
	afterNode,
	beforeDeclaration,
	afterDeclaration,
};

function beforeNode(comments, previousNode, node, currentInitialIndex) {
	if (!previousNode || previousNode.type !== 'comment') {
		return comments;
	}

	if (
		!previousNode.raws.before ||
		(previousNode.raws.before.indexOf('\n') === -1 && previousNode.prev())
	) {
		return comments;
	}

	currentInitialIndex = currentInitialIndex || node.initialIndex;

	previousNode.position = node.position;
	previousNode.initialIndex = currentInitialIndex - 0.0001;

	const newComments = [previousNode].concat(comments);

	return beforeNode(newComments, previousNode.prev(), node, previousNode.initialIndex);
}

function afterNode(comments, nextNode, node, currentInitialIndex) {
	if (!nextNode || nextNode.type !== 'comment') {
		return comments;
	}

	if (!nextNode.raws.before || nextNode.raws.before.indexOf('\n') >= 0) {
		return comments;
	}

	currentInitialIndex = currentInitialIndex || node.initialIndex;

	nextNode.position = node.position;
	nextNode.initialIndex = currentInitialIndex + 0.0001;

	return afterNode(comments.concat(nextNode), nextNode.next(), node, nextNode.initialIndex);
}

function beforeDeclaration(comments, previousNode, nodeData, currentInitialIndex) {
	if (!previousNode || previousNode.type !== 'comment') {
		return comments;
	}

	if (!previousNode.raws.before || previousNode.raws.before.indexOf('\n') === -1) {
		return comments;
	}

	currentInitialIndex = currentInitialIndex || nodeData.initialIndex;

	const commentData = {
		orderData: nodeData.orderData,
		node: previousNode,
		unprefixedName: nodeData.unprefixedName, // related property name for alphabetical order
		unspecifiedPropertiesPosition: nodeData.unspecifiedPropertiesPosition,
	};

	commentData.initialIndex = currentInitialIndex - 0.0001;

	// add a marker
	previousNode.sortProperty = true;

	const newComments = [commentData].concat(comments);

	return beforeDeclaration(newComments, previousNode.prev(), nodeData, commentData.initialIndex);
}

function afterDeclaration(comments, nextNode, nodeData, currentInitialIndex) {
	if (!nextNode || nextNode.type !== 'comment') {
		return comments;
	}

	if (!nextNode.raws.before || nextNode.raws.before.indexOf('\n') >= 0) {
		return comments;
	}

	currentInitialIndex = currentInitialIndex || nodeData.initialIndex;

	const commentData = {
		orderData: nodeData.orderData,
		node: nextNode,
		unprefixedName: nodeData.unprefixedName, // related property name for alphabetical order
		unspecifiedPropertiesPosition: nodeData.unspecifiedPropertiesPosition,
	};

	commentData.initialIndex = currentInitialIndex + 0.0001;

	// add a marker
	nextNode.sortProperty = true;

	return afterDeclaration(
		comments.concat(commentData),
		nextNode.next(),
		nodeData,
		commentData.initialIndex
	);
}
