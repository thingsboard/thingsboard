if (typeof require === 'function') {
  var SchemaInspector = require('../');
}

SchemaInspector.Validation.extend({
	unique: function (schema, candidate, callback) {
		console.log(this.origin._connection);
    callback();
	}
});

var schema = {
	type: 'array',
	items: {
		type: 'number', $unique: true
	}
};

var obj = [ 0, 5, 10, 15, 17, 20];

// -----------------------------------------------------------------------------
obj._connection = 'test';
SchemaInspector.validate(schema, obj, function (err, r) {
	console.log(r);
	console.log(r.format());
});
