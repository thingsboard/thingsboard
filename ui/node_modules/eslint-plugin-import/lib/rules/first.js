'use strict';

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('first')
    },
    fixable: 'code',
    schema: [{
      type: 'string',
      enum: ['absolute-first']
    }]
  },

  create: function (context) {
    function isPossibleDirective(node) {
      return node.type === 'ExpressionStatement' && node.expression.type === 'Literal' && typeof node.expression.value === 'string';
    }

    return {
      'Program': function (n) {
        const body = n.body,
              absoluteFirst = context.options[0] === 'absolute-first',
              message = 'Import in body of module; reorder to top.',
              sourceCode = context.getSourceCode(),
              originSourceCode = sourceCode.getText();
        let nonImportCount = 0,
            anyExpressions = false,
            anyRelative = false,
            lastLegalImp = null,
            errorInfos = [],
            shouldSort = true,
            lastSortNodesIndex = 0;
        body.forEach(function (node, index) {
          if (!anyExpressions && isPossibleDirective(node)) {
            return;
          }

          anyExpressions = true;

          if (node.type === 'ImportDeclaration') {
            if (absoluteFirst) {
              if (/^\./.test(node.source.value)) {
                anyRelative = true;
              } else if (anyRelative) {
                context.report({
                  node: node.source,
                  message: 'Absolute imports should come before relative imports.'
                });
              }
            }
            if (nonImportCount > 0) {
              for (let variable of context.getDeclaredVariables(node)) {
                if (!shouldSort) break;
                const references = variable.references;
                if (references.length) {
                  for (let reference of references) {
                    if (reference.identifier.range[0] < node.range[1]) {
                      shouldSort = false;
                      break;
                    }
                  }
                }
              }
              shouldSort && (lastSortNodesIndex = errorInfos.length);
              errorInfos.push({
                node,
                range: [body[index - 1].range[1], node.range[1]]
              });
            } else {
              lastLegalImp = node;
            }
          } else {
            nonImportCount++;
          }
        });
        if (!errorInfos.length) return;
        errorInfos.forEach(function (errorInfo, index) {
          const node = errorInfo.node,
                infos = {
            node,
            message
          };
          if (index < lastSortNodesIndex) {
            infos.fix = function (fixer) {
              return fixer.insertTextAfter(node, '');
            };
          } else if (index === lastSortNodesIndex) {
            const sortNodes = errorInfos.slice(0, lastSortNodesIndex + 1);
            infos.fix = function (fixer) {
              const removeFixers = sortNodes.map(function (_errorInfo) {
                return fixer.removeRange(_errorInfo.range);
              }),
                    range = [0, removeFixers[removeFixers.length - 1].range[1]];
              let insertSourceCode = sortNodes.map(function (_errorInfo) {
                const nodeSourceCode = String.prototype.slice.apply(originSourceCode, _errorInfo.range);
                if (/\S/.test(nodeSourceCode[0])) {
                  return '\n' + nodeSourceCode;
                }
                return nodeSourceCode;
              }).join(''),
                  insertFixer = null,
                  replaceSourceCode = '';
              if (!lastLegalImp) {
                insertSourceCode = insertSourceCode.trim() + insertSourceCode.match(/^(\s+)/)[0];
              }
              insertFixer = lastLegalImp ? fixer.insertTextAfter(lastLegalImp, insertSourceCode) : fixer.insertTextBefore(body[0], insertSourceCode);
              const fixers = [insertFixer].concat(removeFixers);
              fixers.forEach(function (computedFixer, i) {
                replaceSourceCode += originSourceCode.slice(fixers[i - 1] ? fixers[i - 1].range[1] : 0, computedFixer.range[0]) + computedFixer.text;
              });
              return fixer.replaceTextRange(range, replaceSourceCode);
            };
          }
          context.report(infos);
        });
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9maXJzdC5qcyJdLCJuYW1lcyI6WyJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsInR5cGUiLCJkb2NzIiwidXJsIiwiZml4YWJsZSIsInNjaGVtYSIsImVudW0iLCJjcmVhdGUiLCJjb250ZXh0IiwiaXNQb3NzaWJsZURpcmVjdGl2ZSIsIm5vZGUiLCJleHByZXNzaW9uIiwidmFsdWUiLCJuIiwiYm9keSIsImFic29sdXRlRmlyc3QiLCJvcHRpb25zIiwibWVzc2FnZSIsInNvdXJjZUNvZGUiLCJnZXRTb3VyY2VDb2RlIiwib3JpZ2luU291cmNlQ29kZSIsImdldFRleHQiLCJub25JbXBvcnRDb3VudCIsImFueUV4cHJlc3Npb25zIiwiYW55UmVsYXRpdmUiLCJsYXN0TGVnYWxJbXAiLCJlcnJvckluZm9zIiwic2hvdWxkU29ydCIsImxhc3RTb3J0Tm9kZXNJbmRleCIsImZvckVhY2giLCJpbmRleCIsInRlc3QiLCJzb3VyY2UiLCJyZXBvcnQiLCJ2YXJpYWJsZSIsImdldERlY2xhcmVkVmFyaWFibGVzIiwicmVmZXJlbmNlcyIsImxlbmd0aCIsInJlZmVyZW5jZSIsImlkZW50aWZpZXIiLCJyYW5nZSIsInB1c2giLCJlcnJvckluZm8iLCJpbmZvcyIsImZpeCIsImZpeGVyIiwiaW5zZXJ0VGV4dEFmdGVyIiwic29ydE5vZGVzIiwic2xpY2UiLCJyZW1vdmVGaXhlcnMiLCJtYXAiLCJfZXJyb3JJbmZvIiwicmVtb3ZlUmFuZ2UiLCJpbnNlcnRTb3VyY2VDb2RlIiwibm9kZVNvdXJjZUNvZGUiLCJTdHJpbmciLCJwcm90b3R5cGUiLCJhcHBseSIsImpvaW4iLCJpbnNlcnRGaXhlciIsInJlcGxhY2VTb3VyY2VDb2RlIiwidHJpbSIsIm1hdGNoIiwiaW5zZXJ0VGV4dEJlZm9yZSIsImZpeGVycyIsImNvbmNhdCIsImNvbXB1dGVkRml4ZXIiLCJpIiwidGV4dCIsInJlcGxhY2VUZXh0UmFuZ2UiXSwibWFwcGluZ3MiOiI7O0FBQUE7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QUNELHVCQUFPQSxjQUFQO0FBQ0QsZUFSa0IsRUFRaEJJLElBUmdCLENBUVgsRUFSVyxDQUF2QjtBQUFBLGtCQVNJQyxjQUFjLElBVGxCO0FBQUEsa0JBVUlDLG9CQUFvQixFQVZ4QjtBQVdBLGtCQUFJLENBQUNuQyxZQUFMLEVBQW1CO0FBQ2Y0QixtQ0FDRUEsaUJBQWlCUSxJQUFqQixLQUEwQlIsaUJBQWlCUyxLQUFqQixDQUF1QixRQUF2QixFQUFpQyxDQUFqQyxDQUQ1QjtBQUVIO0FBQ0RILDRCQUFjbEMsZUFDQW9CLE1BQU1DLGVBQU4sQ0FBc0JyQixZQUF0QixFQUFvQzRCLGdCQUFwQyxDQURBLEdBRUFSLE1BQU1rQixnQkFBTixDQUF1QmpELEtBQUssQ0FBTCxDQUF2QixFQUFnQ3VDLGdCQUFoQyxDQUZkO0FBR0Esb0JBQU1XLFNBQVMsQ0FBQ0wsV0FBRCxFQUFjTSxNQUFkLENBQXFCaEIsWUFBckIsQ0FBZjtBQUNBZSxxQkFBT25DLE9BQVAsQ0FBZSxVQUFVcUMsYUFBVixFQUF5QkMsQ0FBekIsRUFBNEI7QUFDekNQLHFDQUFzQnhDLGlCQUFpQjRCLEtBQWpCLENBQ3BCZ0IsT0FBT0csSUFBSSxDQUFYLElBQWdCSCxPQUFPRyxJQUFJLENBQVgsRUFBYzNCLEtBQWQsQ0FBb0IsQ0FBcEIsQ0FBaEIsR0FBeUMsQ0FEckIsRUFDd0IwQixjQUFjMUIsS0FBZCxDQUFvQixDQUFwQixDQUR4QixJQUVsQjBCLGNBQWNFLElBRmxCO0FBR0QsZUFKRDtBQUtBLHFCQUFPdkIsTUFBTXdCLGdCQUFOLENBQXVCN0IsS0FBdkIsRUFBOEJvQixpQkFBOUIsQ0FBUDtBQUNELGFBOUJEO0FBK0JEO0FBQ0RwRCxrQkFBUXlCLE1BQVIsQ0FBZVUsS0FBZjtBQUNELFNBN0NEO0FBOENEO0FBeEdJLEtBQVA7QUEwR0Q7QUFoSWMsQ0FBakIiLCJmaWxlIjoiZmlyc3QuanMiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJ1xuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdzdWdnZXN0aW9uJyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ2ZpcnN0JyksXG4gICAgfSxcbiAgICBmaXhhYmxlOiAnY29kZScsXG4gICAgc2NoZW1hOiBbXG4gICAgICB7XG4gICAgICAgIHR5cGU6ICdzdHJpbmcnLFxuICAgICAgICBlbnVtOiBbJ2Fic29sdXRlLWZpcnN0J10sXG4gICAgICB9LFxuICAgIF0sXG4gIH0sXG5cbiAgY3JlYXRlOiBmdW5jdGlvbiAoY29udGV4dCkge1xuICAgIGZ1bmN0aW9uIGlzUG9zc2libGVEaXJlY3RpdmUgKG5vZGUpIHtcbiAgICAgIHJldHVybiBub2RlLnR5cGUgPT09ICdFeHByZXNzaW9uU3RhdGVtZW50JyAmJlxuICAgICAgICBub2RlLmV4cHJlc3Npb24udHlwZSA9PT0gJ0xpdGVyYWwnICYmXG4gICAgICAgIHR5cGVvZiBub2RlLmV4cHJlc3Npb24udmFsdWUgPT09ICdzdHJpbmcnXG4gICAgfVxuXG4gICAgcmV0dXJuIHtcbiAgICAgICdQcm9ncmFtJzogZnVuY3Rpb24gKG4pIHtcbiAgICAgICAgY29uc3QgYm9keSA9IG4uYm9keVxuICAgICAgICAgICAgLCBhYnNvbHV0ZUZpcnN0ID0gY29udGV4dC5vcHRpb25zWzBdID09PSAnYWJzb2x1dGUtZmlyc3QnXG4gICAgICAgICAgICAsIG1lc3NhZ2UgPSAnSW1wb3J0IGluIGJvZHkgb2YgbW9kdWxlOyByZW9yZGVyIHRvIHRvcC4nXG4gICAgICAgICAgICAsIHNvdXJjZUNvZGUgPSBjb250ZXh0LmdldFNvdXJjZUNvZGUoKVxuICAgICAgICAgICAgLCBvcmlnaW5Tb3VyY2VDb2RlID0gc291cmNlQ29kZS5nZXRUZXh0KClcbiAgICAgICAgbGV0IG5vbkltcG9ydENvdW50ID0gMFxuICAgICAgICAgICwgYW55RXhwcmVzc2lvbnMgPSBmYWxzZVxuICAgICAgICAgICwgYW55UmVsYXRpdmUgPSBmYWxzZVxuICAgICAgICAgICwgbGFzdExlZ2FsSW1wID0gbnVsbFxuICAgICAgICAgICwgZXJyb3JJbmZvcyA9IFtdXG4gICAgICAgICAgLCBzaG91bGRTb3J0ID0gdHJ1ZVxuICAgICAgICAgICwgbGFzdFNvcnROb2Rlc0luZGV4ID0gMFxuICAgICAgICBib2R5LmZvckVhY2goZnVuY3Rpb24gKG5vZGUsIGluZGV4KXtcbiAgICAgICAgICBpZiAoIWFueUV4cHJlc3Npb25zICYmIGlzUG9zc2libGVEaXJlY3RpdmUobm9kZSkpIHtcbiAgICAgICAgICAgIHJldHVyblxuICAgICAgICAgIH1cblxuICAgICAgICAgIGFueUV4cHJlc3Npb25zID0gdHJ1ZVxuXG4gICAgICAgICAgaWYgKG5vZGUudHlwZSA9PT0gJ0ltcG9ydERlY2xhcmF0aW9uJykge1xuICAgICAgICAgICAgaWYgKGFic29sdXRlRmlyc3QpIHtcbiAgICAgICAgICAgICAgaWYgKC9eXFwuLy50ZXN0KG5vZGUuc291cmNlLnZhbHVlKSkge1xuICAgICAgICAgICAgICAgIGFueVJlbGF0aXZlID0gdHJ1ZVxuICAgICAgICAgICAgICB9IGVsc2UgaWYgKGFueVJlbGF0aXZlKSB7XG4gICAgICAgICAgICAgICAgY29udGV4dC5yZXBvcnQoe1xuICAgICAgICAgICAgICAgICAgbm9kZTogbm9kZS5zb3VyY2UsXG4gICAgICAgICAgICAgICAgICBtZXNzYWdlOiAnQWJzb2x1dGUgaW1wb3J0cyBzaG91bGQgY29tZSBiZWZvcmUgcmVsYXRpdmUgaW1wb3J0cy4nLFxuICAgICAgICAgICAgICAgIH0pXG4gICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmIChub25JbXBvcnRDb3VudCA+IDApIHtcbiAgICAgICAgICAgICAgZm9yIChsZXQgdmFyaWFibGUgb2YgY29udGV4dC5nZXREZWNsYXJlZFZhcmlhYmxlcyhub2RlKSkge1xuICAgICAgICAgICAgICAgIGlmICghc2hvdWxkU29ydCkgYnJlYWtcbiAgICAgICAgICAgICAgICBjb25zdCByZWZlcmVuY2VzID0gdmFyaWFibGUucmVmZXJlbmNlc1xuICAgICAgICAgICAgICAgIGlmIChyZWZlcmVuY2VzLmxlbmd0aCkge1xuICAgICAgICAgICAgICAgICAgZm9yIChsZXQgcmVmZXJlbmNlIG9mIHJlZmVyZW5jZXMpIHtcbiAgICAgICAgICAgICAgICAgICAgaWYgKHJlZmVyZW5jZS5pZGVudGlmaWVyLnJhbmdlWzBdIDwgbm9kZS5yYW5nZVsxXSkge1xuICAgICAgICAgICAgICAgICAgICAgIHNob3VsZFNvcnQgPSBmYWxzZVxuICAgICAgICAgICAgICAgICAgICAgIGJyZWFrXG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgc2hvdWxkU29ydCAmJiAobGFzdFNvcnROb2Rlc0luZGV4ID0gZXJyb3JJbmZvcy5sZW5ndGgpXG4gICAgICAgICAgICAgIGVycm9ySW5mb3MucHVzaCh7XG4gICAgICAgICAgICAgICAgbm9kZSxcbiAgICAgICAgICAgICAgICByYW5nZTogW2JvZHlbaW5kZXggLSAxXS5yYW5nZVsxXSwgbm9kZS5yYW5nZVsxXV0sXG4gICAgICAgICAgICAgIH0pXG4gICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICBsYXN0TGVnYWxJbXAgPSBub2RlXG4gICAgICAgICAgICB9XG4gICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIG5vbkltcG9ydENvdW50KytcbiAgICAgICAgICB9XG4gICAgICAgIH0pXG4gICAgICAgIGlmICghZXJyb3JJbmZvcy5sZW5ndGgpIHJldHVyblxuICAgICAgICBlcnJvckluZm9zLmZvckVhY2goZnVuY3Rpb24gKGVycm9ySW5mbywgaW5kZXgpIHtcbiAgICAgICAgICBjb25zdCBub2RlID0gZXJyb3JJbmZvLm5vZGVcbiAgICAgICAgICAgICAgLCBpbmZvcyA9IHtcbiAgICAgICAgICAgICAgICBub2RlLFxuICAgICAgICAgICAgICAgIG1lc3NhZ2UsXG4gICAgICAgICAgICAgIH1cbiAgICAgICAgICBpZiAoaW5kZXggPCBsYXN0U29ydE5vZGVzSW5kZXgpIHtcbiAgICAgICAgICAgIGluZm9zLmZpeCA9IGZ1bmN0aW9uIChmaXhlcikge1xuICAgICAgICAgICAgICByZXR1cm4gZml4ZXIuaW5zZXJ0VGV4dEFmdGVyKG5vZGUsICcnKVxuICAgICAgICAgICAgfVxuICAgICAgICAgIH0gZWxzZSBpZiAoaW5kZXggPT09IGxhc3RTb3J0Tm9kZXNJbmRleCkge1xuICAgICAgICAgICAgY29uc3Qgc29ydE5vZGVzID0gZXJyb3JJbmZvcy5zbGljZSgwLCBsYXN0U29ydE5vZGVzSW5kZXggKyAxKVxuICAgICAgICAgICAgaW5mb3MuZml4ID0gZnVuY3Rpb24gKGZpeGVyKSB7XG4gICAgICAgICAgICAgIGNvbnN0IHJlbW92ZUZpeGVycyA9IHNvcnROb2Rlcy5tYXAoZnVuY3Rpb24gKF9lcnJvckluZm8pIHtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIGZpeGVyLnJlbW92ZVJhbmdlKF9lcnJvckluZm8ucmFuZ2UpXG4gICAgICAgICAgICAgICAgICB9KVxuICAgICAgICAgICAgICAgICAgLCByYW5nZSA9IFswLCByZW1vdmVGaXhlcnNbcmVtb3ZlRml4ZXJzLmxlbmd0aCAtIDFdLnJhbmdlWzFdXVxuICAgICAgICAgICAgICBsZXQgaW5zZXJ0U291cmNlQ29kZSA9IHNvcnROb2Rlcy5tYXAoZnVuY3Rpb24gKF9lcnJvckluZm8pIHtcbiAgICAgICAgICAgICAgICAgICAgY29uc3Qgbm9kZVNvdXJjZUNvZGUgPSBTdHJpbmcucHJvdG90eXBlLnNsaWNlLmFwcGx5KFxuICAgICAgICAgICAgICAgICAgICAgIG9yaWdpblNvdXJjZUNvZGUsIF9lcnJvckluZm8ucmFuZ2VcbiAgICAgICAgICAgICAgICAgICAgKVxuICAgICAgICAgICAgICAgICAgICBpZiAoL1xcUy8udGVzdChub2RlU291cmNlQ29kZVswXSkpIHtcbiAgICAgICAgICAgICAgICAgICAgICByZXR1cm4gJ1xcbicgKyBub2RlU291cmNlQ29kZVxuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIHJldHVybiBub2RlU291cmNlQ29kZVxuICAgICAgICAgICAgICAgICAgfSkuam9pbignJylcbiAgICAgICAgICAgICAgICAsIGluc2VydEZpeGVyID0gbnVsbFxuICAgICAgICAgICAgICAgICwgcmVwbGFjZVNvdXJjZUNvZGUgPSAnJ1xuICAgICAgICAgICAgICBpZiAoIWxhc3RMZWdhbEltcCkge1xuICAgICAgICAgICAgICAgICAgaW5zZXJ0U291cmNlQ29kZSA9XG4gICAgICAgICAgICAgICAgICAgIGluc2VydFNvdXJjZUNvZGUudHJpbSgpICsgaW5zZXJ0U291cmNlQ29kZS5tYXRjaCgvXihcXHMrKS8pWzBdXG4gICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgaW5zZXJ0Rml4ZXIgPSBsYXN0TGVnYWxJbXAgP1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGZpeGVyLmluc2VydFRleHRBZnRlcihsYXN0TGVnYWxJbXAsIGluc2VydFNvdXJjZUNvZGUpIDpcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBmaXhlci5pbnNlcnRUZXh0QmVmb3JlKGJvZHlbMF0sIGluc2VydFNvdXJjZUNvZGUpXG4gICAgICAgICAgICAgIGNvbnN0IGZpeGVycyA9IFtpbnNlcnRGaXhlcl0uY29uY2F0KHJlbW92ZUZpeGVycylcbiAgICAgICAgICAgICAgZml4ZXJzLmZvckVhY2goZnVuY3Rpb24gKGNvbXB1dGVkRml4ZXIsIGkpIHtcbiAgICAgICAgICAgICAgICByZXBsYWNlU291cmNlQ29kZSArPSAob3JpZ2luU291cmNlQ29kZS5zbGljZShcbiAgICAgICAgICAgICAgICAgIGZpeGVyc1tpIC0gMV0gPyBmaXhlcnNbaSAtIDFdLnJhbmdlWzFdIDogMCwgY29tcHV0ZWRGaXhlci5yYW5nZVswXVxuICAgICAgICAgICAgICAgICkgKyBjb21wdXRlZEZpeGVyLnRleHQpXG4gICAgICAgICAgICAgIH0pXG4gICAgICAgICAgICAgIHJldHVybiBmaXhlci5yZXBsYWNlVGV4dFJhbmdlKHJhbmdlLCByZXBsYWNlU291cmNlQ29kZSlcbiAgICAgICAgICAgIH1cbiAgICAgICAgICB9XG4gICAgICAgICAgY29udGV4dC5yZXBvcnQoaW5mb3MpXG4gICAgICAgIH0pXG4gICAgICB9LFxuICAgIH1cbiAgfSxcbn1cbiJdfQ==