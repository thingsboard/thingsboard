var should = require('should');
var si = require('../');

const NB_TEST = 25;

function testRandomCandidates(schema) {
	for (var i = 1; i <= NB_TEST; i++) {
		test('Random candidate #' + i, function () {
			var candidate = si.generate(schema);
			var result = si.validate(schema, candidate);
			result.should.be.an.Object;
			try {
				result.should.have.property('valid').with.equal(true);
				result.should.have.property('error').with.be.an.instanceof(Array)
				.and.be.lengthOf(0);
			}
			catch (e) {
				throw new Error(result.format());
			}
		});
	}
}

exports.generator = function () {
	suite('Schema #1 (Basics schemas with nested object, only considering type)', function () {
		var schema = {
			type: 'object',
			properties: {
				lorem: {
					type: 'object',
					properties: {
						ipsum: { type: ['string', 'integer'] },
						dolor: { type: 'number' },
						sit: { type: 'any'},
						amet: {
							type: 'array',
							items: {
								type: 'string'
							}
						},
						consectetur: {
							type: 'array',
							items: [
								{ type: 'integer' },
								{ type: 'number' }
							]
						}
					}
				}
			}
		};

		testRandomCandidates(schema);
	}); // suite "schema #1"

	suite('Schema #2 (Basic schemas, considering *Length)', function () {
		var schema = {
			type: 'object',
			properties: {
				lorem: {
					type: 'object',
					properties: {
						ipsum: { type: ['string', 'integer'] },
						dolor: { type: 'number' },
						sit: { type: 'any'},
						amet: {
							type: 'array',
							exactLength: 6,
							items: {
								type: 'string',
								exactLength: 10
							}
						},
						consectetur: {
							type: 'array',
							minLength: 4,
							maxLength: 8,
							items: {
								type: 'string',
								minLength: 6,
								maxLength: 12
							}
						}
					}
				}
			}
		};

		testRandomCandidates(schema);
	}); // suite "schema #2"

	suite('Schema #3 (Intermediate schemas, considering *Length, lt, lte, gt, gte, eq and ne)', function () {
		var schema = {
			type: 'object',
			properties: {
				lorem: {
					type: 'object',
					properties: {
						ipsum: { type: ['string', 'integer'] },
						dolor: { type: 'number' },
						sit: { type: 'any' },
						amet: {
							type: 'array',
							exactLength: 6,
							items: {
								type: 'string',
								exactLength: 10
							}
						},
						consectetur: {
							type: 'array',
							maxLength: 16,
							items: [
								{	type: 'number', gt: 50 },
								{	type: 'number', lt: 1 },
								{	type: 'number', gte: 50 },
								{	type: 'number', lte: 1 },
								{	type: 'number', gt: 5, lt: 10 },
								{	type: 'number', gte: 5, lte: 10 },
								{	type: 'number', eq: 7 },
								{	type: 'number', gte: 0, lte: 1, ne: 1 },
								{	type: 'integer', gt: 50 },
								{	type: 'integer', lt: 1 },
								{	type: 'integer', gte: 50 },
								{	type: 'integer', lte: 1 },
								{	type: 'integer', gt: 5, lt: 10 },
								{	type: 'integer', gte: 5, lte: 10 },
								{	type: 'integer', eq: 7 },
								{	type: 'integer', gte: 0, lte: 1, ne: 1 }
							]
						}
					}
				}
			}
		};

		testRandomCandidates(schema);
	}); // suite "schema #3"

	suite('Schema #4 (Advanced schemas, considering *Length, lt, lte, gt, gte, eq, array of ne and optional fields)', function () {
		var schema = {
			type: 'object',
			properties: {
				lorem: {
					type: 'object',
					properties: {
						ipsum: { type: ['string', 'integer'] },
						dolor: { type: 'number', optional: true },
						sit: { type: 'any', optional: true },
						amet: {
							type: 'array',
							exactLength: 6,
							optional: true,
							items: {
								type: 'string',
								optional: true,
								exactLength: 10
							}
						},
						consectetur: {
							type: 'array',
							maxLength: 18,
							optional: true,
							items: [
								{	type: 'number', gt: 50 },
								{	type: 'number', lt: 1 },
								{	type: 'number', gte: 50 },
								{	type: 'number', lte: 1 },
								{	type: 'number', gt: 5, lt: 10 },
								{	type: 'number', gte: 5, lte: 10 },
								{	type: 'number', eq: 7 },
								{	type: 'number', gte: 0, lte: 2, ne: [0, 1] },
								{	type: 'integer', gt: 50 },
								{	type: 'integer', lt: 1 },
								{	type: 'integer', gte: 50 },
								{	type: 'integer', lte: 1 },
								{	type: 'integer', gt: 5, lt: 10 },
								{	type: 'integer', gte: 5, lte: 10 },
								{	type: 'integer', eq: 7 },
								{	type: 'integer', gte: 0, lte: 2, ne: [0, 1] },
								{	type: 'integer', gte: 0, lte: 3, ne: [0, 1, 2] },
								{	type: 'integer', gte: 0, lte: 4, ne: [0, 1, 2, 3] }
							]
						}
					}
				}
			}
		};

		testRandomCandidates(schema);
	}); // suite "schema #4"

	suite('Schema #5 (Advanced schemas, considering *Length, eq and format fields)', function () {
		var schema = {
			type: 'object',
			properties: {
				lorem: {
					type: 'array',
					items: [
						{ type: 'string', pattern: 'date-time' },
						{ type: 'string', pattern: 'date-time' },
						{ type: 'string', pattern: 'date' },
						{ type: 'string', pattern: 'time' },
						{ type: 'string', pattern: 'color' },
						{ type: 'string', pattern: 'color', eq: '#FFffFF' },
						{ type: 'string', pattern: 'numeric' },
						{ type: 'string', pattern: 'integer' },
						{ type: 'string', pattern: 'decimal' },
						{ type: 'string', pattern: 'alpha' },
						{ type: 'string', pattern: 'alphaNumeric' },
						{ type: 'string', pattern: 'alphaDash' },
						{ type: 'string', pattern: 'javascript' }
					]
				}
			}
		};

		testRandomCandidates(schema);
	}); // suite "schema #5"

	suite('Schema #6 (Generate several candidate with one call)', function () {
		var schema = {
			type: 'object',
			properties: {
				lorem: { type: 'integer' }
			}
		};

		var candidates = si.generate(schema, NB_TEST);
		candidates.should.be.an.instanceof(Array).with.lengthOf(NB_TEST);
		candidates.forEach(function (candidate, i) {
			test('Random candidate #' + (i + 1), function () {
				var result = si.validate(schema, candidate);
				result.should.be.an.Object;
				try {
					result.should.have.property('valid').with.equal(true);
					result.should.have.property('error').with.be.an.instanceof(Array)
					.and.be.lengthOf(0);
				}
				catch (e) {
					throw new Error(result.format());
				}
			});
		});

	}); // suite "schema #6"

	suite('Schema #7 (Globing for Object keys)', function () {
		var schema = {
			type: 'object',
			properties: {
				globString: {
					type: 'object',
					properties: {
						'*': { type: 'string' }
					}
				},
				globInteger: {
					type: 'object',
					properties: {
						'*': { type: 'integer' }
					}
				}
			}
		};

		for (var i = 1; i <= NB_TEST; i++) {
			test('Random candidate #' + i, function () {
				var candidate = si.generate(schema);
				var result = si.validate(schema, candidate);
				result.should.be.an.Object;
				candidate.globString.should.not.have.property('*');
				candidate.globInteger.should.not.have.property('*');
				try {
					result.should.have.property('valid').with.equal(true);
					result.should.have.property('error').with.be.an.instanceof(Array)
					.and.be.lengthOf(0);
				}
				catch (e) {
					throw new Error(result.format());
				}
			});
		}
	}); // suite "schema #7"

};
