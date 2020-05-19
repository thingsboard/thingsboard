'use strict';

jest.dontMock('../SchemaForm');
jest.dontMock('../utils');
jest.dontMock('lodash');

var React = require('react');
var TestUtils = require('react-addons-test-utils');
var SchemaForm = require('../SchemaForm');

describe('SchemaForm', function () {

  beforeEach(function () {});

  it('shows SchemaForm', function () {
    var shallowRenderer = TestUtils.createRenderer();
    var cfg = {
      form: {},
      schema: {
        'type': 'object'
      },
      model: {},
      mapper: {}
    };
    shallowRenderer.render(React.createElement(SchemaForm, {
      schema: cfg.schema,
      mapper: cfg.mapper
    }));

    var result = shallowRenderer.getRenderOutput();
    console.log('result = ', result.props);
    expect(result.props.children).toEqual('SchemaForm');
  });
});
;

var _temp = function () {
  if (typeof __REACT_HOT_LOADER__ === 'undefined') {
    return;
  }
}();

;