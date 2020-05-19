'use strict';

/**
 * Check whether a combinator is standard
 *
 * @param {import('postcss-selector-parser').Combinator} node postcss-selector-parser node (of type combinator)
 * @return {boolean} If `true`, the combinator is standard
 */
module.exports = function(node) {
	// Ghost descendant combinators around reference combinators like `/deep/`
	// postcss-selector-parser parsers references combinators as tag selectors surrounded
	// by descendant combinators
	const prev = node.prev();
	const next = node.next();

	if (
		(prev &&
			prev.type === 'tag' &&
			typeof prev.value === 'string' &&
			prev.value.startsWith('/') &&
			prev.value.endsWith('/')) ||
		(next &&
			next.type === 'tag' &&
			typeof next.value === 'string' &&
			next.value.startsWith('/') &&
			next.value.endsWith('/'))
	) {
		return false;
	}

	return true;
};
