var inspector = require('../');

// Custom type schema
var customValidation = {
	validDate: function (schema, date) {
      if (schema.$validDate === true
         && Object.prototype.toString.call(date) === "[object Date]"
         && isNaN(date.getTime())) {
          this.report('must be a valid date');
      }
  }
};
inspector.Validation.extend(customValidation);

var data = {
	validDate: new Date(),
	invalidDate: new Date('invalid'),
	noDate: 'hello!'
};
var schema = {
	type: 'object',
	items: {
		type: 'date',
		$validDate: true
	}
};

var result = inspector.validate(schema, data);
console.log(result.format());
/*
Property @[invalidDate]: must be a valid date
Property @[noDate]: must be date, but is string
*/
