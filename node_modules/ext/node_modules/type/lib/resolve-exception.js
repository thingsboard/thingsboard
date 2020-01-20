"use strict";

var isValue             = require("../value/is")
  , isObject            = require("../object/is")
  , resolveErrorMessage = require("./resolve-error-message");

module.exports = function (value, defaultMessage, inputOptions) {
	if (isObject(inputOptions) && !isValue(value)) {
		if ("default" in inputOptions) return inputOptions["default"];
		if (inputOptions.isOptional) return null;
	}
	throw new TypeError(resolveErrorMessage(defaultMessage, value, inputOptions));
};
