'use strict';

var _slicedToArray = function () { function sliceIterator(arr, i) { var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i["return"]) _i["return"](); } finally { if (_d) throw _e; } } return _arr; } return function (arr, i) { if (Array.isArray(arr)) { return arr; } else if (Symbol.iterator in Object(arr)) { return sliceIterator(arr, i); } else { throw new TypeError("Invalid attempt to destructure non-iterable instance"); } }; }();

var _resolve = require('eslint-module-utils/resolve');

var _resolve2 = _interopRequireDefault(_resolve);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _toConsumableArray(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) arr2[i] = arr[i]; return arr2; } else { return Array.from(arr); } }

function _toArray(arr) { return Array.isArray(arr) ? arr : Array.from(arr); }

function checkImports(imported, context) {
  for (const _ref of imported.entries()) {
    var _ref2 = _slicedToArray(_ref, 2);

    const module = _ref2[0];
    const nodes = _ref2[1];

    if (nodes.length > 1) {
      const message = `'${module}' imported multiple times.`;

      var _nodes = _toArray(nodes);

      const first = _nodes[0],
            rest = _nodes.slice(1);

      const sourceCode = context.getSourceCode();
      const fix = getFix(first, rest, sourceCode);

      context.report({
        node: first.source,
        message,
        fix // Attach the autofix (if any) to the first import.
      });

      for (const node of rest) {
        context.report({
          node: node.source,
          message
        });
      }
    }
  }
}

function getFix(first, rest, sourceCode) {
  // Sorry ESLint <= 3 users, no autofix for you. Autofixing duplicate imports
  // requires multiple `fixer.whatever()` calls in the `fix`: We both need to
  // update the first one, and remove the rest. Support for multiple
  // `fixer.whatever()` in a single `fix` was added in ESLint 4.1.
  // `sourceCode.getCommentsBefore` was added in 4.0, so that's an easy thing to
  // check for.
  if (typeof sourceCode.getCommentsBefore !== 'function') {
    return undefined;
  }

  // Adjusting the first import might make it multiline, which could break
  // `eslint-disable-next-line` comments and similar, so bail if the first
  // import has comments. Also, if the first import is `import * as ns from
  // './foo'` there's nothing we can do.
  if (hasProblematicComments(first, sourceCode) || hasNamespace(first)) {
    return undefined;
  }

  const defaultImportNames = new Set([first].concat(_toConsumableArray(rest)).map(getDefaultImportName).filter(Boolean));

  // Bail if there are multiple different default import names – it's up to the
  // user to choose which one to keep.
  if (defaultImportNames.size > 1) {
    return undefined;
  }

  // Leave it to the user to handle comments. Also skip `import * as ns from
  // './foo'` imports, since they cannot be merged into another import.
  const restWithoutComments = rest.filter(node => !(hasProblematicComments(node, sourceCode) || hasNamespace(node)));

  const specifiers = restWithoutComments.map(node => {
    const tokens = sourceCode.getTokens(node);
    const openBrace = tokens.find(token => isPunctuator(token, '{'));
    const closeBrace = tokens.find(token => isPunctuator(token, '}'));

    if (openBrace == null || closeBrace == null) {
      return undefined;
    }

    return {
      importNode: node,
      text: sourceCode.text.slice(openBrace.range[1], closeBrace.range[0]),
      hasTrailingComma: isPunctuator(sourceCode.getTokenBefore(closeBrace), ','),
      isEmpty: !hasSpecifiers(node)
    };
  }).filter(Boolean);

  const unnecessaryImports = restWithoutComments.filter(node => !hasSpecifiers(node) && !hasNamespace(node) && !specifiers.some(specifier => specifier.importNode === node));

  const shouldAddDefault = getDefaultImportName(first) == null && defaultImportNames.size === 1;
  const shouldAddSpecifiers = specifiers.length > 0;
  const shouldRemoveUnnecessary = unnecessaryImports.length > 0;

  if (!(shouldAddDefault || shouldAddSpecifiers || shouldRemoveUnnecessary)) {
    return undefined;
  }

  return fixer => {
    const tokens = sourceCode.getTokens(first);
    const openBrace = tokens.find(token => isPunctuator(token, '{'));
    const closeBrace = tokens.find(token => isPunctuator(token, '}'));
    const firstToken = sourceCode.getFirstToken(first);

    var _defaultImportNames = _slicedToArray(defaultImportNames, 1);

    const defaultImportName = _defaultImportNames[0];


    const firstHasTrailingComma = closeBrace != null && isPunctuator(sourceCode.getTokenBefore(closeBrace), ',');
    const firstIsEmpty = !hasSpecifiers(first);

    var _specifiers$reduce = specifiers.reduce((_ref3, specifier) => {
      var _ref4 = _slicedToArray(_ref3, 2);

      let result = _ref4[0],
          needsComma = _ref4[1];

      return [needsComma && !specifier.isEmpty ? `${result},${specifier.text}` : `${result}${specifier.text}`, specifier.isEmpty ? needsComma : true];
    }, ['', !firstHasTrailingComma && !firstIsEmpty]),
        _specifiers$reduce2 = _slicedToArray(_specifiers$reduce, 1);

    const specifiersText = _specifiers$reduce2[0];


    const fixes = [];

    if (shouldAddDefault && openBrace == null && shouldAddSpecifiers) {
      // `import './foo'` → `import def, {...} from './foo'`
      fixes.push(fixer.insertTextAfter(firstToken, ` ${defaultImportName}, {${specifiersText}} from`));
    } else if (shouldAddDefault && openBrace == null && !shouldAddSpecifiers) {
      // `import './foo'` → `import def from './foo'`
      fixes.push(fixer.insertTextAfter(firstToken, ` ${defaultImportName} from`));
    } else if (shouldAddDefault && openBrace != null && closeBrace != null) {
      // `import {...} from './foo'` → `import def, {...} from './foo'`
      fixes.push(fixer.insertTextAfter(firstToken, ` ${defaultImportName},`));
      if (shouldAddSpecifiers) {
        // `import def, {...} from './foo'` → `import def, {..., ...} from './foo'`
        fixes.push(fixer.insertTextBefore(closeBrace, specifiersText));
      }
    } else if (!shouldAddDefault && openBrace == null && shouldAddSpecifiers) {
      // `import './foo'` → `import {...} from './foo'`
      fixes.push(fixer.insertTextAfter(firstToken, ` {${specifiersText}} from`));
    } else if (!shouldAddDefault && openBrace != null && closeBrace != null) {
      // `import {...} './foo'` → `import {..., ...} from './foo'`
      fixes.push(fixer.insertTextBefore(closeBrace, specifiersText));
    }

    // Remove imports whose specifiers have been moved into the first import.
    for (const specifier of specifiers) {
      fixes.push(fixer.remove(specifier.importNode));
    }

    // Remove imports whose default import has been moved to the first import,
    // and side-effect-only imports that are unnecessary due to the first
    // import.
    for (const node of unnecessaryImports) {
      fixes.push(fixer.remove(node));
    }

    return fixes;
  };
}

function isPunctuator(node, value) {
  return node.type === 'Punctuator' && node.value === value;
}

// Get the name of the default import of `node`, if any.
function getDefaultImportName(node) {
  const defaultSpecifier = node.specifiers.find(specifier => specifier.type === 'ImportDefaultSpecifier');
  return defaultSpecifier != null ? defaultSpecifier.local.name : undefined;
}

// Checks whether `node` has a namespace import.
function hasNamespace(node) {
  const specifiers = node.specifiers.filter(specifier => specifier.type === 'ImportNamespaceSpecifier');
  return specifiers.length > 0;
}

// Checks whether `node` has any non-default specifiers.
function hasSpecifiers(node) {
  const specifiers = node.specifiers.filter(specifier => specifier.type === 'ImportSpecifier');
  return specifiers.length > 0;
}

// It's not obvious what the user wants to do with comments associated with
// duplicate imports, so skip imports with comments when autofixing.
function hasProblematicComments(node, sourceCode) {
  return hasCommentBefore(node, sourceCode) || hasCommentAfter(node, sourceCode) || hasCommentInsideNonSpecifiers(node, sourceCode);
}

// Checks whether `node` has a comment (that ends) on the previous line or on
// the same line as `node` (starts).
function hasCommentBefore(node, sourceCode) {
  return sourceCode.getCommentsBefore(node).some(comment => comment.loc.end.line >= node.loc.start.line - 1);
}

// Checks whether `node` has a comment (that starts) on the same line as `node`
// (ends).
function hasCommentAfter(node, sourceCode) {
  return sourceCode.getCommentsAfter(node).some(comment => comment.loc.start.line === node.loc.end.line);
}

// Checks whether `node` has any comments _inside,_ except inside the `{...}`
// part (if any).
function hasCommentInsideNonSpecifiers(node, sourceCode) {
  const tokens = sourceCode.getTokens(node);
  const openBraceIndex = tokens.findIndex(token => isPunctuator(token, '{'));
  const closeBraceIndex = tokens.findIndex(token => isPunctuator(token, '}'));
  // Slice away the first token, since we're no looking for comments _before_
  // `node` (only inside). If there's a `{...}` part, look for comments before
  // the `{`, but not before the `}` (hence the `+1`s).
  const someTokens = openBraceIndex >= 0 && closeBraceIndex >= 0 ? tokens.slice(1, openBraceIndex + 1).concat(tokens.slice(closeBraceIndex + 1)) : tokens.slice(1);
  return someTokens.some(token => sourceCode.getCommentsBefore(token).length > 0);
}

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2.default)('no-duplicates')
    },
    fixable: 'code',
    schema: [{
      type: 'object',
      properties: {
        considerQueryString: {
          type: 'boolean'
        }
      },
      additionalProperties: false
    }]
  },

  create: function (context) {
    // Prepare the resolver from options.
    const considerQueryStringOption = context.options[0] && context.options[0]['considerQueryString'];
    const defaultResolver = sourcePath => (0, _resolve2.default)(sourcePath, context) || sourcePath;
    const resolver = considerQueryStringOption ? sourcePath => {
      const parts = sourcePath.match(/^([^?]*)\?(.*)$/);
      if (!parts) {
        return defaultResolver(sourcePath);
      }
      return defaultResolver(parts[1]) + '?' + parts[2];
    } : defaultResolver;

    const imported = new Map();
    const nsImported = new Map();
    const typesImported = new Map();
    return {
      'ImportDeclaration': function (n) {
        // resolved path will cover aliased duplicates
        const resolvedPath = resolver(n.source.value);
        const importMap = n.importKind === 'type' ? typesImported : hasNamespace(n) ? nsImported : imported;

        if (importMap.has(resolvedPath)) {
          importMap.get(resolvedPath).push(n);
        } else {
          importMap.set(resolvedPath, [n]);
        }
      },

      'Program:exit': function () {
        checkImports(imported, context);
        checkImports(nsImported, context);
        checkImports(typesImported, context);
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1kdXBsaWNhdGVzLmpzIl0sIm5hbWVzIjpbImNoZWNrSW1wb3J0cyIsImltcG9ydGVkIiwiY29udGV4dCIsImVudHJpZXMiLCJtb2R1bGUiLCJub2RlcyIsImxlbmd0aCIsIm1lc3NhZ2UiLCJmaXJzdCIsInJlc3QiLCJzb3VyY2VDb2RlIiwiZ2V0U291cmNlQ29kZSIsImZpeCIsImdldEZpeCIsInJlcG9ydCIsIm5vZGUiLCJzb3VyY2UiLCJnZXRDb21tZW50c0JlZm9yZSIsInVuZGVmaW5lZCIsImhhc1Byb2JsZW1hdGljQ29tbWVudHMiLCJoYXNOYW1lc3BhY2UiLCJkZWZhdWx0SW1wb3J0TmFtZXMiLCJTZXQiLCJtYXAiLCJnZXREZWZhdWx0SW1wb3J0TmFtZSIsImZpbHRlciIsIkJvb2xlYW4iLCJzaXplIiwicmVzdFdpdGhvdXRDb21tZW50cyIsInNwZWNpZmllcnMiLCJ0b2tlbnMiLCJnZXRUb2tlbnMiLCJvcGVuQnJhY2UiLCJmaW5kIiwidG9rZW4iLCJpc1B1bmN0dWF0b3IiLCJjbG9zZUJyYWNlIiwiaW1wb3J0Tm9kZSIsInRleHQiLCJzbGljZSIsInJhbmdlIiwiaGFzVHJhaWxpbmdDb21tYSIsImdldFRva2VuQmVmb3JlIiwiaXNFbXB0eSIsImhhc1NwZWNpZmllcnMiLCJ1bm5lY2Vzc2FyeUltcG9ydHMiLCJzb21lIiwic3BlY2lmaWVyIiwic2hvdWxkQWRkRGVmYXVsdCIsInNob3VsZEFkZFNwZWNpZmllcnMiLCJzaG91bGRSZW1vdmVVbm5lY2Vzc2FyeSIsImZpeGVyIiwiZmlyc3RUb2tlbiIsImdldEZpcnN0VG9rZW4iLCJkZWZhdWx0SW1wb3J0TmFtZSIsImZpcnN0SGFzVHJhaWxpbmdDb21tYSIsImZpcnN0SXNFbXB0eSIsInJlZHVjZSIsInJlc3VsdCIsIm5lZWRzQ29tbWEiLCJzcGVjaWZpZXJzVGV4dCIsImZpeGVzIiwicHVzaCIsImluc2VydFRleHRBZnRlciIsImluc2VydFRleHRCZWZvcmUiLCJyZW1vdmUiLCJ2YWx1ZSIsInR5cGUiLCJkZWZhdWx0U3BlY2lmaWVyIiwibG9jYWwiLCJuYW1lIiwiaGFzQ29tbWVudEJlZm9yZSIsImhhc0NvbW1lbnRBZnRlciIsImhhc0NvbW1lbnRJbnNpZGVOb25TcGVjaWZpZXJzIiwiY29tbWVudCIsImxvYyIsImVuZCIsImxpbmUiLCJzdGFydCIsImdldENvbW1lbnRzQWZ0ZXIiLCJvcGVuQnJhY2VJbmRleCIsImZpbmRJbmRleCIsImNsb3NlQnJhY2VJbmRleCIsInNvbWVUb2tlbnMiLCJjb25jYXQiLCJleHBvcnRzIiwibWV0YSIsImRvY3MiLCJ1cmwiLCJmaXhhYmxlIiwic2NoZW1hIiwicHJvcGVydGllcyIsImNvbnNpZGVyUXVlcnlTdHJpbmciLCJhZGRpdGlvbmFsUHJvcGVydGllcyIsImNyZWF0ZSIsImNvbnNpZGVyUXVlcnlTdHJpbmdPcHRpb24iLCJvcHRpb25zIiwiZGVmYXVsdFJlc29sdmVyIiwic291cmNlUGF0aCIsInJlc29sdmVyIiwicGFydHMiLCJtYXRjaCIsIk1hcCIsIm5zSW1wb3J0ZWQiLCJ0eXBlc0ltcG9ydGVkIiwibiIsInJlc29sdmVkUGF0aCIsImltcG9ydE1hcCIsImltcG9ydEtpbmQiLCJoYXMiLCJnZXQiLCJzZXQiXSwibWFwcGluZ3MiOiI7Ozs7QUFBQTs7OztBQUNBOzs7Ozs7Ozs7O0FBRUEsU0FBU0EsWUFBVCxDQUFzQkMsUUFBdEIsRUFBZ0NDLE9BQWhDLEVBQXlDO0FBQ3ZDLHFCQUE4QkQsU0FBU0UsT0FBVCxFQUE5QixFQUFrRDtBQUFBOztBQUFBLFVBQXRDQyxNQUFzQztBQUFBLFVBQTlCQyxLQUE4Qjs7QUFDaEQsUUFBSUEsTUFBTUMsTUFBTixHQUFlLENBQW5CLEVBQXNCO0FBQ3BCLFlBQU1DLFVBQVcsSUFBR0gsTUFBTyw0QkFBM0I7O0FBRG9CLDRCQUVLQyxLQUZMOztBQUFBLFlBRWJHLEtBRmE7QUFBQSxZQUVIQyxJQUZHOztBQUdwQixZQUFNQyxhQUFhUixRQUFRUyxhQUFSLEVBQW5CO0FBQ0EsWUFB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RyxNQUFNLENBQU4sQ0FBaEIsSUFBNEIsR0FBNUIsR0FBa0NBLE1BQU0sQ0FBTixDQUF6QztBQUNELEtBTmdCLEdBTVpILGVBTkw7O0FBUUEsVUFBTWhHLFdBQVcsSUFBSXFHLEdBQUosRUFBakI7QUFDQSxVQUFNQyxhQUFhLElBQUlELEdBQUosRUFBbkI7QUFDQSxVQUFNRSxnQkFBZ0IsSUFBSUYsR0FBSixFQUF0QjtBQUNBLFdBQU87QUFDTCwyQkFBcUIsVUFBVUcsQ0FBVixFQUFhO0FBQ2hDO0FBQ0EsY0FBTUMsZUFBZVAsU0FBU00sRUFBRXpGLE1BQUYsQ0FBU2tELEtBQWxCLENBQXJCO0FBQ0EsY0FBTXlDLFlBQVlGLEVBQUVHLFVBQUYsS0FBaUIsTUFBakIsR0FBMEJKLGFBQTFCLEdBQ2ZwRixhQUFhcUYsQ0FBYixJQUFrQkYsVUFBbEIsR0FBK0J0RyxRQURsQzs7QUFHQSxZQUFJMEcsVUFBVUUsR0FBVixDQUFjSCxZQUFkLENBQUosRUFBaUM7QUFDL0JDLG9CQUFVRyxHQUFWLENBQWNKLFlBQWQsRUFBNEI1QyxJQUE1QixDQUFpQzJDLENBQWpDO0FBQ0QsU0FGRCxNQUVPO0FBQ0xFLG9CQUFVSSxHQUFWLENBQWNMLFlBQWQsRUFBNEIsQ0FBQ0QsQ0FBRCxDQUE1QjtBQUNEO0FBQ0YsT0FaSTs7QUFjTCxzQkFBZ0IsWUFBWTtBQUMxQnpHLHFCQUFhQyxRQUFiLEVBQXVCQyxPQUF2QjtBQUNBRixxQkFBYXVHLFVBQWIsRUFBeUJyRyxPQUF6QjtBQUNBRixxQkFBYXdHLGFBQWIsRUFBNEJ0RyxPQUE1QjtBQUNEO0FBbEJJLEtBQVA7QUFvQkQ7QUF4RGMsQ0FBakIiLCJmaWxlIjoibm8tZHVwbGljYXRlcy5qcyIsInNvdXJjZXNDb250ZW50IjpbImltcG9ydCByZXNvbHZlIGZyb20gJ2VzbGludC1tb2R1bGUtdXRpbHMvcmVzb2x2ZSdcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnXG5cbmZ1bmN0aW9uIGNoZWNrSW1wb3J0cyhpbXBvcnRlZCwgY29udGV4dCkge1xuICBmb3IgKGNvbnN0IFttb2R1bGUsIG5vZGVzXSBvZiBpbXBvcnRlZC5lbnRyaWVzKCkpIHtcbiAgICBpZiAobm9kZXMubGVuZ3RoID4gMSkge1xuICAgICAgY29uc3QgbWVzc2FnZSA9IGAnJHttb2R1bGV9JyBpbXBvcnRlZCBtdWx0aXBsZSB0aW1lcy5gXG4gICAgICBjb25zdCBbZmlyc3QsIC4uLnJlc3RdID0gbm9kZXNcbiAgICAgIGNvbnN0IHNvdXJjZUNvZGUgPSBjb250ZXh0LmdldFNvdXJjZUNvZGUoKVxuICAgICAgY29uc3QgZml4ID0gZ2V0Rml4KGZpcnN0LCByZXN0LCBzb3VyY2VDb2RlKVxuXG4gICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgIG5vZGU6IGZpcnN0LnNvdXJjZSxcbiAgICAgICAgbWVzc2FnZSxcbiAgICAgICAgZml4LCAvLyBBdHRhY2ggdGhlIGF1dG9maXggKGlmIGFueSkgdG8gdGhlIGZpcnN0IGltcG9ydC5cbiAgICAgIH0pXG5cbiAgICAgIGZvciAoY29uc3Qgbm9kZSBvZiByZXN0KSB7XG4gICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICBub2RlOiBub2RlLnNvdXJjZSxcbiAgICAgICAgICBtZXNzYWdlLFxuICAgICAgICB9KVxuICAgICAgfVxuICAgIH1cbiAgfVxufVxuXG5mdW5jdGlvbiBnZXRGaXgoZmlyc3QsIHJlc3QsIHNvdXJjZUNvZGUpIHtcbiAgLy8gU29ycnkgRVNMaW50IDw9IDMgdXNlcnMsIG5vIGF1dG9maXggZm9yIHlvdS4gQXV0b2ZpeGluZyBkdXBsaWNhdGUgaW1wb3J0c1xuICAvLyByZXF1aXJlcyBtdWx0aXBsZSBgZml4ZXIud2hhdGV2ZXIoKWAgY2FsbHMgaW4gdGhlIGBmaXhgOiBXZSBib3RoIG5lZWQgdG9cbiAgLy8gdXBkYXRlIHRoZSBmaXJzdCBvbmUsIGFuZCByZW1vdmUgdGhlIHJlc3QuIFN1cHBvcnQgZm9yIG11bHRpcGxlXG4gIC8vIGBmaXhlci53aGF0ZXZlcigpYCBpbiBhIHNpbmdsZSBgZml4YCB3YXMgYWRkZWQgaW4gRVNMaW50IDQuMS5cbiAgLy8gYHNvdXJjZUNvZGUuZ2V0Q29tbWVudHNCZWZvcmVgIHdhcyBhZGRlZCBpbiA0LjAsIHNvIHRoYXQncyBhbiBlYXN5IHRoaW5nIHRvXG4gIC8vIGNoZWNrIGZvci5cbiAgaWYgKHR5cGVvZiBzb3VyY2VDb2RlLmdldENvbW1lbnRzQmVmb3JlICE9PSAnZnVuY3Rpb24nKSB7XG4gICAgcmV0dXJuIHVuZGVmaW5lZFxuICB9XG5cbiAgLy8gQWRqdXN0aW5nIHRoZSBmaXJzdCBpbXBvcnQgbWlnaHQgbWFrZSBpdCBtdWx0aWxpbmUsIHdoaWNoIGNvdWxkIGJyZWFrXG4gIC8vIGBlc2xpbnQtZGlzYWJsZS1uZXh0LWxpbmVgIGNvbW1lbnRzIGFuZCBzaW1pbGFyLCBzbyBiYWlsIGlmIHRoZSBmaXJzdFxuICAvLyBpbXBvcnQgaGFzIGNvbW1lbnRzLiBBbHNvLCBpZiB0aGUgZmlyc3QgaW1wb3J0IGlzIGBpbXBvcnQgKiBhcyBucyBmcm9tXG4gIC8vICcuL2ZvbydgIHRoZXJlJ3Mgbm90aGluZyB3ZSBjYW4gZG8uXG4gIGlmIChoYXNQcm9ibGVtYXRpY0NvbW1lbnRzKGZpcnN0LCBzb3VyY2VDb2RlKSB8fCBoYXNOYW1lc3BhY2UoZmlyc3QpKSB7XG4gICAgcmV0dXJuIHVuZGVmaW5lZFxuICB9XG5cbiAgY29uc3QgZGVmYXVsdEltcG9ydE5hbWVzID0gbmV3IFNldChcbiAgICBbZmlyc3QsIC4uLnJlc3RdLm1hcChnZXREZWZhdWx0SW1wb3J0TmFtZSkuZmlsdGVyKEJvb2xlYW4pXG4gIClcblxuICAvLyBCYWlsIGlmIHRoZXJlIGFyZSBtdWx0aXBsZSBkaWZmZXJlbnQgZGVmYXVsdCBpbXBvcnQgbmFtZXMg4oCTIGl0J3MgdXAgdG8gdGhlXG4gIC8vIHVzZXIgdG8gY2hvb3NlIHdoaWNoIG9uZSB0byBrZWVwLlxuICBpZiAoZGVmYXVsdEltcG9ydE5hbWVzLnNpemUgPiAxKSB7XG4gICAgcmV0dXJuIHVuZGVmaW5lZFxuICB9XG5cbiAgLy8gTGVhdmUgaXQgdG8gdGhlIHVzZXIgdG8gaGFuZGxlIGNvbW1lbnRzLiBBbHNvIHNraXAgYGltcG9ydCAqIGFzIG5zIGZyb21cbiAgLy8gJy4vZm9vJ2AgaW1wb3J0cywgc2luY2UgdGhleSBjYW5ub3QgYmUgbWVyZ2VkIGludG8gYW5vdGhlciBpbXBvcnQuXG4gIGNvbnN0IHJlc3RXaXRob3V0Q29tbWVudHMgPSByZXN0LmZpbHRlcihub2RlID0+ICEoXG4gICAgaGFzUHJvYmxlbWF0aWNDb21tZW50cyhub2RlLCBzb3VyY2VDb2RlKSB8fFxuICAgIGhhc05hbWVzcGFjZShub2RlKVxuICApKVxuXG4gIGNvbnN0IHNwZWNpZmllcnMgPSByZXN0V2l0aG91dENvbW1lbnRzXG4gICAgLm1hcChub2RlID0+IHtcbiAgICAgIGNvbnN0IHRva2VucyA9IHNvdXJjZUNvZGUuZ2V0VG9rZW5zKG5vZGUpXG4gICAgICBjb25zdCBvcGVuQnJhY2UgPSB0b2tlbnMuZmluZCh0b2tlbiA9PiBpc1B1bmN0dWF0b3IodG9rZW4sICd7JykpXG4gICAgICBjb25zdCBjbG9zZUJyYWNlID0gdG9rZW5zLmZpbmQodG9rZW4gPT4gaXNQdW5jdHVhdG9yKHRva2VuLCAnfScpKVxuXG4gICAgICBpZiAob3BlbkJyYWNlID09IG51bGwgfHwgY2xvc2VCcmFjZSA9PSBudWxsKSB7XG4gICAgICAgIHJldHVybiB1bmRlZmluZWRcbiAgICAgIH1cblxuICAgICAgcmV0dXJuIHtcbiAgICAgICAgaW1wb3J0Tm9kZTogbm9kZSxcbiAgICAgICAgdGV4dDogc291cmNlQ29kZS50ZXh0LnNsaWNlKG9wZW5CcmFjZS5yYW5nZVsxXSwgY2xvc2VCcmFjZS5yYW5nZVswXSksXG4gICAgICAgIGhhc1RyYWlsaW5nQ29tbWE6IGlzUHVuY3R1YXRvcihzb3VyY2VDb2RlLmdldFRva2VuQmVmb3JlKGNsb3NlQnJhY2UpLCAnLCcpLFxuICAgICAgICBpc0VtcHR5OiAhaGFzU3BlY2lmaWVycyhub2RlKSxcbiAgICAgIH1cbiAgICB9KVxuICAgIC5maWx0ZXIoQm9vbGVhbilcblxuICBjb25zdCB1bm5lY2Vzc2FyeUltcG9ydHMgPSByZXN0V2l0aG91dENvbW1lbnRzLmZpbHRlcihub2RlID0+XG4gICAgIWhhc1NwZWNpZmllcnMobm9kZSkgJiZcbiAgICAhaGFzTmFtZXNwYWNlKG5vZGUpICYmXG4gICAgIXNwZWNpZmllcnMuc29tZShzcGVjaWZpZXIgPT4gc3BlY2lmaWVyLmltcG9ydE5vZGUgPT09IG5vZGUpXG4gIClcblxuICBjb25zdCBzaG91bGRBZGREZWZhdWx0ID0gZ2V0RGVmYXVsdEltcG9ydE5hbWUoZmlyc3QpID09IG51bGwgJiYgZGVmYXVsdEltcG9ydE5hbWVzLnNpemUgPT09IDFcbiAgY29uc3Qgc2hvdWxkQWRkU3BlY2lmaWVycyA9IHNwZWNpZmllcnMubGVuZ3RoID4gMFxuICBjb25zdCBzaG91bGRSZW1vdmVVbm5lY2Vzc2FyeSA9IHVubmVjZXNzYXJ5SW1wb3J0cy5sZW5ndGggPiAwXG5cbiAgaWYgKCEoc2hvdWxkQWRkRGVmYXVsdCB8fCBzaG91bGRBZGRTcGVjaWZpZXJzIHx8IHNob3VsZFJlbW92ZVVubmVjZXNzYXJ5KSkge1xuICAgIHJldHVybiB1bmRlZmluZWRcbiAgfVxuXG4gIHJldHVybiBmaXhlciA9PiB7XG4gICAgY29uc3QgdG9rZW5zID0gc291cmNlQ29kZS5nZXRUb2tlbnMoZmlyc3QpXG4gICAgY29uc3Qgb3BlbkJyYWNlID0gdG9rZW5zLmZpbmQodG9rZW4gPT4gaXNQdW5jdHVhdG9yKHRva2VuLCAneycpKVxuICAgIGNvbnN0IGNsb3NlQnJhY2UgPSB0b2tlbnMuZmluZCh0b2tlbiA9PiBpc1B1bmN0dWF0b3IodG9rZW4sICd9JykpXG4gICAgY29uc3QgZmlyc3RUb2tlbiA9IHNvdXJjZUNvZGUuZ2V0Rmlyc3RUb2tlbihmaXJzdClcbiAgICBjb25zdCBbZGVmYXVsdEltcG9ydE5hbWVdID0gZGVmYXVsdEltcG9ydE5hbWVzXG5cbiAgICBjb25zdCBmaXJzdEhhc1RyYWlsaW5nQ29tbWEgPVxuICAgICAgY2xvc2VCcmFjZSAhPSBudWxsICYmXG4gICAgICBpc1B1bmN0dWF0b3Ioc291cmNlQ29kZS5nZXRUb2tlbkJlZm9yZShjbG9zZUJyYWNlKSwgJywnKVxuICAgIGNvbnN0IGZpcnN0SXNFbXB0eSA9ICFoYXNTcGVjaWZpZXJzKGZpcnN0KVxuXG4gICAgY29uc3QgW3NwZWNpZmllcnNUZXh0XSA9IHNwZWNpZmllcnMucmVkdWNlKFxuICAgICAgKFtyZXN1bHQsIG5lZWRzQ29tbWFdLCBzcGVjaWZpZXIpID0+IHtcbiAgICAgICAgcmV0dXJuIFtcbiAgICAgICAgICBuZWVkc0NvbW1hICYmICFzcGVjaWZpZXIuaXNFbXB0eVxuICAgICAgICAgICAgPyBgJHtyZXN1bHR9LCR7c3BlY2lmaWVyLnRleHR9YFxuICAgICAgICAgICAgOiBgJHtyZXN1bHR9JHtzcGVjaWZpZXIudGV4dH1gLFxuICAgICAgICAgIHNwZWNpZmllci5pc0VtcHR5ID8gbmVlZHNDb21tYSA6IHRydWUsXG4gICAgICAgIF1cbiAgICAgIH0sXG4gICAgICBbJycsICFmaXJzdEhhc1RyYWlsaW5nQ29tbWEgJiYgIWZpcnN0SXNFbXB0eV1cbiAgICApXG5cbiAgICBjb25zdCBmaXhlcyA9IFtdXG5cbiAgICBpZiAoc2hvdWxkQWRkRGVmYXVsdCAmJiBvcGVuQnJhY2UgPT0gbnVsbCAmJiBzaG91bGRBZGRTcGVjaWZpZXJzKSB7XG4gICAgICAvLyBgaW1wb3J0ICcuL2ZvbydgIOKGkiBgaW1wb3J0IGRlZiwgey4uLn0gZnJvbSAnLi9mb28nYFxuICAgICAgZml4ZXMucHVzaChcbiAgICAgICAgZml4ZXIuaW5zZXJ0VGV4dEFmdGVyKGZpcnN0VG9rZW4sIGAgJHtkZWZhdWx0SW1wb3J0TmFtZX0sIHske3NwZWNpZmllcnNUZXh0fX0gZnJvbWApXG4gICAgICApXG4gICAgfSBlbHNlIGlmIChzaG91bGRBZGREZWZhdWx0ICYmIG9wZW5CcmFjZSA9PSBudWxsICYmICFzaG91bGRBZGRTcGVjaWZpZXJzKSB7XG4gICAgICAvLyBgaW1wb3J0ICcuL2ZvbydgIOKGkiBgaW1wb3J0IGRlZiBmcm9tICcuL2ZvbydgXG4gICAgICBmaXhlcy5wdXNoKGZpeGVyLmluc2VydFRleHRBZnRlcihmaXJzdFRva2VuLCBgICR7ZGVmYXVsdEltcG9ydE5hbWV9IGZyb21gKSlcbiAgICB9IGVsc2UgaWYgKHNob3VsZEFkZERlZmF1bHQgJiYgb3BlbkJyYWNlICE9IG51bGwgJiYgY2xvc2VCcmFjZSAhPSBudWxsKSB7XG4gICAgICAvLyBgaW1wb3J0IHsuLi59IGZyb20gJy4vZm9vJ2Ag4oaSIGBpbXBvcnQgZGVmLCB7Li4ufSBmcm9tICcuL2ZvbydgXG4gICAgICBmaXhlcy5wdXNoKGZpeGVyLmluc2VydFRleHRBZnRlcihmaXJzdFRva2VuLCBgICR7ZGVmYXVsdEltcG9ydE5hbWV9LGApKVxuICAgICAgaWYgKHNob3VsZEFkZFNwZWNpZmllcnMpIHtcbiAgICAgICAgLy8gYGltcG9ydCBkZWYsIHsuLi59IGZyb20gJy4vZm9vJ2Ag4oaSIGBpbXBvcnQgZGVmLCB7Li4uLCAuLi59IGZyb20gJy4vZm9vJ2BcbiAgICAgICAgZml4ZXMucHVzaChmaXhlci5pbnNlcnRUZXh0QmVmb3JlKGNsb3NlQnJhY2UsIHNwZWNpZmllcnNUZXh0KSlcbiAgICAgIH1cbiAgICB9IGVsc2UgaWYgKCFzaG91bGRBZGREZWZhdWx0ICYmIG9wZW5CcmFjZSA9PSBudWxsICYmIHNob3VsZEFkZFNwZWNpZmllcnMpIHtcbiAgICAgIC8vIGBpbXBvcnQgJy4vZm9vJ2Ag4oaSIGBpbXBvcnQgey4uLn0gZnJvbSAnLi9mb28nYFxuICAgICAgZml4ZXMucHVzaChmaXhlci5pbnNlcnRUZXh0QWZ0ZXIoZmlyc3RUb2tlbiwgYCB7JHtzcGVjaWZpZXJzVGV4dH19IGZyb21gKSlcbiAgICB9IGVsc2UgaWYgKCFzaG91bGRBZGREZWZhdWx0ICYmIG9wZW5CcmFjZSAhPSBudWxsICYmIGNsb3NlQnJhY2UgIT0gbnVsbCkge1xuICAgICAgLy8gYGltcG9ydCB7Li4ufSAnLi9mb28nYCDihpIgYGltcG9ydCB7Li4uLCAuLi59IGZyb20gJy4vZm9vJ2BcbiAgICAgIGZpeGVzLnB1c2goZml4ZXIuaW5zZXJ0VGV4dEJlZm9yZShjbG9zZUJyYWNlLCBzcGVjaWZpZXJzVGV4dCkpXG4gICAgfVxuXG4gICAgLy8gUmVtb3ZlIGltcG9ydHMgd2hvc2Ugc3BlY2lmaWVycyBoYXZlIGJlZW4gbW92ZWQgaW50byB0aGUgZmlyc3QgaW1wb3J0LlxuICAgIGZvciAoY29uc3Qgc3BlY2lmaWVyIG9mIHNwZWNpZmllcnMpIHtcbiAgICAgIGZpeGVzLnB1c2goZml4ZXIucmVtb3ZlKHNwZWNpZmllci5pbXBvcnROb2RlKSlcbiAgICB9XG5cbiAgICAvLyBSZW1vdmUgaW1wb3J0cyB3aG9zZSBkZWZhdWx0IGltcG9ydCBoYXMgYmVlbiBtb3ZlZCB0byB0aGUgZmlyc3QgaW1wb3J0LFxuICAgIC8vIGFuZCBzaWRlLWVmZmVjdC1vbmx5IGltcG9ydHMgdGhhdCBhcmUgdW5uZWNlc3NhcnkgZHVlIHRvIHRoZSBmaXJzdFxuICAgIC8vIGltcG9ydC5cbiAgICBmb3IgKGNvbnN0IG5vZGUgb2YgdW5uZWNlc3NhcnlJbXBvcnRzKSB7XG4gICAgICBmaXhlcy5wdXNoKGZpeGVyLnJlbW92ZShub2RlKSlcbiAgICB9XG5cbiAgICByZXR1cm4gZml4ZXNcbiAgfVxufVxuXG5mdW5jdGlvbiBpc1B1bmN0dWF0b3Iobm9kZSwgdmFsdWUpIHtcbiAgcmV0dXJuIG5vZGUudHlwZSA9PT0gJ1B1bmN0dWF0b3InICYmIG5vZGUudmFsdWUgPT09IHZhbHVlXG59XG5cbi8vIEdldCB0aGUgbmFtZSBvZiB0aGUgZGVmYXVsdCBpbXBvcnQgb2YgYG5vZGVgLCBpZiBhbnkuXG5mdW5jdGlvbiBnZXREZWZhdWx0SW1wb3J0TmFtZShub2RlKSB7XG4gIGNvbnN0IGRlZmF1bHRTcGVjaWZpZXIgPSBub2RlLnNwZWNpZmllcnNcbiAgICAuZmluZChzcGVjaWZpZXIgPT4gc3BlY2lmaWVyLnR5cGUgPT09ICdJbXBvcnREZWZhdWx0U3BlY2lmaWVyJylcbiAgcmV0dXJuIGRlZmF1bHRTcGVjaWZpZXIgIT0gbnVsbCA/IGRlZmF1bHRTcGVjaWZpZXIubG9jYWwubmFtZSA6IHVuZGVmaW5lZFxufVxuXG4vLyBDaGVja3Mgd2hldGhlciBgbm9kZWAgaGFzIGEgbmFtZXNwYWNlIGltcG9ydC5cbmZ1bmN0aW9uIGhhc05hbWVzcGFjZShub2RlKSB7XG4gIGNvbnN0IHNwZWNpZmllcnMgPSBub2RlLnNwZWNpZmllcnNcbiAgICAuZmlsdGVyKHNwZWNpZmllciA9PiBzcGVjaWZpZXIudHlwZSA9PT0gJ0ltcG9ydE5hbWVzcGFjZVNwZWNpZmllcicpXG4gIHJldHVybiBzcGVjaWZpZXJzLmxlbmd0aCA+IDBcbn1cblxuLy8gQ2hlY2tzIHdoZXRoZXIgYG5vZGVgIGhhcyBhbnkgbm9uLWRlZmF1bHQgc3BlY2lmaWVycy5cbmZ1bmN0aW9uIGhhc1NwZWNpZmllcnMobm9kZSkge1xuICBjb25zdCBzcGVjaWZpZXJzID0gbm9kZS5zcGVjaWZpZXJzXG4gICAgLmZpbHRlcihzcGVjaWZpZXIgPT4gc3BlY2lmaWVyLnR5cGUgPT09ICdJbXBvcnRTcGVjaWZpZXInKVxuICByZXR1cm4gc3BlY2lmaWVycy5sZW5ndGggPiAwXG59XG5cbi8vIEl0J3Mgbm90IG9idmlvdXMgd2hhdCB0aGUgdXNlciB3YW50cyB0byBkbyB3aXRoIGNvbW1lbnRzIGFzc29jaWF0ZWQgd2l0aFxuLy8gZHVwbGljYXRlIGltcG9ydHMsIHNvIHNraXAgaW1wb3J0cyB3aXRoIGNvbW1lbnRzIHdoZW4gYXV0b2ZpeGluZy5cbmZ1bmN0aW9uIGhhc1Byb2JsZW1hdGljQ29tbWVudHMobm9kZSwgc291cmNlQ29kZSkge1xuICByZXR1cm4gKFxuICAgIGhhc0NvbW1lbnRCZWZvcmUobm9kZSwgc291cmNlQ29kZSkgfHxcbiAgICBoYXNDb21tZW50QWZ0ZXIobm9kZSwgc291cmNlQ29kZSkgfHxcbiAgICBoYXNDb21tZW50SW5zaWRlTm9uU3BlY2lmaWVycyhub2RlLCBzb3VyY2VDb2RlKVxuICApXG59XG5cbi8vIENoZWNrcyB3aGV0aGVyIGBub2RlYCBoYXMgYSBjb21tZW50ICh0aGF0IGVuZHMpIG9uIHRoZSBwcmV2aW91cyBsaW5lIG9yIG9uXG4vLyB0aGUgc2FtZSBsaW5lIGFzIGBub2RlYCAoc3RhcnRzKS5cbmZ1bmN0aW9uIGhhc0NvbW1lbnRCZWZvcmUobm9kZSwgc291cmNlQ29kZSkge1xuICByZXR1cm4gc291cmNlQ29kZS5nZXRDb21tZW50c0JlZm9yZShub2RlKVxuICAgIC5zb21lKGNvbW1lbnQgPT4gY29tbWVudC5sb2MuZW5kLmxpbmUgPj0gbm9kZS5sb2Muc3RhcnQubGluZSAtIDEpXG59XG5cbi8vIENoZWNrcyB3aGV0aGVyIGBub2RlYCBoYXMgYSBjb21tZW50ICh0aGF0IHN0YXJ0cykgb24gdGhlIHNhbWUgbGluZSBhcyBgbm9kZWBcbi8vIChlbmRzKS5cbmZ1bmN0aW9uIGhhc0NvbW1lbnRBZnRlcihub2RlLCBzb3VyY2VDb2RlKSB7XG4gIHJldHVybiBzb3VyY2VDb2RlLmdldENvbW1lbnRzQWZ0ZXIobm9kZSlcbiAgICAuc29tZShjb21tZW50ID0+IGNvbW1lbnQubG9jLnN0YXJ0LmxpbmUgPT09IG5vZGUubG9jLmVuZC5saW5lKVxufVxuXG4vLyBDaGVja3Mgd2hldGhlciBgbm9kZWAgaGFzIGFueSBjb21tZW50cyBfaW5zaWRlLF8gZXhjZXB0IGluc2lkZSB0aGUgYHsuLi59YFxuLy8gcGFydCAoaWYgYW55KS5cbmZ1bmN0aW9uIGhhc0NvbW1lbnRJbnNpZGVOb25TcGVjaWZpZXJzKG5vZGUsIHNvdXJjZUNvZGUpIHtcbiAgY29uc3QgdG9rZW5zID0gc291cmNlQ29kZS5nZXRUb2tlbnMobm9kZSlcbiAgY29uc3Qgb3BlbkJyYWNlSW5kZXggPSB0b2tlbnMuZmluZEluZGV4KHRva2VuID0+IGlzUHVuY3R1YXRvcih0b2tlbiwgJ3snKSlcbiAgY29uc3QgY2xvc2VCcmFjZUluZGV4ID0gdG9rZW5zLmZpbmRJbmRleCh0b2tlbiA9PiBpc1B1bmN0dWF0b3IodG9rZW4sICd9JykpXG4gIC8vIFNsaWNlIGF3YXkgdGhlIGZpcnN0IHRva2VuLCBzaW5jZSB3ZSdyZSBubyBsb29raW5nIGZvciBjb21tZW50cyBfYmVmb3JlX1xuICAvLyBgbm9kZWAgKG9ubHkgaW5zaWRlKS4gSWYgdGhlcmUncyBhIGB7Li4ufWAgcGFydCwgbG9vayBmb3IgY29tbWVudHMgYmVmb3JlXG4gIC8vIHRoZSBge2AsIGJ1dCBub3QgYmVmb3JlIHRoZSBgfWAgKGhlbmNlIHRoZSBgKzFgcykuXG4gIGNvbnN0IHNvbWVUb2tlbnMgPSBvcGVuQnJhY2VJbmRleCA+PSAwICYmIGNsb3NlQnJhY2VJbmRleCA+PSAwXG4gICAgPyB0b2tlbnMuc2xpY2UoMSwgb3BlbkJyYWNlSW5kZXggKyAxKS5jb25jYXQodG9rZW5zLnNsaWNlKGNsb3NlQnJhY2VJbmRleCArIDEpKVxuICAgIDogdG9rZW5zLnNsaWNlKDEpXG4gIHJldHVybiBzb21lVG9rZW5zLnNvbWUodG9rZW4gPT4gc291cmNlQ29kZS5nZXRDb21tZW50c0JlZm9yZSh0b2tlbikubGVuZ3RoID4gMClcbn1cblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAncHJvYmxlbScsXG4gICAgZG9jczoge1xuICAgICAgdXJsOiBkb2NzVXJsKCduby1kdXBsaWNhdGVzJyksXG4gICAgfSxcbiAgICBmaXhhYmxlOiAnY29kZScsXG4gICAgc2NoZW1hOiBbXG4gICAgICB7XG4gICAgICAgIHR5cGU6ICdvYmplY3QnLFxuICAgICAgICBwcm9wZXJ0aWVzOiB7XG4gICAgICAgICAgY29uc2lkZXJRdWVyeVN0cmluZzoge1xuICAgICAgICAgICAgdHlwZTogJ2Jvb2xlYW4nLFxuICAgICAgICAgIH0sXG4gICAgICAgIH0sXG4gICAgICAgIGFkZGl0aW9uYWxQcm9wZXJ0aWVzOiBmYWxzZSxcbiAgICAgIH0sXG4gICAgXSxcbiAgfSxcblxuICBjcmVhdGU6IGZ1bmN0aW9uIChjb250ZXh0KSB7XG4gICAgLy8gUHJlcGFyZSB0aGUgcmVzb2x2ZXIgZnJvbSBvcHRpb25zLlxuICAgIGNvbnN0IGNvbnNpZGVyUXVlcnlTdHJpbmdPcHRpb24gPSBjb250ZXh0Lm9wdGlvbnNbMF0gJiZcbiAgICAgIGNvbnRleHQub3B0aW9uc1swXVsnY29uc2lkZXJRdWVyeVN0cmluZyddXG4gICAgY29uc3QgZGVmYXVsdFJlc29sdmVyID0gc291cmNlUGF0aCA9PiByZXNvbHZlKHNvdXJjZVBhdGgsIGNvbnRleHQpIHx8IHNvdXJjZVBhdGhcbiAgICBjb25zdCByZXNvbHZlciA9IGNvbnNpZGVyUXVlcnlTdHJpbmdPcHRpb24gPyAoc291cmNlUGF0aCA9PiB7XG4gICAgICBjb25zdCBwYXJ0cyA9IHNvdXJjZVBhdGgubWF0Y2goL14oW14/XSopXFw/KC4qKSQvKVxuICAgICAgaWYgKCFwYXJ0cykge1xuICAgICAgICByZXR1cm4gZGVmYXVsdFJlc29sdmVyKHNvdXJjZVBhdGgpXG4gICAgICB9XG4gICAgICByZXR1cm4gZGVmYXVsdFJlc29sdmVyKHBhcnRzWzFdKSArICc/JyArIHBhcnRzWzJdXG4gICAgfSkgOiBkZWZhdWx0UmVzb2x2ZXJcblxuICAgIGNvbnN0IGltcG9ydGVkID0gbmV3IE1hcCgpXG4gICAgY29uc3QgbnNJbXBvcnRlZCA9IG5ldyBNYXAoKVxuICAgIGNvbnN0IHR5cGVzSW1wb3J0ZWQgPSBuZXcgTWFwKClcbiAgICByZXR1cm4ge1xuICAgICAgJ0ltcG9ydERlY2xhcmF0aW9uJzogZnVuY3Rpb24gKG4pIHtcbiAgICAgICAgLy8gcmVzb2x2ZWQgcGF0aCB3aWxsIGNvdmVyIGFsaWFzZWQgZHVwbGljYXRlc1xuICAgICAgICBjb25zdCByZXNvbHZlZFBhdGggPSByZXNvbHZlcihuLnNvdXJjZS52YWx1ZSlcbiAgICAgICAgY29uc3QgaW1wb3J0TWFwID0gbi5pbXBvcnRLaW5kID09PSAndHlwZScgPyB0eXBlc0ltcG9ydGVkIDpcbiAgICAgICAgICAoaGFzTmFtZXNwYWNlKG4pID8gbnNJbXBvcnRlZCA6IGltcG9ydGVkKVxuXG4gICAgICAgIGlmIChpbXBvcnRNYXAuaGFzKHJlc29sdmVkUGF0aCkpIHtcbiAgICAgICAgICBpbXBvcnRNYXAuZ2V0KHJlc29sdmVkUGF0aCkucHVzaChuKVxuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgIGltcG9ydE1hcC5zZXQocmVzb2x2ZWRQYXRoLCBbbl0pXG4gICAgICAgIH1cbiAgICAgIH0sXG5cbiAgICAgICdQcm9ncmFtOmV4aXQnOiBmdW5jdGlvbiAoKSB7XG4gICAgICAgIGNoZWNrSW1wb3J0cyhpbXBvcnRlZCwgY29udGV4dClcbiAgICAgICAgY2hlY2tJbXBvcnRzKG5zSW1wb3J0ZWQsIGNvbnRleHQpXG4gICAgICAgIGNoZWNrSW1wb3J0cyh0eXBlc0ltcG9ydGVkLCBjb250ZXh0KVxuICAgICAgfSxcbiAgICB9XG4gIH0sXG59XG4iXX0=