module.exports = function createExpectedPropertiesOrder(input) {
	const order = {};

	input.forEach((property, propertyIndex) => {
		order[property] = {
			propertyIndex,
		};
	});

	return order;
};
