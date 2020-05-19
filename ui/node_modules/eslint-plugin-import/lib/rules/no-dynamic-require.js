'use strict';

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function isRequire(node) {
  return node && node.callee && node.callee.type === 'Identifier' && node.callee.name === 'require' && node.arguments.length >= 1;
}

function isStaticValue(arg) {
  return arg.type === 'Literal' || arg.type === 'TemplateLiteral' && arg.expressions.length === 0;
}

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('no-dynamic-require')
    },
    schema: []
  },

  create: function (context) {
    return {
      CallExpression(node) {
        if (isRequire(node) && !isStaticValue(node.arguments[0])) {
          context.report({
            node,
            message: 'Calls to require() should use string literals'
          });
        }
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1keW5hbWljLXJlcXVpcmUuanMiXSwibmFtZXMiOlsiaXNSZXF1aXJlIiwibm9kZSIsImNhbGxlZSIsInR5cGUiLCJuYW1lIiwiYXJndW1lbnRzIiwibGVuZ3RoIiwiaXNTdGF0aWNWYWx1ZSIsImFyZyIsImV4cHJlc3Npb25zIiwibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJkb2NzIiwidXJsIiwic2NoZW1hIiwiY3JlYXRlIiwiY29udGV4dCIsIkNhbGxFeHByZXNzaW9uIiwicmVwb3J0IiwibWVzc2FnZSJdLCJtYXBwaW5ncyI6Ijs7QUFBQTs7Ozs7O0FBRUEsU0FBU0EsU0FBVCxDQUFtQkMsSUFBbkIsRUFBeUI7QUFDdkIsU0FBT0EsUUFDTEEsS0FBS0MsTUFEQSxJQUVMRCxLQUFLQyxNQUFMLENBQVlDLElBQVosS0FBcUIsWUFGaEIsSUFHTEYsS0FBS0MsTUFBTCxDQUFZRSxJQUFaLEtBQXFCLFNBSGhCLElBSUxILEtBQUtJLFNBQUwsQ0FBZUMsTUFBZixJQUF5QixDQUozQjtBQUtEOztBQUVELFNBQVNDLGFBQVQsQ0FBdUJDLEdBQXZCLEVBQTRCO0FBQzFCLFNBQU9BLElBQUlMLElBQUosS0FBYSxTQUFiLElBQ0pLLElBQUlMLElBQUosS0FBYSxpQkFBYixJQUFrQ0ssSUFBSUMsV0FBSixDQUFnQkgsTUFBaEIsS0FBMkIsQ0FEaEU7QUFFRDs7QUFFREksT0FBT0MsT0FBUCxHQUFpQjtBQUNmQyxRQUFNO0FBQ0pULFVBQU0sWUFERjtBQUVKVSxVQUFNO0FBQ0pDLFdBQUssdUJBQVEsb0JBQVI7QUFERCxLQUZGO0FBS0pDLFlBQVE7QUFMSixHQURTOztBQVNmQyxVQUFRLFVBQVVDLE9BQVYsRUFBbUI7QUFDekIsV0FBTztBQUNMQyxxQkFBZWpCLElBQWYsRUFBcUI7QUFDbkIsWUFBSUQsVUFBVUMsSUFBVixLQUFtQixDQUFDTSxjQUFjTixLQUFLSSxTQUFMLENBQWUsQ0FBZixDQUFkLENBQXhCLEVBQTBEO0FBQ3hEWSxrQkFBUUUsTUFBUixDQUFlO0FBQ2JsQixnQkFEYTtBQUVibUIscUJBQVM7QUFGSSxXQUFmO0FBSUQ7QUFDRjtBQVJJLEtBQVA7QUFVRDtBQXBCYyxDQUFqQiIsImZpbGUiOiJuby1keW5hbWljLXJlcXVpcmUuanMiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJ1xuXG5mdW5jdGlvbiBpc1JlcXVpcmUobm9kZSkge1xuICByZXR1cm4gbm9kZSAmJlxuICAgIG5vZGUuY2FsbGVlICYmXG4gICAgbm9kZS5jYWxsZWUudHlwZSA9PT0gJ0lkZW50aWZpZXInICYmXG4gICAgbm9kZS5jYWxsZWUubmFtZSA9PT0gJ3JlcXVpcmUnICYmXG4gICAgbm9kZS5hcmd1bWVudHMubGVuZ3RoID49IDFcbn1cblxuZnVuY3Rpb24gaXNTdGF0aWNWYWx1ZShhcmcpIHtcbiAgcmV0dXJuIGFyZy50eXBlID09PSAnTGl0ZXJhbCcgfHxcbiAgICAoYXJnLnR5cGUgPT09ICdUZW1wbGF0ZUxpdGVyYWwnICYmIGFyZy5leHByZXNzaW9ucy5sZW5ndGggPT09IDApXG59XG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3N1Z2dlc3Rpb24nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnbm8tZHluYW1pYy1yZXF1aXJlJyksXG4gICAgfSxcbiAgICBzY2hlbWE6IFtdLFxuICB9LFxuXG4gIGNyZWF0ZTogZnVuY3Rpb24gKGNvbnRleHQpIHtcbiAgICByZXR1cm4ge1xuICAgICAgQ2FsbEV4cHJlc3Npb24obm9kZSkge1xuICAgICAgICBpZiAoaXNSZXF1aXJlKG5vZGUpICYmICFpc1N0YXRpY1ZhbHVlKG5vZGUuYXJndW1lbnRzWzBdKSkge1xuICAgICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICAgIG5vZGUsXG4gICAgICAgICAgICBtZXNzYWdlOiAnQ2FsbHMgdG8gcmVxdWlyZSgpIHNob3VsZCB1c2Ugc3RyaW5nIGxpdGVyYWxzJyxcbiAgICAgICAgICB9KVxuICAgICAgICB9XG4gICAgICB9LFxuICAgIH1cbiAgfSxcbn1cbiJdfQ==