/**
 * Check whether a property is a custom one
 *
 * @param {string} property
 * @return {boolean} If `true`, property is a custom one
 */

module.exports = function isCustomProperty(property) {
	return property.startsWith('--');
};
