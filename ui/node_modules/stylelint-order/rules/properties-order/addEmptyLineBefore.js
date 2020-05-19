const _ = require('lodash');

// Add an empty line before a node. Mutates the node.
module.exports = function addEmptyLineBefore(node, newline) {
	if (!/\r?\n/.test(node.raws.before)) {
		node.raws.before = _.repeat(newline, 2) + node.raws.before;
	} else if (/^\r?\n/.test(node.raws.before)) {
		node.raws.before = newline + node.raws.before;
	} else if (/\r?\n$/.test(node.raws.before)) {
		node.raws.before = node.raws.before + newline;
	} else {
		node.raws.before = node.raws.before.replace(/(\r?\n)/, `${newline}$1`);
	}

	return node;
};
