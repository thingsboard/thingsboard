'use strict';

var _staticRequire = require('../core/staticRequire');

var _staticRequire2 = _interopRequireDefault(_staticRequire);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function reportIfNonStandard(context, node, name) {
  if (name.indexOf('!') !== -1) {
    context.report(node, `Unexpected '!' in '${name}'. ` + 'Do not use import syntax to configure webpack loaders.');
  }
}

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2.default)('no-webpack-loader-syntax')
    },
    schema: []
  },

  create: function (context) {
    return {
      ImportDeclaration: function handleImports(node) {
        reportIfNonStandard(context, node, node.source.value);
      },
      CallExpression: function handleRequires(node) {
        if ((0, _staticRequire2.default)(node)) {
          reportIfNonStandard(context, node, node.arguments[0].value);
        }
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby13ZWJwYWNrLWxvYWRlci1zeW50YXguanMiXSwibmFtZXMiOlsicmVwb3J0SWZOb25TdGFuZGFyZCIsImNvbnRleHQiLCJub2RlIiwibmFtZSIsImluZGV4T2YiLCJyZXBvcnQiLCJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsInR5cGUiLCJkb2NzIiwidXJsIiwic2NoZW1hIiwiY3JlYXRlIiwiSW1wb3J0RGVjbGFyYXRpb24iLCJoYW5kbGVJbXBvcnRzIiwic291cmNlIiwidmFsdWUiLCJDYWxsRXhwcmVzc2lvbiIsImhhbmRsZVJlcXVpcmVzIiwiYXJndW1lbnRzIl0sIm1hcHBpbmdzIjoiOztBQUFBOzs7O0FBQ0E7Ozs7OztBQUVBLFNBQVNBLG1CQUFULENBQTZCQyxPQUE3QixFQUFzQ0MsSUFBdEMsRUFBNENDLElBQTVDLEVBQWtEO0FBQ2hELE1BQUlBLEtBQUtDLE9BQUwsQ0FBYSxHQUFiLE1BQXNCLENBQUMsQ0FBM0IsRUFBOEI7QUFDNUJILFlBQVFJLE1BQVIsQ0FBZUgsSUFBZixFQUFzQixzQkFBcUJDLElBQUssS0FBM0IsR0FDbkIsd0RBREY7QUFHRDtBQUNGOztBQUVERyxPQUFPQyxPQUFQLEdBQWlCO0FBQ2ZDLFFBQU07QUFDSkMsVUFBTSxTQURGO0FBRUpDLFVBQU07QUFDSkMsV0FBSyx1QkFBUSwwQkFBUjtBQURELEtBRkY7QUFLSkMsWUFBUTtBQUxKLEdBRFM7O0FBU2ZDLFVBQVEsVUFBVVosT0FBVixFQUFtQjtBQUN6QixXQUFPO0FBQ0xhLHlCQUFtQixTQUFTQyxhQUFULENBQXVCYixJQUF2QixFQUE2QjtBQUM5Q0YsNEJBQW9CQyxPQUFwQixFQUE2QkMsSUFBN0IsRUFBbUNBLEtBQUtjLE1BQUwsQ0FBWUMsS0FBL0M7QUFDRCxPQUhJO0FBSUxDLHNCQUFnQixTQUFTQyxjQUFULENBQXdCakIsSUFBeEIsRUFBOEI7QUFDNUMsWUFBSSw2QkFBZ0JBLElBQWhCLENBQUosRUFBMkI7QUFDekJGLDhCQUFvQkMsT0FBcEIsRUFBNkJDLElBQTdCLEVBQW1DQSxLQUFLa0IsU0FBTCxDQUFlLENBQWYsRUFBa0JILEtBQXJEO0FBQ0Q7QUFDRjtBQVJJLEtBQVA7QUFVRDtBQXBCYyxDQUFqQiIsImZpbGUiOiJuby13ZWJwYWNrLWxvYWRlci1zeW50YXguanMiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgaXNTdGF0aWNSZXF1aXJlIGZyb20gJy4uL2NvcmUvc3RhdGljUmVxdWlyZSdcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnXG5cbmZ1bmN0aW9uIHJlcG9ydElmTm9uU3RhbmRhcmQoY29udGV4dCwgbm9kZSwgbmFtZSkge1xuICBpZiAobmFtZS5pbmRleE9mKCchJykgIT09IC0xKSB7XG4gICAgY29udGV4dC5yZXBvcnQobm9kZSwgYFVuZXhwZWN0ZWQgJyEnIGluICcke25hbWV9Jy4gYCArXG4gICAgICAnRG8gbm90IHVzZSBpbXBvcnQgc3ludGF4IHRvIGNvbmZpZ3VyZSB3ZWJwYWNrIGxvYWRlcnMuJ1xuICAgIClcbiAgfVxufVxuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdwcm9ibGVtJyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ25vLXdlYnBhY2stbG9hZGVyLXN5bnRheCcpLFxuICAgIH0sXG4gICAgc2NoZW1hOiBbXSxcbiAgfSxcblxuICBjcmVhdGU6IGZ1bmN0aW9uIChjb250ZXh0KSB7XG4gICAgcmV0dXJuIHtcbiAgICAgIEltcG9ydERlY2xhcmF0aW9uOiBmdW5jdGlvbiBoYW5kbGVJbXBvcnRzKG5vZGUpIHtcbiAgICAgICAgcmVwb3J0SWZOb25TdGFuZGFyZChjb250ZXh0LCBub2RlLCBub2RlLnNvdXJjZS52YWx1ZSlcbiAgICAgIH0sXG4gICAgICBDYWxsRXhwcmVzc2lvbjogZnVuY3Rpb24gaGFuZGxlUmVxdWlyZXMobm9kZSkge1xuICAgICAgICBpZiAoaXNTdGF0aWNSZXF1aXJlKG5vZGUpKSB7XG4gICAgICAgICAgcmVwb3J0SWZOb25TdGFuZGFyZChjb250ZXh0LCBub2RlLCBub2RlLmFyZ3VtZW50c1swXS52YWx1ZSlcbiAgICAgICAgfVxuICAgICAgfSxcbiAgICB9XG4gIH0sXG59XG4iXX0=