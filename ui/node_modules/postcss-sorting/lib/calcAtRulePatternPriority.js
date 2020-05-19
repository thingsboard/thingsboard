module.exports = function calcAtRulePatternPriority(pattern, node) {
	// 0 — it pattern doesn't match
	// 1 — pattern without `name` and `hasBlock`
	// 10010 — pattern match `hasBlock`
	// 10100 — pattern match `name`
	// 20110 — pattern match `name` and `hasBlock`
	// 21100 — patter match `name` and `parameter`
	// 31110 — patter match `name`, `parameter`, and `hasBlock`

	let priority = 0;

	// match `hasBlock`
	if (pattern.hasOwnProperty('hasBlock') && node.hasBlock === pattern.hasBlock) {
		priority += 10;
		priority += 10000;
	}

	// match `name`
	if (pattern.hasOwnProperty('name') && node.name === pattern.name) {
		priority += 100;
		priority += 10000;
	}

	// match `name`
	if (pattern.hasOwnProperty('parameter') && pattern.parameter.test(node.parameter)) {
		priority += 1100;
		priority += 10000;
	}

	// doesn't have `name` and `hasBlock`
	if (
		!pattern.hasOwnProperty('hasBlock') &&
		!pattern.hasOwnProperty('name') &&
		!pattern.hasOwnProperty('paremeter')
	) {
		priority = 1;
	}

	// patter has `name` and `hasBlock`, but it doesn't match both properties
	if (pattern.hasOwnProperty('hasBlock') && pattern.hasOwnProperty('name') && priority < 20000) {
		priority = 0;
	}

	// patter has `name` and `parameter`, but it doesn't match both properties
	if (pattern.hasOwnProperty('name') && pattern.hasOwnProperty('parameter') && priority < 21100) {
		priority = 0;
	}

	// patter has `name`, `parameter`, and `hasBlock`, but it doesn't match all properties
	if (
		pattern.hasOwnProperty('name') &&
		pattern.hasOwnProperty('parameter') &&
		pattern.hasOwnProperty('hasBlock') &&
		priority < 30000
	) {
		priority = 0;
	}

	return priority;
};
