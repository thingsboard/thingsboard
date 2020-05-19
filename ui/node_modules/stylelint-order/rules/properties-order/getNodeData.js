const postcss = require('postcss');
const { isProperty } = require('../../utils');

module.exports = function getNodeData(node, expectedOrder) {
	let nodeData = {
		node,
	};

	if (isProperty(node)) {
		let { prop } = node;
		let unprefixedPropName = postcss.vendor.unprefixed(prop);

		// Hack to allow -moz-osx-font-smoothing to be understood
		// just like -webkit-font-smoothing
		if (unprefixedPropName.startsWith('osx-')) {
			unprefixedPropName = unprefixedPropName.slice(4);
		}

		nodeData.name = prop;
		nodeData.unprefixedName = unprefixedPropName;
		nodeData.orderData = expectedOrder[unprefixedPropName];
	}

	return nodeData;
};
