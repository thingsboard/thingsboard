'use strict';
module.exports = str => {
	const match = str.match(/^[ \t]*(?=\S)/gm);

	if (!match) {
		return 0;
	}

	// TODO: Use spread operator when targeting Node.js 6
	return Math.min.apply(Math, match.map(x => x.length));
};
