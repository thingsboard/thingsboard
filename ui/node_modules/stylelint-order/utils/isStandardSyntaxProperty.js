/**
 * Check whether a property is standard
 *
 * @param {string} property
 * @return {boolean} If `true`, the property is standard
 */

module.exports = function isStandardSyntaxProperty(property) {
	// SCSS var (e.g. $var: x), list (e.g. $list: (x)) or map (e.g. $map: (key:value))
	if (property.startsWith('$')) {
		return false;
	}

	// Less var (e.g. @var: x)
	if (property.startsWith('@')) {
		return false;
	}

	// SCSS or Less interpolation
	if (/#{.+?}|@{.+?}|\$\(.+?\)/.test(property)) {
		return false;
	}

	return true;
};
