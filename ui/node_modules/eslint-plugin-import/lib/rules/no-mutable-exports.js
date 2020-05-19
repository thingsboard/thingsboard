'use strict';

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('no-mutable-exports')
    },
    schema: []
  },

  create: function (context) {
    function checkDeclaration(node) {
      const kind = node.kind;

      if (kind === 'var' || kind === 'let') {
        context.report(node, `Exporting mutable '${kind}' binding, use 'const' instead.`);
      }
    }

    function checkDeclarationsInScope(_ref, name) {
      let variables = _ref.variables;

      for (let variable of variables) {
        if (variable.name === name) {
          for (let def of variable.defs) {
            if (def.type === 'Variable' && def.parent) {
              checkDeclaration(def.parent);
            }
          }
        }
      }
    }

    function handleExportDefault(node) {
      const scope = context.getScope();

      if (node.declaration.name) {
        checkDeclarationsInScope(scope, node.declaration.name);
      }
    }

    function handleExportNamed(node) {
      const scope = context.getScope();

      if (node.declaration) {
        checkDeclaration(node.declaration);
      } else if (!node.source) {
        for (let specifier of node.specifiers) {
          checkDeclarationsInScope(scope, specifier.local.name);
        }
      }
    }

    return {
      'ExportDefaultDeclaration': handleExportDefault,
      'ExportNamedDeclaration': handleExportNamed
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1tdXRhYmxlLWV4cG9ydHMuanMiXSwibmFtZXMiOlsibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsInNjaGVtYSIsImNyZWF0ZSIsImNvbnRleHQiLCJjaGVja0RlY2xhcmF0aW9uIiwibm9kZSIsImtpbmQiLCJyZXBvcnQiLCJjaGVja0RlY2xhcmF0aW9uc0luU2NvcGUiLCJuYW1lIiwidmFyaWFibGVzIiwidmFyaWFibGUiLCJkZWYiLCJkZWZzIiwicGFyZW50IiwiaGFuZGxlRXhwb3J0RGVmYXVsdCIsInNjb3BlIiwiZ2V0U2NvcGUiLCJkZWNsYXJhdGlvbiIsImhhbmRsZUV4cG9ydE5hbWVkIiwic291cmNlIiwic3BlY2lmaWVyIiwic3BlY2lmaWVycyIsImxvY2FsIl0sIm1hcHBpbmdzIjoiOztBQUFBOzs7Ozs7QUFFQUEsT0FBT0MsT0FBUCxHQUFpQjtBQUNmQyxRQUFNO0FBQ0pDLFVBQU0sWUFERjtBQUVKQyxVQUFNO0FBQ0pDLFdBQUssdUJBQVEsb0JBQVI7QUFERCxLQUZGO0FBS0pDLFlBQVE7QUFMSixHQURTOztBQVNmQyxVQUFRLFVBQVVDLE9BQVYsRUFBbUI7QUFDekIsYUFBU0MsZ0JBQVQsQ0FBMEJDLElBQTFCLEVBQWdDO0FBQUEsWUFDdkJDLElBRHVCLEdBQ2ZELElBRGUsQ0FDdkJDLElBRHVCOztBQUU5QixVQUFJQSxTQUFTLEtBQVQsSUFBa0JBLFNBQVMsS0FBL0IsRUFBc0M7QUFDcENILGdCQUFRSSxNQUFSLENBQWVGLElBQWYsRUFBc0Isc0JBQXFCQyxJQUFLLGlDQUFoRDtBQUNEO0FBQ0Y7O0FBRUQsYUFBU0Usd0JBQVQsT0FBK0NDLElBQS9DLEVBQXFEO0FBQUEsVUFBbEJDLFNBQWtCLFFBQWxCQSxTQUFrQjs7QUFDbkQsV0FBSyxJQUFJQyxRQUFULElBQXFCRCxTQUFyQixFQUFnQztBQUM5QixZQUFJQyxTQUFTRixJQUFULEtBQWtCQSxJQUF0QixFQUE0QjtBQUMxQixlQUFLLElBQUlHLEdBQVQsSUFBZ0JELFNBQVNFLElBQXpCLEVBQStCO0FBQzdCLGdCQUFJRCxJQUFJZCxJQUFKLEtBQWEsVUFBYixJQUEyQmMsSUFBSUUsTUFBbkMsRUFBMkM7QUFDekNWLCtCQUFpQlEsSUFBSUUsTUFBckI7QUFDRDtBQUNGO0FBQ0Y7QUFDRjtBQUNGOztBQUVELGFBQVNDLG1CQUFULENBQTZCVixJQUE3QixFQUFtQztBQUNqQyxZQUFNVyxRQUFRYixRQUFRYyxRQUFSLEVBQWQ7O0FBRUEsVUFBSVosS0FBS2EsV0FBTCxDQUFpQlQsSUFBckIsRUFBMkI7QUFDekJELGlDQUF5QlEsS0FBekIsRUFBZ0NYLEtBQUthLFdBQUwsQ0FBaUJULElBQWpEO0FBQ0Q7QUFDRjs7QUFFRCxhQUFTVSxpQkFBVCxDQUEyQmQsSUFBM0IsRUFBaUM7QUFDL0IsWUFBTVcsUUFBUWIsUUFBUWMsUUFBUixFQUFkOztBQUVBLFVBQUlaLEtBQUthLFdBQVQsRUFBdUI7QUFDckJkLHlCQUFpQkMsS0FBS2EsV0FBdEI7QUFDRCxPQUZELE1BRU8sSUFBSSxDQUFDYixLQUFLZSxNQUFWLEVBQWtCO0FBQ3ZCLGFBQUssSUFBSUMsU0FBVCxJQUFzQmhCLEtBQUtpQixVQUEzQixFQUF1QztBQUNyQ2QsbUNBQXlCUSxLQUF6QixFQUFnQ0ssVUFBVUUsS0FBVixDQUFnQmQsSUFBaEQ7QUFDRDtBQUNGO0FBQ0Y7O0FBRUQsV0FBTztBQUNMLGtDQUE0Qk0sbUJBRHZCO0FBRUwsZ0NBQTBCSTtBQUZyQixLQUFQO0FBSUQ7QUFyRGMsQ0FBakIiLCJmaWxlIjoibm8tbXV0YWJsZS1leHBvcnRzLmpzIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IGRvY3NVcmwgZnJvbSAnLi4vZG9jc1VybCdcblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAnc3VnZ2VzdGlvbicsXG4gICAgZG9jczoge1xuICAgICAgdXJsOiBkb2NzVXJsKCduby1tdXRhYmxlLWV4cG9ydHMnKSxcbiAgICB9LFxuICAgIHNjaGVtYTogW10sXG4gIH0sXG5cbiAgY3JlYXRlOiBmdW5jdGlvbiAoY29udGV4dCkge1xuICAgIGZ1bmN0aW9uIGNoZWNrRGVjbGFyYXRpb24obm9kZSkge1xuICAgICAgY29uc3Qge2tpbmR9ID0gbm9kZVxuICAgICAgaWYgKGtpbmQgPT09ICd2YXInIHx8IGtpbmQgPT09ICdsZXQnKSB7XG4gICAgICAgIGNvbnRleHQucmVwb3J0KG5vZGUsIGBFeHBvcnRpbmcgbXV0YWJsZSAnJHtraW5kfScgYmluZGluZywgdXNlICdjb25zdCcgaW5zdGVhZC5gKVxuICAgICAgfVxuICAgIH1cblxuICAgIGZ1bmN0aW9uIGNoZWNrRGVjbGFyYXRpb25zSW5TY29wZSh7dmFyaWFibGVzfSwgbmFtZSkge1xuICAgICAgZm9yIChsZXQgdmFyaWFibGUgb2YgdmFyaWFibGVzKSB7XG4gICAgICAgIGlmICh2YXJpYWJsZS5uYW1lID09PSBuYW1lKSB7XG4gICAgICAgICAgZm9yIChsZXQgZGVmIG9mIHZhcmlhYmxlLmRlZnMpIHtcbiAgICAgICAgICAgIGlmIChkZWYudHlwZSA9PT0gJ1ZhcmlhYmxlJyAmJiBkZWYucGFyZW50KSB7XG4gICAgICAgICAgICAgIGNoZWNrRGVjbGFyYXRpb24oZGVmLnBhcmVudClcbiAgICAgICAgICAgIH1cbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgIH1cbiAgICB9XG5cbiAgICBmdW5jdGlvbiBoYW5kbGVFeHBvcnREZWZhdWx0KG5vZGUpIHtcbiAgICAgIGNvbnN0IHNjb3BlID0gY29udGV4dC5nZXRTY29wZSgpXG5cbiAgICAgIGlmIChub2RlLmRlY2xhcmF0aW9uLm5hbWUpIHtcbiAgICAgICAgY2hlY2tEZWNsYXJhdGlvbnNJblNjb3BlKHNjb3BlLCBub2RlLmRlY2xhcmF0aW9uLm5hbWUpXG4gICAgICB9XG4gICAgfVxuXG4gICAgZnVuY3Rpb24gaGFuZGxlRXhwb3J0TmFtZWQobm9kZSkge1xuICAgICAgY29uc3Qgc2NvcGUgPSBjb250ZXh0LmdldFNjb3BlKClcblxuICAgICAgaWYgKG5vZGUuZGVjbGFyYXRpb24pICB7XG4gICAgICAgIGNoZWNrRGVjbGFyYXRpb24obm9kZS5kZWNsYXJhdGlvbilcbiAgICAgIH0gZWxzZSBpZiAoIW5vZGUuc291cmNlKSB7XG4gICAgICAgIGZvciAobGV0IHNwZWNpZmllciBvZiBub2RlLnNwZWNpZmllcnMpIHtcbiAgICAgICAgICBjaGVja0RlY2xhcmF0aW9uc0luU2NvcGUoc2NvcGUsIHNwZWNpZmllci5sb2NhbC5uYW1lKVxuICAgICAgICB9XG4gICAgICB9XG4gICAgfVxuXG4gICAgcmV0dXJuIHtcbiAgICAgICdFeHBvcnREZWZhdWx0RGVjbGFyYXRpb24nOiBoYW5kbGVFeHBvcnREZWZhdWx0LFxuICAgICAgJ0V4cG9ydE5hbWVkRGVjbGFyYXRpb24nOiBoYW5kbGVFeHBvcnROYW1lZCxcbiAgICB9XG4gIH0sXG59XG4iXX0=