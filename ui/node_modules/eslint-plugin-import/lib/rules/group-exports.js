'use strict';

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

var _object = require('object.values');

var _object2 = _interopRequireDefault(_object);

var _arrayPrototype = require('array.prototype.flat');

var _arrayPrototype2 = _interopRequireDefault(_arrayPrototype);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

const meta = {
  type: 'suggestion',
  docs: {
    url: (0, _docsUrl2.default)('group-exports')
  }
  /* eslint-disable max-len */
};const errors = {
  ExportNamedDeclaration: 'Multiple named export declarations; consolidate all named exports into a single export declaration',
  AssignmentExpression: 'Multiple CommonJS exports; consolidate all exports into a single assignment to `module.exports`'
  /* eslint-enable max-len */

  /**
   * Returns an array with names of the properties in the accessor chain for MemberExpression nodes
   *
   * Example:
   *
   * `module.exports = {}` => ['module', 'exports']
   * `module.exports.property = true` => ['module', 'exports', 'property']
   *
   * @param     {Node}    node    AST Node (MemberExpression)
   * @return    {Array}           Array with the property names in the chain
   * @private
   */
};function accessorChain(node) {
  const chain = [];

  do {
    chain.unshift(node.property.name);

    if (node.object.type === 'Identifier') {
      chain.unshift(node.object.name);
      break;
    }

    node = node.object;
  } while (node.type === 'MemberExpression');

  return chain;
}

function create(context) {
  const nodes = {
    modules: new Set(),
    commonjs: new Set(),
    sources: {}
  };

  return {
    ExportNamedDeclaration(node) {
      if (!node.source) {
        nodes.modules.add(node);
      } else if (Array.isArray(nodes.sources[node.source.value])) {
        nodes.sources[node.source.value].push(node);
      } else {
        nodes.sources[node.source.value] = [node];
      }
    },

    AssignmentExpression(node) {
      if (node.left.type !== 'MemberExpression') {
        return;
      }

      const chain = accessorChain(node.left);

      // Assignments to module.exports
      // Deeper assignments are ignored since they just modify what's already being exported
      // (ie. module.exports.exported.prop = true is ignored)
      if (chain[0] === 'module' && chain[1] === 'exports' && chain.length <= 3) {
        nodes.commonjs.add(node);
        return;
      }

      // Assignments to exports (exports.* = *)
      if (chain[0] === 'exports' && chain.length === 2) {
        nodes.commonjs.add(node);
        return;
      }
    },

    'Program:exit': function onExit() {
      // Report multiple `export` declarations (ES2015 modules)
      if (nodes.modules.size > 1) {
        nodes.modules.forEach(node => {
          context.report({
            node,
            message: errors[node.type]
          });
        });
      }

      // Report multiple `aggregated exports` from the same module (ES2015 modules)
      (0, _arrayPrototype2.default)((0, _object2.default)(nodes.sources).filter(nodesWithSource => Array.isArray(nodesWithSource) && nodesWithSource.length > 1)).forEach(node => {
        context.report({
          node,
          message: errors[node.type]
        });
      });

      // Report multiple `module.exports` assignments (CommonJS)
      if (nodes.commonjs.size > 1) {
        nodes.commonjs.forEach(node => {
          context.report({
            node,
            message: errors[node.type]
          });
        });
      }
    }
  };
}

module.exports = {
  meta,
  create
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9ncm91cC1leHBvcnRzLmpzIl0sIm5hbWVzIjpbIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsImVycm9ycyIsIkV4cG9ydE5hbWVkRGVjbGFyYXRpb24iLCJBc3NpZ25tZW50RXhwcmVzc2lvbiIsImFjY2Vzc29yQ2hhaW4iLCJub2RlIiwiY2hhaW4iLCJ1bnNoaWZ0IiwicHJvcGVydHkiLCJuYW1lIiwib2JqZWN0IiwiY3JlYXRlIiwiY29udGV4dCIsIm5vZGVzIiwibW9kdWxlcyIsIlNldCIsImNvbW1vbmpzIiwic291cmNlcyIsInNvdXJjZSIsImFkZCIsIkFycmF5IiwiaXNBcnJheSIsInZhbHVlIiwicHVzaCIsImxlZnQiLCJsZW5ndGgiLCJvbkV4aXQiLCJzaXplIiwiZm9yRWFjaCIsInJlcG9ydCIsIm1lc3NhZ2UiLCJmaWx0ZXIiLCJub2Rlc1dpdGhTb3VyY2UiLCJtb2R1bGUiLCJleHBvcnRzIl0sIm1hcHBpbmdzIjoiOztBQUFBOzs7O0FBQ0E7Ozs7QUFDQTs7Ozs7O0FBRUEsTUFBTUEsT0FBTztBQUNYQyxRQUFNLFlBREs7QUFFWEMsUUFBTTtBQUNKQyxTQUFLLHVCQUFRLGVBQVI7QUFERDtBQUlSO0FBTmEsQ0FBYixDQU9BLE1BQU1DLFNBQVM7QUFDYkMsMEJBQXdCLG9HQURYO0FBRWJDLHdCQUFzQjtBQUV4Qjs7QUFFQTs7Ozs7Ozs7Ozs7O0FBTmUsQ0FBZixDQWtCQSxTQUFTQyxhQUFULENBQXVCQyxJQUF2QixFQUE2QjtBQUMzQixRQUFNQyxRQUFRLEVBQWQ7O0FBRUEsS0FBRztBQUNEQSxVQUFNQyxPQUFOLENBQWNGLEtBQUtHLFFBQUwsQ0FBY0MsSUFBNUI7O0FBRUEsUUFBSUosS0FBS0ssTUFBTCxDQUFZWixJQUFaLEtBQXFCLFlBQXpCLEVBQXVDO0FBQ3JDUSxZQUFNQyxPQUFOLENBQWNGLEtBQUtLLE1BQUwsQ0FBWUQsSUFBMUI7QUFDQTtBQUNEOztBQUVESixXQUFPQSxLQUFLSyxNQUFaO0FBQ0Qs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ICcuLi9kb2NzVXJsJ1xuaW1wb3J0IHZhbHVlcyBmcm9tICdvYmplY3QudmFsdWVzJ1xuaW1wb3J0IGZsYXQgZnJvbSAnYXJyYXkucHJvdG90eXBlLmZsYXQnXG5cbmNvbnN0IG1ldGEgPSB7XG4gIHR5cGU6ICdzdWdnZXN0aW9uJyxcbiAgZG9jczoge1xuICAgIHVybDogZG9jc1VybCgnZ3JvdXAtZXhwb3J0cycpLFxuICB9LFxufVxuLyogZXNsaW50LWRpc2FibGUgbWF4LWxlbiAqL1xuY29uc3QgZXJyb3JzID0ge1xuICBFeHBvcnROYW1lZERlY2xhcmF0aW9uOiAnTXVsdGlwbGUgbmFtZWQgZXhwb3J0IGRlY2xhcmF0aW9uczsgY29uc29saWRhdGUgYWxsIG5hbWVkIGV4cG9ydHMgaW50byBhIHNpbmdsZSBleHBvcnQgZGVjbGFyYXRpb24nLFxuICBBc3NpZ25tZW50RXhwcmVzc2lvbjogJ011bHRpcGxlIENvbW1vbkpTIGV4cG9ydHM7IGNvbnNvbGlkYXRlIGFsbCBleHBvcnRzIGludG8gYSBzaW5nbGUgYXNzaWdubWVudCB0byBgbW9kdWxlLmV4cG9ydHNgJyxcbn1cbi8qIGVzbGludC1lbmFibGUgbWF4LWxlbiAqL1xuXG4vKipcbiAqIFJldHVybnMgYW4gYXJyYXkgd2l0aCBuYW1lcyBvZiB0aGUgcHJvcGVydGllcyBpbiB0aGUgYWNjZXNzb3IgY2hhaW4gZm9yIE1lbWJlckV4cHJlc3Npb24gbm9kZXNcbiAqXG4gKiBFeGFtcGxlOlxuICpcbiAqIGBtb2R1bGUuZXhwb3J0cyA9IHt9YCA9PiBbJ21vZHVsZScsICdleHBvcnRzJ11cbiAqIGBtb2R1bGUuZXhwb3J0cy5wcm9wZXJ0eSA9IHRydWVgID0+IFsnbW9kdWxlJywgJ2V4cG9ydHMnLCAncHJvcGVydHknXVxuICpcbiAqIEBwYXJhbSAgICAge05vZGV9ICAgIG5vZGUgICAgQVNUIE5vZGUgKE1lbWJlckV4cHJlc3Npb24pXG4gKiBAcmV0dXJuICAgIHtBcnJheX0gICAgICAgICAgIEFycmF5IHdpdGggdGhlIHByb3BlcnR5IG5hbWVzIGluIHRoZSBjaGFpblxuICogQHByaXZhdGVcbiAqL1xuZnVuY3Rpb24gYWNjZXNzb3JDaGFpbihub2RlKSB7XG4gIGNvbnN0IGNoYWluID0gW11cblxuICBkbyB7XG4gICAgY2hhaW4udW5zaGlmdChub2RlLnByb3BlcnR5Lm5hbWUpXG5cbiAgICBpZiAobm9kZS5vYmplY3QudHlwZSA9PT0gJ0lkZW50aWZpZXInKSB7XG4gICAgICBjaGFpbi51bnNoaWZ0KG5vZGUub2JqZWN0Lm5hbWUpXG4gICAgICBicmVha1xuICAgIH1cblxuICAgIG5vZGUgPSBub2RlLm9iamVjdFxuICB9IHdoaWxlIChub2RlLnR5cGUgPT09ICdNZW1iZXJFeHByZXNzaW9uJylcblxuICByZXR1cm4gY2hhaW5cbn1cblxuZnVuY3Rpb24gY3JlYXRlKGNvbnRleHQpIHtcbiAgY29uc3Qgbm9kZXMgPSB7XG4gICAgbW9kdWxlczogbmV3IFNldCgpLFxuICAgIGNvbW1vbmpzOiBuZXcgU2V0KCksXG4gICAgc291cmNlczoge30sXG4gIH1cblxuICByZXR1cm4ge1xuICAgIEV4cG9ydE5hbWVkRGVjbGFyYXRpb24obm9kZSkge1xuICAgICAgaWYgKCFub2RlLnNvdXJjZSkge1xuICAgICAgICBub2Rlcy5tb2R1bGVzLmFkZChub2RlKVxuICAgICAgfSBlbHNlIGlmIChBcnJheS5pc0FycmF5KG5vZGVzLnNvdXJjZXNbbm9kZS5zb3VyY2UudmFsdWVdKSkge1xuICAgICAgICBub2Rlcy5zb3VyY2VzW25vZGUuc291cmNlLnZhbHVlXS5wdXNoKG5vZGUpXG4gICAgICB9IGVsc2Uge1xuICAgICAgICBub2Rlcy5zb3VyY2VzW25vZGUuc291cmNlLnZhbHVlXSA9IFtub2RlXVxuICAgICAgfVxuICAgIH0sXG5cbiAgICBBc3NpZ25tZW50RXhwcmVzc2lvbihub2RlKSB7XG4gICAgICBpZiAobm9kZS5sZWZ0LnR5cGUgIT09ICdNZW1iZXJFeHByZXNzaW9uJykge1xuICAgICAgICByZXR1cm5cbiAgICAgIH1cblxuICAgICAgY29uc3QgY2hhaW4gPSBhY2Nlc3NvckNoYWluKG5vZGUubGVmdClcblxuICAgICAgLy8gQXNzaWdubWVudHMgdG8gbW9kdWxlLmV4cG9ydHNcbiAgICAgIC8vIERlZXBlciBhc3NpZ25tZW50cyBhcmUgaWdub3JlZCBzaW5jZSB0aGV5IGp1c3QgbW9kaWZ5IHdoYXQncyBhbHJlYWR5IGJlaW5nIGV4cG9ydGVkXG4gICAgICAvLyAoaWUuIG1vZHVsZS5leHBvcnRzLmV4cG9ydGVkLnByb3AgPSB0cnVlIGlzIGlnbm9yZWQpXG4gICAgICBpZiAoY2hhaW5bMF0gPT09ICdtb2R1bGUnICYmIGNoYWluWzFdID09PSAnZXhwb3J0cycgJiYgY2hhaW4ubGVuZ3RoIDw9IDMpIHtcbiAgICAgICAgbm9kZXMuY29tbW9uanMuYWRkKG5vZGUpXG4gICAgICAgIHJldHVyblxuICAgICAgfVxuXG4gICAgICAvLyBBc3NpZ25tZW50cyB0byBleHBvcnRzIChleHBvcnRzLiogPSAqKVxuICAgICAgaWYgKGNoYWluWzBdID09PSAnZXhwb3J0cycgJiYgY2hhaW4ubGVuZ3RoID09PSAyKSB7XG4gICAgICAgIG5vZGVzLmNvbW1vbmpzLmFkZChub2RlKVxuICAgICAgICByZXR1cm5cbiAgICAgIH1cbiAgICB9LFxuXG4gICAgJ1Byb2dyYW06ZXhpdCc6IGZ1bmN0aW9uIG9uRXhpdCgpIHtcbiAgICAgIC8vIFJlcG9ydCBtdWx0aXBsZSBgZXhwb3J0YCBkZWNsYXJhdGlvbnMgKEVTMjAxNSBtb2R1bGVzKVxuICAgICAgaWYgKG5vZGVzLm1vZHVsZXMuc2l6ZSA+IDEpIHtcbiAgICAgICAgbm9kZXMubW9kdWxlcy5mb3JFYWNoKG5vZGUgPT4ge1xuICAgICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICAgIG5vZGUsXG4gICAgICAgICAgICBtZXNzYWdlOiBlcnJvcnNbbm9kZS50eXBlXSxcbiAgICAgICAgICB9KVxuICAgICAgICB9KVxuICAgICAgfVxuXG4gICAgICAvLyBSZXBvcnQgbXVsdGlwbGUgYGFnZ3JlZ2F0ZWQgZXhwb3J0c2AgZnJvbSB0aGUgc2FtZSBtb2R1bGUgKEVTMjAxNSBtb2R1bGVzKVxuICAgICAgZmxhdCh2YWx1ZXMobm9kZXMuc291cmNlcylcbiAgICAgICAgLmZpbHRlcihub2Rlc1dpdGhTb3VyY2UgPT4gQXJyYXkuaXNBcnJheShub2Rlc1dpdGhTb3VyY2UpICYmIG5vZGVzV2l0aFNvdXJjZS5sZW5ndGggPiAxKSlcbiAgICAgICAgLmZvckVhY2goKG5vZGUpID0+IHtcbiAgICAgICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgICAgICBub2RlLFxuICAgICAgICAgICAgbWVzc2FnZTogZXJyb3JzW25vZGUudHlwZV0sXG4gICAgICAgICAgfSlcbiAgICAgICAgfSlcblxuICAgICAgLy8gUmVwb3J0IG11bHRpcGxlIGBtb2R1bGUuZXhwb3J0c2AgYXNzaWdubWVudHMgKENvbW1vbkpTKVxuICAgICAgaWYgKG5vZGVzLmNvbW1vbmpzLnNpemUgPiAxKSB7XG4gICAgICAgIG5vZGVzLmNvbW1vbmpzLmZvckVhY2gobm9kZSA9PiB7XG4gICAgICAgICAgY29udGV4dC5yZXBvcnQoe1xuICAgICAgICAgICAgbm9kZSxcbiAgICAgICAgICAgIG1lc3NhZ2U6IGVycm9yc1tub2RlLnR5cGVdLFxuICAgICAgICAgIH0pXG4gICAgICAgIH0pXG4gICAgICB9XG4gICAgfSxcbiAgfVxufVxuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YSxcbiAgY3JlYXRlLFxufVxuIl19