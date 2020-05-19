'use strict';

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function isNonExportStatement(_ref) {
  let type = _ref.type;

  return type !== 'ExportDefaultDeclaration' && type !== 'ExportNamedDeclaration' && type !== 'ExportAllDeclaration';
}

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('exports-last')
    },
    schema: []
  },

  create: function (context) {
    return {
      Program: function (_ref2) {
        let body = _ref2.body;

        const lastNonExportStatementIndex = body.reduce(function findLastIndex(acc, item, index) {
          if (isNonExportStatement(item)) {
            return index;
          }
          return acc;
        }, -1);

        if (lastNonExportStatementIndex !== -1) {
          body.slice(0, lastNonExportStatementIndex).forEach(function checkNonExport(node) {
            if (!isNonExportStatement(node)) {
              context.report({
                node,
                message: 'Export statements should appear at the end of the file'
              });
            }
          });
        }
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9leHBvcnRzLWxhc3QuanMiXSwibmFtZXMiOlsiaXNOb25FeHBvcnRTdGF0ZW1lbnQiLCJ0eXBlIiwibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJkb2NzIiwidXJsIiwic2NoZW1hIiwiY3JlYXRlIiwiY29udGV4dCIsIlByb2dyYW0iLCJib2R5IiwibGFzdE5vbkV4cG9ydFN0YXRlbWVudEluZGV4IiwicmVkdWNlIiwiZmluZExhc3RJbmRleCIsImFjYyIsIml0ZW0iLCJpbmRleCIsInNsaWNlIiwiZm9yRWFjaCIsImNoZWNrTm9uRXhwb3J0Iiwibm9kZSIsInJlcG9ydCIsIm1lc3NhZ2UiXSwibWFwcGluZ3MiOiI7O0FBQUE7Ozs7OztBQUVBLFNBQVNBLG9CQUFULE9BQXdDO0FBQUEsTUFBUkMsSUFBUSxRQUFSQSxJQUFROztBQUN0QyxTQUFPQSxTQUFTLDBCQUFULElBQ0xBLFNBQVMsd0JBREosSUFFTEEsU0FBUyxzQkFGWDtBQUdEOztBQUVEQyxPQUFPQyxPQUFQLEdBQWlCO0FBQ2ZDLFFBQU07QUFDSkgsVUFBTSxZQURGO0FBRUpJLFVBQU07QUFDSkMsV0FBSyx1QkFBUSxjQUFSO0FBREQsS0FGRjtBQUtKQyxZQUFRO0FBTEosR0FEUzs7QUFTZkMsVUFBUSxVQUFVQyxPQUFWLEVBQW1CO0FBQ3pCLFdBQU87QUFDTEMsZUFBUyxpQkFBb0I7QUFBQSxZQUFSQyxJQUFRLFNBQVJBLElBQVE7O0FBQzNCLGNBQU1DLDhCQUE4QkQsS0FBS0UsTUFBTCxDQUFZLFNBQVNDLGFBQVQsQ0FBdUJDLEdBQXZCLEVBQTRCQyxJQUE1QixFQUFrQ0MsS0FBbEMsRUFBeUM7QUFDdkYsY0FBSWpCLHFCQUFxQmdCLElBQXJCLENBQUosRUFBZ0M7QUFDOUIsbUJBQU9DLEtBQVA7QUFDRDtBQUNELGlCQUFPRixHQUFQO0FBQ0QsU0FMbUMsRUFLakMsQ0FBQyxDQUxnQyxDQUFwQzs7QUFPQSxZQUFJSCxnQ0FBZ0MsQ0FBQyxDQUFyQyxFQUF3QztBQUN0Q0QsZUFBS08sS0FBTCxDQUFXLENBQVgsRUFBY04sMkJBQWQsRUFBMkNPLE9BQTNDLENBQW1ELFNBQVNDLGNBQVQsQ0FBd0JDLElBQXhCLEVBQThCO0FBQy9FLGdCQUFJLENBQUNyQixxQkFBcUJxQixJQUFyQixDQUFMLEVBQWlDO0FBQy9CWixzQkFBUWEsTUFBUixDQUFlO0FBQ2JELG9CQURhO0FBRWJFLHlCQUFTO0FBRkksZUFBZjtBQUlEO0FBQ0YsV0FQRDtBQVFEO0FBQ0Y7QUFuQkksS0FBUDtBQXFCRDtBQS9CYyxDQUFqQiIsImZpbGUiOiJleHBvcnRzLWxhc3QuanMiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJ1xuXG5mdW5jdGlvbiBpc05vbkV4cG9ydFN0YXRlbWVudCh7IHR5cGUgfSkge1xuICByZXR1cm4gdHlwZSAhPT0gJ0V4cG9ydERlZmF1bHREZWNsYXJhdGlvbicgJiZcbiAgICB0eXBlICE9PSAnRXhwb3J0TmFtZWREZWNsYXJhdGlvbicgJiZcbiAgICB0eXBlICE9PSAnRXhwb3J0QWxsRGVjbGFyYXRpb24nXG59XG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3N1Z2dlc3Rpb24nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnZXhwb3J0cy1sYXN0JyksXG4gICAgfSxcbiAgICBzY2hlbWE6IFtdLFxuICB9LFxuXG4gIGNyZWF0ZTogZnVuY3Rpb24gKGNvbnRleHQpIHtcbiAgICByZXR1cm4ge1xuICAgICAgUHJvZ3JhbTogZnVuY3Rpb24gKHsgYm9keSB9KSB7XG4gICAgICAgIGNvbnN0IGxhc3ROb25FeHBvcnRTdGF0ZW1lbnRJbmRleCA9IGJvZHkucmVkdWNlKGZ1bmN0aW9uIGZpbmRMYXN0SW5kZXgoYWNjLCBpdGVtLCBpbmRleCkge1xuICAgICAgICAgIGlmIChpc05vbkV4cG9ydFN0YXRlbWVudChpdGVtKSkge1xuICAgICAgICAgICAgcmV0dXJuIGluZGV4XG4gICAgICAgICAgfVxuICAgICAgICAgIHJldHVybiBhY2NcbiAgICAgICAgfSwgLTEpXG5cbiAgICAgICAgaWYgKGxhc3ROb25FeHBvcnRTdGF0ZW1lbnRJbmRleCAhPT0gLTEpIHtcbiAgICAgICAgICBib2R5LnNsaWNlKDAsIGxhc3ROb25FeHBvcnRTdGF0ZW1lbnRJbmRleCkuZm9yRWFjaChmdW5jdGlvbiBjaGVja05vbkV4cG9ydChub2RlKSB7XG4gICAgICAgICAgICBpZiAoIWlzTm9uRXhwb3J0U3RhdGVtZW50KG5vZGUpKSB7XG4gICAgICAgICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICAgICAgICBub2RlLFxuICAgICAgICAgICAgICAgIG1lc3NhZ2U6ICdFeHBvcnQgc3RhdGVtZW50cyBzaG91bGQgYXBwZWFyIGF0IHRoZSBlbmQgb2YgdGhlIGZpbGUnLFxuICAgICAgICAgICAgICB9KVxuICAgICAgICAgICAgfVxuICAgICAgICAgIH0pXG4gICAgICAgIH1cbiAgICAgIH0sXG4gICAgfVxuICB9LFxufVxuIl19