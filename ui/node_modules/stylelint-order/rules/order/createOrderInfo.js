const _ = require('lodash');
const getDescription = require('./getDescription');

module.exports = function createOrderInfo(input) {
	let order = {};
	let expectedPosition = 0;

	input.forEach(item => {
		expectedPosition += 1;

		// Convert 'rules' into extended pattern
		if (item === 'rules') {
			item = {
				type: 'rule',
			};
		}

		if (item.type === 'rule') {
			// It there are no nodes like that create array for them
			if (!order[item.type]) {
				order[item.type] = [];
			}

			let nodeData = {
				expectedPosition,
				description: getDescription(item),
			};

			if (item.selector) {
				nodeData.selector = item.selector;

				if (_.isString(item.selector)) {
					nodeData.selector = new RegExp(item.selector);
				}
			}

			order[item.type].push(nodeData);
		}

		// Convert 'at-rules' into extended pattern
		if (item === 'at-rules') {
			item = {
				type: 'at-rule',
			};
		}

		if (item.type === 'at-rule') {
			// It there are no nodes like that create array for them
			if (!order[item.type]) {
				order[item.type] = [];
			}

			let nodeData = {
				expectedPosition,
				description: getDescription(item),
			};

			if (item.name) {
				nodeData.name = item.name;
			}

			if (item.parameter) {
				nodeData.parameter = item.parameter;

				if (_.isString(item.parameter)) {
					nodeData.parameter = new RegExp(item.parameter);
				}
			}

			if (!_.isUndefined(item.hasBlock)) {
				nodeData.hasBlock = item.hasBlock;
			}

			order[item.type].push(nodeData);
		}

		if (_.isString(item)) {
			order[item] = {
				expectedPosition,
				description: getDescription(item),
			};
		}
	});

	return order;
};
