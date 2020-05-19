'use strict';

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('prefer-default-export')
    },
    schema: []
  },

  create: function (context) {
    let specifierExportCount = 0;
    let hasDefaultExport = false;
    let hasStarExport = false;
    let hasTypeExport = false;
    let namedExportNode = null;

    function captureDeclaration(identifierOrPattern) {
      if (identifierOrPattern.type === 'ObjectPattern') {
        // recursively capture
        identifierOrPattern.properties.forEach(function (property) {
          captureDeclaration(property.value);
        });
      } else if (identifierOrPattern.type === 'ArrayPattern') {
        identifierOrPattern.elements.forEach(captureDeclaration);
      } else {
        // assume it's a single standard identifier
        specifierExportCount++;
      }
    }

    return {
      'ExportDefaultSpecifier': function () {
        hasDefaultExport = true;
      },

      'ExportSpecifier': function (node) {
        if (node.exported.name === 'default') {
          hasDefaultExport = true;
        } else {
          specifierExportCount++;
          namedExportNode = node;
        }
      },

      'ExportNamedDeclaration': function (node) {
        // if there are specifiers, node.declaration should be null
        if (!node.declaration) return;

        const type = node.declaration.type;


        if (type === 'TSTypeAliasDeclaration' || type === 'TypeAlias' || type === 'TSInterfaceDeclaration' || type === 'InterfaceDeclaration') {
          specifierExportCount++;
          hasTypeExport = true;
          return;
        }

        if (node.declaration.declarations) {
          node.declaration.declarations.forEach(function (declaration) {
            captureDeclaration(declaration.id);
          });
        } else {
          // captures 'export function foo() {}' syntax
          specifierExportCount++;
        }

        namedExportNode = node;
      },

      'ExportDefaultDeclaration': function () {
        hasDefaultExport = true;
      },

      'ExportAllDeclaration': function () {
        hasStarExport = true;
      },

      'Program:exit': function () {
        if (specifierExportCount === 1 && !hasDefaultExport && !hasStarExport && !hasTypeExport) {
          context.report(namedExportNode, 'Prefer default export.');
        }
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9wcmVmZXItZGVmYXVsdC1leHBvcnQuanMiXSwibmFtZXMiOlsibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsInNjaGVtYSIsImNyZWF0ZSIsImNvbnRleHQiLCJzcGVjaWZpZXJFeHBvcnRDb3VudCIsImhhc0RlZmF1bHRFeHBvcnQiLCJoYXNTdGFyRXhwb3J0IiwiaGFzVHlwZUV4cG9ydCIsIm5hbWVkRXhwb3J0Tm9kZSIsImNhcHR1cmVEZWNsYXJhdGlvbiIsImlkZW50aWZpZXJPclBhdHRlcm4iLCJwcm9wZXJ0aWVzIiwiZm9yRWFjaCIsInByb3BlcnR5IiwidmFsdWUiLCJlbGVtZW50cyIsIm5vZGUiLCJleHBvcnRlZCIsIm5hbWUiLCJkZWNsYXJhdGlvbiIsImRlY2xhcmF0aW9ucyIsImlkIiwicmVwb3J0Il0sIm1hcHBpbmdzIjoiQUFBQTs7QUFFQTs7Ozs7O0FBRUFBLE9BQU9DLE9BQVAsR0FBaUI7QUFDZkMsUUFBTTtBQUNKQyxVQUFNLFlBREY7QUFFSkMsVUFBTTtBQUNKQyxXQUFLLHVCQUFRLHVCQUFSO0FBREQsS0FGRjtBQUtKQyxZQUFRO0FBTEosR0FEUzs7QUFTZkMsVUFBUSxVQUFTQyxPQUFULEVBQWtCO0FBQ3hCLFFBQUlDLHVCQUF1QixDQUEzQjtBQUNBLFFBQUlDLG1CQUFtQixLQUF2QjtBQUNBLFFBQUlDLGdCQUFnQixLQUFwQjtBQUNBLFFBQUlDLGdCQUFnQixLQUFwQjtBQUNBLFFBQUlDLGtCQUFrQixJQUF0Qjs7QUFFQSxhQUFTQyxrQkFBVCxDQUE0QkMsbUJBQTVCLEVBQWlEO0FBQy9DLFVBQUlBLG9CQUFvQlosSUFBcEIsS0FBNkIsZUFBakMsRUFBa0Q7QUFDaEQ7QUFDQVksNEJBQW9CQyxVQUFwQixDQUNHQyxPQURILENBQ1csVUFBU0MsUUFBVCxFQUFtQjtBQUMxQkosNkJBQW1CSSxTQUFTQyxLQUE1QjtBQUNELFNBSEg7QUFJRCxPQU5ELE1BTU8sSUFBSUosb0JBQW9CWixJQUFwQixLQUE2QixjQUFqQyxFQUFpRDtBQUN0RFksNEJBQW9CSyxRQUFwQixDQUNHSCxPQURILENBQ1dILGtCQURYO0FBRUQsT0FITSxNQUdDO0FBQ1I7QUFDRUw7QUFDRDtBQUNGOztBQUVELFdBQU87QUFDTCxnQ0FBMEIsWUFBVztBQUNuQ0MsMkJBQW1CLElBQW5CO0FBQ0QsT0FISTs7QUFLTCx5QkFBbUIsVUFBU1csSUFBVCxFQUFlO0FBQ2hDLFlBQUlBLEtBQUtDLFFBQUwsQ0FBY0MsSUFBZCxLQUF1QixTQUEzQixFQUFzQztBQUNwQ2IsNkJBQW1CLElBQW5CO0FBQ0QsU0FGRCxNQUVPO0FBQ0xEO0FBQ0FJLDRCQUFrQlEsSUFBbEI7QUFDRDtBQUNGLE9BWkk7O0FBY0wsZ0NBQTBCLFVBQVNBLElBQVQsRUFBZTtBQUN2QztBQUNBLFlBQUksQ0FBQ0EsS0FBS0csV0FBVixFQUF1Qjs7QUFGZ0IsY0FJL0JyQixJQUorQixHQUl0QmtCLEtBQUtHLFdBSmlCLENBSS9CckIsSUFKK0I7OztBQU12QyxZQUNFQSxTQUFTLHdCQUFULElBQ0FBLFNBQVMsV0FEVCxJQUVBQSxTQUFTLHdCQUZULElBR0FBLFNBQVMsc0JBSlgsRUFLRTtBQUNBTTtBQUNBRywwQkFBZ0IsSUFBaEI7QUFDQTtBQUNEOztBQUVELFlBQUlTLEtBQUtHLFdBQUwsQ0FBaUJDLFlBQXJCLEVBQW1DO0FBQ2pDSixlQUFLRyxXQUFMLENBQWlCQyxZQUFqQixDQUE4QlIsT0FBOUIsQ0FBc0MsVUFBU08sV0FBVCxFQUFzQjtBQUMxRFYsK0JBQW1CVSxZQUFZRSxFQUEvQjtBQUNELFdBRkQ7QUFHRCxTQUpELE1BS0s7QUFDSDtBQUNBakI7QUFDRDs7QUFFREksMEJBQWtCUSxJQUFsQjtBQUNELE9BMUNJOztBQTRDTCxrQ0FBNEIsWUFBVztBQUNyQ1gsMkJBQW1CLElBQW5CO0FBQ0QsT0E5Q0k7O0FBZ0RMLDhCQUF3QixZQUFXO0FBQ2pDQyx3QkFBZ0IsSUFBaEI7QUFDRCxPQWxESTs7QUFvREwsc0JBQWdCLFlBQVc7QUFDekIsWUFBSUYseUJBQXlCLENBQXpCLElBQThCLENBQUNDLGdCQUEvQixJQUFtRCxDQUFDQyxhQUFwRCxJQUFxRSxDQUFDQyxhQUExRSxFQUF5RjtBQUN2Rkosa0JBQVFtQixNQUFSLENBQWVkLGVBQWYsRUFBZ0Msd0JBQWhDO0FBQ0Q7QUFDRjtBQXhESSxLQUFQO0FBMEREO0FBMUZjLENBQWpCIiwiZmlsZSI6InByZWZlci1kZWZhdWx0LWV4cG9ydC5qcyIsInNvdXJjZXNDb250ZW50IjpbIid1c2Ugc3RyaWN0J1xuXG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJ1xuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdzdWdnZXN0aW9uJyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ3ByZWZlci1kZWZhdWx0LWV4cG9ydCcpLFxuICAgIH0sXG4gICAgc2NoZW1hOiBbXSxcbiAgfSxcblxuICBjcmVhdGU6IGZ1bmN0aW9uKGNvbnRleHQpIHtcbiAgICBsZXQgc3BlY2lmaWVyRXhwb3J0Q291bnQgPSAwXG4gICAgbGV0IGhhc0RlZmF1bHRFeHBvcnQgPSBmYWxzZVxuICAgIGxldCBoYXNTdGFyRXhwb3J0ID0gZmFsc2VcbiAgICBsZXQgaGFzVHlwZUV4cG9ydCA9IGZhbHNlXG4gICAgbGV0IG5hbWVkRXhwb3J0Tm9kZSA9IG51bGxcblxuICAgIGZ1bmN0aW9uIGNhcHR1cmVEZWNsYXJhdGlvbihpZGVudGlmaWVyT3JQYXR0ZXJuKSB7XG4gICAgICBpZiAoaWRlbnRpZmllck9yUGF0dGVybi50eXBlID09PSAnT2JqZWN0UGF0dGVybicpIHtcbiAgICAgICAgLy8gcmVjdXJzaXZlbHkgY2FwdHVyZVxuICAgICAgICBpZGVudGlmaWVyT3JQYXR0ZXJuLnByb3BlcnRpZXNcbiAgICAgICAgICAuZm9yRWFjaChmdW5jdGlvbihwcm9wZXJ0eSkge1xuICAgICAgICAgICAgY2FwdHVyZURlY2xhcmF0aW9uKHByb3BlcnR5LnZhbHVlKVxuICAgICAgICAgIH0pXG4gICAgICB9IGVsc2UgaWYgKGlkZW50aWZpZXJPclBhdHRlcm4udHlwZSA9PT0gJ0FycmF5UGF0dGVybicpIHtcbiAgICAgICAgaWRlbnRpZmllck9yUGF0dGVybi5lbGVtZW50c1xuICAgICAgICAgIC5mb3JFYWNoKGNhcHR1cmVEZWNsYXJhdGlvbilcbiAgICAgIH0gZWxzZSAge1xuICAgICAgLy8gYXNzdW1lIGl0J3MgYSBzaW5nbGUgc3RhbmRhcmQgaWRlbnRpZmllclxuICAgICAgICBzcGVjaWZpZXJFeHBvcnRDb3VudCsrXG4gICAgICB9XG4gICAgfVxuXG4gICAgcmV0dXJuIHtcbiAgICAgICdFeHBvcnREZWZhdWx0U3BlY2lmaWVyJzogZnVuY3Rpb24oKSB7XG4gICAgICAgIGhhc0RlZmF1bHRFeHBvcnQgPSB0cnVlXG4gICAgICB9LFxuXG4gICAgICAnRXhwb3J0U3BlY2lmaWVyJzogZnVuY3Rpb24obm9kZSkge1xuICAgICAgICBpZiAobm9kZS5leHBvcnRlZC5uYW1lID09PSAnZGVmYXVsdCcpIHtcbiAgICAgICAgICBoYXNEZWZhdWx0RXhwb3J0ID0gdHJ1ZVxuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgIHNwZWNpZmllckV4cG9ydENvdW50KytcbiAgICAgICAgICBuYW1lZEV4cG9ydE5vZGUgPSBub2RlXG4gICAgICAgIH1cbiAgICAgIH0sXG5cbiAgICAgICdFeHBvcnROYW1lZERlY2xhcmF0aW9uJzogZnVuY3Rpb24obm9kZSkge1xuICAgICAgICAvLyBpZiB0aGVyZSBhcmUgc3BlY2lmaWVycywgbm9kZS5kZWNsYXJhdGlvbiBzaG91bGQgYmUgbnVsbFxuICAgICAgICBpZiAoIW5vZGUuZGVjbGFyYXRpb24pIHJldHVyblxuXG4gICAgICAgIGNvbnN0IHsgdHlwZSB9ID0gbm9kZS5kZWNsYXJhdGlvblxuXG4gICAgICAgIGlmIChcbiAgICAgICAgICB0eXBlID09PSAnVFNUeXBlQWxpYXNEZWNsYXJhdGlvbicgfHxcbiAgICAgICAgICB0eXBlID09PSAnVHlwZUFsaWFzJyB8fFxuICAgICAgICAgIHR5cGUgPT09ICdUU0ludGVyZmFjZURlY2xhcmF0aW9uJyB8fFxuICAgICAgICAgIHR5cGUgPT09ICdJbnRlcmZhY2VEZWNsYXJhdGlvbidcbiAgICAgICAgKSB7XG4gICAgICAgICAgc3BlY2lmaWVyRXhwb3J0Q291bnQrK1xuICAgICAgICAgIGhhc1R5cGVFeHBvcnQgPSB0cnVlXG4gICAgICAgICAgcmV0dXJuXG4gICAgICAgIH1cblxuICAgICAgICBpZiAobm9kZS5kZWNsYXJhdGlvbi5kZWNsYXJhdGlvbnMpIHtcbiAgICAgICAgICBub2RlLmRlY2xhcmF0aW9uLmRlY2xhcmF0aW9ucy5mb3JFYWNoKGZ1bmN0aW9uKGRlY2xhcmF0aW9uKSB7XG4gICAgICAgICAgICBjYXB0dXJlRGVjbGFyYXRpb24oZGVjbGFyYXRpb24uaWQpXG4gICAgICAgICAgfSlcbiAgICAgICAgfVxuICAgICAgICBlbHNlIHtcbiAgICAgICAgICAvLyBjYXB0dXJlcyAnZXhwb3J0IGZ1bmN0aW9uIGZvbygpIHt9JyBzeW50YXhcbiAgICAgICAgICBzcGVjaWZpZXJFeHBvcnRDb3VudCsrXG4gICAgICAgIH1cblxuICAgICAgICBuYW1lZEV4cG9ydE5vZGUgPSBub2RlXG4gICAgICB9LFxuXG4gICAgICAnRXhwb3J0RGVmYXVsdERlY2xhcmF0aW9uJzogZnVuY3Rpb24oKSB7XG4gICAgICAgIGhhc0RlZmF1bHRFeHBvcnQgPSB0cnVlXG4gICAgICB9LFxuXG4gICAgICAnRXhwb3J0QWxsRGVjbGFyYXRpb24nOiBmdW5jdGlvbigpIHtcbiAgICAgICAgaGFzU3RhckV4cG9ydCA9IHRydWVcbiAgICAgIH0sXG5cbiAgICAgICdQcm9ncmFtOmV4aXQnOiBmdW5jdGlvbigpIHtcbiAgICAgICAgaWYgKHNwZWNpZmllckV4cG9ydENvdW50ID09PSAxICYmICFoYXNEZWZhdWx0RXhwb3J0ICYmICFoYXNTdGFyRXhwb3J0ICYmICFoYXNUeXBlRXhwb3J0KSB7XG4gICAgICAgICAgY29udGV4dC5yZXBvcnQobmFtZWRFeHBvcnROb2RlLCAnUHJlZmVyIGRlZmF1bHQgZXhwb3J0LicpXG4gICAgICAgIH1cbiAgICAgIH0sXG4gICAgfVxuICB9LFxufVxuIl19