"use strict";
const Stringifier = require("postcss/lib/stringifier");

class TemplateStringifier extends Stringifier {
	literal (node) {
		this.builder(node.text, node);
		if (node.raws.ownSemicolon) {
			this.builder(node.raws.ownSemicolon, node, "end");
		}
	}
};

module.exports = TemplateStringifier;
