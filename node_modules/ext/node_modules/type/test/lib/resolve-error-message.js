"use strict";

var assert              = require("chai").assert
  , resolveErrorMessage = require("../../lib/resolve-error-message");

describe("lib/resolve-error-message", function () {
	it("Should insert value", function () {
		assert.equal(resolveErrorMessage("%v is invalid", 12), "12 is invalid");
	});
	it("Should support custome error message via inputOptions.errorMessage", function () {
		assert.equal(
			resolveErrorMessage("%v is invalid", null, { errorMessage: "%v is not supported age" }),
			"null is not supported age"
		);
	});
});
