'use strict';

var _importType = require('../core/importType');

var _importType2 = _interopRequireDefault(_importType);

var _staticRequire = require('../core/staticRequire');

var _staticRequire2 = _interopRequireDefault(_staticRequire);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function reportIfMissing(context, node, allowed, name) {
  if (allowed.indexOf(name) === -1 && (0, _importType2.default)(name, context) === 'builtin') {
    context.report(node, 'Do not import Node.js builtin module "' + name + '"');
  }
}

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('no-nodejs-modules')
    },
    schema: [{
      type: 'object',
      properties: {
        allow: {
          type: 'array',
          uniqueItems: true,
          items: {
            type: 'string'
          }
        }
      },
      additionalProperties: false
    }]
  },

  create: function (context) {
    const options = context.options[0] || {};
    const allowed = options.allow || [];

    return {
      ImportDeclaration: function handleImports(node) {
        reportIfMissing(context, node, allowed, node.source.value);
      },
      CallExpression: function handleRequires(node) {
        if ((0, _staticRequire2.default)(node)) {
          reportIfMissing(context, node, allowed, node.arguments[0].value);
        }
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1ub2RlanMtbW9kdWxlcy5qcyJdLCJuYW1lcyI6WyJyZXBvcnRJZk1pc3NpbmciLCJjb250ZXh0Iiwibm9kZSIsImFsbG93ZWQiLCJuYW1lIiwiaW5kZXhPZiIsInJlcG9ydCIsIm1vZHVsZSIsImV4cG9ydHMiLCJtZXRhIiwidHlwZSIsImRvY3MiLCJ1cmwiLCJzY2hlbWEiLCJwcm9wZXJ0aWVzIiwiYWxsb3ciLCJ1bmlxdWVJdGVtcyIsIml0ZW1zIiwiYWRkaXRpb25hbFByb3BlcnRpZXMiLCJjcmVhdGUiLCJvcHRpb25zIiwiSW1wb3J0RGVjbGFyYXRpb24iLCJoYW5kbGVJbXBvcnRzIiwic291cmNlIiwidmFsdWUiLCJDYWxsRXhwcmVzc2lvbiIsImhhbmRsZVJlcXVpcmVzIiwiYXJndW1lbnRzIl0sIm1hcHBpbmdzIjoiOztBQUFBOzs7O0FBQ0E7Ozs7QUFDQTs7Ozs7O0FBRUEsU0FBU0EsZUFBVCxDQUF5QkMsT0FBekIsRUFBa0NDLElBQWxDLEVBQXdDQyxPQUF4QyxFQUFpREMsSUFBakQsRUFBdUQ7QUFDckQsTUFBSUQsUUFBUUUsT0FBUixDQUFnQkQsSUFBaEIsTUFBMEIsQ0FBQyxDQUEzQixJQUFnQywwQkFBV0EsSUFBWCxFQUFpQkgsT0FBakIsTUFBOEIsU0FBbEUsRUFBNkU7QUFDM0VBLFlBQVFLLE1BQVIsQ0FBZUosSUFBZixFQUFxQiwyQ0FBMkNFLElBQTNDLEdBQWtELEdBQXZFO0FBQ0Q7QUFDRjs7QUFFREcsT0FBT0MsT0FBUCxHQUFpQjtBQUNmQyxRQUFNO0FBQ0pDLFVBQU0sWUFERjtBQUVKQyxVQUFNO0FBQ0pDLFdBQUssdUJBQVEsbUJBQVI7QUFERCxLQUZGO0FBS0pDLFlBQVEsQ0FDTjtBQUNFSCxZQUFNLFFBRFI7QUFFRUksa0JBQVk7QUFDVkMsZUFBTztBQUNMTCxnQkFBTSxPQUREO0FBRUxNLHVCQUFhLElBRlI7QUFHTEMsaUJBQU87QUFDTFAsa0JBQU07QUFERDtBQUhGO0FBREcsT0FGZDtBQVdFUSw0QkFBc0I7QUFYeEIsS0FETTtBQUxKLEdBRFM7O0FBdUJmQyxVQUFRLFVBQVVsQixPQUFWLEVBQW1CO0FBQ3pCLFVBQU1tQixVQUFVbkIsUUFBUW1CLE9BQVIsQ0FBZ0IsQ0FBaEIsS0FBc0IsRUFBdEM7QUFDQSxVQUFNakIsVUFBVWlCLFFBQVFMLEtBQVIsSUFBaUIsRUFBakM7O0FBRUEsV0FBTztBQUNMTSx5QkFBbUIsU0FBU0MsYUFBVCxDQUF1QnBCLElBQXZCLEVBQTZCO0FBQzlDRix3QkFBZ0JDLE9BQWhCLEVBQXlCQyxJQUF6QixFQUErQkMsT0FBL0IsRUFBd0NELEtBQUtxQixNQUFMLENBQVlDLEtBQXBEO0FBQ0QsT0FISTtBQUlMQyxzQkFBZ0IsU0FBU0MsY0FBVCxDQUF3QnhCLElBQXhCLEVBQThCO0FBQzVDLFlBQUksNkJBQWdCQSxJQUFoQixDQUFKLEVBQTJCO0FBQ3pCRiwwQkFBZ0JDLE9BQWhCLEVBQXlCQyxJQUF6QixFQUErQkMsT0FBL0IsRUFBd0NELEtBQUt5QixTQUFMLENBQWUsQ0FBZixFQUFrQkgsS0FBMUQ7QUFDRDtBQUNGO0FBUkksS0FBUDtBQVVEO0FBckNjLENBQWpCIiwiZmlsZSI6Im5vLW5vZGVqcy1tb2R1bGVzLmpzIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IGltcG9ydFR5cGUgZnJvbSAnLi4vY29yZS9pbXBvcnRUeXBlJ1xuaW1wb3J0IGlzU3RhdGljUmVxdWlyZSBmcm9tICcuLi9jb3JlL3N0YXRpY1JlcXVpcmUnXG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJ1xuXG5mdW5jdGlvbiByZXBvcnRJZk1pc3NpbmcoY29udGV4dCwgbm9kZSwgYWxsb3dlZCwgbmFtZSkge1xuICBpZiAoYWxsb3dlZC5pbmRleE9mKG5hbWUpID09PSAtMSAmJiBpbXBvcnRUeXBlKG5hbWUsIGNvbnRleHQpID09PSAnYnVpbHRpbicpIHtcbiAgICBjb250ZXh0LnJlcG9ydChub2RlLCAnRG8gbm90IGltcG9ydCBOb2RlLmpzIGJ1aWx0aW4gbW9kdWxlIFwiJyArIG5hbWUgKyAnXCInKVxuICB9XG59XG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3N1Z2dlc3Rpb24nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnbm8tbm9kZWpzLW1vZHVsZXMnKSxcbiAgICB9LFxuICAgIHNjaGVtYTogW1xuICAgICAge1xuICAgICAgICB0eXBlOiAnb2JqZWN0JyxcbiAgICAgICAgcHJvcGVydGllczoge1xuICAgICAgICAgIGFsbG93OiB7XG4gICAgICAgICAgICB0eXBlOiAnYXJyYXknLFxuICAgICAgICAgICAgdW5pcXVlSXRlbXM6IHRydWUsXG4gICAgICAgICAgICBpdGVtczoge1xuICAgICAgICAgICAgICB0eXBlOiAnc3RyaW5nJyxcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgfSxcbiAgICAgICAgfSxcbiAgICAgICAgYWRkaXRpb25hbFByb3BlcnRpZXM6IGZhbHNlLFxuICAgICAgfSxcbiAgICBdLFxuICB9LFxuXG4gIGNyZWF0ZTogZnVuY3Rpb24gKGNvbnRleHQpIHtcbiAgICBjb25zdCBvcHRpb25zID0gY29udGV4dC5vcHRpb25zWzBdIHx8IHt9XG4gICAgY29uc3QgYWxsb3dlZCA9IG9wdGlvbnMuYWxsb3cgfHwgW11cblxuICAgIHJldHVybiB7XG4gICAgICBJbXBvcnREZWNsYXJhdGlvbjogZnVuY3Rpb24gaGFuZGxlSW1wb3J0cyhub2RlKSB7XG4gICAgICAgIHJlcG9ydElmTWlzc2luZyhjb250ZXh0LCBub2RlLCBhbGxvd2VkLCBub2RlLnNvdXJjZS52YWx1ZSlcbiAgICAgIH0sXG4gICAgICBDYWxsRXhwcmVzc2lvbjogZnVuY3Rpb24gaGFuZGxlUmVxdWlyZXMobm9kZSkge1xuICAgICAgICBpZiAoaXNTdGF0aWNSZXF1aXJlKG5vZGUpKSB7XG4gICAgICAgICAgcmVwb3J0SWZNaXNzaW5nKGNvbnRleHQsIG5vZGUsIGFsbG93ZWQsIG5vZGUuYXJndW1lbnRzWzBdLnZhbHVlKVxuICAgICAgICB9XG4gICAgICB9LFxuICAgIH1cbiAgfSxcbn1cbiJdfQ==