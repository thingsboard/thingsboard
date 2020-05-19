var validation = require('./validation_test').validation;
var sanitization = require('./sanitization_test').sanitization;
var generator = require('./generator_test').generator;

// testing issues with shims
Array.prototype.TMP = function(){};

suite('Validation', validation);
suite('Sanitization', sanitization);
suite('Generator', generator);
