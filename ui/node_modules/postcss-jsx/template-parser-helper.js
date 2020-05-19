"use strict";
const Literal = require("./literal");
const isLiteral = token => token[0] === "word" && /^\$\{[\s\S]*\}$/.test(token[1]);
function literal (start) {
	if (!isLiteral(start)) {
		return;
	}
	const tokens = [];
	let hasWord;
	let type;
	let token;
	while ((token = this.tokenizer.nextToken())) {
		tokens.push(token);
		type = token[0];
		if (type.length === 1) {
			break;
		} else if (type === "word") {
			hasWord = true;
		}
	}

	while (tokens.length) {
		this.tokenizer.back(tokens.pop());
	}

	if (type === "{" || (type === ":" && !hasWord)) {
		return;
	}

	const node = new Literal({
		text: start[1],
	});

	this.init(node, start[2], start[3]);

	return node;
}

function freeSemicolon (token) {
	this.spaces += token[1];
	const nodes = this.current.nodes;
	const prev = nodes && nodes[nodes.length - 1];
	if (prev && /^(rule|literal)$/.test(prev.type) && !prev.raws.ownSemicolon) {
		prev.raws.ownSemicolon = this.spaces;
		this.spaces = "";
	}
}

module.exports = {
	freeSemicolon: freeSemicolon,
	literal: literal,
};
