'use strict';

var _slicedToArray = function () { function sliceIterator(arr, i) { var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i["return"]) _i["return"](); } finally { if (_d) throw _e; } } return _arr; } return function (arr, i) { if (Array.isArray(arr)) { return arr; } else if (Symbol.iterator in Object(arr)) { return sliceIterator(arr, i); } else { throw new TypeError("Invalid attempt to destructure non-iterable instance"); } }; }();

var _ExportMap = require('../ExportMap');

var _ExportMap2 = _interopRequireDefault(_ExportMap);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

var _arrayIncludes = require('array-includes');

var _arrayIncludes2 = _interopRequireDefault(_arrayIncludes);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/*
Notes on TypeScript namespaces aka TSModuleDeclaration:

There are two forms:
- active namespaces: namespace Foo {} / module Foo {}
- ambient modules; declare module "eslint-plugin-import" {}

active namespaces:
- cannot contain a default export
- cannot contain an export all
- cannot contain a multi name export (export { a, b })
- can have active namespaces nested within them

ambient namespaces:
- can only be defined in .d.ts files
- cannot be nested within active namespaces
- have no other restrictions
*/

const rootProgram = 'root';
const tsTypePrefix = 'type:';

/**
 * Detect function overloads like:
 * ```ts
 * export function foo(a: number);
 * export function foo(a: string);
 * export function foo(a: number|string) { return a; }
 * ```
 * @param {Set<Object>} nodes
 * @returns {boolean}
 */
function isTypescriptFunctionOverloads(nodes) {
  const types = new Set(Array.from(nodes, node => node.parent.type));
  return types.has('TSDeclareFunction') && (types.size === 1 || types.size === 2 && types.has('FunctionDeclaration'));
}

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2.default)('export')
    },
    schema: []
  },

  create: function (context) {
    const namespace = new Map([[rootProgram, new Map()]]);

    function addNamed(name, node, parent, isType) {
      if (!namespace.has(parent)) {
        namespace.set(parent, new Map());
      }
      const named = namespace.get(parent);

      const key = isType ? `${tsTypePrefix}${name}` : name;
      let nodes = named.get(key);

      if (nodes == null) {
        nodes = new Set();
        named.set(key, nodes);
      }

      nodes.add(node);
    }

    function getParent(node) {
      if (node.parent && node.parent.type === 'TSModuleBlock') {
        return node.parent.parent;
      }

      // just in case somehow a non-ts namespace export declaration isn't directly
      // parented to the root Program node
      return rootProgram;
    }

    return {
      'ExportDefaultDeclaration': node => addNamed('default', node, getParent(node)),

      'ExportSpecifier': node => addNamed(node.exported.name, node.exported, getParent(node)),

      'ExportNamedDeclaration': function (node) {
        if (node.declaration == null) return;

        const parent = getParent(node);
        // support for old TypeScript versions
        const isTypeVariableDecl = node.declaration.kind === 'type';

        if (node.declaration.id != null) {
          if ((0, _arrayIncludes2.default)(['TSTypeAliasDeclaration', 'TSInterfaceDeclaration'], node.declaration.type)) {
            addNamed(node.declaration.id.name, node.declaration.id, parent, true);
          } else {
            addNamed(node.declaration.id.name, node.declaration.id, parent, isTypeVariableDecl);
          }
        }

        if (node.declaration.declarations != null) {
          for (let declaration of node.declaration.declarations) {
            (0, _ExportMap.recursivePatternCapture)(declaration.id, v => addNamed(v.name, v, parent, isTypeVariableDecl));
          }
        }
      },

      'ExportAllDeclaration': function (node) {
        if (node.source == null) return; // not sure if this is ever true

        const remoteExports = _ExportMap2.default.get(node.source.value, context);
        if (remoteExports == null) return;

        if (remoteExports.errors.length) {
          remoteExports.reportErrors(context, node);
          return;
        }

        const parent = getParent(node);

        let any = false;
        remoteExports.forEach((v, name) => name !== 'default' && (any = true) && // poor man's filter
        addNamed(name, node, parent));

        if (!any) {
          context.report(node.source, `No named exports found in module '${node.source.value}'.`);
        }
      },

      'Program:exit': function () {
        for (let _ref of namespace) {
          var _ref2 = _slicedToArray(_ref, 2);

          let named = _ref2[1];

          for (let _ref3 of named) {
            var _ref4 = _slicedToArray(_ref3, 2);

            let name = _ref4[0];
            let nodes = _ref4[1];

            if (nodes.size <= 1) continue;

            if (isTypescriptFunctionOverloads(nodes)) continue;

            for (let node of nodes) {
              if (name === 'default') {
                context.report(node, 'Multiple default exports.');
              } else {
                context.report(node, `Multiple exports of name '${name.replace(tsTypePrefix, '')}'.`);
              }
            }
          }
        }
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9leHBvcnQuanMiXSwibmFtZXMiOlsicm9vdFByb2dyYW0iLCJ0c1R5cGVQcmVmaXgiLCJpc1R5cGVzY3JpcHRGdW5jdGlvbk92ZXJsb2FkcyIsIm5vZGVzIiwidHlwZXMiLCJTZXQiLCJBcnJheSIsImZyb20iLCJub2RlIiwicGFyZW50IiwidHlwZSIsImhhcyIsInNpemUiLCJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsImRvY3MiLCJ1cmwiLCJzY2hlbWEiLCJjcmVhdGUiLCJjb250ZXh0IiwibmFtZXNwYWNlIiwiTWFwIiwiYWRkTmFtZWQiLCJuYW1lIiwiaXNUeXBlIiwic2V0IiwibmFtZWQiLCJnZXQiLCJrZXkiLCJhZGQiLCJnZXRQYXJlbnQiLCJleHBvcnRlZCIsImRlY2xhcmF0aW9uIiwiaXNUeXBlVmFyaWFibGVEZWNsIiwia2luZCIsImlkIiwiZGVjbGFyYXRpb25zIiwidiIsInNvdXJjZSIsInJlbW90ZUV4cG9ydHMiLCJFeHBvcnRNYXAiLCJ2YWx1ZSIsImVycm9ycyIsImxlbmd0aCIsInJlcG9ydEVycm9ycyIsImFueSIsImZvckVhY2giLCJyZXBvcnQiLCJyZXBsYWNlIl0sIm1hcHBpbmdzIjoiOzs7O0FBQUE7Ozs7QUFDQTs7OztBQUNBOzs7Ozs7QUFFQTs7Ozs7Ozs7Ozs7Ozs7Ozs7OztBQW1CQSxNQUFNQSxjQUFjLE1BQXBCO0FBQ0EsTUFBTUMsZUFBZSxPQUFyQjs7QUFFQTs7Ozs7Ozs7OztBQVVBLFNBQVNDLDZCQUFULENBQXVDQyxLQUF2QyxFQUE4QztBQUM1QyxRQUFNQyxRQUFRLElBQUlDLEdBQUosQ0FBUUMsTUFBTUMsSUFBTixDQUFXSixLQUFYLEVBQWtCSyxRQUFRQSxLQUFLQyxNQUFMLENBQVlDLElBQXRDLENBQVIsQ0FBZDtBQUNBLFNBQ0VOLE1BQU1PLEdBQU4sQ0FBVSxtQkFBVixNQUVFUCxNQUFNUSxJQUFOLEtBQWUsQ0FBZixJQUNDUixNQUFNUSxJQUFOLEtBQWUsQ0FBZixJQUFvQlIs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QUFDL0IsZ0JBQUlBLE1BQU1TLElBQU4sSUFBYyxDQUFsQixFQUFxQjs7QUFFckIsZ0JBQUlWLDhCQUE4QkMsS0FBOUIsQ0FBSixFQUEwQzs7QUFFMUMsaUJBQUssSUFBSUssSUFBVCxJQUFpQkwsS0FBakIsRUFBd0I7QUFDdEIsa0JBQUlxQixTQUFTLFNBQWIsRUFBd0I7QUFDdEJKLHdCQUFRNEIsTUFBUixDQUFleEMsSUFBZixFQUFxQiwyQkFBckI7QUFDRCxlQUZELE1BRU87QUFDTFksd0JBQVE0QixNQUFSLENBQ0V4QyxJQURGLEVBRUcsNkJBQTRCZ0IsS0FBS3lCLE9BQUwsQ0FBYWhELFlBQWIsRUFBMkIsRUFBM0IsQ0FBK0IsSUFGOUQ7QUFJRDtBQUNGO0FBQ0Y7QUFDRjtBQUNGO0FBM0VJLEtBQVA7QUE2RUQ7QUFwSGMsQ0FBakIiLCJmaWxlIjoiZXhwb3J0LmpzIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IEV4cG9ydE1hcCwgeyByZWN1cnNpdmVQYXR0ZXJuQ2FwdHVyZSB9IGZyb20gJy4uL0V4cG9ydE1hcCdcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnXG5pbXBvcnQgaW5jbHVkZXMgZnJvbSAnYXJyYXktaW5jbHVkZXMnXG5cbi8qXG5Ob3RlcyBvbiBUeXBlU2NyaXB0IG5hbWVzcGFjZXMgYWthIFRTTW9kdWxlRGVjbGFyYXRpb246XG5cblRoZXJlIGFyZSB0d28gZm9ybXM6XG4tIGFjdGl2ZSBuYW1lc3BhY2VzOiBuYW1lc3BhY2UgRm9vIHt9IC8gbW9kdWxlIEZvbyB7fVxuLSBhbWJpZW50IG1vZHVsZXM7IGRlY2xhcmUgbW9kdWxlIFwiZXNsaW50LXBsdWdpbi1pbXBvcnRcIiB7fVxuXG5hY3RpdmUgbmFtZXNwYWNlczpcbi0gY2Fubm90IGNvbnRhaW4gYSBkZWZhdWx0IGV4cG9ydFxuLSBjYW5ub3QgY29udGFpbiBhbiBleHBvcnQgYWxsXG4tIGNhbm5vdCBjb250YWluIGEgbXVsdGkgbmFtZSBleHBvcnQgKGV4cG9ydCB7IGEsIGIgfSlcbi0gY2FuIGhhdmUgYWN0aXZlIG5hbWVzcGFjZXMgbmVzdGVkIHdpdGhpbiB0aGVtXG5cbmFtYmllbnQgbmFtZXNwYWNlczpcbi0gY2FuIG9ubHkgYmUgZGVmaW5lZCBpbiAuZC50cyBmaWxlc1xuLSBjYW5ub3QgYmUgbmVzdGVkIHdpdGhpbiBhY3RpdmUgbmFtZXNwYWNlc1xuLSBoYXZlIG5vIG90aGVyIHJlc3RyaWN0aW9uc1xuKi9cblxuY29uc3Qgcm9vdFByb2dyYW0gPSAncm9vdCdcbmNvbnN0IHRzVHlwZVByZWZpeCA9ICd0eXBlOidcblxuLyoqXG4gKiBEZXRlY3QgZnVuY3Rpb24gb3ZlcmxvYWRzIGxpa2U6XG4gKiBgYGB0c1xuICogZXhwb3J0IGZ1bmN0aW9uIGZvbyhhOiBudW1iZXIpO1xuICogZXhwb3J0IGZ1bmN0aW9uIGZvbyhhOiBzdHJpbmcpO1xuICogZXhwb3J0IGZ1bmN0aW9uIGZvbyhhOiBudW1iZXJ8c3RyaW5nKSB7IHJldHVybiBhOyB9XG4gKiBgYGBcbiAqIEBwYXJhbSB7U2V0PE9iamVjdD59IG5vZGVzXG4gKiBAcmV0dXJucyB7Ym9vbGVhbn1cbiAqL1xuZnVuY3Rpb24gaXNUeXBlc2NyaXB0RnVuY3Rpb25PdmVybG9hZHMobm9kZXMpIHtcbiAgY29uc3QgdHlwZXMgPSBuZXcgU2V0KEFycmF5LmZyb20obm9kZXMsIG5vZGUgPT4gbm9kZS5wYXJlbnQudHlwZSkpXG4gIHJldHVybiAoXG4gICAgdHlwZXMuaGFzKCdUU0RlY2xhcmVGdW5jdGlvbicpICYmXG4gICAgKFxuICAgICAgdHlwZXMuc2l6ZSA9PT0gMSB8fFxuICAgICAgKHR5cGVzLnNpemUgPT09IDIgJiYgdHlwZXMuaGFzKCdGdW5jdGlvbkRlY2xhcmF0aW9uJykpXG4gICAgKVxuICApXG59XG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3Byb2JsZW0nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnZXhwb3J0JyksXG4gICAgfSxcbiAgICBzY2hlbWE6IFtdLFxuICB9LFxuXG4gIGNyZWF0ZTogZnVuY3Rpb24gKGNvbnRleHQpIHtcbiAgICBjb25zdCBuYW1lc3BhY2UgPSBuZXcgTWFwKFtbcm9vdFByb2dyYW0sIG5ldyBNYXAoKV1dKVxuXG4gICAgZnVuY3Rpb24gYWRkTmFtZWQobmFtZSwgbm9kZSwgcGFyZW50LCBpc1R5cGUpIHtcbiAgICAgIGlmICghbmFtZXNwYWNlLmhhcyhwYXJlbnQpKSB7XG4gICAgICAgIG5hbWVzcGFjZS5zZXQocGFyZW50LCBuZXcgTWFwKCkpXG4gICAgICB9XG4gICAgICBjb25zdCBuYW1lZCA9IG5hbWVzcGFjZS5nZXQocGFyZW50KVxuXG4gICAgICBjb25zdCBrZXkgPSBpc1R5cGUgPyBgJHt0c1R5cGVQcmVmaXh9JHtuYW1lfWAgOiBuYW1lXG4gICAgICBsZXQgbm9kZXMgPSBuYW1lZC5nZXQoa2V5KVxuXG4gICAgICBpZiAobm9kZXMgPT0gbnVsbCkge1xuICAgICAgICBub2RlcyA9IG5ldyBTZXQoKVxuICAgICAgICBuYW1lZC5zZXQoa2V5LCBub2RlcylcbiAgICAgIH1cblxuICAgICAgbm9kZXMuYWRkKG5vZGUpXG4gICAgfVxuXG4gICAgZnVuY3Rpb24gZ2V0UGFyZW50KG5vZGUpIHtcbiAgICAgIGlmIChub2RlLnBhcmVudCAmJiBub2RlLnBhcmVudC50eXBlID09PSAnVFNNb2R1bGVCbG9jaycpIHtcbiAgICAgICAgcmV0dXJuIG5vZGUucGFyZW50LnBhcmVudFxuICAgICAgfVxuXG4gICAgICAvLyBqdXN0IGluIGNhc2Ugc29tZWhvdyBhIG5vbi10cyBuYW1lc3BhY2UgZXhwb3J0IGRlY2xhcmF0aW9uIGlzbid0IGRpcmVjdGx5XG4gICAgICAvLyBwYXJlbnRlZCB0byB0aGUgcm9vdCBQcm9ncmFtIG5vZGVcbiAgICAgIHJldHVybiByb290UHJvZ3JhbVxuICAgIH1cblxuICAgIHJldHVybiB7XG4gICAgICAnRXhwb3J0RGVmYXVsdERlY2xhcmF0aW9uJzogKG5vZGUpID0+IGFkZE5hbWVkKCdkZWZhdWx0Jywgbm9kZSwgZ2V0UGFyZW50KG5vZGUpKSxcblxuICAgICAgJ0V4cG9ydFNwZWNpZmllcic6IChub2RlKSA9PiBhZGROYW1lZChub2RlLmV4cG9ydGVkLm5hbWUsIG5vZGUuZXhwb3J0ZWQsIGdldFBhcmVudChub2RlKSksXG5cbiAgICAgICdFeHBvcnROYW1lZERlY2xhcmF0aW9uJzogZnVuY3Rpb24gKG5vZGUpIHtcbiAgICAgICAgaWYgKG5vZGUuZGVjbGFyYXRpb24gPT0gbnVsbCkgcmV0dXJuXG5cbiAgICAgICAgY29uc3QgcGFyZW50ID0gZ2V0UGFyZW50KG5vZGUpXG4gICAgICAgIC8vIHN1cHBvcnQgZm9yIG9sZCBUeXBlU2NyaXB0IHZlcnNpb25zXG4gICAgICAgIGNvbnN0IGlzVHlwZVZhcmlhYmxlRGVjbCA9IG5vZGUuZGVjbGFyYXRpb24ua2luZCA9PT0gJ3R5cGUnXG5cbiAgICAgICAgaWYgKG5vZGUuZGVjbGFyYXRpb24uaWQgIT0gbnVsbCkge1xuICAgICAgICAgIGlmIChpbmNsdWRlcyhbXG4gICAgICAgICAgICAnVFNUeXBlQWxpYXNEZWNsYXJhdGlvbicsXG4gICAgICAgICAgICAnVFNJbnRlcmZhY2VEZWNsYXJhdGlvbicsXG4gICAgICAgICAgXSwgbm9kZS5kZWNsYXJhdGlvbi50eXBlKSkge1xuICAgICAgICAgICAgYWRkTmFtZWQobm9kZS5kZWNsYXJhdGlvbi5pZC5uYW1lLCBub2RlLmRlY2xhcmF0aW9uLmlkLCBwYXJlbnQsIHRydWUpXG4gICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIGFkZE5hbWVkKG5vZGUuZGVjbGFyYXRpb24uaWQubmFtZSwgbm9kZS5kZWNsYXJhdGlvbi5pZCwgcGFyZW50LCBpc1R5cGVWYXJpYWJsZURlY2wpXG4gICAgICAgICAgfVxuICAgICAgICB9XG5cbiAgICAgICAgaWYgKG5vZGUuZGVjbGFyYXRpb24uZGVjbGFyYXRpb25zICE9IG51bGwpIHtcbiAgICAgICAgICBmb3IgKGxldCBkZWNsYXJhdGlvbiBvZiBub2RlLmRlY2xhcmF0aW9uLmRlY2xhcmF0aW9ucykge1xuICAgICAgICAgICAgcmVjdXJzaXZlUGF0dGVybkNhcHR1cmUoZGVjbGFyYXRpb24uaWQsIHYgPT5cbiAgICAgICAgICAgICAgYWRkTmFtZWQodi5uYW1lLCB2LCBwYXJlbnQsIGlzVHlwZVZhcmlhYmxlRGVjbCkpXG4gICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICB9LFxuXG4gICAgICAnRXhwb3J0QWxsRGVjbGFyYXRpb24nOiBmdW5jdGlvbiAobm9kZSkge1xuICAgICAgICBpZiAobm9kZS5zb3VyY2UgPT0gbnVsbCkgcmV0dXJuIC8vIG5vdCBzdXJlIGlmIHRoaXMgaXMgZXZlciB0cnVlXG5cbiAgICAgICAgY29uc3QgcmVtb3RlRXhwb3J0cyA9IEV4cG9ydE1hcC5nZXQobm9kZS5zb3VyY2UudmFsdWUsIGNvbnRleHQpXG4gICAgICAgIGlmIChyZW1vdGVFeHBvcnRzID09IG51bGwpIHJldHVyblxuXG4gICAgICAgIGlmIChyZW1vdGVFeHBvcnRzLmVycm9ycy5sZW5ndGgpIHtcbiAgICAgICAgICByZW1vdGVFeHBvcnRzLnJlcG9ydEVycm9ycyhjb250ZXh0LCBub2RlKVxuICAgICAgICAgIHJldHVyblxuICAgICAgICB9XG5cbiAgICAgICAgY29uc3QgcGFyZW50ID0gZ2V0UGFyZW50KG5vZGUpXG5cbiAgICAgICAgbGV0IGFueSA9IGZhbHNlXG4gICAgICAgIHJlbW90ZUV4cG9ydHMuZm9yRWFjaCgodiwgbmFtZSkgPT5cbiAgICAgICAgICBuYW1lICE9PSAnZGVmYXVsdCcgJiZcbiAgICAgICAgICAoYW55ID0gdHJ1ZSkgJiYgLy8gcG9vciBtYW4ncyBmaWx0ZXJcbiAgICAgICAgICBhZGROYW1lZChuYW1lLCBub2RlLCBwYXJlbnQpKVxuXG4gICAgICAgIGlmICghYW55KSB7XG4gICAgICAgICAgY29udGV4dC5yZXBvcnQobm9kZS5zb3VyY2UsXG4gICAgICAgICAgICBgTm8gbmFtZWQgZXhwb3J0cyBmb3VuZCBpbiBtb2R1bGUgJyR7bm9kZS5zb3VyY2UudmFsdWV9Jy5gKVxuICAgICAgICB9XG4gICAgICB9LFxuXG4gICAgICAnUHJvZ3JhbTpleGl0JzogZnVuY3Rpb24gKCkge1xuICAgICAgICBmb3IgKGxldCBbLCBuYW1lZF0gb2YgbmFtZXNwYWNlKSB7XG4gICAgICAgICAgZm9yIChsZXQgW25hbWUsIG5vZGVzXSBvZiBuYW1lZCkge1xuICAgICAgICAgICAgaWYgKG5vZGVzLnNpemUgPD0gMSkgY29udGludWVcblxuICAgICAgICAgICAgaWYgKGlzVHlwZXNjcmlwdEZ1bmN0aW9uT3ZlcmxvYWRzKG5vZGVzKSkgY29udGludWVcblxuICAgICAgICAgICAgZm9yIChsZXQgbm9kZSBvZiBub2Rlcykge1xuICAgICAgICAgICAgICBpZiAobmFtZSA9PT0gJ2RlZmF1bHQnKSB7XG4gICAgICAgICAgICAgICAgY29udGV4dC5yZXBvcnQobm9kZSwgJ011bHRpcGxlIGRlZmF1bHQgZXhwb3J0cy4nKVxuICAgICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgIGNvbnRleHQucmVwb3J0KFxuICAgICAgICAgICAgICAgICAgbm9kZSxcbiAgICAgICAgICAgICAgICAgIGBNdWx0aXBsZSBleHBvcnRzIG9mIG5hbWUgJyR7bmFtZS5yZXBsYWNlKHRzVHlwZVByZWZpeCwgJycpfScuYFxuICAgICAgICAgICAgICAgIClcbiAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgfSxcbiAgICB9XG4gIH0sXG59XG4iXX0=