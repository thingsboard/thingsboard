// Remove empty lines before a node. Mutates the node.
module.exports = function removeEmptyLinesBefore(node, newline) {
	node.raws.before = node.raws.before.replace(/(\r?\n\s*\r?\n)+/g, newline);

	return node;
};
