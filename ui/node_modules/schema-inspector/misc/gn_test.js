var schema = {
	type: 'object',
	properties: {
		'*': { type: 'any' }
	}
};

var candidate = SchemaInspector.generate(schema);
var s = SchemaInspector.validate(schema, candidate);

console.log(candidate);
console.log('Valid:', s.valid);
if (s.valid !== true) {
	throw new Error(s.format());
}
