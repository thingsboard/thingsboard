'use strict';

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _toConsumableArray(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) arr2[i] = arr[i]; return arr2; } else { return Array.from(arr); } } /**
                                                                                                                                                                                                 * @fileoverview Rule to disallow namespace import
                                                                                                                                                                                                 * @author Radek Benkel
                                                                                                                                                                                                 */

//------------------------------------------------------------------------------
// Rule Definition
//------------------------------------------------------------------------------


module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('no-namespace')
    },
    fixable: 'code',
    schema: []
  },

  create: function (context) {
    return {
      'ImportNamespaceSpecifier': function (node) {
        const scopeVariables = context.getScope().variables;
        const namespaceVariable = scopeVariables.find(variable => variable.defs[0].node === node);
        const namespaceReferences = namespaceVariable.references;
        const namespaceIdentifiers = namespaceReferences.map(reference => reference.identifier);
        const canFix = namespaceIdentifiers.length > 0 && !usesNamespaceAsObject(namespaceIdentifiers);

        context.report({
          node,
          message: `Unexpected namespace import.`,
          fix: canFix && (fixer => {
            const scopeManager = context.getSourceCode().scopeManager;
            const fixes = [];

            // Pass 1: Collect variable names that are already in scope for each reference we want
            // to transform, so that we can be sure that we choose non-conflicting import names
            const importNameConflicts = {};
            namespaceIdentifiers.forEach(identifier => {
              const parent = identifier.parent;
              if (parent && parent.type === 'MemberExpression') {
                const importName = getMemberPropertyName(parent);
                const localConflicts = getVariableNamesInScope(scopeManager, parent);
                if (!importNameConflicts[importName]) {
                  importNameConflicts[importName] = localConflicts;
                } else {
                  localConflicts.forEach(c => importNameConflicts[importName].add(c));
                }
              }
            });

            // Choose new names for each import
            const importNames = Object.keys(importNameConflicts);
            const importLocalNames = generateLocalNames(importNames, importNameConflicts, namespaceVariable.name);

            // Replace the ImportNamespaceSpecifier with a list of ImportSpecifiers
            const namedImportSpecifiers = importNames.map(importName => importName === importLocalNames[importName] ? importName : `${importName} as ${importLocalNames[importName]}`);
            fixes.push(fixer.replaceText(node, `{ ${namedImportSpecifiers.join(', ')} }`));

            // Pass 2: Replace references to the namespace with references to the named imports
            namespaceIdentifiers.forEach(identifier => {
              const parent = identifier.parent;
              if (parent && parent.type === 'MemberExpression') {
                const importName = getMemberPropertyName(parent);
                fixes.push(fixer.replaceText(parent, importLocalNames[importName]));
              }
            });

            return fixes;
          })
        });
      }
    };
  }

  /**
   * @param {Identifier[]} namespaceIdentifiers
   * @returns {boolean} `true` if the namespace variable is more than just a glorified constant
   */
};function usesNamespaceAsObject(namespaceIdentifiers) {
  return !namespaceIdentifiers.every(identifier => {
    const parent = identifier.parent;

    // `namespace.x` or `namespace['x']`
    return parent && parent.type === 'MemberExpression' && (parent.property.type === 'Identifier' || parent.property.type === 'Literal');
  });
}

/**
 * @param {MemberExpression} memberExpression
 * @returns {string} the name of the member in the object expression, e.g. the `x` in `namespace.x`
 */
function getMemberPropertyName(memberExpression) {
  return memberExpression.property.type === 'Identifier' ? memberExpression.property.name : memberExpression.property.value;
}

/**
 * @param {ScopeManager} scopeManager
 * @param {ASTNode} node
 * @return {Set<string>}
 */
function getVariableNamesInScope(scopeManager, node) {
  let currentNode = node;
  let scope = scopeManager.acquire(currentNode);
  while (scope == null) {
    currentNode = currentNode.parent;
    scope = scopeManager.acquire(currentNode, true);
  }
  return new Set([].concat(_toConsumableArray(scope.variables.map(variable => variable.name)), _toConsumableArray(scope.upper.variables.map(variable => variable.name))));
}

/**
 *
 * @param {*} names
 * @param {*} nameConflicts
 * @param {*} namespaceName
 */
function generateLocalNames(names, nameConflicts, namespaceName) {
  const localNames = {};
  names.forEach(name => {
    let localName;
    if (!nameConflicts[name].has(name)) {
      localName = name;
    } else if (!nameConflicts[name].has(`${namespaceName}_${name}`)) {
      localName = `${namespaceName}_${name}`;
    } else {
      for (let i = 1; i < Infinity; i++) {
        if (!nameConflicts[name].has(`${namespaceName}_${name}_${i}`)) {
          localName = `${namespaceName}_${name}_${i}`;
          break;
        }
      }
    }
    localNames[name] = localName;
  });
  return localNames;
}
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1uYW1lc3BhY2UuanMiXSwibmFtZXMiOlsibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsImZpeGFibGUiLCJzY2hlbWEiLCJjcmVhdGUiLCJjb250ZXh0Iiwibm9kZSIsInNjb3BlVmFyaWFibGVzIiwiZ2V0U2NvcGUiLCJ2YXJpYWJsZXMiLCJuYW1lc3BhY2VWYXJpYWJsZSIsImZpbmQiLCJ2YXJpYWJsZSIsImRlZnMiLCJuYW1lc3BhY2VSZWZlcmVuY2VzIiwicmVmZXJlbmNlcyIsIm5hbWVzcGFjZUlkZW50aWZpZXJzIiwibWFwIiwicmVmZXJlbmNlIiwiaWRlbnRpZmllciIsImNhbkZpeCIsImxlbmd0aCIsInVzZXNOYW1lc3BhY2VBc09iamVjdCIsInJlcG9ydCIsIm1lc3NhZ2UiLCJmaXgiLCJmaXhlciIsInNjb3BlTWFuYWdlciIsImdldFNvdXJjZUNvZGUiLCJmaXhlcyIsImltcG9ydE5hbWVDb25mbGljdHMiLCJmb3JFYWNoIiwicGFyZW50IiwiaW1wb3J0TmFtZSIsImdldE1lbWJlclByb3BlcnR5TmFtZSIsImxvY2FsQ29uZmxpY3RzIiwiZ2V0VmFyaWFibGVOYW1lc0luU2NvcGUiLCJjIiwiYWRkIiwiaW1wb3J0TmFtZXMiLCJPYmplY3QiLCJrZXlzIiwiaW1wb3J0TG9jYWxOYW1lcyIsImdlbmVyYXRlTG9jYWxOYW1lcyIsIm5hbWUiLCJuYW1lZEltcG9ydFNwZWNpZmllcnMiLCJwdXNoIiwicmVwbGFjZVRleHQiLCJqb2luIiwiZXZlcnkiLCJwcm9wZXJ0eSIsIm1lbWJlckV4cHJlc3Npb24iLCJ2YWx1ZSIsImN1cnJlbnROb2RlIiwic2NvcGUiLCJhY3F1aXJlIiwiU2V0IiwidXBwZXIiLCJuYW1lcyIsIm5hbWVDb25mbGljdHMiLCJuYW1lc3BhY2VOYW1lIiwibG9jYWxOYW1lcyIsImxvY2FsTmFtZSIsImhhcyIsImkiLCJJbmZpbml0eSJdLCJtYXBwaW5ncyI6Ijs7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LEdBQXRCLENBQTBCTCxZQUFZQSxTQUFTZ0MsSUFBL0MsQ0FGRSxHQUFQO0FBSUQ7O0FBRUQ7Ozs7OztBQU1BLFNBQVNELGtCQUFULENBQTRCZSxLQUE1QixFQUFtQ0MsYUFBbkMsRUFBa0RDLGFBQWxELEVBQWlFO0FBQy9ELFFBQU1DLGFBQWEsRUFBbkI7QUFDQUgsUUFBTTNCLE9BQU4sQ0FBZWEsSUFBRCxJQUFVO0FBQ3RCLFFBQUlrQixTQUFKO0FBQ0EsUUFBSSxDQUFDSCxjQUFjZixJQUFkLEVBQW9CbUIsR0FBcEIsQ0FBd0JuQixJQUF4QixDQUFMLEVBQW9DO0FBQ2xDa0Isa0JBQVlsQixJQUFaO0FBQ0QsS0FGRCxNQUVPLElBQUksQ0FBQ2UsY0FBY2YsSUFBZCxFQUFvQm1CLEdBQXBCLENBQXlCLEdBQUVILGFBQWMsSUFBR2hCLElBQUssRUFBakQsQ0FBTCxFQUEwRDtBQUMvRGtCLGtCQUFhLEdBQUVGLGFBQWMsSUFBR2hCLElBQUssRUFBckM7QUFDRCxLQUZNLE1BRUE7QUFDTCxXQUFLLElBQUlvQixJQUFJLENBQWIsRUFBZ0JBLElBQUlDLFFBQXBCLEVBQThCRCxHQUE5QixFQUFtQztBQUNqQyxZQUFJLENBQUNMLGNBQWNmLElBQWQsRUFBb0JtQixHQUFwQixDQUF5QixHQUFFSCxhQUFjLElBQUdoQixJQUFLLElBQUdvQixDQUFFLEVBQXRELENBQUwsRUFBK0Q7QUFDN0RGLHNCQUFhLEdBQUVGLGFBQWMsSUFBR2hCLElBQUssSUFBR29CLENBQUUsRUFBMUM7QUFDQTtBQUNEO0FBQ0Y7QUFDRjtBQUNESCxlQUFXakIsSUFBWCxJQUFtQmtCLFNBQW5CO0FBQ0QsR0FmRDtBQWdCQSxTQUFPRCxVQUFQO0FBQ0QiLCJmaWxlIjoibm8tbmFtZXNwYWNlLmpzIiwic291cmNlc0NvbnRlbnQiOlsiLyoqXG4gKiBAZmlsZW92ZXJ2aWV3IFJ1bGUgdG8gZGlzYWxsb3cgbmFtZXNwYWNlIGltcG9ydFxuICogQGF1dGhvciBSYWRlayBCZW5rZWxcbiAqL1xuXG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJ1xuXG4vLy0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuLy8gUnVsZSBEZWZpbml0aW9uXG4vLy0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuXG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3N1Z2dlc3Rpb24nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnbm8tbmFtZXNwYWNlJyksXG4gICAgfSxcbiAgICBmaXhhYmxlOiAnY29kZScsXG4gICAgc2NoZW1hOiBbXSxcbiAgfSxcblxuICBjcmVhdGU6IGZ1bmN0aW9uIChjb250ZXh0KSB7XG4gICAgcmV0dXJuIHtcbiAgICAgICdJbXBvcnROYW1lc3BhY2VTcGVjaWZpZXInOiBmdW5jdGlvbiAobm9kZSkge1xuICAgICAgICBjb25zdCBzY29wZVZhcmlhYmxlcyA9IGNvbnRleHQuZ2V0U2NvcGUoKS52YXJpYWJsZXNcbiAgICAgICAgY29uc3QgbmFtZXNwYWNlVmFyaWFibGUgPSBzY29wZVZhcmlhYmxlcy5maW5kKCh2YXJpYWJsZSkgPT5cbiAgICAgICAgICB2YXJpYWJsZS5kZWZzWzBdLm5vZGUgPT09IG5vZGVcbiAgICAgICAgKVxuICAgICAgICBjb25zdCBuYW1lc3BhY2VSZWZlcmVuY2VzID0gbmFtZXNwYWNlVmFyaWFibGUucmVmZXJlbmNlc1xuICAgICAgICBjb25zdCBuYW1lc3BhY2VJZGVudGlmaWVycyA9IG5hbWVzcGFjZVJlZmVyZW5jZXMubWFwKHJlZmVyZW5jZSA9PiByZWZlcmVuY2UuaWRlbnRpZmllcilcbiAgICAgICAgY29uc3QgY2FuRml4ID0gbmFtZXNwYWNlSWRlbnRpZmllcnMubGVuZ3RoID4gMCAmJiAhdXNlc05hbWVzcGFjZUFzT2JqZWN0KG5hbWVzcGFjZUlkZW50aWZpZXJzKVxuXG4gICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICBub2RlLFxuICAgICAgICAgIG1lc3NhZ2U6IGBVbmV4cGVjdGVkIG5hbWVzcGFjZSBpbXBvcnQuYCxcbiAgICAgICAgICBmaXg6IGNhbkZpeCAmJiAoZml4ZXIgPT4ge1xuICAgICAgICAgICAgY29uc3Qgc2NvcGVNYW5hZ2VyID0gY29udGV4dC5nZXRTb3VyY2VDb2RlKCkuc2NvcGVNYW5hZ2VyXG4gICAgICAgICAgICBjb25zdCBmaXhlcyA9IFtdXG5cbiAgICAgICAgICAgIC8vIFBhc3MgMTogQ29sbGVjdCB2YXJpYWJsZSBuYW1lcyB0aGF0IGFyZSBhbHJlYWR5IGluIHNjb3BlIGZvciBlYWNoIHJlZmVyZW5jZSB3ZSB3YW50XG4gICAgICAgICAgICAvLyB0byB0cmFuc2Zvcm0sIHNvIHRoYXQgd2UgY2FuIGJlIHN1cmUgdGhhdCB3ZSBjaG9vc2Ugbm9uLWNvbmZsaWN0aW5nIGltcG9ydCBuYW1lc1xuICAgICAgICAgICAgY29uc3QgaW1wb3J0TmFtZUNvbmZsaWN0cyA9IHt9XG4gICAgICAgICAgICBuYW1lc3BhY2VJZGVudGlmaWVycy5mb3JFYWNoKChpZGVudGlmaWVyKSA9PiB7XG4gICAgICAgICAgICAgIGNvbnN0IHBhcmVudCA9IGlkZW50aWZpZXIucGFyZW50XG4gICAgICAgICAgICAgIGlmIChwYXJlbnQgJiYgcGFyZW50LnR5cGUgPT09ICdNZW1iZXJFeHByZXNzaW9uJykge1xuICAgICAgICAgICAgICAgIGNvbnN0IGltcG9ydE5hbWUgPSBnZXRNZW1iZXJQcm9wZXJ0eU5hbWUocGFyZW50KVxuICAgICAgICAgICAgICAgIGNvbnN0IGxvY2FsQ29uZmxpY3RzID0gZ2V0VmFyaWFibGVOYW1lc0luU2NvcGUoc2NvcGVNYW5hZ2VyLCBwYXJlbnQpXG4gICAgICAgICAgICAgICAgaWYgKCFpbXBvcnROYW1lQ29uZmxpY3RzW2ltcG9ydE5hbWVdKSB7XG4gICAgICAgICAgICAgICAgICBpbXBvcnROYW1lQ29uZmxpY3RzW2ltcG9ydE5hbWVdID0gbG9jYWxDb25mbGljdHNcbiAgICAgICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgICAgbG9jYWxDb25mbGljdHMuZm9yRWFjaCgoYykgPT4gaW1wb3J0TmFtZUNvbmZsaWN0c1tpbXBvcnROYW1lXS5hZGQoYykpXG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9KVxuXG4gICAgICAgICAgICAvLyBDaG9vc2UgbmV3IG5hbWVzIGZvciBlYWNoIGltcG9ydFxuICAgICAgICAgICAgY29uc3QgaW1wb3J0TmFtZXMgPSBPYmplY3Qua2V5cyhpbXBvcnROYW1lQ29uZmxpY3RzKVxuICAgICAgICAgICAgY29uc3QgaW1wb3J0TG9jYWxOYW1lcyA9IGdlbmVyYXRlTG9jYWxOYW1lcyhcbiAgICAgICAgICAgICAgaW1wb3J0TmFtZXMsXG4gICAgICAgICAgICAgIGltcG9ydE5hbWVDb25mbGljdHMsXG4gICAgICAgICAgICAgIG5hbWVzcGFjZVZhcmlhYmxlLm5hbWVcbiAgICAgICAgICAgIClcblxuICAgICAgICAgICAgLy8gUmVwbGFjZSB0aGUgSW1wb3J0TmFtZXNwYWNlU3BlY2lmaWVyIHdpdGggYSBsaXN0IG9mIEltcG9ydFNwZWNpZmllcnNcbiAgICAgICAgICAgIGNvbnN0IG5hbWVkSW1wb3J0U3BlY2lmaWVycyA9IGltcG9ydE5hbWVzLm1hcCgoaW1wb3J0TmFtZSkgPT5cbiAgICAgICAgICAgICAgaW1wb3J0TmFtZSA9PT0gaW1wb3J0TG9jYWxOYW1lc1tpbXBvcnROYW1lXVxuICAgICAgICAgICAgICAgID8gaW1wb3J0TmFtZVxuICAgICAgICAgICAgICAgIDogYCR7aW1wb3J0TmFtZX0gYXMgJHtpbXBvcnRMb2NhbE5hbWVzW2ltcG9ydE5hbWVdfWBcbiAgICAgICAgICAgIClcbiAgICAgICAgICAgIGZpeGVzLnB1c2goZml4ZXIucmVwbGFjZVRleHQobm9kZSwgYHsgJHtuYW1lZEltcG9ydFNwZWNpZmllcnMuam9pbignLCAnKX0gfWApKVxuXG4gICAgICAgICAgICAvLyBQYXNzIDI6IFJlcGxhY2UgcmVmZXJlbmNlcyB0byB0aGUgbmFtZXNwYWNlIHdpdGggcmVmZXJlbmNlcyB0byB0aGUgbmFtZWQgaW1wb3J0c1xuICAgICAgICAgICAgbmFtZXNwYWNlSWRlbnRpZmllcnMuZm9yRWFjaCgoaWRlbnRpZmllcikgPT4ge1xuICAgICAgICAgICAgICBjb25zdCBwYXJlbnQgPSBpZGVudGlmaWVyLnBhcmVudFxuICAgICAgICAgICAgICBpZiAocGFyZW50ICYmIHBhcmVudC50eXBlID09PSAnTWVtYmVyRXhwcmVzc2lvbicpIHtcbiAgICAgICAgICAgICAgICBjb25zdCBpbXBvcnROYW1lID0gZ2V0TWVtYmVyUHJvcGVydHlOYW1lKHBhcmVudClcbiAgICAgICAgICAgICAgICBmaXhlcy5wdXNoKGZpeGVyLnJlcGxhY2VUZXh0KHBhcmVudCwgaW1wb3J0TG9jYWxOYW1lc1tpbXBvcnROYW1lXSkpXG4gICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH0pXG5cbiAgICAgICAgICAgIHJldHVybiBmaXhlc1xuICAgICAgICAgIH0pLFxuICAgICAgICB9KVxuICAgICAgfSxcbiAgICB9XG4gIH0sXG59XG5cbi8qKlxuICogQHBhcmFtIHtJZGVudGlmaWVyW119IG5hbWVzcGFjZUlkZW50aWZpZXJzXG4gKiBAcmV0dXJucyB7Ym9vbGVhbn0gYHRydWVgIGlmIHRoZSBuYW1lc3BhY2UgdmFyaWFibGUgaXMgbW9yZSB0aGFuIGp1c3QgYSBnbG9yaWZpZWQgY29uc3RhbnRcbiAqL1xuZnVuY3Rpb24gdXNlc05hbWVzcGFjZUFzT2JqZWN0KG5hbWVzcGFjZUlkZW50aWZpZXJzKSB7XG4gIHJldHVybiAhbmFtZXNwYWNlSWRlbnRpZmllcnMuZXZlcnkoKGlkZW50aWZpZXIpID0+IHtcbiAgICBjb25zdCBwYXJlbnQgPSBpZGVudGlmaWVyLnBhcmVudFxuXG4gICAgLy8gYG5hbWVzcGFjZS54YCBvciBgbmFtZXNwYWNlWyd4J11gXG4gICAgcmV0dXJuIChcbiAgICAgIHBhcmVudCAmJiBwYXJlbnQudHlwZSA9PT0gJ01lbWJlckV4cHJlc3Npb24nICYmXG4gICAgICAocGFyZW50LnByb3BlcnR5LnR5cGUgPT09ICdJZGVudGlmaWVyJyB8fCBwYXJlbnQucHJvcGVydHkudHlwZSA9PT0gJ0xpdGVyYWwnKVxuICAgIClcbiAgfSlcbn1cblxuLyoqXG4gKiBAcGFyYW0ge01lbWJlckV4cHJlc3Npb259IG1lbWJlckV4cHJlc3Npb25cbiAqIEByZXR1cm5zIHtzdHJpbmd9IHRoZSBuYW1lIG9mIHRoZSBtZW1iZXIgaW4gdGhlIG9iamVjdCBleHByZXNzaW9uLCBlLmcuIHRoZSBgeGAgaW4gYG5hbWVzcGFjZS54YFxuICovXG5mdW5jdGlvbiBnZXRNZW1iZXJQcm9wZXJ0eU5hbWUobWVtYmVyRXhwcmVzc2lvbikge1xuICByZXR1cm4gbWVtYmVyRXhwcmVzc2lvbi5wcm9wZXJ0eS50eXBlID09PSAnSWRlbnRpZmllcidcbiAgICA/IG1lbWJlckV4cHJlc3Npb24ucHJvcGVydHkubmFtZVxuICAgIDogbWVtYmVyRXhwcmVzc2lvbi5wcm9wZXJ0eS52YWx1ZVxufVxuXG4vKipcbiAqIEBwYXJhbSB7U2NvcGVNYW5hZ2VyfSBzY29wZU1hbmFnZXJcbiAqIEBwYXJhbSB7QVNUTm9kZX0gbm9kZVxuICogQHJldHVybiB7U2V0PHN0cmluZz59XG4gKi9cbmZ1bmN0aW9uIGdldFZhcmlhYmxlTmFtZXNJblNjb3BlKHNjb3BlTWFuYWdlciwgbm9kZSkge1xuICBsZXQgY3VycmVudE5vZGUgPSBub2RlXG4gIGxldCBzY29wZSA9IHNjb3BlTWFuYWdlci5hY3F1aXJlKGN1cnJlbnROb2RlKVxuICB3aGlsZSAoc2NvcGUgPT0gbnVsbCkge1xuICAgIGN1cnJlbnROb2RlID0gY3VycmVudE5vZGUucGFyZW50XG4gICAgc2NvcGUgPSBzY29wZU1hbmFnZXIuYWNxdWlyZShjdXJyZW50Tm9kZSwgdHJ1ZSlcbiAgfVxuICByZXR1cm4gbmV3IFNldChbXG4gICAgLi4uc2NvcGUudmFyaWFibGVzLm1hcCh2YXJpYWJsZSA9PiB2YXJpYWJsZS5uYW1lKSxcbiAgICAuLi5zY29wZS51cHBlci52YXJpYWJsZXMubWFwKHZhcmlhYmxlID0+IHZhcmlhYmxlLm5hbWUpLFxuICBdKVxufVxuXG4vKipcbiAqXG4gKiBAcGFyYW0geyp9IG5hbWVzXG4gKiBAcGFyYW0geyp9IG5hbWVDb25mbGljdHNcbiAqIEBwYXJhbSB7Kn0gbmFtZXNwYWNlTmFtZVxuICovXG5mdW5jdGlvbiBnZW5lcmF0ZUxvY2FsTmFtZXMobmFtZXMsIG5hbWVDb25mbGljdHMsIG5hbWVzcGFjZU5hbWUpIHtcbiAgY29uc3QgbG9jYWxOYW1lcyA9IHt9XG4gIG5hbWVzLmZvckVhY2goKG5hbWUpID0+IHtcbiAgICBsZXQgbG9jYWxOYW1lXG4gICAgaWYgKCFuYW1lQ29uZmxpY3RzW25hbWVdLmhhcyhuYW1lKSkge1xuICAgICAgbG9jYWxOYW1lID0gbmFtZVxuICAgIH0gZWxzZSBpZiAoIW5hbWVDb25mbGljdHNbbmFtZV0uaGFzKGAke25hbWVzcGFjZU5hbWV9XyR7bmFtZX1gKSkge1xuICAgICAgbG9jYWxOYW1lID0gYCR7bmFtZXNwYWNlTmFtZX1fJHtuYW1lfWBcbiAgICB9IGVsc2Uge1xuICAgICAgZm9yIChsZXQgaSA9IDE7IGkgPCBJbmZpbml0eTsgaSsrKSB7XG4gICAgICAgIGlmICghbmFtZUNvbmZsaWN0c1tuYW1lXS5oYXMoYCR7bmFtZXNwYWNlTmFtZX1fJHtuYW1lfV8ke2l9YCkpIHtcbiAgICAgICAgICBsb2NhbE5hbWUgPSBgJHtuYW1lc3BhY2VOYW1lfV8ke25hbWV9XyR7aX1gXG4gICAgICAgICAgYnJlYWtcbiAgICAgICAgfVxuICAgICAgfVxuICAgIH1cbiAgICBsb2NhbE5hbWVzW25hbWVdID0gbG9jYWxOYW1lXG4gIH0pXG4gIHJldHVybiBsb2NhbE5hbWVzXG59XG4iXX0=