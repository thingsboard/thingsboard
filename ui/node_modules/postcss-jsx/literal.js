
"use strict";
const Node = require("postcss/lib/node");

/**
 * Represents a JS literal
 *
 * @extends Container
 *
 * @example
 * const root = postcss.parse('{}');
 * const literal = root.first;
 * literal.type       //=> 'literal'
 * literal.toString() //=> 'a{}'
 */
class Literal extends Node {
	constructor (defaults) {
		super(defaults);
		this.type = "literal";
	}
}

module.exports = Literal;
