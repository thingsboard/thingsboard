"use strict";

var assert            = require("chai").assert
  , ensureString      = require("../../string/ensure")
  , ensurePlainObject = require("../../plain-object/ensure");

describe("plain-object/ensure", function () {
	it("Should return input value", function () {
		var value = {};
		assert.equal(ensurePlainObject(value), value);
	});
	it("Should crash on invalid value", function () {
		try {
			ensurePlainObject(null);
			throw new Error("Unexpected");
		} catch (error) {
			assert.equal(error.name, "TypeError");
			assert.equal(error.message, "null is not a valid plain object");
		}
	});
	it("Should support allowedKeys option", function () {
		var value = { foo: "bar", marko: "elo" };
		assert.equal(ensurePlainObject(value, { allowedKeys: ["foo", "marko"] }), value);
		try {
			ensurePlainObject(value, { allowedKeys: ["marko"] });
			throw new Error("Unexpected");
		} catch (error) {
			assert.equal(error.name, "TypeError");
			assert.equal(error.message.indexOf("is not a valid plain object") !== -1, true);
		}
	});

	it("Should support ensurePropertyValue option", function () {
		assert.deepEqual(
			ensurePlainObject({ foo: "bar", marko: 12 }, { ensurePropertyValue: ensureString }),
			{ foo: "bar", marko: "12" }
		);
		try {
			ensurePlainObject({ foo: "bar", marko: {} }, { ensurePropertyValue: ensureString });
			throw new Error("Unexpected");
		} catch (error) {
			assert.equal(error.name, "TypeError");
			assert.equal(error.message.indexOf("is not a valid plain object") !== -1, true);
		}
	});
});
