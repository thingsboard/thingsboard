module.exports = function isCustomProperty(property) {
	return property.slice(0, 2) === '--';
};
