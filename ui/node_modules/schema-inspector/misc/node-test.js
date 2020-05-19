var inspector = require('../');

var schema = {
	type: 'object',
	properties: {
		lorem: { type: 'string', eq: 'ipsum' },
		dolor: {
			type: 'array',
			items: { type: 'number' }
		}
	}
};

var candidate = {
	lorem: 'not_ipsum',
	dolor: [ 12, 34, 'ERROR', 45, 'INVALID' ]
};
var result = inspector.validate(schema, candidate, function (err, result) {
	console.log(result);
});

// var schema = { type: 'object', properties: { lol: { someKeys: ['pouet'] } } };
// var subject = { lol: null };
// inspector.validate(schema, subject);
