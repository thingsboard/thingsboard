'use strict';

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

module.exports = {
  meta: {
    type: 'suggestion',
    docs: { url: (0, _docsUrl2.default)('no-named-export') },
    schema: []
  },

  create(context) {
    // ignore non-modules
    if (context.parserOptions.sourceType !== 'module') {
      return {};
    }

    const message = 'Named exports are not allowed.';

    return {
      ExportAllDeclaration(node) {
        context.report({ node, message });
      },

      ExportNamedDeclaration(node) {
        if (node.specifiers.length === 0) {
          return context.report({ node, message });
        }

        const someNamed = node.specifiers.some(specifier => specifier.exported.name !== 'default');
        if (someNamed) {
          context.report({ node, message });
        }
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1uYW1lZC1leHBvcnQuanMiXSwibmFtZXMiOlsibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsInNjaGVtYSIsImNyZWF0ZSIsImNvbnRleHQiLCJwYXJzZXJPcHRpb25zIiwic291cmNlVHlwZSIsIm1lc3NhZ2UiLCJFeHBvcnRBbGxEZWNsYXJhdGlvbiIsIm5vZGUiLCJyZXBvcnQiLCJFeHBvcnROYW1lZERlY2xhcmF0aW9uIiwic3BlY2lmaWVycyIsImxlbmd0aCIsInNvbWVOYW1lZCIsInNvbWUiLCJzcGVjaWZpZXIiLCJleHBvcnRlZCIsIm5hbWUiXSwibWFwcGluZ3MiOiI7O0FBQUE7Ozs7OztBQUVBQSxPQUFPQyxPQUFQLEdBQWlCO0FBQ2ZDLFFBQU07QUFDSkMsVUFBTSxZQURGO0FBRUpDLFVBQU0sRUFBRUMsS0FBSyx1QkFBUSxpQkFBUixDQUFQLEVBRkY7QUFHSkMsWUFBUTtBQUhKLEdBRFM7O0FBT2ZDLFNBQU9DLE9BQVAsRUFBZ0I7QUFDZDtBQUNBLFFBQUlBLFFBQVFDLGFBQVIsQ0FBc0JDLFVBQXRCLEtBQXFDLFFBQXpDLEVBQW1EO0FBQ2pELGFBQU8sRUFBUDtBQUNEOztBQUVELFVBQU1DLFVBQVUsZ0NBQWhCOztBQUVBLFdBQU87QUFDTEMsMkJBQXFCQyxJQUFyQixFQUEyQjtBQUN6QkwsZ0JBQVFNLE1BQVIsQ0FBZSxFQUFDRCxJQUFELEVBQU9GLE9BQVAsRUFBZjtBQUNELE9BSEk7O0FBS0xJLDZCQUF1QkYsSUFBdkIsRUFBNkI7QUFDM0IsWUFBSUEsS0FBS0csVUFBTCxDQUFnQkMsTUFBaEIsS0FBMkIsQ0FBL0IsRUFBa0M7QUFDaEMsaUJBQU9ULFFBQVFNLE1BQVIsQ0FBZSxFQUFDRCxJQUFELEVBQU9GLE9BQVAsRUFBZixDQUFQO0FBQ0Q7O0FBRUQsY0FBTU8sWUFBWUwsS0FBS0csVUFBTCxDQUFnQkcsSUFBaEIsQ0FBcUJDLGFBQWFBLFVBQVVDLFFBQVYsQ0FBbUJDLElBQW5CLEtBQTRCLFNBQTlELENBQWxCO0FBQ0EsWUFBSUosU0FBSixFQUFlO0FBQ2JWLGtCQUFRTSxNQUFSLENBQWUsRUFBQ0QsSUFBRCxFQUFPRixPQUFQLEVBQWY7QUFDRDtBQUNGO0FBZEksS0FBUDtBQWdCRDtBQS9CYyxDQUFqQiIsImZpbGUiOiJuby1uYW1lZC1leHBvcnQuanMiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJ1xuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdzdWdnZXN0aW9uJyxcbiAgICBkb2NzOiB7IHVybDogZG9jc1VybCgnbm8tbmFtZWQtZXhwb3J0JykgfSxcbiAgICBzY2hlbWE6IFtdLFxuICB9LFxuXG4gIGNyZWF0ZShjb250ZXh0KSB7XG4gICAgLy8gaWdub3JlIG5vbi1tb2R1bGVzXG4gICAgaWYgKGNvbnRleHQucGFyc2VyT3B0aW9ucy5zb3VyY2VUeXBlICE9PSAnbW9kdWxlJykge1xuICAgICAgcmV0dXJuIHt9XG4gICAgfVxuXG4gICAgY29uc3QgbWVzc2FnZSA9ICdOYW1lZCBleHBvcnRzIGFyZSBub3QgYWxsb3dlZC4nXG5cbiAgICByZXR1cm4ge1xuICAgICAgRXhwb3J0QWxsRGVjbGFyYXRpb24obm9kZSkge1xuICAgICAgICBjb250ZXh0LnJlcG9ydCh7bm9kZSwgbWVzc2FnZX0pXG4gICAgICB9LFxuXG4gICAgICBFeHBvcnROYW1lZERlY2xhcmF0aW9uKG5vZGUpIHtcbiAgICAgICAgaWYgKG5vZGUuc3BlY2lmaWVycy5sZW5ndGggPT09IDApIHtcbiAgICAgICAgICByZXR1cm4gY29udGV4dC5yZXBvcnQoe25vZGUsIG1lc3NhZ2V9KVxuICAgICAgICB9XG5cbiAgICAgICAgY29uc3Qgc29tZU5hbWVkID0gbm9kZS5zcGVjaWZpZXJzLnNvbWUoc3BlY2lmaWVyID0+IHNwZWNpZmllci5leHBvcnRlZC5uYW1lICE9PSAnZGVmYXVsdCcpXG4gICAgICAgIGlmIChzb21lTmFtZWQpIHtcbiAgICAgICAgICBjb250ZXh0LnJlcG9ydCh7bm9kZSwgbWVzc2FnZX0pXG4gICAgICAgIH1cbiAgICAgIH0sXG4gICAgfVxuICB9LFxufVxuIl19