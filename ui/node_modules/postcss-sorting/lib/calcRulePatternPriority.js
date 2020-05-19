module.exports = function calcRulePatternPriority(pattern, node) {
	// 0 — it pattern doesn't match
	// 1 — pattern without `selector`
	// 2 — pattern match `selector`

	let priority = 0;

	// doesn't have `selector`
	if (!pattern.hasOwnProperty('selector')) {
		priority = 1;
	}

	// match `selector`
	if (pattern.hasOwnProperty('selector') && pattern.selector.test(node.selector)) {
		priority = 2;
	}

	return priority;
};
