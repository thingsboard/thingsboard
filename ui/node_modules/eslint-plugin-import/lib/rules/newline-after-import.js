'use strict';

var _staticRequire = require('../core/staticRequire');

var _staticRequire2 = _interopRequireDefault(_staticRequire);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

var _debug = require('debug');

var _debug2 = _interopRequireDefault(_debug);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

const log = (0, _debug2.default)('eslint-plugin-import:rules:newline-after-import');

//------------------------------------------------------------------------------
// Rule Definition
//------------------------------------------------------------------------------

/**
 * @fileoverview Rule to enforce new line after import not followed by another import.
 * @author Radek Benkel
 */

function containsNodeOrEqual(outerNode, innerNode) {
  return outerNode.range[0] <= innerNode.range[0] && outerNode.range[1] >= innerNode.range[1];
}

function getScopeBody(scope) {
  if (scope.block.type === 'SwitchStatement') {
    log('SwitchStatement scopes not supported');
    return null;
  }

  const body = scope.block.body;

  if (body && body.type === 'BlockStatement') {
    return body.body;
  }

  return body;
}

function findNodeIndexInScopeBody(body, nodeToFind) {
  return body.findIndex(node => containsNodeOrEqual(node, nodeToFind));
}

function getLineDifference(node, nextNode) {
  return nextNode.loc.start.line - node.loc.end.line;
}

function isClassWithDecorator(node) {
  return node.type === 'ClassDeclaration' && node.decorators && node.decorators.length;
}

module.exports = {
  meta: {
    type: 'layout',
    docs: {
      url: (0, _docsUrl2.default)('newline-after-import')
    },
    fixable: 'whitespace',
    schema: [{
      'type': 'object',
      'properties': {
        'count': {
          'type': 'integer',
          'minimum': 1
        }
      },
      'additionalProperties': false
    }]
  },
  create: function (context) {
    let level = 0;
    const requireCalls = [];

    function checkForNewLine(node, nextNode, type) {
      if (isClassWithDecorator(nextNode)) {
        nextNode = nextNode.decorators[0];
      }

      const options = context.options[0] || { count: 1 };
      const lineDifference = getLineDifference(node, nextNode);
      const EXPECTED_LINE_DIFFERENCE = options.count + 1;

      if (lineDifference < EXPECTED_LINE_DIFFERENCE) {
        let column = node.loc.start.column;

        if (node.loc.start.line !== node.loc.end.line) {
          column = 0;
        }

        context.report({
          loc: {
            line: node.loc.end.line,
            column
          },
          message: `Expected ${options.count} empty line${options.count > 1 ? 's' : ''} \
after ${type} statement not followed by another ${type}.`,
          fix: fixer => fixer.insertTextAfter(node, '\n'.repeat(EXPECTED_LINE_DIFFERENCE - lineDifference))
        });
      }
    }

    function incrementLevel() {
      level++;
    }
    function decrementLevel() {
      level--;
    }

    return {
      ImportDeclaration: function (node) {
        const parent = node.parent;

        const nodePosition = parent.body.indexOf(node);
        const nextNode = parent.body[nodePosition + 1];

        if (nextNode && nextNode.type !== 'ImportDeclaration') {
          checkForNewLine(node, nextNode, 'import');
        }
      },
      CallExpression: function (node) {
        if ((0, _staticRequire2.default)(node) && level === 0) {
          requireCalls.push(node);
        }
      },
      'Program:exit': function () {
        log('exit processing for', context.getFilename());
        const scopeBody = getScopeBody(context.getScope());
        log('got scope:', scopeBody);

        requireCalls.forEach(function (node, index) {
          const nodePosition = findNodeIndexInScopeBody(scopeBody, node);
          log('node position in scope:', nodePosition);

          const statementWithRequireCall = scopeBody[nodePosition];
          const nextStatement = scopeBody[nodePosition + 1];
          const nextRequireCall = requireCalls[index + 1];

          if (nextRequireCall && containsNodeOrEqual(statementWithRequireCall, nextRequireCall)) {
            return;
          }

          if (nextStatement && (!nextRequireCall || !containsNodeOrEqual(nextStatement, nextRequireCall))) {

            checkForNewLine(statementWithRequireCall, nextStatement, 'require');
          }
        });
      },
      FunctionDeclaration: incrementLevel,
      FunctionExpression: incrementLevel,
      ArrowFunctionExpression: incrementLevel,
      BlockStatement: incrementLevel,
      ObjectExpression: incrementLevel,
      Decorator: incrementLevel,
      'FunctionDeclaration:exit': decrementLevel,
      'FunctionExpression:exit': decrementLevel,
      'ArrowFunctionExpression:exit': decrementLevel,
      'BlockStatement:exit': decrementLevel,
      'ObjectExpression:exit': decrementLevel,
      'Decorator:exit': decrementLevel
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uZXdsaW5lLWFmdGVyLWltcG9ydC5qcyJdLCJuYW1lcyI6WyJsb2ciLCJjb250YWluc05vZGVPckVxdWFsIiwib3V0ZXJOb2RlIiwiaW5uZXJOb2RlIiwicmFuZ2UiLCJnZXRTY29wZUJvZHkiLCJzY29wZSIsImJsb2NrIiwidHlwZSIsImJvZHkiLCJmaW5kTm9kZUluZGV4SW5TY29wZUJvZHkiLCJub2RlVG9GaW5kIiwiZmluZEluZGV4Iiwibm9kZSIsImdldExpbmVEaWZmZXJlbmNlIiwibmV4dE5vZGUiLCJsb2MiLCJzdGFydCIsImxpbmUiLCJlbmQiLCJpc0NsYXNzV2l0aERlY29yYXRvciIsImRlY29yYXRvcnMiLCJsZW5ndGgiLCJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsImRvY3MiLCJ1cmwiLCJmaXhhYmxlIiwic2NoZW1hIiwiY3JlYXRlIiwiY29udGV4dCIsImxldmVsIiwicmVxdWlyZUNhbGxzIiwiY2hlY2tGb3JOZXdMaW5lIiwib3B0aW9ucyIsImNvdW50IiwibGluZURpZmZlcmVuY2UiLCJFWFBFQ1RFRF9MSU5FX0RJRkZFUkVOQ0UiLCJjb2x1bW4iLCJyZXBvcnQiLCJtZXNzYWdlIiwiZml4IiwiZml4ZXIiLCJpbnNlcnRUZXh0QWZ0ZXIiLCJyZXBlYXQiLCJpbmNyZW1lbnRMZXZlbCIsImRlY3JlbWVudExldmVsIiwiSW1wb3J0RGVjbGFyYXRpb24iLCJwYXJlbnQiLCJub2RlUG9zaXRpb24iLCJpbmRleE9mIiwiQ2FsbEV4cHJlc3Npb24iLCJwdXNoIiwiZ2V0RmlsZW5hbWUiLCJzY29wZUJvZHkiLCJnZXRTY29wZSIsImZvckVhY2giLCJpbmRleCIsInN0YXRlbWVudFdpdGhSZXF1aXJlQ2FsbCIsIm5leHRTdGF0ZW1lbnQiLCJuZXh0UmVxdWlyZUNhbGwiLCJGdW5jdGlvbkRlY2xhcmF0aW9uIiwiRnVuY3Rpb25FeHByZXNzaW9uIiwiQXJyb3dGdW5jdGlvbkV4cHJlc3Npb24iLCJCbG9ja1N0YXRl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QUFqQiIsImZpbGUiOiJuZXdsaW5lLWFmdGVyLWltcG9ydC5qcyIsInNvdXJjZXNDb250ZW50IjpbIi8qKlxuICogQGZpbGVvdmVydmlldyBSdWxlIHRvIGVuZm9yY2UgbmV3IGxpbmUgYWZ0ZXIgaW1wb3J0IG5vdCBmb2xsb3dlZCBieSBhbm90aGVyIGltcG9ydC5cbiAqIEBhdXRob3IgUmFkZWsgQmVua2VsXG4gKi9cblxuaW1wb3J0IGlzU3RhdGljUmVxdWlyZSBmcm9tICcuLi9jb3JlL3N0YXRpY1JlcXVpcmUnXG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJ1xuXG5pbXBvcnQgZGVidWcgZnJvbSAnZGVidWcnXG5jb25zdCBsb2cgPSBkZWJ1ZygnZXNsaW50LXBsdWdpbi1pbXBvcnQ6cnVsZXM6bmV3bGluZS1hZnRlci1pbXBvcnQnKVxuXG4vLy0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuLy8gUnVsZSBEZWZpbml0aW9uXG4vLy0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuXG5mdW5jdGlvbiBjb250YWluc05vZGVPckVxdWFsKG91dGVyTm9kZSwgaW5uZXJOb2RlKSB7XG4gICAgcmV0dXJuIG91dGVyTm9kZS5yYW5nZVswXSA8PSBpbm5lck5vZGUucmFuZ2VbMF0gJiYgb3V0ZXJOb2RlLnJhbmdlWzFdID49IGlubmVyTm9kZS5yYW5nZVsxXVxufVxuXG5mdW5jdGlvbiBnZXRTY29wZUJvZHkoc2NvcGUpIHtcbiAgICBpZiAoc2NvcGUuYmxvY2sudHlwZSA9PT0gJ1N3aXRjaFN0YXRlbWVudCcpIHtcbiAgICAgIGxvZygnU3dpdGNoU3RhdGVtZW50IHNjb3BlcyBub3Qgc3VwcG9ydGVkJylcbiAgICAgIHJldHVybiBudWxsXG4gICAgfVxuXG4gICAgY29uc3QgeyBib2R5IH0gPSBzY29wZS5ibG9ja1xuICAgIGlmIChib2R5ICYmIGJvZHkudHlwZSA9PT0gJ0Jsb2NrU3RhdGVtZW50Jykge1xuICAgICAgICByZXR1cm4gYm9keS5ib2R5XG4gICAgfVxuXG4gICAgcmV0dXJuIGJvZHlcbn1cblxuZnVuY3Rpb24gZmluZE5vZGVJbmRleEluU2NvcGVCb2R5KGJvZHksIG5vZGVUb0ZpbmQpIHtcbiAgICByZXR1cm4gYm9keS5maW5kSW5kZXgoKG5vZGUpID0+IGNvbnRhaW5zTm9kZU9yRXF1YWwobm9kZSwgbm9kZVRvRmluZCkpXG59XG5cbmZ1bmN0aW9uIGdldExpbmVEaWZmZXJlbmNlKG5vZGUsIG5leHROb2RlKSB7XG4gIHJldHVybiBuZXh0Tm9kZS5sb2Muc3RhcnQubGluZSAtIG5vZGUubG9jLmVuZC5saW5lXG59XG5cbmZ1bmN0aW9uIGlzQ2xhc3NXaXRoRGVjb3JhdG9yKG5vZGUpIHtcbiAgcmV0dXJuIG5vZGUudHlwZSA9PT0gJ0NsYXNzRGVjbGFyYXRpb24nICYmIG5vZGUuZGVjb3JhdG9ycyAmJiBub2RlLmRlY29yYXRvcnMubGVuZ3RoXG59XG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ2xheW91dCcsXG4gICAgZG9jczoge1xuICAgICAgdXJsOiBkb2NzVXJsKCduZXdsaW5lLWFmdGVyLWltcG9ydCcpLFxuICAgIH0sXG4gICAgZml4YWJsZTogJ3doaXRlc3BhY2UnLFxuICAgIHNjaGVtYTogW1xuICAgICAge1xuICAgICAgICAndHlwZSc6ICdvYmplY3QnLFxuICAgICAgICAncHJvcGVydGllcyc6IHtcbiAgICAgICAgICAnY291bnQnOiB7XG4gICAgICAgICAgICAndHlwZSc6ICdpbnRlZ2VyJyxcbiAgICAgICAgICAgICdtaW5pbXVtJzogMSxcbiAgICAgICAgICB9LFxuICAgICAgICB9LFxuICAgICAgICAnYWRkaXRpb25hbFByb3BlcnRpZXMnOiBmYWxzZSxcbiAgICAgIH0sXG4gICAgXSxcbiAgfSxcbiAgY3JlYXRlOiBmdW5jdGlvbiAoY29udGV4dCkge1xuICAgIGxldCBsZXZlbCA9IDBcbiAgICBjb25zdCByZXF1aXJlQ2FsbHMgPSBbXVxuXG4gICAgZnVuY3Rpb24gY2hlY2tGb3JOZXdMaW5lKG5vZGUsIG5leHROb2RlLCB0eXBlKSB7XG4gICAgICBpZiAoaXNDbGFzc1dpdGhEZWNvcmF0b3IobmV4dE5vZGUpKSB7XG4gICAgICAgIG5leHROb2RlID0gbmV4dE5vZGUuZGVjb3JhdG9yc1swXVxuICAgICAgfVxuXG4gICAgICBjb25zdCBvcHRpb25zID0gY29udGV4dC5vcHRpb25zWzBdIHx8IHsgY291bnQ6IDEgfVxuICAgICAgY29uc3QgbGluZURpZmZlcmVuY2UgPSBnZXRMaW5lRGlmZmVyZW5jZShub2RlLCBuZXh0Tm9kZSlcbiAgICAgIGNvbnN0IEVYUEVDVEVEX0xJTkVfRElGRkVSRU5DRSA9IG9wdGlvbnMuY291bnQgKyAxXG5cbiAgICAgIGlmIChsaW5lRGlmZmVyZW5jZSA8IEVYUEVDVEVEX0xJTkVfRElGRkVSRU5DRSkge1xuICAgICAgICBsZXQgY29sdW1uID0gbm9kZS5sb2Muc3RhcnQuY29sdW1uXG5cbiAgICAgICAgaWYgKG5vZGUubG9jLnN0YXJ0LmxpbmUgIT09IG5vZGUubG9jLmVuZC5saW5lKSB7XG4gICAgICAgICAgY29sdW1uID0gMFxuICAgICAgICB9XG5cbiAgICAgICAgY29udGV4dC5yZXBvcnQoe1xuICAgICAgICAgIGxvYzoge1xuICAgICAgICAgICAgbGluZTogbm9kZS5sb2MuZW5kLmxpbmUsXG4gICAgICAgICAgICBjb2x1bW4sXG4gICAgICAgICAgfSxcbiAgICAgICAgICBtZXNzYWdlOiBgRXhwZWN0ZWQgJHtvcHRpb25zLmNvdW50fSBlbXB0eSBsaW5lJHtvcHRpb25zLmNvdW50ID4gMSA/ICdzJyA6ICcnfSBcXFxuYWZ0ZXIgJHt0eXBlfSBzdGF0ZW1lbnQgbm90IGZvbGxvd2VkIGJ5IGFub3RoZXIgJHt0eXBlfS5gLFxuICAgICAgICAgIGZpeDogZml4ZXIgPT4gZml4ZXIuaW5zZXJ0VGV4dEFmdGVyKFxuICAgICAgICAgICAgbm9kZSxcbiAgICAgICAgICAgICdcXG4nLnJlcGVhdChFWFBFQ1RFRF9MSU5FX0RJRkZFUkVOQ0UgLSBsaW5lRGlmZmVyZW5jZSlcbiAgICAgICAgICApLFxuICAgICAgICB9KVxuICAgICAgfVxuICAgIH1cblxuICAgIGZ1bmN0aW9uIGluY3JlbWVudExldmVsKCkge1xuICAgICAgbGV2ZWwrK1xuICAgIH1cbiAgICBmdW5jdGlvbiBkZWNyZW1lbnRMZXZlbCgpIHtcbiAgICAgIGxldmVsLS1cbiAgICB9XG5cbiAgICByZXR1cm4ge1xuICAgICAgSW1wb3J0RGVjbGFyYXRpb246IGZ1bmN0aW9uIChub2RlKSB7XG4gICAgICAgIGNvbnN0IHsgcGFyZW50IH0gPSBub2RlXG4gICAgICAgIGNvbnN0IG5vZGVQb3NpdGlvbiA9IHBhcmVudC5ib2R5LmluZGV4T2Yobm9kZSlcbiAgICAgICAgY29uc3QgbmV4dE5vZGUgPSBwYXJlbnQuYm9keVtub2RlUG9zaXRpb24gKyAxXVxuXG4gICAgICAgIGlmIChuZXh0Tm9kZSAmJiBuZXh0Tm9kZS50eXBlICE9PSAnSW1wb3J0RGVjbGFyYXRpb24nKSB7XG4gICAgICAgICAgY2hlY2tGb3JOZXdMaW5lKG5vZGUsIG5leHROb2RlLCAnaW1wb3J0JylcbiAgICAgICAgfVxuICAgICAgfSxcbiAgICAgIENhbGxFeHByZXNzaW9uOiBmdW5jdGlvbihub2RlKSB7XG4gICAgICAgIGlmIChpc1N0YXRpY1JlcXVpcmUobm9kZSkgJiYgbGV2ZWwgPT09IDApIHtcbiAgICAgICAgICByZXF1aXJlQ2FsbHMucHVzaChub2RlKVxuICAgICAgICB9XG4gICAgICB9LFxuICAgICAgJ1Byb2dyYW06ZXhpdCc6IGZ1bmN0aW9uICgpIHtcbiAgICAgICAgbG9nKCdleGl0IHByb2Nlc3NpbmcgZm9yJywgY29udGV4dC5nZXRGaWxlbmFtZSgpKVxuICAgICAgICBjb25zdCBzY29wZUJvZHkgPSBnZXRTY29wZUJvZHkoY29udGV4dC5nZXRTY29wZSgpKVxuICAgICAgICBsb2coJ2dvdCBzY29wZTonLCBzY29wZUJvZHkpXG5cbiAgICAgICAgcmVxdWlyZUNhbGxzLmZvckVhY2goZnVuY3Rpb24gKG5vZGUsIGluZGV4KSB7XG4gICAgICAgICAgY29uc3Qgbm9kZVBvc2l0aW9uID0gZmluZE5vZGVJbmRleEluU2NvcGVCb2R5KHNjb3BlQm9keSwgbm9kZSlcbiAgICAgICAgICBsb2coJ25vZGUgcG9zaXRpb24gaW4gc2NvcGU6Jywgbm9kZVBvc2l0aW9uKVxuXG4gICAgICAgICAgY29uc3Qgc3RhdGVtZW50V2l0aFJlcXVpcmVDYWxsID0gc2NvcGVCb2R5W25vZGVQb3NpdGlvbl1cbiAgICAgICAgICBjb25zdCBuZXh0U3RhdGVtZW50ID0gc2NvcGVCb2R5W25vZGVQb3NpdGlvbiArIDFdXG4gICAgICAgICAgY29uc3QgbmV4dFJlcXVpcmVDYWxsID0gcmVxdWlyZUNhbGxzW2luZGV4ICsgMV1cblxuICAgICAgICAgIGlmIChuZXh0UmVxdWlyZUNhbGwgJiYgY29udGFpbnNOb2RlT3JFcXVhbChzdGF0ZW1lbnRXaXRoUmVxdWlyZUNhbGwsIG5leHRSZXF1aXJlQ2FsbCkpIHtcbiAgICAgICAgICAgIHJldHVyblxuICAgICAgICAgIH1cblxuICAgICAgICAgIGlmIChuZXh0U3RhdGVtZW50ICYmXG4gICAgICAgICAgICAgKCFuZXh0UmVxdWlyZUNhbGwgfHwgIWNvbnRhaW5zTm9kZU9yRXF1YWwobmV4dFN0YXRlbWVudCwgbmV4dFJlcXVpcmVDYWxsKSkpIHtcblxuICAgICAgICAgICAgY2hlY2tGb3JOZXdMaW5lKHN0YXRlbWVudFdpdGhSZXF1aXJlQ2FsbCwgbmV4dFN0YXRlbWVudCwgJ3JlcXVpcmUnKVxuICAgICAgICAgIH1cbiAgICAgICAgfSlcbiAgICAgIH0sXG4gICAgICBGdW5jdGlvbkRlY2xhcmF0aW9uOiBpbmNyZW1lbnRMZXZlbCxcbiAgICAgIEZ1bmN0aW9uRXhwcmVzc2lvbjogaW5jcmVtZW50TGV2ZWwsXG4gICAgICBBcnJvd0Z1bmN0aW9uRXhwcmVzc2lvbjogaW5jcmVtZW50TGV2ZWwsXG4gICAgICBCbG9ja1N0YXRlbWVudDogaW5jcmVtZW50TGV2ZWwsXG4gICAgICBPYmplY3RFeHByZXNzaW9uOiBpbmNyZW1lbnRMZXZlbCxcbiAgICAgIERlY29yYXRvcjogaW5jcmVtZW50TGV2ZWwsXG4gICAgICAnRnVuY3Rpb25EZWNsYXJhdGlvbjpleGl0JzogZGVjcmVtZW50TGV2ZWwsXG4gICAgICAnRnVuY3Rpb25FeHByZXNzaW9uOmV4aXQnOiBkZWNyZW1lbnRMZXZlbCxcbiAgICAgICdBcnJvd0Z1bmN0aW9uRXhwcmVzc2lvbjpleGl0JzogZGVjcmVtZW50TGV2ZWwsXG4gICAgICAnQmxvY2tTdGF0ZW1lbnQ6ZXhpdCc6IGRlY3JlbWVudExldmVsLFxuICAgICAgJ09iamVjdEV4cHJlc3Npb246ZXhpdCc6IGRlY3JlbWVudExldmVsLFxuICAgICAgJ0RlY29yYXRvcjpleGl0JzogZGVjcmVtZW50TGV2ZWwsXG4gICAgfVxuICB9LFxufVxuIl19