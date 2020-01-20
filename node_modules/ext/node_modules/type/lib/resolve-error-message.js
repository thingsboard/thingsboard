"use strict";

var isObject      = require("../object/is")
  , stringCoerce  = require("../string/coerce")
  , toShortString = require("./to-short-string");

module.exports = function (errorMessage, value, inputOptions) {
	var customErrorMessage;
	if (isObject(inputOptions) && inputOptions.errorMessage) {
		customErrorMessage = stringCoerce(inputOptions.errorMessage);
	}
	return (customErrorMessage || errorMessage).replace("%v", toShortString(value));
};
