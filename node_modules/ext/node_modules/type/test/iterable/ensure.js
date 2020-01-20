"use strict";

var assert         = require("chai").assert
  , ensureString   = require("../../string/ensure")
  , isArray        = require("../../array/is")
  , ensureIterable = require("../../iterable/ensure");

describe("iterable/ensure", function () {
	it("Should return input value", function () {
		var value = [];
		assert.equal(ensureIterable(value), value);
	});
	it("Should allow strings with allowString option", function () {
		var value = "foo";
		assert.equal(ensureIterable(value, { allowString: true }), value);
	});
	it("Should crash on invalid value", function () {
		try {
			ensureIterable("foo");
			throw new Error("Unexpected");
		} catch (error) {
			assert.equal(error.name, "TypeError");
			assert(error.message.includes("is not expected iterable value"));
		}
	});
	describe("Should support 'ensureItem' option", function () {
		it("Should resolve coerced array", function () {
			var coercedValue = ensureIterable(new Set(["foo", 12]), { ensureItem: ensureString });
			assert(isArray(coercedValue));
			assert.deepEqual(coercedValue, ["foo", "12"]);
		});
		it("Should crash if some item is invalid", function () {
			try {
				ensureIterable(["foo", {}], { ensureItem: ensureString });
				throw new Error("Unexpected");
			} catch (error) {
				assert.equal(error.name, "TypeError");
				assert(error.message.includes("is not expected iterable value"));
			}
		});
	});
});
