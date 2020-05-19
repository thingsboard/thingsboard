'use strict';
// This file exists to remove the need for Flow's ignore_non_literal_requires option

/**
 * @param {string} name
 * @return {any} any module
 */
module.exports = function(name) {
	return require(name);
};
