var should = require('should');
var si = require('../');

exports.sanitization = function () {
	suite('schema #1 (type casting [string])', function () {
		var schema = {
			type: 'array',
			items: { type: 'string' }
		};

		test('candidate #1 | boolean -> string', function () {
			var candidate = [true, false, 'true', 'false'];

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(2);
			result.reporting[0].property.should.be.equal('@[0]');
			result.reporting[1].property.should.be.equal('@[1]');
			candidate.should.be.eql(['true', 'false', 'true', 'false']);
		});

		test('candidate #2 | number -> string', function () {
			var candidate = [0, 12, 3.14159, -12, -3.14159, '1234', '-1234'];

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(5);
			result.reporting[0].property.should.be.equal('@[0]');
			result.reporting[1].property.should.be.equal('@[1]');
			result.reporting[2].property.should.be.equal('@[2]');
			result.reporting[3].property.should.be.equal('@[3]');
			result.reporting[4].property.should.be.equal('@[4]');
			candidate.should.eql(['0', '12', '3.14159', '-12', '-3.14159', '1234', '-1234']);
		});

		test('candidate #3 | date -> string', function () {
			var d = new Date();
			var candidate = [d, d.toString()];

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(1);
			result.reporting[0].property.should.be.equal('@[0]');
			candidate.should.eql([d.toString(), d.toString()]);
		});

		test('candidate #4 | object -> string', function () {
			var obj = { test: true };
			var candidate = [obj, JSON.stringify(obj)];

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(1);
			result.reporting[0].property.should.be.equal('@[0]');
			candidate.should.eql([JSON.stringify(obj), JSON.stringify(obj)]);
		});

		test('candidate #5 | array -> string', function () {
			var candidate = [ [ 'one', 'two', true ] ];

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(1);
			result.reporting[0].property.should.be.equal('@[0]');
			candidate.should.eql([ 'one,two,true' ]);
		});

		test('candidate #6 | array -> string with joinWith="|"', function () {
			var candidate = [ [ 'one', 'two', true ] ];

			schema.items.joinWith = '|';
			var result = si.sanitize(schema, candidate);
			delete schema.items.joinWith;
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(1);
			result.reporting[0].property.should.be.equal('@[0]');
			candidate.should.eql([ 'one|two|true' ]);
		});

	}); // suite "schema #1"

	suite('schema #2 (type casting [integer])', function () {
		var schema = {
			type: 'array',
			items: { type: 'integer', def: -1 }
		};

		test('candidate #1 | string -> integer', function () {
			var candidate = ['foo', '4', '3', '2', '1', '1 500', '16,2', ''];

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(8);
			result.reporting[0].property.should.be.equal('@[0]');
			result.reporting[1].property.should.be.equal('@[1]');
			result.reporting[2].property.should.be.equal('@[2]');
			result.reporting[3].property.should.be.equal('@[3]');
			result.reporting[4].property.should.be.equal('@[4]');
			result.reporting[5].property.should.be.equal('@[5]');
			result.reporting[6].property.should.be.equal('@[6]');
			candidate.should.be.eql([-1, 4, 3, 2, 1, 1500, 16, -1]); // default is -1
		});

		test('candidate #2 | number -> integer', function () {
			var candidate = [12.25, -12.25, 12.75, -12.75, 0, 12];

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(4);
			result.reporting[0].property.should.be.equal('@[0]');
			result.reporting[1].property.should.be.equal('@[1]');
			result.reporting[2].property.should.be.equal('@[2]');
			result.reporting[3].property.should.be.equal('@[3]');
			candidate.should.be.eql([12, -12, 12, -12, 0, 12]);
		});

		test('candidate #3 | date -> integer', function () {
			var date = new Date();
			var candidate = [ new Date(300), date, new Date("2014-01-01"), new Date("INVALID")];

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(4);
			result.reporting[0].property.should.be.equal('@[0]');
			result.reporting[1].property.should.be.equal('@[1]');
			result.reporting[2].property.should.be.equal('@[2]');
			result.reporting[3].property.should.be.equal('@[3]');
			candidate.should.be.eql([300, +date, 1388534400000, -1]);
		});

		test('candidate #4 | string -> integer', function () {
			var result = si.sanitize({ type: 'integer' }, '42');
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(1);
			result.reporting[0].property.should.be.equal('@');
			result.data.should.be.eql(42);
		});

		test('candidate #5 | string -> integer or def: null', function () {
			var result = si.sanitize({ type: 'integer', def: null }, 'abc');
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(1);
			result.reporting[0].property.should.be.equal('@');
			should.equal(result.data, null);
		});

		test('candidate #6 | object with properties -> number and def: 0', function () {
			var s = {
				type: 'object',
				optional: false,
				def: {},
				properties: {
					orderProducts: { type: 'number', def: 0 }, // if he gives ''
					orderServices: { type: 'number', def: 0 }, // if he gives ''
				}
			};
			var result = si.sanitize(s, {});
			result.data.should.be.eql({})
		});

		test('candidate #6 | object with properties -> number and def: 0', function () {
			var s = {
				type: 'object',
				optional: false,
				def: {},
				properties: {
					orderProducts: { type: 'number', def: 0 }, // if he gives ''
					orderServices: { type: 'number', def: 0 }, // if he gives ''
				}
			};
			var result = si.sanitize(s, { orderProducts: '', orderServices: '' });
			result.data.should.be.eql({ orderProducts: 0, orderServices: 0 });
		});

	}); // suite "schema #2"

	suite('schema #3 (type casting [number])', function () {
		var schema = {
			type: 'array',
			items: { type: 'number', def: -1 }
		};

		test('candidate #1 | string -> number', function () {
			var candidate = ['foo', '-4', '-3.234', '2', '1.234', '14,45', ''];

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(candidate.length);
			result.reporting[0].property.should.be.equal('@[0]');
			result.reporting[1].property.should.be.equal('@[1]');
			result.reporting[2].property.should.be.equal('@[2]');
			result.reporting[3].property.should.be.equal('@[3]');
			result.reporting[4].property.should.be.equal('@[4]');
			candidate.should.be.eql([-1, -4, -3.234, 2, 1.234, 14.45, -1]); // default is -1
		});

		test('candidate #2 | date -> number (same as integer)', function () {
			var date = new Date();
			var candidate = [ new Date(300), date, new Date("2013-12-01"), new Date("INVALID")];

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(4);
			result.reporting[0].property.should.be.equal('@[0]');
			result.reporting[1].property.should.be.equal('@[1]');
			result.reporting[2].property.should.be.equal('@[2]');
			result.reporting[3].property.should.be.equal('@[3]');
			candidate.should.be.eql([300, +date, +new Date("2013-12-01"), -1]);
		});

	}); // suite "schema #3"

	suite('schema #4 (type casting [boolean])', function () {
		var schema = {
			type: 'array',
			items: { type: 'boolean' }
		};

		test('candidate #1 | number -> boolean', function () {
			var candidate = [0, 12, -12];

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(3);
			result.reporting[0].property.should.be.equal('@[0]');
			result.reporting[1].property.should.be.equal('@[1]');
			result.reporting[2].property.should.be.equal('@[2]');
			candidate.should.eql([false, true, true]);
		});

		test('candidate #2 | string -> boolean', function () {
			var candidate = ['', '12', 'NikitaJS'];

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(3);
			result.reporting[0].property.should.be.equal('@[0]');
			result.reporting[1].property.should.be.equal('@[1]');
			result.reporting[2].property.should.be.equal('@[2]');
			candidate.should.eql([false, true, true]);
		});

		test('candidate #3 | null -> boolean', function () {
			var candidate = [null];

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(1);
			result.reporting[0].property.should.be.equal('@[0]');
			candidate.should.eql([false]);
		});

	}); // suite "schema #4"

	suite('schema #5 (type casting [object])', function () {
		var schema = {
			type: 'object',
			properties: {
				json: { type: 'object' },
				objt: { type: 'object' }
			}
		};

		test('candidate #1 | string -> object', function () {
			var obj = { lorem: { ipsum: 'dolor' } };
			var candidate = {
				json: JSON.stringify(obj),
				objt: obj
			};

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(1);
			result.reporting[0].property.should.be.equal('@.json');
			candidate.json.should.be.an.instanceof(Object);
			candidate.json.should.eql(obj);
			candidate.objt.should.be.an.instanceof(Object);
			candidate.objt.should.eql(obj);
		});

	}); // suite "schema #5"

	suite('schema #6 (deeply nested object sanitization)', function () {
		var schema = {
			type: 'object',
			properties: {
				lorem: {
					type: 'object',
					properties: {
						ipsum: {
							type: 'object',
							properties: {
								dolor: {
									type: 'object',
									properties: {
										sit: {
											type: 'object',
											properties: {
												amet: { type: 'number' }
											}
										}
									}
								}
							}
						}
					}
				}
			}
		};

		test('candidate #1', function () {
			var candidate = {
				lorem: { ipsum: { dolor: { sit: { amet: '1234' } } } }
			};

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(1);
			result.reporting[0].property.should.be.equal('@.lorem.ipsum.dolor.sit.amet');
		});

		test('candidate #2', function () {
			var candidate = {
				lorem: { ipsum: { dolor: { sit: JSON.stringify({ amet: '1234' }) } } }
			};

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(2);
			result.reporting[0].property.should.be.equal('@.lorem.ipsum.dolor.sit');
			result.reporting[1].property.should.be.equal('@.lorem.ipsum.dolor.sit.amet');
		});

		test('candidate #3', function () {
			var candidate = {
				lorem: { ipsum: { dolor: JSON.stringify({ sit: { amet: '1234' } }) } }
			};

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(2);
			result.reporting[0].property.should.be.equal('@.lorem.ipsum.dolor');
			result.reporting[1].property.should.be.equal('@.lorem.ipsum.dolor.sit.amet');
		});

	}); // suite "schema #6"

	suite('schema #7 (array sanitization with an array of schema)', function () {
		var schema = {
			type: 'object',
			properties: {
				lorem: {
					type: 'object',
					properties: {
						ipsum: {
							type: 'array',
							items: [
								{ type: 'integer' },
								{ type: 'string' },
								{ type: 'integer' }
							]
						}
					}
				}
			}
		};

		test('candidate #1', function () {
			var candidate = {
				lorem: {
					ipsum: ['123', '234', '345']
				}
			};

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(2);
			result.reporting[0].property.should.be.equal('@.lorem.ipsum[0]');
			result.reporting[1].property.should.be.equal('@.lorem.ipsum[2]');
			candidate.should.eql({
				lorem: { ipsum: [123, '234', 345] }
			});
		});

		test('candidate #2', function () {
			var result = si.sanitize({ type: 'array', optional: false, def: [], items: { type: 'object' } }, { prop: 'value' });
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(1);
			result.reporting[0].property.should.be.equal('@');
			result.data.should.eql([ { prop: 'value' } ]);
		});

	}); // suite "schema #7"

	suite('schema #8 (array sanitization with an hash of schema)', function () {
		var schema = {
			type: 'object',
			properties: {
				lorem: {
					type: 'object',
					properties: {
						ipsum: {
							type: 'array',
							items: { type: 'integer' }
						}
					}
				}
			}
		};

		test('candidate #1', function () {
			var candidate = {
				lorem: {
					ipsum: ['123', '234', '345']
				}
			};

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(3);
			result.reporting[0].property.should.be.equal('@.lorem.ipsum[0]');
			result.reporting[1].property.should.be.equal('@.lorem.ipsum[1]');
			result.reporting[2].property.should.be.equal('@.lorem.ipsum[2]');
			candidate.should.eql({
				lorem: { ipsum: [123, 234, 345] }
			});
		});

	}); // suite "schema #8"

	suite('schema #9 (Creation of a property if it does not exist)', function () {
		var schema = {
			type: 'object',
			properties: {
				hash: {
					type: 'object',
					properties: {
						one: { type: 'integer', optional: false, def: 1 },
						two: { type: 'integer', optional: false, def: 2 },
						three: { type: 'integer', optional: false, def: 3 },
						four: { type: 'integer', optional: false, def: 4 },
						five: { type: 'integer', optional: "false", def: 5 }
					}
				}
			}
		};

		test('candidate #1', function () {
			var candidate = {
				hash: {
					one: 11,
					two: 22,
					three: 33
				}
			};

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(2);
			result.reporting[0].property.should.be.equal('@.hash.four');
			result.reporting[1].property.should.be.equal('@.hash.five');
			candidate.should.eql({
				hash: {
					one: 11,
					two: 22,
					three: 33,
					four: 4,
					five: 5
				}
			});
		});

		test('candidate #2', function () {
			var candidate = {
				hash: {
					two: 22,
				}
			};

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(4);
			result.reporting[0].property.should.be.equal('@.hash.one');
			result.reporting[1].property.should.be.equal('@.hash.three');
			result.reporting[2].property.should.be.equal('@.hash.four');
			result.reporting[3].property.should.be.equal('@.hash.five');
			candidate.should.eql({
				hash: {
					one: 1,
					two: 22,
					three: 3,
					four: 4,
					five: 5
				}
			});
		});

		test('candidate #3', function () {
			var candidate = {
				hash: {
					four: 44
				}
			};

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(4);
			result.reporting[0].property.should.be.equal('@.hash.one');
			result.reporting[1].property.should.be.equal('@.hash.two');
			result.reporting[2].property.should.be.equal('@.hash.three');
			result.reporting[3].property.should.be.equal('@.hash.five');
			candidate.should.eql({
				hash: {
					one: 1,
					two: 2,
					three: 3,
					four: 44,
					five: 5
				}
			});
		});

	}); // suite "schema #9"

	suite('schema #10 (Creation of a property [nested object] if it does not exist)', function () {
		var schema = {
			type: 'object',
			properties: {
				one: {
					optional: false,
					def: {},
					type: 'object',
					properties: {
						two: {
							optional: false,
							def: {},
							type: 'object',
							properties: {
								three: {
									optional: false,
									def: {},
									type: 'object',
									properties: {
										four: {
											optional: false,
											def: 'value',
											type: 'string'
										}
									}
								}
							}
						}
					}
				}
			}
		};

		test('candidate #1', function () {
			var candidate = {
				one: {}
			};

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(3);
			result.reporting[0].property.should.be.equal('@.one.two');
			result.reporting[1].property.should.be.equal('@.one.two.three');
			result.reporting[2].property.should.be.equal('@.one.two.three.four');
			candidate.should.eql({
				one: {
					two: {
						three: {
							four: 'value'
						}
					}
				}
			});
		});

	}); // suite "schema #10"

	suite('schema #10.1 (test of optional: true)', function () {
		var schema = {
			type: 'object',
			properties: {
				lorem: {
					optional: true,
					def: {},
					type: 'object',
					properties: {
						ipsum: { type: 'string', def: 'Nikita', optional: true },
						ipsum2: { type: 'string', def: 'Atinux', optional: 'true' },
					}
				}
			}
		};

		test('candidate #1', function () {
			var candidate = {
				lorem: {
				}
			};

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(2);
			result.reporting[0].property.should.be.equal('@.lorem.ipsum');
			result.reporting[1].property.should.be.equal('@.lorem.ipsum2');
			candidate.should.eql({
				lorem: {
					ipsum: 'Nikita',
					ipsum2: 'Atinux'
				}
			});
		});

		test('candidate #2', function () {
			var candidate = {
			};

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(3);
			result.reporting[0].property.should.be.equal('@.lorem');
			result.reporting[1].property.should.be.equal('@.lorem.ipsum');
			result.reporting[2].property.should.be.equal('@.lorem.ipsum2');
			candidate.should.eql({
				lorem: {
					ipsum: 'Nikita',
					ipsum2: 'Atinux'
				}
			});
		});

	}); // suite "schema 10.1"

	suite('schema #10.2 (test of optional: true, without field type)', function () {
		var schema = {
			type: 'object',
			properties: {
				lorem: {
					optional: true,
					def: {},
					properties: {
						ipsum: { def: 'Nikita', optional: true },
						ipsum2: { def: 'Nikita', optional: 'true' }
					}
				}
			}
		};

		test('candidate #1', function () {
			var candidate = {
				lorem: {
				}
			};

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(0);
			candidate.should.eql({
				lorem: {
				}
			});
		});

		test('candidate #2', function () {
			var candidate = {
			};

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(0);
			candidate.should.eql({
			});
		});
	}); // suite "schema 10.2"

	suite('schema #11 (hash sanitization with an hash of schema)', function () {
			var schema = {
				type: 'object',
				properties: {
					specifications: {
						type: 'object',
						items: {
							type: 'string',
							items: {
								type: 'string'
							}
						}
					}
				}
			};

			test('candidate #1', function () {
				var candidate = {
					specifications: {
						couleur: ['rouge', 15],
						taille: 180
					}
				};

				var result = si.sanitize(schema, candidate);
				result.should.be.an.Object;
				result.should.have.property('reporting').with.be.an.instanceof(Array)
				.and.be.lengthOf(2);
				result.reporting[0].property.should.be.equal('@.specifications[couleur][1]');
				result.reporting[1].property.should.be.equal('@.specifications[taille]');
				candidate.should.eql({
					specifications: {
						couleur: ['rouge', '15'],
						taille: '180'
					}
				});
			});

		}); // suite "schema #11"

	suite('schema #12 (field "alias" testing)', function () {
			var schema = {
				type: 'object',
				properties: {
					id: {
						alias: 'MyID (alias)',
						type: 'integer'
					}
				}
			};

			test('candidate #1', function () {
				var candidate = {
					id: '1234'
				};

				var result = si.sanitize(schema, candidate);
				result.should.be.an.Object;
				result.should.have.property('reporting').with.be.an.instanceof(Array)
				.and.be.lengthOf(1);
				result.reporting[0].property.should.be.equal(schema.properties.id.alias + ' (@.id)');
				candidate.should.eql({ id: 1234 });
			});

	}); // suite "schema #12"

	suite('schema #13 (field "rules" testing)', function () {
		var schema = {
			type: 'object',
			properties: {
				stringU: { type: 'string', rules: 'upper' },
				stringL: { type: 'string', rules: 'lower' },
				stringC: { type: 'string', rules: 'capitalize' },
				stringUC: { type: 'string', rules: 'ucfirst' }
			}
		};
		const STRING = 'cOucou a TouT lE moNDe';

		test('candidat #1', function () {
			var candidate = {
				stringU: STRING,
				stringL: STRING,
				stringC: STRING,
				stringUC: STRING,
			};

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(4);
			result.reporting[0].property.should.be.equal('@.stringU');
			result.reporting[1].property.should.be.equal('@.stringL');
			result.reporting[2].property.should.be.equal('@.stringC');
			result.reporting[3].property.should.be.equal('@.stringUC');
			candidate.stringU.should.equal(STRING.toUpperCase());
			candidate.stringL.should.equal(STRING.toLowerCase());
			candidate.stringC.should.equal(STRING.charAt(0).toUpperCase() + STRING.substr(1).toLowerCase());
			candidate.stringUC.should.equal(STRING.charAt(0).toUpperCase() + STRING.substr(1));
		});
	}); // suite "schema #13"

	suite('schema #14 (field "rules" with an array of string)', function () {
    var schema = {
      type: 'object',
      properties: {
        string: { type: 'string', rules: ['lower', 'upper'] },
        toTrim: { type: 'string', rules: 'trim' },
        complex: { type: 'string', rules: 'trim', minLength: 10 }
      }
    };

		test('candidat #1', function () {
			const STRING = 'cOucou a TouT lE moNDe';
			var candidate = {
				string: STRING
			};

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(1);
			result.reporting[0].property.should.be.equal('@.string');
			candidate.string.should.equal(STRING.toUpperCase());
		});

		test('candidat #2', function () {
			var STRING = '    Hi! I shall be trimed!    ';
			var candidate = {
				toTrim: STRING
			};

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(1);
			result.reporting[0].property.should.be.equal('@.toTrim');
			candidate.toTrim.should.equal(STRING.trim());
		});

		// rules have a higher proprity than minLength/maxLength
		//
		test('candidat #3', function () {
			var STRING = '   coucou  ';
			var candidate = {
				complex: STRING
			};

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(1);
			result.reporting[0].property.should.be.equal('@.complex');
			candidate.complex.should.equal(STRING.trim() + '----');
		});
  }); // suite "schema #14"

	suite('schema #15 (field "exec")', function () {
		var schema = {
			type: 'array',
			items: {
				type: 'string',
				exec: function (schema, post) {
					if ((/^nikita$/i).test(post)) {
						this.report();
						return 'God';
					}
					return post;
				}
			}
		};

		test('candidat #1', function () {
			var candidate = 'Hello Nikita is coding! nikita'.split(' ');

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(2);
			result.reporting[0].property.should.be.equal('@[1]');
			result.reporting[1].property.should.be.equal('@[4]');
			candidate[1].should.equal('God');
			candidate[4].should.equal('God');
		});
	}); // suite "schema #15"

	suite('schema #16 (Asynchronous call)', function () {
		var schema = {
			type: 'object',
			properties: {
				lorem: {
					type: 'object',
					properties: {
						ipsum: {
							type: 'object',
							properties: {
								dolor: {
									type: 'object',
									properties: {
										sit: {
											type: 'object',
											properties: {
												amet: { type: 'number' }
											}
										}
									}
								}
							}
						}
					}
				}
			}
		};

		test('candidate #1', function (done) {
			var candidate = {
				lorem: { ipsum: { dolor: { sit: { amet: '1234' } } } }
			};

			si.sanitize(schema, candidate, function (err, result) {
				should.not.exist(err);
				result.should.be.an.Object;
				result.should.have.property('reporting').with.be.an.instanceof(Array)
				.and.be.lengthOf(1);
				result.reporting[0].property.should.be.equal('@.lorem.ipsum.dolor.sit.amet');
				candidate.lorem.ipsum.dolor.sit.amet.should.equal(1234);
				done();
			});
		});

		test('candidate #2', function (done) {
			var candidate = {
				lorem: { ipsum: { dolor: { sit: JSON.stringify({ amet: '1234' }) } } }
			};

			si.sanitize(schema, candidate, function (err, result) {
				should.not.exist(err);
				result.should.be.an.Object;
				result.should.have.property('reporting').with.be.an.instanceof(Array)
				.and.be.lengthOf(2);
				result.reporting[0].property.should.be.equal('@.lorem.ipsum.dolor.sit');
				result.reporting[1].property.should.be.equal('@.lorem.ipsum.dolor.sit.amet');
				done();
			});
		});

		test('candidate #3', function (done) {
			var candidate = {
				lorem: { ipsum: { dolor: JSON.stringify({ sit: { amet: '1234' } }) } }
			};

			si.sanitize(schema, candidate, function (err, result) {
				should.not.exist(err);
				result.should.be.an.Object;
				result.should.have.property('reporting').with.be.an.instanceof(Array)
				.and.be.lengthOf(2);
				result.reporting[0].property.should.be.equal('@.lorem.ipsum.dolor');
				result.reporting[1].property.should.be.equal('@.lorem.ipsum.dolor.sit.amet');
				done();
			});
		});

		test('candidate #4', function (done) {
			var customSchema = {
				type: 'array',
				items: {
					type: 'object',
					properties: {
						prop: {
							exec: function (schema, post, cb) {
								cb(null, 'coucou');
							}
						}
					}
				}
			};
			si.sanitize(customSchema, { prop: 'value' }, function (err, result) {
				should.not.exist(err);
				result.should.be.an.Object;
				result.should.have.property('reporting').with.be.an.instanceof(Array)
				.and.be.lengthOf(1);
				result.reporting[0].property.should.be.equal('@');
				result.data.should.be.eql([ { prop: 'coucou' } ]);
				done();
			});
		});
	}); // suite "schema #16"

	suite('schema #16.1 (Asynchronous call + globing)', function () {
		var schema = {
			type: 'object',
			properties: {
				lorem: {
					type: 'object',
					properties: {
						'*': { type: [ 'number', 'string' ], min: 10, minLength: 4 },
						consectetur: { type: 'string', optional: true, maxLength: 10 }
					}
				}
			}
		};

		test('candidate #1', function (done) {
			var candidate = {
				lorem: {
					ipsum: 12,
					dolor: 34,
					sit: 'amet'
				}
			};

			si.sanitize(schema, candidate, function (err, result) {
				should.not.exist(err);
				result.should.be.an.Object;
				result.should.have.property('reporting').with.be.an.instanceof(Array)
				.and.be.lengthOf(0);
				candidate.should.eql({
					lorem: {
						ipsum: 12,
						dolor: 34,
						sit: 'amet'
					}
				});
				done();
			});
		});

		test('candidate #2', function (done) {
			var candidate = {
				lorem: {
					ipsum: 5,
					dolor: 34,
					sit: 'am',
					consectetur: 'adipiscing elit'
				}
			};

			si.sanitize(schema, candidate, function (err, result) {
				should.not.exist(err);
				result.should.be.an.Object;
				result.should.have.property('reporting').with.be.an.instanceof(Array)
				.and.be.lengthOf(3);
	 			result.reporting[0].property.should.be.equal('@.lorem.ipsum');
				result.reporting[1].property.should.be.equal('@.lorem.sit');
				result.reporting[2].property.should.be.equal('@.lorem.consectetur');
				candidate.should.eql({
					lorem: {
						ipsum: 10,
						dolor: 34,
						sit: 'am--',
						consectetur: 'adipiscing'
					}
				});
				done();
			});
		});
	}); // suite "schema #16.1"

	suite('schema #16.2 (field "exec")', function () {
		var schema = {
			type: 'array',
			items: {
				type: 'string',
				exec: function (schema, post, callback) {
					var self = this;
					process.nextTick(function () {
						if ((/^nikita$/i).test(post)) {
							self.report();
							return callback(null, 'God');
						}
						callback(null, post);
					});
				}
			}
		};

		test('candidat #1', function (done) {
			var candidate = 'Hello Nikita is coding! nikita'.split(/\s+/);

			si.sanitize(schema, candidate, function (err, result) {
				should.not.exist(err);
				result.should.be.an.Object;
				result.should.have.property('reporting').with.be.an.instanceof(Array)
				.and.be.lengthOf(2);
				result.reporting[0].property.should.be.equal('@[1]');
				result.reporting[1].property.should.be.equal('@[4]');
				candidate.should.eql(['Hello', 'God', 'is', 'coding!', 'God'])
				done();
			});
		});

		test('candidat #2', function (done) {
			var candidate = 'niKita   is   nikita  and  is   cool'.split(/\s+/);

			si.sanitize(schema, candidate, function (err, result) {
				should.not.exist(err);
				result.should.be.an.Object;
				result.should.have.property('reporting').with.be.an.instanceof(Array)
				.and.be.lengthOf(2);
				result.reporting[0].property.should.be.equal('@[0]');
				result.reporting[1].property.should.be.equal('@[2]');
				candidate.should.eql([ 'God', 'is', 'God', 'and', 'is', 'cool' ]);
				done();
			});
		});
	}); // suite "schema #16.2"

	suite('schema #16.3 (field "exec" and context)', function () {
		var schema = {
			type: 'object',
			properties: {
				lorem: {
					type: 'object',
					properties: {
						ipsum: { type: 'string' }
					}
				},
				sit: {
					exec: function (schema, candidate) {
						this.report();
						return this.origin.lorem;
					}
				}
			}
		};

		test('candidate #1', function () {
			var candidate = {
				lorem: {
					ipsum: 'dolor'
				},
				sit: 'amet'
			};

			si.sanitize(schema, candidate, function (err, result) {
				should.not.exist(err);
				result.should.be.an.Object;
				result.should.have.property('reporting').with.be.an.instanceof(Array)
				.and.be.lengthOf(1);
				candidate.should.eql({
					lorem: {
						ipsum: 'dolor'
					},
					sit: {
						ipsum: 'dolor'
					}
				});
			});
		});
	});

	suite('schema #16.4 (Asynchronous call with custom field)', function () {
		var schema = {
			type: 'object',
			properties: {
				lorem: {
					type: 'number',
					$superiorMod: 5
				}
			}
		};

		var custom = {
			superiorMod: function (schema, post, callback) {
				var spm = schema.$superiorMod;
				if (typeof spm !== 'number' || typeof post !== 'number') {
					callback();
				}
				var self = this;
				process.nextTick(function () {
					var mod = post % spm;
					if (mod !== 0) {
						self.report();
						return callback(null, post + spm - mod);
					}
					callback(null, post);
				});
			}
		};

		test('candidat #1', function (done) {
			var candidate = {
				lorem: 5
			};

			si.sanitize(schema, candidate, custom, function (err, result) {
				should.not.exist(err);
				result.should.be.an.Object;
				result.should.have.property('reporting').with.be.an.instanceof(Array)
				.and.be.lengthOf(0);
				done();
			});
		});

		test('candidat #2', function (done) {
			var candidate = {
				lorem: 7
			};

			si.sanitize(schema, candidate, custom, function (err, result) {
				should.not.exist(err);
				result.should.be.an.Object;
				result.should.have.property('reporting').with.be.an.instanceof(Array)
				.and.be.lengthOf(1);
				result.reporting[0].property.should.be.equal('@.lorem');
				candidate.lorem.should.equal(10);
				done();
			});
		});
	}); // suite "schema #16.4"

	suite('Schema #16.5 (Asynchronous call with exec "field" with synchrous function', function () {
		var schema = {
			type: 'array',
			items: {
				type: 'string',
				exec: function (schema, post) {
					if (typeof post === 'string' && !/^nikita$/i.test(post)) {
						this.report();
						return 'INVALID';
					}
					return post;
				}
			}
		};

		test('candidate #1', function (done) {
			var candidate = [ 'Nikita', 'nikita', 'NIKITA' ];

			si.sanitize(schema, candidate, function (err, result) {
				should.not.exist(err);
				result.should.be.an.Object;
				result.should.have.property('reporting').with.be.an.instanceof(Array)
				.and.be.lengthOf(0);
				candidate.should.eql([ 'Nikita', 'nikita', 'NIKITA' ]);
				done();
			});
		});

		test('candidate #2', function (done) {
			var candidate = [ 'Nikita', 'lol', 'NIKITA', 'thisIsGonnaBeSanitized!' ];

			si.sanitize(schema, candidate, function (err, result) {
				should.not.exist(err);
				result.should.be.an.Object;
				result.should.have.property('reporting').with.be.an.instanceof(Array)
				.and.be.lengthOf(2);
				result.reporting[0].property.should.be.equal('@[1]');
				result.reporting[1].property.should.be.equal('@[3]');
				candidate.should.eql([ 'Nikita', 'INVALID', 'NIKITA', 'INVALID' ])
				done();
			});
		});
	}); // suite "schema #16.5"

	suite('schema #16.6 (Default custom field)', function () {
		var schema = {
			type: 'object',
			properties: {
				lorem: {
					type: 'number',
					$superiorMod: 5
				}
			}
		};

		var custom = {
			superiorMod: function (schema, post) {
				var spm = schema.$superiorMod;
				if (typeof spm !== 'number' || typeof post !== 'number') {
					return post;
				}
				var mod = post % spm;
				if (mod !== 0) {
					this.report();
					return (post + spm - mod);
				}
				return post;
			}
		};

		si.Sanitization.extend(custom);

		test('candidat #1', function (done) {
			var candidate = {
				lorem: 5
			};

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(0);
			done();
		});

		test('candidat #2', function (done) {
			var candidate = {
				lorem: 7
			};

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(1);
			result.reporting[0].property.should.be.equal('@.lorem');
			candidate.lorem.should.equal(10);
			done();
		});

		test('Reseting default schema', function () {
			si.Sanitization.reset();
			si.Sanitization.custom.should.eql({});
		});
	}); // suite "schema #16.6"
	suite('schema #17 (type casting [array])', function () {
		var schema = {
			type: 'object',
			properties: {
				tab: {
					type: 'array',
					optional: false,
					def: [],
					items: { type: 'string' }
				}
			}
		};

		test('candidate #1 | number -> [ string ]', function () {
			var candidate = { tab: 15 };

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(2);
			result.reporting[0].property.should.be.equal('@.tab');
			result.reporting[1].property.should.be.equal('@.tab[0]');
			candidate.should.be.eql({ tab: ['15'] });
		});

		test('candidate #2 | null -> [ null ]', function () {
			var candidate = { tab: null };

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(1);
			result.reporting[0].property.should.be.equal('@.tab');
			candidate.should.be.eql({ tab: [null] });
		});

		test('candidate #3 | undefined -> []', function () {
			var candidate = { tab: undefined };

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(1);
			result.reporting[0].property.should.be.equal('@.tab');
			candidate.should.eql({ tab: [] });
		});

		test('candidate #4 | [ number, boolean ] -> [ string, string ]', function () {
			var candidate = { tab: [ 15, true ] };

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(2);
			result.reporting[0].property.should.be.equal('@.tab[0]');
			result.reporting[1].property.should.be.equal('@.tab[1]');
			candidate.should.eql({ tab: [ '15', 'true' ] });
		});

		test('candidate #5 | "one,two,three" -> [ "one", "two", "three" ]', function () {
			var candidate = { tab: 'one,two,three' };

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(1);
			result.reporting[0].property.should.be.equal('@.tab');
			candidate.should.eql({ tab: [ 'one', 'two', 'three' ] });
		});

		test('candidate #6 | "one;two;three" -> [ "one", "two", "three" ]', function () {
			var candidate = { tab: 'one;two;three' };

			schema.properties.tab.splitWith = ';';
			var result = si.sanitize(schema, candidate);
			delete schema.properties.tab.splitWith;
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(1);
			result.reporting[0].property.should.be.equal('@.tab');
			candidate.should.eql({ tab: [ 'one', 'two', 'three' ] });
		});

		test('candidate #7 | "[JSON String]" -> [ 1, "two", { three: true } ]', function () {
			var candidate = { tab: JSON.stringify([1, 'two', { three: true }]) };

			schema.properties.tab.items.type = 'any';
			var result = si.sanitize(schema, candidate);
			schema.properties.tab.items.type = 'string';
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(1);
			result.reporting[0].property.should.be.equal('@.tab');
			candidate.should.eql({ tab: [ 1, 'two', { three: true } ] });
		});

	});
	// suite "schema #18"
	suite('schema #18 (strict option)', function () {
		var schema = {
			type: 'object',
			strict: true,
			properties: {
				good: { type: 'string' }
			}
		};

		test('candidate #1 | remove useless keys', function () {
			var candidate = {
				good: 'key',
				bad: 'key'
			};

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			candidate.should.be.eql({ good: 'key' });
		});

		test('candidate #2 | remove nothing because candidate is not an object', function () {
			var candidate = 'coucou';

			var result = si.sanitize(schema, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(0);
			candidate.should.be.eql('coucou');
		});

		test('candidate #3 | remove nothing because candidate is not an object', function () {
			var schema1 = {
				type: 'object',
				strict: true
			};
			var candidate = {
				good: 'key',
				bad: 'key'
			};

			var result = si.sanitize(schema1, candidate);
			result.should.be.an.Object;
			result.should.have.property('reporting').with.be.an.instanceof(Array)
			.and.be.lengthOf(0);
			candidate.should.be.eql(candidate);
		});

	});
};
