'use strict';

var _path = require('path');

var path = _interopRequireWildcard(_path);

var _ExportMap = require('../ExportMap');

var _ExportMap2 = _interopRequireDefault(_ExportMap);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } else { var newObj = {}; if (obj != null) { for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) newObj[key] = obj[key]; } } newObj.default = obj; return newObj; } }

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2.default)('named')
    },
    schema: []
  },

  create: function (context) {
    function checkSpecifiers(key, type, node) {
      // ignore local exports and type imports/exports
      if (node.source == null || node.importKind === 'type' || node.importKind === 'typeof' || node.exportKind === 'type') {
        return;
      }

      if (!node.specifiers.some(function (im) {
        return im.type === type;
      })) {
        return; // no named imports/exports
      }

      const imports = _ExportMap2.default.get(node.source.value, context);
      if (imports == null) return;

      if (imports.errors.length) {
        imports.reportErrors(context, node);
        return;
      }

      node.specifiers.forEach(function (im) {
        if (im.type !== type) return;

        // ignore type imports
        if (im.importKind === 'type' || im.importKind === 'typeof') return;

        const deepLookup = imports.hasDeep(im[key].name);

        if (!deepLookup.found) {
          if (deepLookup.path.length > 1) {
            const deepPath = deepLookup.path.map(i => path.relative(path.dirname(context.getFilename()), i.path)).join(' -> ');

            context.report(im[key], `${im[key].name} not found via ${deepPath}`);
          } else {
            context.report(im[key], im[key].name + ' not found in \'' + node.source.value + '\'');
          }
        }
      });
    }

    return {
      'ImportDeclaration': checkSpecifiers.bind(null, 'imported', 'ImportSpecifier'),

      'ExportNamedDeclaration': checkSpecifiers.bind(null, 'local', 'ExportSpecifier')
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uYW1lZC5qcyJdLCJuYW1lcyI6WyJwYXRoIiwibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsInNjaGVtYSIsImNyZWF0ZSIsImNvbnRleHQiLCJjaGVja1NwZWNpZmllcnMiLCJrZXkiLCJub2RlIiwic291cmNlIiwiaW1wb3J0S2luZCIsImV4cG9ydEtpbmQiLCJzcGVjaWZpZXJzIiwic29tZSIsImltIiwiaW1wb3J0cyIsIkV4cG9ydHMiLCJnZXQiLCJ2YWx1ZSIsImVycm9ycyIsImxlbmd0aCIsInJlcG9ydEVycm9ycyIsImZvckVhY2giLCJkZWVwTG9va3VwIiwiaGFzRGVlcCIsIm5hbWUiLCJmb3VuZCIsImRlZXBQYXRoIiwibWFwIiwiaSIsInJlbGF0aXZlIiwiZGlybmFtZSIsImdldEZpbGVuYW1lIiwiam9pbiIsInJlcG9ydCIsImJpbmQiXSwibWFwcGluZ3MiOiI7O0FBQUE7O0lBQVlBLEk7O0FBQ1o7Ozs7QUFDQTs7Ozs7Ozs7QUFFQUMsT0FBT0MsT0FBUCxHQUFpQjtBQUNmQyxRQUFNO0FBQ0pDLFVBQU0sU0FERjtBQUVKQyxVQUFNO0FBQ0pDLFdBQUssdUJBQVEsT0FBUjtBQURELEtBRkY7QUFLSkMsWUFBUTtBQUxKLEdBRFM7O0FBU2ZDLFVBQVEsVUFBVUMsT0FBVixFQUFtQjtBQUN6QixhQUFTQyxlQUFULENBQXlCQyxHQUF6QixFQUE4QlAsSUFBOUIsRUFBb0NRLElBQXBDLEVBQTBDO0FBQ3hDO0FBQ0EsVUFBSUEsS0FBS0MsTUFBTCxJQUFlLElBQWYsSUFBdUJELEtBQUtFLFVBQUwsS0FBb0IsTUFBM0MsSUFDQUYsS0FBS0UsVUFBTCxLQUFvQixRQURwQixJQUNpQ0YsS0FBS0csVUFBTCxLQUFvQixNQUR6RCxFQUNpRTtBQUMvRDtBQUNEOztBQUVELFVBQUksQ0FBQ0gsS0FBS0ksVUFBTCxDQUNFQyxJQURGLENBQ08sVUFBVUMsRUFBVixFQUFjO0FBQUUsZUFBT0EsR0FBR2QsSUFBSCxLQUFZQSxJQUFuQjtBQUF5QixPQURoRCxDQUFMLEVBQ3dEO0FBQ3RELGVBRHNELENBQy9DO0FBQ1I7O0FBRUQsWUFBTWUsVUFBVUMsb0JBQVFDLEdBQVIsQ0FBWVQsS0FBS0MsTUFBTCxDQUFZUyxLQUF4QixFQUErQmIsT0FBL0IsQ0FBaEI7QUFDQSxVQUFJVSxXQUFXLElBQWYsRUFBcUI7O0FBRXJCLFVBQUlBLFFBQVFJLE1BQVIsQ0FBZUMsTUFBbkIsRUFBMkI7QUFDekJMLGdCQUFRTSxZQUFSLENBQXFCaEIsT0FBckIsRUFBOEJHLElBQTlCO0FBQ0E7QUFDRDs7QUFFREEsV0FBS0ksVUFBTCxDQUFnQlUsT0FBaEIsQ0FBd0IsVUFBVVIsRUFBVixFQUFjO0FBQ3BDLFlBQUlBLEdBQUdkLElBQUgsS0FBWUEsSUFBaEIsRUFBc0I7O0FBRXRCO0FBQ0EsWUFBSWMsR0FBR0osVUFBSCxLQUFrQixNQUFsQixJQUE0QkksR0FBR0osVUFBSCxLQUFrQixRQUFsRCxFQUE0RDs7QUFFNUQsY0FBTWEsYUFBYVIsUUFBUVMsT0FBUixDQUFnQlYsR0FBR1AsR0FBSCxFQUFRa0IsSUFBeEIsQ0FBbkI7O0FBRUEsWUFBSSxDQUFDRixXQUFXRyxLQUFoQixFQUF1QjtBQUNyQixjQUFJSCxXQUFXM0IsSUFBWCxDQUFnQndCLE1BQWhCLEdBQXlCLENBQTdCLEVBQWdDO0FBQzlCLGtCQUFNTyxXQUFXSixXQUFXM0IsSUFBWCxDQUNkZ0MsR0FEYyxDQUNWQyxLQUFLakMsS0FBS2tDLFFBQUwsQ0FBY2xDLEtBQUttQyxPQUFMLENBQWExQixRQUFRMkIsV0FBUixFQUFiLENBQWQsRUFBbURILEVBQUVqQyxJQUFyRCxDQURLLEVBRWRxQyxJQUZjLENBRVQsTUFGUyxDQUFqQjs7QUFJQTVCLG9CQUFRNkIsTUFBUixDQUFlcEIsR0FBR1AsR0FBSCxDQUFmLEVBQ0csR0FBRU8sR0FBR1AsR0FBSCxFQUFRa0IsSUFBSyxrQkFBaUJFLFFBQVMsRUFENUM7QUFFRCxXQVBELE1BT087QUFDTHRCLG9CQUFRNkIsTUFBUixDQUFlcEIsR0FBR1AsR0FBSCxDQUFmLEVBQ0VPLEdBQUdQLEdBQUgsRUFBUWtCLElBQVIsR0FBZSxrQkFBZixHQUFvQ2pCLEtBQUtDLE1BQUwsQ0FBWVMsS0FBaEQsR0FBd0QsSUFEMUQ7QUFFRDtBQUNGO0FBQ0YsT0FyQkQ7QUFzQkQ7O0FBRUQsV0FBTztBQUNMLDJCQUFxQlosZ0JBQWdCNkIsSUFBaEIsQ0FBc0IsSUFBdEIsRUFDc0IsVUFEdEIsRUFFc0IsaUJBRnRCLENBRGhCOztBQU1MLGdDQUEwQjdCLGdCQUFnQjZCLElBQWhCLENBQXNCLElBQXRCLEVBQ3NCLE9BRHRCLEVBRXNCLGlCQUZ0QjtBQU5yQixLQUFQO0FBWUQ7QUFsRWMsQ0FBakIiLCJmaWxlIjoibmFtZWQuanMiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgKiBhcyBwYXRoIGZyb20gJ3BhdGgnXG5pbXBvcnQgRXhwb3J0cyBmcm9tICcuLi9FeHBvcnRNYXAnXG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJ1xuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdwcm9ibGVtJyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ25hbWVkJyksXG4gICAgfSxcbiAgICBzY2hlbWE6IFtdLFxuICB9LFxuXG4gIGNyZWF0ZTogZnVuY3Rpb24gKGNvbnRleHQpIHtcbiAgICBmdW5jdGlvbiBjaGVja1NwZWNpZmllcnMoa2V5LCB0eXBlLCBub2RlKSB7XG4gICAgICAvLyBpZ25vcmUgbG9jYWwgZXhwb3J0cyBhbmQgdHlwZSBpbXBvcnRzL2V4cG9ydHNcbiAgICAgIGlmIChub2RlLnNvdXJjZSA9PSBudWxsIHx8IG5vZGUuaW1wb3J0S2luZCA9PT0gJ3R5cGUnIHx8XG4gICAgICAgICAgbm9kZS5pbXBvcnRLaW5kID09PSAndHlwZW9mJyAgfHwgbm9kZS5leHBvcnRLaW5kID09PSAndHlwZScpIHtcbiAgICAgICAgcmV0dXJuXG4gICAgICB9XG5cbiAgICAgIGlmICghbm9kZS5zcGVjaWZpZXJzXG4gICAgICAgICAgICAuc29tZShmdW5jdGlvbiAoaW0pIHsgcmV0dXJuIGltLnR5cGUgPT09IHR5cGUgfSkpIHtcbiAgICAgICAgcmV0dXJuIC8vIG5vIG5hbWVkIGltcG9ydHMvZXhwb3J0c1xuICAgICAgfVxuXG4gICAgICBjb25zdCBpbXBvcnRzID0gRXhwb3J0cy5nZXQobm9kZS5zb3VyY2UudmFsdWUsIGNvbnRleHQpXG4gICAgICBpZiAoaW1wb3J0cyA9PSBudWxsKSByZXR1cm5cblxuICAgICAgaWYgKGltcG9ydHMuZXJyb3JzLmxlbmd0aCkge1xuICAgICAgICBpbXBvcnRzLnJlcG9ydEVycm9ycyhjb250ZXh0LCBub2RlKVxuICAgICAgICByZXR1cm5cbiAgICAgIH1cblxuICAgICAgbm9kZS5zcGVjaWZpZXJzLmZvckVhY2goZnVuY3Rpb24gKGltKSB7XG4gICAgICAgIGlmIChpbS50eXBlICE9PSB0eXBlKSByZXR1cm5cblxuICAgICAgICAvLyBpZ25vcmUgdHlwZSBpbXBvcnRzXG4gICAgICAgIGlmIChpbS5pbXBvcnRLaW5kID09PSAndHlwZScgfHwgaW0uaW1wb3J0S2luZCA9PT0gJ3R5cGVvZicpIHJldHVyblxuXG4gICAgICAgIGNvbnN0IGRlZXBMb29rdXAgPSBpbXBvcnRzLmhhc0RlZXAoaW1ba2V5XS5uYW1lKVxuXG4gICAgICAgIGlmICghZGVlcExvb2t1cC5mb3VuZCkge1xuICAgICAgICAgIGlmIChkZWVwTG9va3VwLnBhdGgubGVuZ3RoID4gMSkge1xuICAgICAgICAgICAgY29uc3QgZGVlcFBhdGggPSBkZWVwTG9va3VwLnBhdGhcbiAgICAgICAgICAgICAgLm1hcChpID0+IHBhdGgucmVsYXRpdmUocGF0aC5kaXJuYW1lKGNvbnRleHQuZ2V0RmlsZW5hbWUoKSksIGkucGF0aCkpXG4gICAgICAgICAgICAgIC5qb2luKCcgLT4gJylcblxuICAgICAgICAgICAgY29udGV4dC5yZXBvcnQoaW1ba2V5XSxcbiAgICAgICAgICAgICAgYCR7aW1ba2V5XS5uYW1lfSBub3QgZm91bmQgdmlhICR7ZGVlcFBhdGh9YClcbiAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgY29udGV4dC5yZXBvcnQoaW1ba2V5XSxcbiAgICAgICAgICAgICAgaW1ba2V5XS5uYW1lICsgJyBub3QgZm91bmQgaW4gXFwnJyArIG5vZGUuc291cmNlLnZhbHVlICsgJ1xcJycpXG4gICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICB9KVxuICAgIH1cblxuICAgIHJldHVybiB7XG4gICAgICAnSW1wb3J0RGVjbGFyYXRpb24nOiBjaGVja1NwZWNpZmllcnMuYmluZCggbnVsbFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAsICdpbXBvcnRlZCdcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLCAnSW1wb3J0U3BlY2lmaWVyJ1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICApLFxuXG4gICAgICAnRXhwb3J0TmFtZWREZWNsYXJhdGlvbic6IGNoZWNrU3BlY2lmaWVycy5iaW5kKCBudWxsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLCAnbG9jYWwnXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLCAnRXhwb3J0U3BlY2lmaWVyJ1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICksXG4gICAgfVxuXG4gIH0sXG59XG4iXX0=