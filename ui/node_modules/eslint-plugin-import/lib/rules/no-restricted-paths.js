'use strict';

var _slicedToArray = function () { function sliceIterator(arr, i) { var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i["return"]) _i["return"](); } finally { if (_d) throw _e; } } return _arr; } return function (arr, i) { if (Array.isArray(arr)) { return arr; } else if (Symbol.iterator in Object(arr)) { return sliceIterator(arr, i); } else { throw new TypeError("Invalid attempt to destructure non-iterable instance"); } }; }();

var _containsPath = require('contains-path');

var _containsPath2 = _interopRequireDefault(_containsPath);

var _path = require('path');

var _path2 = _interopRequireDefault(_path);

var _resolve = require('eslint-module-utils/resolve');

var _resolve2 = _interopRequireDefault(_resolve);

var _staticRequire = require('../core/staticRequire');

var _staticRequire2 = _interopRequireDefault(_staticRequire);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

var _importType = require('../core/importType');

var _importType2 = _interopRequireDefault(_importType);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2.default)('no-restricted-paths')
    },

    schema: [{
      type: 'object',
      properties: {
        zones: {
          type: 'array',
          minItems: 1,
          items: {
            type: 'object',
            properties: {
              target: { type: 'string' },
              from: { type: 'string' },
              except: {
                type: 'array',
                items: {
                  type: 'string'
                },
                uniqueItems: true
              }
            },
            additionalProperties: false
          }
        },
        basePath: { type: 'string' }
      },
      additionalProperties: false
    }]
  },

  create: function noRestrictedPaths(context) {
    const options = context.options[0] || {};
    const restrictedPaths = options.zones || [];
    const basePath = options.basePath || process.cwd();
    const currentFilename = context.getFilename();
    const matchingZones = restrictedPaths.filter(zone => {
      const targetPath = _path2.default.resolve(basePath, zone.target);

      return (0, _containsPath2.default)(currentFilename, targetPath);
    });

    function isValidExceptionPath(absoluteFromPath, absoluteExceptionPath) {
      const relativeExceptionPath = _path2.default.relative(absoluteFromPath, absoluteExceptionPath);

      return (0, _importType2.default)(relativeExceptionPath, context) !== 'parent';
    }

    function reportInvalidExceptionPath(node) {
      context.report({
        node,
        message: 'Restricted path exceptions must be descendants of the configured `from` path for that zone.'
      });
    }

    function checkForRestrictedImportPath(importPath, node) {
      const absoluteImportPath = (0, _resolve2.default)(importPath, context);

      if (!absoluteImportPath) {
        return;
      }

      matchingZones.forEach(zone => {
        const exceptionPaths = zone.except || [];
        const absoluteFrom = _path2.default.resolve(basePath, zone.from);

        if (!(0, _containsPath2.default)(absoluteImportPath, absoluteFrom)) {
          return;
        }

        const absoluteExceptionPaths = exceptionPaths.map(exceptionPath => _path2.default.resolve(absoluteFrom, exceptionPath));
        const hasValidExceptionPaths = absoluteExceptionPaths.every(absoluteExceptionPath => isValidExceptionPath(absoluteFrom, absoluteExceptionPath));

        if (!hasValidExceptionPaths) {
          reportInvalidExceptionPath(node);
          return;
        }

        const pathIsExcepted = absoluteExceptionPaths.some(absoluteExceptionPath => (0, _containsPath2.default)(absoluteImportPath, absoluteExceptionPath));

        if (pathIsExcepted) {
          return;
        }

        context.report({
          node,
          message: `Unexpected path "{{importPath}}" imported in restricted zone.`,
          data: { importPath }
        });
      });
    }

    return {
      ImportDeclaration(node) {
        checkForRestrictedImportPath(node.source.value, node.source);
      },
      CallExpression(node) {
        if ((0, _staticRequire2.default)(node)) {
          var _node$arguments = _slicedToArray(node.arguments, 1);

          const firstArgument = _node$arguments[0];


          checkForRestrictedImportPath(firstArgument.value, firstArgument);
        }
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1yZXN0cmljdGVkLXBhdGhzLmpzIl0sIm5hbWVzIjpbIm1vZHVsZSIsImV4cG9ydHMiLCJtZXRhIiwidHlwZSIsImRvY3MiLCJ1cmwiLCJzY2hlbWEiLCJwcm9wZXJ0aWVzIiwiem9uZXMiLCJtaW5JdGVtcyIsIml0ZW1zIiwidGFyZ2V0IiwiZnJvbSIsImV4Y2VwdCIsInVuaXF1ZUl0ZW1zIiwiYWRkaXRpb25hbFByb3BlcnRpZXMiLCJiYXNlUGF0aCIsImNyZWF0ZSIsIm5vUmVzdHJpY3RlZFBhdGhzIiwiY29udGV4dCIsIm9wdGlvbnMiLCJyZXN0cmljdGVkUGF0aHMiLCJwcm9jZXNzIiwiY3dkIiwiY3VycmVudEZpbGVuYW1lIiwiZ2V0RmlsZW5hbWUiLCJtYXRjaGluZ1pvbmVzIiwiZmlsdGVyIiwiem9uZSIsInRhcmdldFBhdGgiLCJwYXRoIiwicmVzb2x2ZSIsImlzVmFsaWRFeGNlcHRpb25QYXRoIiwiYWJzb2x1dGVGcm9tUGF0aCIsImFic29sdXRlRXhjZXB0aW9uUGF0aCIsInJlbGF0aXZlRXhjZXB0aW9uUGF0aCIsInJlbGF0aXZlIiwicmVwb3J0SW52YWxpZEV4Y2VwdGlvblBhdGgiLCJub2RlIiwicmVwb3J0IiwibWVzc2FnZSIsImNoZWNrRm9yUmVzdHJpY3RlZEltcG9ydFBhdGgiLCJpbXBvcnRQYXRoIiwiYWJzb2x1dGVJbXBvcnRQYXRoIiwiZm9yRWFjaCIsImV4Y2VwdGlvblBhdGhzIiwiYWJzb2x1dGVGcm9tIiwiYWJzb2x1dGVFeGNlcHRpb25QYXRocyIsIm1hcCIsImV4Y2VwdGlvblBhdGgiLCJoYXNWYWxpZEV4Y2VwdGlvblBhdGhzIiwiZXZlcnkiLCJwYXRoSXNFeGNlcHRlZCIsInNvbWUiLCJkYXRhIiwiSW1wb3J0RGVjbGFyYXRpb24iLCJzb3VyY2UiLCJ2YWx1ZSIsIkNhbGxFeHByZXNzaW9uIiwiYXJndW1lbnRzIiwiZmlyc3RBcmd1bWVudCJdLCJtYXBwaW5ncyI6Ijs7OztBQUFBOzs7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O0FBQ0E7QUFDRDs7QUFFRCxjQUFNYyxpQkFBaUJMLHVCQUNwQk0sSUFEb0IsQ0FDZG5CLHFCQUFELElBQTJCLDRCQUFhUyxrQkFBYixFQUFpQ1QscUJBQWpDLENBRFosQ0FBdkI7O0FBR0EsWUFBSWtCLGNBQUosRUFBb0I7QUFDbEI7QUFDRDs7QUFFRGpDLGdCQUFRb0IsTUFBUixDQUFlO0FBQ2JELGNBRGE7QUFFYkUsbUJBQVUsK0RBRkc7QUFHYmMsZ0JBQU0sRUFBRVosVUFBRjtBQUhPLFNBQWY7QUFLRCxPQS9CRDtBQWdDSDs7QUFFRCxXQUFPO0FBQ0xhLHdCQUFrQmpCLElBQWxCLEVBQXdCO0FBQ3RCRyxxQ0FBNkJILEtBQUtrQixNQUFMLENBQVlDLEtBQXpDLEVBQWdEbkIsS0FBS2tCLE1BQXJEO0FBQ0QsT0FISTtBQUlMRSxxQkFBZXBCLElBQWYsRUFBcUI7QUFDbkIsWUFBSSw2QkFBZ0JBLElBQWhCLENBQUosRUFBMkI7QUFBQSwrQ0FDQ0EsS0FBS3FCLFNBRE47O0FBQUEsZ0JBQ2pCQyxhQURpQjs7O0FBR3pCbkIsdUNBQTZCbUIsY0FBY0gsS0FBM0MsRUFBa0RHLGFBQWxEO0FBQ0Q7QUFDRjtBQVZJLEtBQVA7QUFZRDtBQWxIYyxDQUFqQiIsImZpbGUiOiJuby1yZXN0cmljdGVkLXBhdGhzLmpzIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IGNvbnRhaW5zUGF0aCBmcm9tICdjb250YWlucy1wYXRoJ1xuaW1wb3J0IHBhdGggZnJvbSAncGF0aCdcblxuaW1wb3J0IHJlc29sdmUgZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9yZXNvbHZlJ1xuaW1wb3J0IGlzU3RhdGljUmVxdWlyZSBmcm9tICcuLi9jb3JlL3N0YXRpY1JlcXVpcmUnXG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJ1xuaW1wb3J0IGltcG9ydFR5cGUgZnJvbSAnLi4vY29yZS9pbXBvcnRUeXBlJ1xuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdwcm9ibGVtJyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ25vLXJlc3RyaWN0ZWQtcGF0aHMnKSxcbiAgICB9LFxuXG4gICAgc2NoZW1hOiBbXG4gICAgICB7XG4gICAgICAgIHR5cGU6ICdvYmplY3QnLFxuICAgICAgICBwcm9wZXJ0aWVzOiB7XG4gICAgICAgICAgem9uZXM6IHtcbiAgICAgICAgICAgIHR5cGU6ICdhcnJheScsXG4gICAgICAgICAgICBtaW5JdGVtczogMSxcbiAgICAgICAgICAgIGl0ZW1zOiB7XG4gICAgICAgICAgICAgIHR5cGU6ICdvYmplY3QnLFxuICAgICAgICAgICAgICBwcm9wZXJ0aWVzOiB7XG4gICAgICAgICAgICAgICAgdGFyZ2V0OiB7IHR5cGU6ICdzdHJpbmcnIH0sXG4gICAgICAgICAgICAgICAgZnJvbTogeyB0eXBlOiAnc3RyaW5nJyB9LFxuICAgICAgICAgICAgICAgIGV4Y2VwdDoge1xuICAgICAgICAgICAgICAgICAgdHlwZTogJ2FycmF5JyxcbiAgICAgICAgICAgICAgICAgIGl0ZW1zOiB7XG4gICAgICAgICAgICAgICAgICAgIHR5cGU6ICdzdHJpbmcnLFxuICAgICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICAgIHVuaXF1ZUl0ZW1zOiB0cnVlLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgIGFkZGl0aW9uYWxQcm9wZXJ0aWVzOiBmYWxzZSxcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgfSxcbiAgICAgICAgICBiYXNlUGF0aDogeyB0eXBlOiAnc3RyaW5nJyB9LFxuICAgICAgICB9LFxuICAgICAgICBhZGRpdGlvbmFsUHJvcGVydGllczogZmFsc2UsXG4gICAgICB9LFxuICAgIF0sXG4gIH0sXG5cbiAgY3JlYXRlOiBmdW5jdGlvbiBub1Jlc3RyaWN0ZWRQYXRocyhjb250ZXh0KSB7XG4gICAgY29uc3Qgb3B0aW9ucyA9IGNvbnRleHQub3B0aW9uc1swXSB8fCB7fVxuICAgIGNvbnN0IHJlc3RyaWN0ZWRQYXRocyA9IG9wdGlvbnMuem9uZXMgfHwgW11cbiAgICBjb25zdCBiYXNlUGF0aCA9IG9wdGlvbnMuYmFzZVBhdGggfHwgcHJvY2Vzcy5jd2QoKVxuICAgIGNvbnN0IGN1cnJlbnRGaWxlbmFtZSA9IGNvbnRleHQuZ2V0RmlsZW5hbWUoKVxuICAgIGNvbnN0IG1hdGNoaW5nWm9uZXMgPSByZXN0cmljdGVkUGF0aHMuZmlsdGVyKCh6b25lKSA9PiB7XG4gICAgICBjb25zdCB0YXJnZXRQYXRoID0gcGF0aC5yZXNvbHZlKGJhc2VQYXRoLCB6b25lLnRhcmdldClcblxuICAgICAgcmV0dXJuIGNvbnRhaW5zUGF0aChjdXJyZW50RmlsZW5hbWUsIHRhcmdldFBhdGgpXG4gICAgfSlcblxuICAgIGZ1bmN0aW9uIGlzVmFsaWRFeGNlcHRpb25QYXRoKGFic29sdXRlRnJvbVBhdGgsIGFic29sdXRlRXhjZXB0aW9uUGF0aCkge1xuICAgICAgY29uc3QgcmVsYXRpdmVFeGNlcHRpb25QYXRoID0gcGF0aC5yZWxhdGl2ZShhYnNvbHV0ZUZyb21QYXRoLCBhYnNvbHV0ZUV4Y2VwdGlvblBhdGgpXG5cbiAgICAgIHJldHVybiBpbXBvcnRUeXBlKHJlbGF0aXZlRXhjZXB0aW9uUGF0aCwgY29udGV4dCkgIT09ICdwYXJlbnQnXG4gICAgfVxuXG4gICAgZnVuY3Rpb24gcmVwb3J0SW52YWxpZEV4Y2VwdGlvblBhdGgobm9kZSkge1xuICAgICAgY29udGV4dC5yZXBvcnQoe1xuICAgICAgICBub2RlLFxuICAgICAgICBtZXNzYWdlOiAnUmVzdHJpY3RlZCBwYXRoIGV4Y2VwdGlvbnMgbXVzdCBiZSBkZXNjZW5kYW50cyBvZiB0aGUgY29uZmlndXJlZCBgZnJvbWAgcGF0aCBmb3IgdGhhdCB6b25lLicsXG4gICAgICB9KVxuICAgIH1cblxuICAgIGZ1bmN0aW9uIGNoZWNrRm9yUmVzdHJpY3RlZEltcG9ydFBhdGgoaW1wb3J0UGF0aCwgbm9kZSkge1xuICAgICAgICBjb25zdCBhYnNvbHV0ZUltcG9ydFBhdGggPSByZXNvbHZlKGltcG9ydFBhdGgsIGNvbnRleHQpXG5cbiAgICAgICAgaWYgKCFhYnNvbHV0ZUltcG9ydFBhdGgpIHtcbiAgICAgICAgICByZXR1cm5cbiAgICAgICAgfVxuXG4gICAgICAgIG1hdGNoaW5nWm9uZXMuZm9yRWFjaCgoem9uZSkgPT4ge1xuICAgICAgICAgIGNvbnN0IGV4Y2VwdGlvblBhdGhzID0gem9uZS5leGNlcHQgfHwgW11cbiAgICAgICAgICBjb25zdCBhYnNvbHV0ZUZyb20gPSBwYXRoLnJlc29sdmUoYmFzZVBhdGgsIHpvbmUuZnJvbSlcblxuICAgICAgICAgIGlmICghY29udGFpbnNQYXRoKGFic29sdXRlSW1wb3J0UGF0aCwgYWJzb2x1dGVGcm9tKSkge1xuICAgICAgICAgICAgcmV0dXJuXG4gICAgICAgICAgfVxuXG4gICAgICAgICAgY29uc3QgYWJzb2x1dGVFeGNlcHRpb25QYXRocyA9IGV4Y2VwdGlvblBhdGhzLm1hcCgoZXhjZXB0aW9uUGF0aCkgPT5cbiAgICAgICAgICAgIHBhdGgucmVzb2x2ZShhYnNvbHV0ZUZyb20sIGV4Y2VwdGlvblBhdGgpXG4gICAgICAgICAgKVxuICAgICAgICAgIGNvbnN0IGhhc1ZhbGlkRXhjZXB0aW9uUGF0aHMgPSBhYnNvbHV0ZUV4Y2VwdGlvblBhdGhzXG4gICAgICAgICAgICAuZXZlcnkoKGFic29sdXRlRXhjZXB0aW9uUGF0aCkgPT4gaXNWYWxpZEV4Y2VwdGlvblBhdGgoYWJzb2x1dGVGcm9tLCBhYnNvbHV0ZUV4Y2VwdGlvblBhdGgpKVxuXG4gICAgICAgICAgaWYgKCFoYXNWYWxpZEV4Y2VwdGlvblBhdGhzKSB7XG4gICAgICAgICAgICByZXBvcnRJbnZhbGlkRXhjZXB0aW9uUGF0aChub2RlKVxuICAgICAgICAgICAgcmV0dXJuXG4gICAgICAgICAgfVxuXG4gICAgICAgICAgY29uc3QgcGF0aElzRXhjZXB0ZWQgPSBhYnNvbHV0ZUV4Y2VwdGlvblBhdGhzXG4gICAgICAgICAgICAuc29tZSgoYWJzb2x1dGVFeGNlcHRpb25QYXRoKSA9PiBjb250YWluc1BhdGgoYWJzb2x1dGVJbXBvcnRQYXRoLCBhYnNvbHV0ZUV4Y2VwdGlvblBhdGgpKVxuXG4gICAgICAgICAgaWYgKHBhdGhJc0V4Y2VwdGVkKSB7XG4gICAgICAgICAgICByZXR1cm5cbiAgICAgICAgICB9XG5cbiAgICAgICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgICAgICBub2RlLFxuICAgICAgICAgICAgbWVzc2FnZTogYFVuZXhwZWN0ZWQgcGF0aCBcInt7aW1wb3J0UGF0aH19XCIgaW1wb3J0ZWQgaW4gcmVzdHJpY3RlZCB6b25lLmAsXG4gICAgICAgICAgICBkYXRhOiB7IGltcG9ydFBhdGggfSxcbiAgICAgICAgICB9KVxuICAgICAgICB9KVxuICAgIH1cblxuICAgIHJldHVybiB7XG4gICAgICBJbXBvcnREZWNsYXJhdGlvbihub2RlKSB7XG4gICAgICAgIGNoZWNrRm9yUmVzdHJpY3RlZEltcG9ydFBhdGgobm9kZS5zb3VyY2UudmFsdWUsIG5vZGUuc291cmNlKVxuICAgICAgfSxcbiAgICAgIENhbGxFeHByZXNzaW9uKG5vZGUpIHtcbiAgICAgICAgaWYgKGlzU3RhdGljUmVxdWlyZShub2RlKSkge1xuICAgICAgICAgIGNvbnN0IFsgZmlyc3RBcmd1bWVudCBdID0gbm9kZS5hcmd1bWVudHNcblxuICAgICAgICAgIGNoZWNrRm9yUmVzdHJpY3RlZEltcG9ydFBhdGgoZmlyc3RBcmd1bWVudC52YWx1ZSwgZmlyc3RBcmd1bWVudClcbiAgICAgICAgfVxuICAgICAgfSxcbiAgICB9XG4gIH0sXG59XG4iXX0=