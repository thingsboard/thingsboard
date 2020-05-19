'use strict';

const { isPathValid } = require('ignore').default;

/**
 * @param {import('ignore').Ignore} ignorer
 * @param {string[]} filePaths
 * @returns {string[]}
 */
module.exports = function filterFilePaths(ignorer, filePaths) {
	const validForIgnore = filePaths.filter(isPathValid);
	// Paths which starts with `..` are not valid for `ignore`, e. g. `../style.css`
	const notValidForIgnore = filePaths.filter((p) => !validForIgnore.includes(p));

	const filteredByIgnore = ignorer.filter(validForIgnore);

	// Preserving files order, while removing paths which were filtered by `ignore`
	return filePaths.filter((p) => notValidForIgnore.includes(p) || filteredByIgnore.includes(p));
};
