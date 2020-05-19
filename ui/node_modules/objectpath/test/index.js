'use strict';

var assert = require('chai').assert;
var ObjectPath = require('../index.js');

describe('Parse', function(){
	it('parses simple paths in dot notation', function(){
		assert.deepEqual(ObjectPath.parse('a'), ['a'], 'incorrectly parsed single node');
		assert.deepEqual(ObjectPath.parse('a.b.c'), ['a','b','c'], 'incorrectly parsed multi-node');
	});

	it('parses simple paths in bracket notation', function(){
		assert.deepEqual(ObjectPath.parse('["c"]'), ['c'], 'incorrectly parsed single headless node');
		assert.deepEqual(ObjectPath.parse('a["b"]["c"]'), ['a','b','c'], 'incorrectly parsed multi-node');
		assert.deepEqual(ObjectPath.parse('["a"]["b"]["c"]'), ['a','b','c'], 'incorrectly parsed headless multi-node');
	});

	it('parses a numberic nodes in bracket notation', function(){
		assert.deepEqual(ObjectPath.parse('[5]'), ['5'], 'incorrectly parsed single headless numeric node');
		assert.deepEqual(ObjectPath.parse('[5]["a"][3]'), ['5','a','3'], 'incorrectly parsed headless numeric multi-node');
	});

	it('parses a combination of dot and bracket notation', function(){
		assert.deepEqual(ObjectPath.parse('a[1].b.c.d["e"]["f"].g'), ['a','1','b','c','d','e','f','g']);
	});

	it('parses unicode characters', function(){
		assert.deepEqual(ObjectPath.parse('∑´ƒ©∫∆.ø'), ['∑´ƒ©∫∆','ø'], 'incorrectly parsed unicode characters from dot notation');
		assert.deepEqual(ObjectPath.parse('["∑´ƒ©∫∆"]["ø"]'), ['∑´ƒ©∫∆','ø'], 'incorrectly parsed unicode characters from bracket notation');
	});

	it('parses nodes with control characters', function(){
		assert.deepEqual(ObjectPath.parse('["a.b."]'), ['a.b.'], 'incorrectly parsed dots from inside brackets');
		assert.deepEqual(ObjectPath.parse('["\""][\'\\\'\']'), ['"','\''], 'incorrectly parsed escaped quotes');
		assert.deepEqual(ObjectPath.parse('["\'"][\'"\']'), ['\'','"'], 'incorrectly parsed unescaped quotes');
		assert.deepEqual(ObjectPath.parse('["\\""][\'\\\'\']'), ['"','\''], 'incorrectly parsed escaped quotes');
		assert.deepEqual(ObjectPath.parse('[\'\\"\']["\\\'"]'), ['\\"','\\\''], 'incorrectly parsed escape characters');
		assert.deepEqual(ObjectPath.parse('["\\"]"]["\\"]\\"]"]'), ['"]','"]"]'], 'incorrectly parsed escape characters');
		assert.deepEqual(ObjectPath.parse('["[\'a\']"][\'[\\"a\\"]\']'), ['[\'a\']','[\\"a\\"]'], 'incorrectly parsed escape character');
	});
});

describe('Stringify', function(){
	it('stringifys simple paths with single quotes', function(){
		assert.deepEqual(ObjectPath.stringify(['a']), '[\'a\']', 'incorrectly stringified single node');
		assert.deepEqual(ObjectPath.stringify(['a','b','c']), '[\'a\'][\'b\'][\'c\']', 'incorrectly stringified multi-node');
		assert.deepEqual(ObjectPath.stringify(['a'], '\''), '[\'a\']', 'incorrectly stringified single node with excplicit single quote');
		assert.deepEqual(ObjectPath.stringify(['a','b','c'], '\''), '[\'a\'][\'b\'][\'c\']', 'incorrectly stringified multi-node with excplicit single quote');
	});
	it('stringifys simple paths with double quotes', function(){
		assert.deepEqual(ObjectPath.stringify(['a'],'"'), '["a"]', 'incorrectly stringified single node');
		assert.deepEqual(ObjectPath.stringify(['a','b','c'],'"'), '["a"]["b"]["c"]', 'incorrectly stringified multi-node');
	});

	it('stringifys a numberic nodes in bracket notation with single quotes', function(){
		assert.deepEqual(ObjectPath.stringify(['5']), '[\'5\']', 'incorrectly stringified single headless numeric node');
		assert.deepEqual(ObjectPath.stringify(['5','a','3']), '[\'5\'][\'a\'][\'3\']', 'incorrectly stringified headless numeric multi-node');
	});

	it('stringifys a numberic nodes in bracket notation with double quotes', function(){
		assert.deepEqual(ObjectPath.stringify(['5'],'"'), '["5"]', 'incorrectly stringified single headless numeric node');
		assert.deepEqual(ObjectPath.stringify(['5','a','3'],'"'), '["5"]["a"]["3"]', 'incorrectly stringified headless numeric multi-node');
	});

	it('stringifys a combination of dot and bracket notation with single quotes', function(){
		assert.deepEqual(ObjectPath.stringify(['a','1','b','c','d','e','f','g']), '[\'a\'][\'1\'][\'b\'][\'c\'][\'d\'][\'e\'][\'f\'][\'g\']');
	});

	it('stringifys a combination of dot and bracket notation with double quotes', function(){
		assert.deepEqual(ObjectPath.stringify(['a','1','b','c','d','e','f','g'],'"'), '["a"]["1"]["b"]["c"]["d"]["e"]["f"]["g"]');
	});

	it('stringifys unicode characters with single quotes', function(){
		assert.deepEqual(ObjectPath.stringify(['∑´ƒ©∫∆']), '[\'∑´ƒ©∫∆\']', 'incorrectly stringified single node path with unicode');
		assert.deepEqual(ObjectPath.stringify(['∑´ƒ©∫∆','ø']), '[\'∑´ƒ©∫∆\'][\'ø\']', 'incorrectly stringified multi-node path with unicode characters');
	});

	it('stringifys unicode characters with double quotes', function(){
		assert.deepEqual(ObjectPath.stringify(['∑´ƒ©∫∆'],'"'), '["∑´ƒ©∫∆"]', 'incorrectly stringified single node path with unicode');
		assert.deepEqual(ObjectPath.stringify(['∑´ƒ©∫∆','ø'],'"'), '["∑´ƒ©∫∆"]["ø"]', 'incorrectly stringified multi-node path with unicode characters');
	});

	it("stringifys nodes with control characters and single quotes", function(){
		assert.deepEqual(ObjectPath.stringify(["a.b."],"'"), "['a.b.']", "incorrectly stringified dots from inside brackets");
		assert.deepEqual(ObjectPath.stringify(["'","\\\""],"'"), "['\\\'']['\\\"']", "incorrectly stringified escaped quotes");
		assert.deepEqual(ObjectPath.stringify(["\"","'"],"'"), "['\"']['\\'']", "incorrectly stringified unescaped quotes");
		assert.deepEqual(ObjectPath.stringify(["\\'","\\\""],"'"), "['\\\\'']['\\\"']", "incorrectly stringified escape character");
		assert.deepEqual(ObjectPath.stringify(["[\"a\"]","[\\'a\\']"],"'"), "['[\"a\"]']['[\\\\'a\\\\']']", "incorrectly stringified escape character");
	});

	it('stringifys nodes with control characters and double quotes', function(){
		assert.deepEqual(ObjectPath.stringify(['a.b.'],'"'), '["a.b."]', 'incorrectly stringified dots from inside brackets');
		assert.deepEqual(ObjectPath.stringify(['"','\\\''],'"'), '["\\\""]["\\\'"]', 'incorrectly stringified escaped quotes');
		assert.deepEqual(ObjectPath.stringify(['\'','"'],'"'), '["\'"]["\\""]', 'incorrectly stringified unescaped quotes');
		assert.deepEqual(ObjectPath.stringify(['\\"','\\\''],'"'), '["\\\\""]["\\\'"]', 'incorrectly stringified escape character');
		assert.deepEqual(ObjectPath.stringify(['[\'a\']','[\\"a\\"]'],'"'), '["[\'a\']"]["[\\\\"a\\\\"]"]', 'incorrectly stringified escape character');
	});
});

describe('Normalize', function(){
	it('normalizes a string', function(){
		assert.deepEqual(ObjectPath.normalize('a.b["c"]'), '[\'a\'][\'b\'][\'c\']', 'incorrectly normalized a string with single quotes');
		assert.deepEqual(ObjectPath.normalize('a.b["c"]','"'), '["a"]["b"]["c"]', 'incorrectly normalized a string with double quotes');
	});

	it('normalizes an array', function(){
		assert.deepEqual(ObjectPath.normalize(['a','b','c']), '[\'a\'][\'b\'][\'c\']', 'incorrectly normalized an array with single quotes');
		assert.deepEqual(ObjectPath.normalize(['a','b','c'],'"'), '["a"]["b"]["c"]', 'incorrectly normalized an array with double quotes');
	});
});
