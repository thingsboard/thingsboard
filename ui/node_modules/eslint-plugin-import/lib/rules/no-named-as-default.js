'use strict';

var _ExportMap = require('../ExportMap');

var _ExportMap2 = _interopRequireDefault(_ExportMap);

var _importDeclaration = require('../importDeclaration');

var _importDeclaration2 = _interopRequireDefault(_importDeclaration);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2.default)('no-named-as-default')
    },
    schema: []
  },

  create: function (context) {
    function checkDefault(nameKey, defaultSpecifier) {
      // #566: default is a valid specifier
      if (defaultSpecifier[nameKey].name === 'default') return;

      var declaration = (0, _importDeclaration2.default)(context);

      var imports = _ExportMap2.default.get(declaration.source.value, context);
      if (imports == null) return;

      if (imports.errors.length) {
        imports.reportErrors(context, declaration);
        return;
      }

      if (imports.has('default') && imports.has(defaultSpecifier[nameKey].name)) {

        context.report(defaultSpecifier, 'Using exported name \'' + defaultSpecifier[nameKey].name + '\' as identifier for default export.');
      }
    }
    return {
      'ImportDefaultSpecifier': checkDefault.bind(null, 'local'),
      'ExportDefaultSpecifier': checkDefault.bind(null, 'exported')
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1uYW1lZC1hcy1kZWZhdWx0LmpzIl0sIm5hbWVzIjpbIm1vZHVsZSIsImV4cG9ydHMiLCJtZXRhIiwidHlwZSIsImRvY3MiLCJ1cmwiLCJzY2hlbWEiLCJjcmVhdGUiLCJjb250ZXh0IiwiY2hlY2tEZWZhdWx0IiwibmFtZUtleSIsImRlZmF1bHRTcGVjaWZpZXIiLCJuYW1lIiwiZGVjbGFyYXRpb24iLCJpbXBvcnRzIiwiRXhwb3J0cyIsImdldCIsInNvdXJjZSIsInZhbHVlIiwiZXJyb3JzIiwibGVuZ3RoIiwicmVwb3J0RXJyb3JzIiwiaGFzIiwicmVwb3J0IiwiYmluZCJdLCJtYXBwaW5ncyI6Ijs7QUFBQTs7OztBQUNBOzs7O0FBQ0E7Ozs7OztBQUVBQSxPQUFPQyxPQUFQLEdBQWlCO0FBQ2ZDLFFBQU07QUFDSkMsVUFBTSxTQURGO0FBRUpDLFVBQU07QUFDSkMsV0FBSyx1QkFBUSxxQkFBUjtBQURELEtBRkY7QUFLSkMsWUFBUTtBQUxKLEdBRFM7O0FBU2ZDLFVBQVEsVUFBVUMsT0FBVixFQUFtQjtBQUN6QixhQUFTQyxZQUFULENBQXNCQyxPQUF0QixFQUErQkMsZ0JBQS9CLEVBQWlEO0FBQy9DO0FBQ0EsVUFBSUEsaUJBQWlCRCxPQUFqQixFQUEwQkUsSUFBMUIsS0FBbUMsU0FBdkMsRUFBa0Q7O0FBRWxELFVBQUlDLGNBQWMsaUNBQWtCTCxPQUFsQixDQUFsQjs7QUFFQSxVQUFJTSxVQUFVQyxvQkFBUUMsR0FBUixDQUFZSCxZQUFZSSxNQUFaLENBQW1CQyxLQUEvQixFQUFzQ1YsT0FBdEMsQ0FBZDtBQUNBLFVBQUlNLFdBQVcsSUFBZixFQUFxQjs7QUFFckIsVUFBSUEsUUFBUUssTUFBUixDQUFlQyxNQUFuQixFQUEyQjtBQUN6Qk4sZ0JBQVFPLFlBQVIsQ0FBcUJiLE9BQXJCLEVBQThCSyxXQUE5QjtBQUNBO0FBQ0Q7O0FBRUQsVUFBSUMsUUFBUVEsR0FBUixDQUFZLFNBQVosS0FDQVIsUUFBUVEsR0FBUixDQUFZWCxpQkFBaUJELE9BQWpCLEVBQTBCRSxJQUF0QyxDQURKLEVBQ2lEOztBQUUvQ0osZ0JBQVFlLE1BQVIsQ0FBZVosZ0JBQWYsRUFDRSwyQkFBMkJBLGlCQUFpQkQsT0FBakIsRUFBMEJFLElBQXJELEdBQ0Esc0NBRkY7QUFJRDtBQUNGO0FBQ0QsV0FBTztBQUNMLGdDQUEwQkgsYUFBYWUsSUFBYixDQUFrQixJQUFsQixFQUF3QixPQUF4QixDQURyQjtBQUVMLGdDQUEwQmYsYUFBYWUsSUFBYixDQUFrQixJQUFsQixFQUF3QixVQUF4QjtBQUZyQixLQUFQO0FBSUQ7QUFyQ2MsQ0FBakIiLCJmaWxlIjoibm8tbmFtZWQtYXMtZGVmYXVsdC5qcyIsInNvdXJjZXNDb250ZW50IjpbImltcG9ydCBFeHBvcnRzIGZyb20gJy4uL0V4cG9ydE1hcCdcbmltcG9ydCBpbXBvcnREZWNsYXJhdGlvbiBmcm9tICcuLi9pbXBvcnREZWNsYXJhdGlvbidcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnXG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3Byb2JsZW0nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnbm8tbmFtZWQtYXMtZGVmYXVsdCcpLFxuICAgIH0sXG4gICAgc2NoZW1hOiBbXSxcbiAgfSxcblxuICBjcmVhdGU6IGZ1bmN0aW9uIChjb250ZXh0KSB7XG4gICAgZnVuY3Rpb24gY2hlY2tEZWZhdWx0KG5hbWVLZXksIGRlZmF1bHRTcGVjaWZpZXIpIHtcbiAgICAgIC8vICM1NjY6IGRlZmF1bHQgaXMgYSB2YWxpZCBzcGVjaWZpZXJcbiAgICAgIGlmIChkZWZhdWx0U3BlY2lmaWVyW25hbWVLZXldLm5hbWUgPT09ICdkZWZhdWx0JykgcmV0dXJuXG5cbiAgICAgIHZhciBkZWNsYXJhdGlvbiA9IGltcG9ydERlY2xhcmF0aW9uKGNvbnRleHQpXG5cbiAgICAgIHZhciBpbXBvcnRzID0gRXhwb3J0cy5nZXQoZGVjbGFyYXRpb24uc291cmNlLnZhbHVlLCBjb250ZXh0KVxuICAgICAgaWYgKGltcG9ydHMgPT0gbnVsbCkgcmV0dXJuXG5cbiAgICAgIGlmIChpbXBvcnRzLmVycm9ycy5sZW5ndGgpIHtcbiAgICAgICAgaW1wb3J0cy5yZXBvcnRFcnJvcnMoY29udGV4dCwgZGVjbGFyYXRpb24pXG4gICAgICAgIHJldHVyblxuICAgICAgfVxuXG4gICAgICBpZiAoaW1wb3J0cy5oYXMoJ2RlZmF1bHQnKSAmJlxuICAgICAgICAgIGltcG9ydHMuaGFzKGRlZmF1bHRTcGVjaWZpZXJbbmFtZUtleV0ubmFtZSkpIHtcblxuICAgICAgICBjb250ZXh0LnJlcG9ydChkZWZhdWx0U3BlY2lmaWVyLFxuICAgICAgICAgICdVc2luZyBleHBvcnRlZCBuYW1lIFxcJycgKyBkZWZhdWx0U3BlY2lmaWVyW25hbWVLZXldLm5hbWUgK1xuICAgICAgICAgICdcXCcgYXMgaWRlbnRpZmllciBmb3IgZGVmYXVsdCBleHBvcnQuJylcblxuICAgICAgfVxuICAgIH1cbiAgICByZXR1cm4ge1xuICAgICAgJ0ltcG9ydERlZmF1bHRTcGVjaWZpZXInOiBjaGVja0RlZmF1bHQuYmluZChudWxsLCAnbG9jYWwnKSxcbiAgICAgICdFeHBvcnREZWZhdWx0U3BlY2lmaWVyJzogY2hlY2tEZWZhdWx0LmJpbmQobnVsbCwgJ2V4cG9ydGVkJyksXG4gICAgfVxuICB9LFxufVxuIl19