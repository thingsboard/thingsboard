'use strict';

const { isRoot } = require('./typeGuards');

/**
 * @param {string} [lang]
 */
function isStandardSyntaxLang(lang) {
	return lang && (lang === 'css' || lang === 'custom-template' || lang === 'template-literal');
}

/**
 * Check whether a declaration is standard
 *
 * @param {import('postcss').Declaration} decl
 */
module.exports = function(decl) {
	const prop = decl.prop;
	const parent = decl.parent;

	// Declarations belong in a declaration block or standard CSS source
	if (
		isRoot(parent) &&
		parent.source &&
		!isStandardSyntaxLang(
			/** @type {import('postcss').NodeSource & {lang?: string}} */ (parent.source).lang,
		)
	) {
		return false;
	}

	// Sass var (e.g. $var: x), nested list (e.g. $list: (x)) or nested map (e.g. $map: (key:value))
	if (prop[0] === '$') {
		return false;
	}

	// Less var (e.g. @var: x), but exclude variable interpolation (e.g. @{var})
	if (prop[0] === '@' && prop[1] !== '{') {
		return false;
	}

	// Sass nested properties (e.g. border: { style: solid; color: red; })
	if (
		// @ts-ignore TODO TYPES selector does not exists
		parent.selector &&
		// @ts-ignore
		parent.selector[parent.selector.length - 1] === ':' &&
		// @ts-ignore
		parent.selector.substring(0, 2) !== '--'
	) {
		return false;
	}

	// Less &:extend
	// @ts-ignore TODO TYPES extend does not exists
	if (decl.extend) {
		return false;
	}

	return true;
};
