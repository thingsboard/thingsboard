'use strict';

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

const EXPORT_MESSAGE = 'Expected "export" or "export default"',
      IMPORT_MESSAGE = 'Expected "import" instead of "require()"'; /**
                                                                    * @fileoverview Rule to prefer ES6 to CJS
                                                                    * @author Jamund Ferguson
                                                                    */

function normalizeLegacyOptions(options) {
  if (options.indexOf('allow-primitive-modules') >= 0) {
    return { allowPrimitiveModules: true };
  }
  return options[0] || {};
}

function allowPrimitive(node, options) {
  if (!options.allowPrimitiveModules) return false;
  if (node.parent.type !== 'AssignmentExpression') return false;
  return node.parent.right.type !== 'ObjectExpression';
}

function allowRequire(node, options) {
  return options.allowRequire;
}

function allowConditionalRequire(node, options) {
  return options.allowConditionalRequire !== false;
}

function validateScope(scope) {
  return scope.variableScope.type === 'module';
}

// https://github.com/estree/estree/blob/master/es5.md
function isConditional(node) {
  if (node.type === 'IfStatement' || node.type === 'TryStatement' || node.type === 'LogicalExpression' || node.type === 'ConditionalExpression') return true;
  if (node.parent) return isConditional(node.parent);
  return false;
}

//------------------------------------------------------------------------------
// Rule Definition
//------------------------------------------------------------------------------

const schemaString = { enum: ['allow-primitive-modules'] };
const schemaObject = {
  type: 'object',
  properties: {
    allowPrimitiveModules: { 'type': 'boolean' },
    allowRequire: { 'type': 'boolean' },
    allowConditionalRequire: { 'type': 'boolean' }
  },
  additionalProperties: false
};

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('no-commonjs')
    },

    schema: {
      anyOf: [{
        type: 'array',
        items: [schemaString],
        additionalItems: false
      }, {
        type: 'array',
        items: [schemaObject],
        additionalItems: false
      }]
    }
  },

  create: function (context) {
    const options = normalizeLegacyOptions(context.options);

    return {

      'MemberExpression': function (node) {

        // module.exports
        if (node.object.name === 'module' && node.property.name === 'exports') {
          if (allowPrimitive(node, options)) return;
          context.report({ node, message: EXPORT_MESSAGE });
        }

        // exports.
        if (node.object.name === 'exports') {
          const isInScope = context.getScope().variables.some(variable => variable.name === 'exports');
          if (!isInScope) {
            context.report({ node, message: EXPORT_MESSAGE });
          }
        }
      },
      'CallExpression': function (call) {
        if (!validateScope(context.getScope())) return;

        if (call.callee.type !== 'Identifier') return;
        if (call.callee.name !== 'require') return;

        if (call.arguments.length !== 1) return;
        var module = call.arguments[0];

        if (module.type !== 'Literal') return;
        if (typeof module.value !== 'string') return;

        if (allowRequire(call, options)) return;

        if (allowConditionalRequire(call, options) && isConditional(call.parent)) return;

        // keeping it simple: all 1-string-arg `require` calls are reported
        context.report({
          node: call.callee,
          message: IMPORT_MESSAGE
        });
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1jb21tb25qcy5qcyJdLCJuYW1lcyI6WyJFWFBPUlRfTUVTU0FHRSIsIklNUE9SVF9NRVNTQUdFIiwibm9ybWFsaXplTGVnYWN5T3B0aW9ucyIsIm9wdGlvbnMiLCJpbmRleE9mIiwiYWxsb3dQcmltaXRpdmVNb2R1bGVzIiwiYWxsb3dQcmltaXRpdmUiLCJub2RlIiwicGFyZW50IiwidHlwZSIsInJpZ2h0IiwiYWxsb3dSZXF1aXJlIiwiYWxsb3dDb25kaXRpb25hbFJlcXVpcmUiLCJ2YWxpZGF0ZVNjb3BlIiwic2NvcGUiLCJ2YXJpYWJsZVNjb3BlIiwiaXNDb25kaXRpb25hbCIsInNjaGVtYVN0cmluZyIsImVudW0iLCJzY2hlbWFPYmplY3QiLCJwcm9wZXJ0aWVzIiwiYWRkaXRpb25hbFByb3BlcnRpZXMiLCJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsImRvY3MiLCJ1cmwiLCJzY2hlbWEiLCJhbnlPZiIsIml0ZW1zIiwiYWRkaXRpb25hbEl0ZW1zIiwiY3JlYXRlIiwiY29udGV4dCIsIm9iamVjdCIsIm5hbWUiLCJwcm9wZXJ0eSIsInJlcG9ydCIsIm1lc3NhZ2UiLCJpc0luU2NvcGUiLCJnZXRTY29wZSIsInZhcmlhYmxlcyIsInNvbWUiLCJ2YXJpYWJsZSIsImNhbGwiLCJjYWxsZWUiLCJhcmd1bWVudHMiLCJsZW5ndGgiLCJ2YWx1ZSJdLCJtYXBwaW5ncyI6Ijs7QUFLQTs7Ozs7O0FBRUEsTUFBTUEsaUJBQWlCLHVDQUF2QjtBQUFBLE1BQ01DLGlCQUFpQiwwQ0FEdkIsQyxDQVBBOzs7OztBQVVBLFNBQVNDLHNCQUFULENBQWdDQyxPQUFoQyxFQUF5QztBQUN2QyxNQUFJQSxRQUFRQyxPQUFSLENBQWdCLHlCQUFoQixLQUE4QyxDQUFsRCxFQUFxRDtBQUNuRCxXQUFPLEVBQUVDLHVCQUF1QixJQUF6QixFQUFQO0FBQ0Q7QUFDRCxTQUFPRixRQUFRLENBQVIsS0FBYyxFQUFyQjtBQUNEOztBQUVELFNBQVNHLGNB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Y0FBakIsRUFBZjtBQUNEO0FBQ0Y7QUFFRixPQXBCSTtBQXFCTCx3QkFBa0IsVUFBVTJDLElBQVYsRUFBZ0I7QUFDaEMsWUFBSSxDQUFDOUIsY0FBY21CLFFBQVFPLFFBQVIsRUFBZCxDQUFMLEVBQXdDOztBQUV4QyxZQUFJSSxLQUFLQyxNQUFMLENBQVluQyxJQUFaLEtBQXFCLFlBQXpCLEVBQXVDO0FBQ3ZDLFlBQUlrQyxLQUFLQyxNQUFMLENBQVlWLElBQVosS0FBcUIsU0FBekIsRUFBb0M7O0FBRXBDLFlBQUlTLEtBQUtFLFNBQUwsQ0FBZUMsTUFBZixLQUEwQixDQUE5QixFQUFpQztBQUNqQyxZQUFJeEIsU0FBU3FCLEtBQUtFLFNBQUwsQ0FBZSxDQUFmLENBQWI7O0FBRUEsWUFBSXZCLE9BQU9iLElBQVAsS0FBZ0IsU0FBcEIsRUFBK0I7QUFDL0IsWUFBSSxPQUFPYSxPQUFPeUIsS0FBZCxLQUF3QixRQUE1QixFQUFzQzs7QUFFdEMsWUFBSXBDLGFBQWFnQyxJQUFiLEVBQW1CeEMsT0FBbkIsQ0FBSixFQUFpQzs7QUFFakMsWUFBSVMsd0JBQXdCK0IsSUFBeEIsRUFBOEJ4QyxPQUE5QixLQUEwQ2EsY0FBYzJCLEtBQUtuQyxNQUFuQixDQUE5QyxFQUEwRTs7QUFFMUU7QUFDQXdCLGdCQUFRSSxNQUFSLENBQWU7QUFDYjdCLGdCQUFNb0MsS0FBS0MsTUFERTtBQUViUCxtQkFBU3BDO0FBRkksU0FBZjtBQUlEO0FBMUNJLEtBQVA7QUE2Q0Q7QUF2RWMsQ0FBakIiLCJmaWxlIjoibm8tY29tbW9uanMuanMiLCJzb3VyY2VzQ29udGVudCI6WyIvKipcbiAqIEBmaWxlb3ZlcnZpZXcgUnVsZSB0byBwcmVmZXIgRVM2IHRvIENKU1xuICogQGF1dGhvciBKYW11bmQgRmVyZ3Vzb25cbiAqL1xuXG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJ1xuXG5jb25zdCBFWFBPUlRfTUVTU0FHRSA9ICdFeHBlY3RlZCBcImV4cG9ydFwiIG9yIFwiZXhwb3J0IGRlZmF1bHRcIidcbiAgICAsIElNUE9SVF9NRVNTQUdFID0gJ0V4cGVjdGVkIFwiaW1wb3J0XCIgaW5zdGVhZCBvZiBcInJlcXVpcmUoKVwiJ1xuXG5mdW5jdGlvbiBub3JtYWxpemVMZWdhY3lPcHRpb25zKG9wdGlvbnMpIHtcbiAgaWYgKG9wdGlvbnMuaW5kZXhPZignYWxsb3ctcHJpbWl0aXZlLW1vZHVsZXMnKSA+PSAwKSB7XG4gICAgcmV0dXJuIHsgYWxsb3dQcmltaXRpdmVNb2R1bGVzOiB0cnVlIH1cbiAgfVxuICByZXR1cm4gb3B0aW9uc1swXSB8fCB7fVxufVxuXG5mdW5jdGlvbiBhbGxvd1ByaW1pdGl2ZShub2RlLCBvcHRpb25zKSB7XG4gIGlmICghb3B0aW9ucy5hbGxvd1ByaW1pdGl2ZU1vZHVsZXMpIHJldHVybiBmYWxzZVxuICBpZiAobm9kZS5wYXJlbnQudHlwZSAhPT0gJ0Fzc2lnbm1lbnRFeHByZXNzaW9uJykgcmV0dXJuIGZhbHNlXG4gIHJldHVybiAobm9kZS5wYXJlbnQucmlnaHQudHlwZSAhPT0gJ09iamVjdEV4cHJlc3Npb24nKVxufVxuXG5mdW5jdGlvbiBhbGxvd1JlcXVpcmUobm9kZSwgb3B0aW9ucykge1xuICByZXR1cm4gb3B0aW9ucy5hbGxvd1JlcXVpcmVcbn1cblxuZnVuY3Rpb24gYWxsb3dDb25kaXRpb25hbFJlcXVpcmUobm9kZSwgb3B0aW9ucykge1xuICByZXR1cm4gb3B0aW9ucy5hbGxvd0NvbmRpdGlvbmFsUmVxdWlyZSAhPT0gZmFsc2Vcbn1cblxuZnVuY3Rpb24gdmFsaWRhdGVTY29wZShzY29wZSkge1xuICByZXR1cm4gc2NvcGUudmFyaWFibGVTY29wZS50eXBlID09PSAnbW9kdWxlJ1xufVxuXG4vLyBodHRwczovL2dpdGh1Yi5jb20vZXN0cmVlL2VzdHJlZS9ibG9iL21hc3Rlci9lczUubWRcbmZ1bmN0aW9uIGlzQ29uZGl0aW9uYWwobm9kZSkge1xuICBpZiAoXG4gICAgbm9kZS50eXBlID09PSAnSWZTdGF0ZW1lbnQnXG4gICAgfHwgbm9kZS50eXBlID09PSAnVHJ5U3RhdGVtZW50J1xuICAgIHx8IG5vZGUudHlwZSA9PT0gJ0xvZ2ljYWxFeHByZXNzaW9uJ1xuICAgIHx8IG5vZGUudHlwZSA9PT0gJ0NvbmRpdGlvbmFsRXhwcmVzc2lvbidcbiAgKSByZXR1cm4gdHJ1ZVxuICBpZiAobm9kZS5wYXJlbnQpIHJldHVybiBpc0NvbmRpdGlvbmFsKG5vZGUucGFyZW50KVxuICByZXR1cm4gZmFsc2Vcbn1cblxuLy8tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbi8vIFJ1bGUgRGVmaW5pdGlvblxuLy8tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cblxuY29uc3Qgc2NoZW1hU3RyaW5nID0geyBlbnVtOiBbJ2FsbG93LXByaW1pdGl2ZS1tb2R1bGVzJ10gfVxuY29uc3Qgc2NoZW1hT2JqZWN0ID0ge1xuICB0eXBlOiAnb2JqZWN0JyxcbiAgcHJvcGVydGllczoge1xuICAgIGFsbG93UHJpbWl0aXZlTW9kdWxlczogeyAndHlwZSc6ICdib29sZWFuJyB9LFxuICAgIGFsbG93UmVxdWlyZTogeyAndHlwZSc6ICdib29sZWFuJyB9LFxuICAgIGFsbG93Q29uZGl0aW9uYWxSZXF1aXJlOiB7ICd0eXBlJzogJ2Jvb2xlYW4nIH0sXG4gIH0sXG4gIGFkZGl0aW9uYWxQcm9wZXJ0aWVzOiBmYWxzZSxcbn1cblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAnc3VnZ2VzdGlvbicsXG4gICAgZG9jczoge1xuICAgICAgdXJsOiBkb2NzVXJsKCduby1jb21tb25qcycpLFxuICAgIH0sXG5cbiAgICBzY2hlbWE6IHtcbiAgICAgIGFueU9mOiBbXG4gICAgICAgIHtcbiAgICAgICAgICB0eXBlOiAnYXJyYXknLFxuICAgICAgICAgIGl0ZW1zOiBbc2NoZW1hU3RyaW5nXSxcbiAgICAgICAgICBhZGRpdGlvbmFsSXRlbXM6IGZhbHNlLFxuICAgICAgICB9LFxuICAgICAgICB7XG4gICAgICAgICAgdHlwZTogJ2FycmF5JyxcbiAgICAgICAgICBpdGVtczogW3NjaGVtYU9iamVjdF0sXG4gICAgICAgICAgYWRkaXRpb25hbEl0ZW1zOiBmYWxzZSxcbiAgICAgICAgfSxcbiAgICAgIF0sXG4gICAgfSxcbiAgfSxcblxuICBjcmVhdGU6IGZ1bmN0aW9uIChjb250ZXh0KSB7XG4gICAgY29uc3Qgb3B0aW9ucyA9IG5vcm1hbGl6ZUxlZ2FjeU9wdGlvbnMoY29udGV4dC5vcHRpb25zKVxuXG4gICAgcmV0dXJuIHtcblxuICAgICAgJ01lbWJlckV4cHJlc3Npb24nOiBmdW5jdGlvbiAobm9kZSkge1xuXG4gICAgICAgIC8vIG1vZHVsZS5leHBvcnRzXG4gICAgICAgIGlmIChub2RlLm9iamVjdC5uYW1lID09PSAnbW9kdWxlJyAmJiBub2RlLnByb3BlcnR5Lm5hbWUgPT09ICdleHBvcnRzJykge1xuICAgICAgICAgIGlmIChhbGxvd1ByaW1pdGl2ZShub2RlLCBvcHRpb25zKSkgcmV0dXJuXG4gICAgICAgICAgY29udGV4dC5yZXBvcnQoeyBub2RlLCBtZXNzYWdlOiBFWFBPUlRfTUVTU0FHRSB9KVxuICAgICAgICB9XG5cbiAgICAgICAgLy8gZXhwb3J0cy5cbiAgICAgICAgaWYgKG5vZGUub2JqZWN0Lm5hbWUgPT09ICdleHBvcnRzJykge1xuICAgICAgICAgIGNvbnN0IGlzSW5TY29wZSA9IGNvbnRleHQuZ2V0U2NvcGUoKVxuICAgICAgICAgICAgLnZhcmlhYmxlc1xuICAgICAgICAgICAgLnNvbWUodmFyaWFibGUgPT4gdmFyaWFibGUubmFtZSA9PT0gJ2V4cG9ydHMnKVxuICAgICAgICAgIGlmICghIGlzSW5TY29wZSkge1xuICAgICAgICAgICAgY29udGV4dC5yZXBvcnQoeyBub2RlLCBtZXNzYWdlOiBFWFBPUlRfTUVTU0FHRSB9KVxuICAgICAgICAgIH1cbiAgICAgICAgfVxuXG4gICAgICB9LFxuICAgICAgJ0NhbGxFeHByZXNzaW9uJzogZnVuY3Rpb24gKGNhbGwpIHtcbiAgICAgICAgaWYgKCF2YWxpZGF0ZVNjb3BlKGNvbnRleHQuZ2V0U2NvcGUoKSkpIHJldHVyblxuXG4gICAgICAgIGlmIChjYWxsLmNhbGxlZS50eXBlICE9PSAnSWRlbnRpZmllcicpIHJldHVyblxuICAgICAgICBpZiAoY2FsbC5jYWxsZWUubmFtZSAhPT0gJ3JlcXVpcmUnKSByZXR1cm5cblxuICAgICAgICBpZiAoY2FsbC5hcmd1bWVudHMubGVuZ3RoICE9PSAxKSByZXR1cm5cbiAgICAgICAgdmFyIG1vZHVsZSA9IGNhbGwuYXJndW1lbnRzWzBdXG5cbiAgICAgICAgaWYgKG1vZHVsZS50eXBlICE9PSAnTGl0ZXJhbCcpIHJldHVyblxuICAgICAgICBpZiAodHlwZW9mIG1vZHVsZS52YWx1ZSAhPT0gJ3N0cmluZycpIHJldHVyblxuXG4gICAgICAgIGlmIChhbGxvd1JlcXVpcmUoY2FsbCwgb3B0aW9ucykpIHJldHVyblxuXG4gICAgICAgIGlmIChhbGxvd0NvbmRpdGlvbmFsUmVxdWlyZShjYWxsLCBvcHRpb25zKSAmJiBpc0NvbmRpdGlvbmFsKGNhbGwucGFyZW50KSkgcmV0dXJuXG5cbiAgICAgICAgLy8ga2VlcGluZyBpdCBzaW1wbGU6IGFsbCAxLXN0cmluZy1hcmcgYHJlcXVpcmVgIGNhbGxzIGFyZSByZXBvcnRlZFxuICAgICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgICAgbm9kZTogY2FsbC5jYWxsZWUsXG4gICAgICAgICAgbWVzc2FnZTogSU1QT1JUX01FU1NBR0UsXG4gICAgICAgIH0pXG4gICAgICB9LFxuICAgIH1cblxuICB9LFxufVxuIl19