'use strict';

/**
 * Created by steve on 11/09/15.
 */
jest.dontMock('../utils');
jest.dontMock('lodash');

describe('utils', function () {

    var utils;
    var _;
    beforeEach(function () {
        utils = require('../utils');
        _ = require('lodash');
    });

    it('gets defaults from schema and form', function () {
        var schema = {
            'type': 'object',
            'properties': {
                'name': {
                    'title': 'Name',
                    'description': 'Gimme yea name lad',
                    'type': 'string'
                },
                'gender': {
                    'title': 'Choose',
                    'type': 'string',
                    'enum': ['undefined', 'null', 'NaN']
                },
                'overEighteen': {
                    'title': 'Are you over 18 years old?',
                    'type': 'boolean',
                    'default': false
                },
                'attributes': {
                    'type': 'object',
                    'required': ['eyecolor'],
                    'properties': {
                        'eyecolor': { 'type': 'string', 'title': 'Eye color' },
                        'haircolor': { 'type': 'string', 'title': 'Hair color' },
                        'shoulders': {
                            'type': 'object',
                            'title': 'Shoulders',
                            'properties': {
                                'left': { 'type': 'string' },
                                'right': { 'type': 'string' }
                            }
                        }
                    }
                }
            }
        };

        var form = [{
            'title': 'Name',
            'description': 'Gimme yea name lad',
            'schema': {
                'title': 'Name',
                'description': 'Gimme yea name lad',
                'type': 'string'
            },
            'ngModelOptions': {},
            'key': ['name'],
            'type': 'text'
        }, {
            'title': 'Choose',
            'schema': {
                'title': 'Choose',
                'type': 'string',
                'enum': ['undefined', 'null', 'NaN']
            },
            'ngModelOptions': {},
            'key': ['gender'],
            'type': 'select',
            'titleMap': [{
                'name': 'undefined',
                'value': 'undefined'
            }, {
                'name': 'null',
                'value': 'null'
            }, {
                'name': 'NaN',
                'value': 'NaN'
            }]
        }, {
            'title': 'Are you over 18 years old?',
            'schema': {
                'title': 'Are you over 18 years old?',
                'type': 'boolean',
                'default': false
            },
            'ngModelOptions': {},
            'key': ['overEighteen'],
            'type': 'checkbox'
        }, {
            'title': 'attributes',
            'schema': {
                'type': 'object',
                'required': ['eyecolor'],
                'properties': {
                    'eyecolor': {
                        'type': 'string',
                        'title': 'Eye color'
                    },
                    'haircolor': {
                        'type': 'string',
                        'title': 'Hair color'
                    },
                    'shoulders': {
                        'type': 'object',
                        'title': 'Shoulders',
                        'properties': {
                            'left': {
                                'type': 'string'
                            },
                            'right': {
                                'type': 'string'
                            }
                        }
                    }
                }
            },
            'ngModelOptions': {},
            'type': 'fieldset',
            'items': [{
                'title': 'Eye color',
                'required': true,
                'schema': {
                    'type': 'string',
                    'title': 'Eye color'
                },
                'ngModelOptions': {},
                'key': ['attributes', 'eyecolor'],
                'type': 'text'
            }, {
                'title': 'Hair color',
                'schema': {
                    'type': 'string',
                    'title': 'Hair color'
                },
                'ngModelOptions': {},
                'key': ['attributes', 'haircolor'],
                'type': 'text'
            }, {
                'title': 'Shoulders',
                'schema': {
                    'type': 'object',
                    'title': 'Shoulders',
                    'properties': {
                        'left': {
                            'type': 'string'
                        },
                        'right': {
                            'type': 'string'
                        }
                    }
                },
                'ngModelOptions': {},
                'type': 'fieldset',
                'items': [{
                    'title': 'left',
                    'schema': {
                        'type': 'string'
                    },
                    'ngModelOptions': {},
                    'key': ['attributes', 'shoulders', 'left'],
                    'type': 'text'
                }, {
                    'title': 'right',
                    'schema': {
                        'type': 'string'
                    },
                    'ngModelOptions': {},
                    'key': ['attributes', 'shoulders', 'right'],
                    'type': 'text'
                }]
            }]
        }];
        var f = utils.getDefaults(schema);
        //console.log('f = ', f);
        expect(f.form).toEqual(form);
    });

    it('should handle global defaults', function () {
        var schema = {
            'type': 'object',
            'properties': {
                'name': {
                    'title': 'Name',
                    'description': 'Gimme yea name lad',
                    'type': 'string'
                }
            }
        };

        var form = [{
            'title': 'Name',
            'description': 'Gimme yea name lad',
            'schema': {
                'title': 'Name',
                'description': 'Gimme yea name lad',
                'type': 'string'
            },
            'ngModelOptions': { 'updateOn': 'blur' },
            'foo': 'bar',
            'key': ['name'],
            'type': 'text'
        }];

        var f = utils.getDefaults(schema, {}, { formDefaults: { foo: 'bar', ngModelOptions: { updateOn: 'blur' } } });
        expect(f.form).toEqual(form);
    });

    it('should handle x-schema-form defaults', function () {
        var schema = {
            'type': 'object',
            'properties': {
                'name': {
                    'title': 'Name',
                    'description': 'Gimme yea name lad',
                    'type': 'string',
                    'x-schema-form': {
                        'type': 'textarea'
                    }
                }
            }
        };
        var f = utils.getDefaults(schema, {});
        expect(f.form[0].type).toEqual('textarea');
    });

    it('should ignore parts of schema in ignore list', function () {
        var schema = {
            'type': 'object',
            'properties': {
                'name': {
                    'title': 'Name',
                    'description': 'Gimme yea name lad',
                    'type': 'string'
                },
                'gender': {
                    'title': 'Choose',
                    'type': 'string',
                    'enum': ['undefined', 'null', 'NaN']
                }
            }
        };

        //no form is implicitly ['*']
        var defaults = utils.getDefaults(schema).form;
        expect(utils.merge(schema, ['*'], { gender: true })).toEqual([defaults[0]]);
    });

    it('merges schema with different forms', function () {
        var schema = {
            'type': 'object',
            'properties': {
                'name': {
                    'title': 'Name',
                    'description': 'Gimme yea name lad',
                    'type': 'string'
                },
                'gender': {
                    'title': 'Choose',
                    'type': 'string',
                    'enum': ['undefined', 'null', 'NaN']
                }
            }
        };

        //no form is implicitly ['*']
        var defaults = utils.getDefaults(schema).form;
        expect(utils.merge(schema)).toEqual(defaults);
        expect(utils.merge(schema, ['*'])).toEqual(defaults);
        expect(utils.merge(schema, ['*', { type: 'fieldset' }])).toEqual(defaults.concat([{ type: 'fieldset' }]));

        //simple form
        expect(utils.merge(schema, ['gender'])).toEqual([defaults[1]]);
        expect(utils.merge(schema, ['gender', 'name'])).toEqual([defaults[1], defaults[0]]);

        //change it up
        var f = _.cloneDeep(defaults[0]);
        f.title = 'Foobar';
        f.type = 'password';
        expect(utils.merge(schema, [{ key: 'name', title: 'Foobar', type: 'password' }])).toEqual([f]);
    });

    it('should translate readOnly in schema to readonly on the merged form defintion', function () {
        var schema = {
            'type': 'object',
            'properties': {
                'name': {
                    'title': 'Name',
                    'description': 'Gimme yea name lad',
                    'type': 'string'
                },
                'gender': {
                    'readOnly': true,
                    'title': 'Choose',
                    'type': 'string',
                    'enum': ['undefined', 'null', 'NaN']
                }
            }
        };

        var merged = utils.merge(schema, ['gender']);
        expect(merged[0].readonly).toEqual(true);
    });

    it('should push readOnly in schema down into objects and arrays', function () {
        var schema = {
            'type': 'object',
            'readOnly': true,
            'properties': {
                'sub': {
                    'type': 'object',
                    'properties': {
                        'array': {
                            'type': 'array',
                            'items': {
                                'type': 'object',
                                'properties': {
                                    'foo': {
                                        'type': 'string'
                                    }
                                }
                            }
                        }
                    }
                }
            }
        };

        var merged = utils.merge(schema, ['*']);

        //sub
        expect(merged[0].readonly).toEqual(true);

        //array
        expect(merged[0].items[0].readonly).toEqual(true);

        //array items
        expect(merged[0].items[0].items[0].readonly).toEqual(true);
    });

    it('should push readonly in form def down into objects and arrays', function () {
        var schema = {
            'type': 'object',
            'properties': {
                'sub': {
                    'type': 'object',
                    'properties': {
                        'array': {
                            'type': 'array',
                            'items': {
                                'type': 'object',
                                'properties': {
                                    'foo': {
                                        'type': 'string'
                                    }
                                }
                            }
                        }
                    }
                }
            }
        };

        var merged = utils.merge(schema, [{ key: 'sub', readonly: true }]);

        //sub
        expect(merged[0].readonly).toEqual(true);

        //array
        expect(merged[0].items[0].readonly).toEqual(true);

        //array items
        expect(merged[0].items[0].items[0].readonly).toEqual(true);
    });

    it('should select and set into objects and arrays', function () {
        var schema = {
            'key': ['comments'],
            'add': 'New',
            'style': {
                'add': 'btn-success'
            },
            'items': [{
                'key': ['comments', '', 'name'],
                'title': 'Name',
                'required': true,
                'schema': {
                    'title': 'Name',
                    'type': 'string'
                },
                'ngModelOptions': {},
                'type': 'text'
            }, {
                'key': ['comments', '', 'email'],
                'title': 'Email',
                'description': 'Email will be used for evil.',
                'schema': {
                    'title': 'Email',
                    'type': 'string',
                    'pattern': '^\\S+@\\S+$',
                    'description': 'Email will be used for evil.'
                },
                'ngModelOptions': {},
                'type': 'text'
            }, {
                'key': ['comments', '', 'spam'],
                'type': 'checkbox',
                'title': 'Yes I want spam.',
                'condition': 'model.comments[arrayIndex].email',
                'schema': {
                    'title': 'Spam',
                    'type': 'boolean',
                    'default': true
                },
                'ngModelOptions': {}
            }, {
                'key': ['comments', '', 'comment'],
                'type': 'textarea',
                'title': 'Comment',
                'required': true,
                'maxlength': 20,
                'validationMessage': 'Don\'t be greedy!',
                'schema': {
                    'title': 'Comment',
                    'type': 'string',
                    'maxLength': 20,
                    'validationMessage': 'Don\'t be greedy!'
                },
                'ngModelOptions': {}
            }],
            'title': 'comments',
            'required': true,
            'schema': {
                'type': 'array',
                'maxItems': 2,
                'items': {
                    'type': 'object',
                    'properties': {
                        'name': {
                            'title': 'Name',
                            'type': 'string'
                        },
                        'email': {
                            'title': 'Email',
                            'type': 'string',
                            'pattern': '^\\S+@\\S+$',
                            'description': 'Email will be used for evil.'
                        },
                        'spam': {
                            'title': 'Spam',
                            'type': 'boolean',
                            'default': true
                        },
                        'comment': {
                            'title': 'Comment',
                            'type': 'string',
                            'maxLength': 20,
                            'validationMessage': 'Don\'t be greedy!'
                        }
                    },
                    'required': ['name', 'comment']
                }
            },
            'ngModelOptions': {},
            'type': 'array'
        };

        var model = {};
        var list = utils.selectOrSet(schema, [{ key: 'sub', readonly: true }]);

        //sub
        expect(merged[0].readonly).toEqual(true);

        //array
        expect(merged[0].items[0].readonly).toEqual(true);

        //array items
        expect(merged[0].items[0].items[0].readonly).toEqual(true);
    });
});
;

var _temp = function () {
    if (typeof __REACT_HOT_LOADER__ === 'undefined') {
        return;
    }
}();

;