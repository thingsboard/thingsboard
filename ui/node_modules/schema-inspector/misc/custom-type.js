var inspector = require('../');

// Custom type schema
var personValidation = {
	type: 'object',
	properties: {
		firstname: { type: 'string', minLength: 1 },
		lastname: { type: 'string', minLength: 1 },
		age: { type: 'integer', gt: 0, lte: 120 }
	}
};
// Custom Validation ($type)
var customValidation = {
	// $type will be like type but with an additional possible value "person"
	type: function (schema, candidate) {
		var result;
		// Custom type
		if (schema.$type === 'person')
			result = inspector.validate(personValidation, candidate);
		// Basic type
		else
			result = inspector.validate({ type: schema.$type }, candidate);
		if (!result.valid)
			return this.report(result.format());
	}
};
// Extend SchemaInspector.Validator
inspector.Validation.extend(customValidation);

var data = {
	firstname: ' sebastien ',
	lastname: 'chopin  ',
	age: '21'
};
var schema = { $type: 'person' };

var result = inspector.validate(schema, data);
if (!result.valid)
	console.log(result.format()); // Property @.age: must be integer, but is string