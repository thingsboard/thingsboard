"use strict";

const TemplateParser = require("./template-parser");
const Input = require("postcss/lib/input");

function templateParse (css, opts) {
	const input = new Input(css, opts);
	input.quasis = opts.quasis;
	const parser = new TemplateParser(input);
	parser.parse();

	return parser.root;
}
module.exports = templateParse;
