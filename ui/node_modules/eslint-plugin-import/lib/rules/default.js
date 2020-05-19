'use strict';

var _ExportMap = require('../ExportMap');

var _ExportMap2 = _interopRequireDefault(_ExportMap);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2.default)('default')
    },
    schema: []
  },

  create: function (context) {

    function checkDefault(specifierType, node) {

      const defaultSpecifier = node.specifiers.find(specifier => specifier.type === specifierType);

      if (!defaultSpecifier) return;
      var imports = _ExportMap2.default.get(node.source.value, context);
      if (imports == null) return;

      if (imports.errors.length) {
        imports.reportErrors(context, node);
      } else if (imports.get('default') === undefined) {
        context.report({
          node: defaultSpecifier,
          message: `No default export found in imported module "${node.source.value}".`
        });
      }
    }

    return {
      'ImportDeclaration': checkDefault.bind(null, 'ImportDefaultSpecifier'),
      'ExportNamedDeclaration': checkDefault.bind(null, 'ExportDefaultSpecifier')
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9kZWZhdWx0LmpzIl0sIm5hbWVzIjpbIm1vZHVsZSIsImV4cG9ydHMiLCJtZXRhIiwidHlwZSIsImRvY3MiLCJ1cmwiLCJzY2hlbWEiLCJjcmVhdGUiLCJjb250ZXh0IiwiY2hlY2tEZWZhdWx0Iiwic3BlY2lmaWVyVHlwZSIsIm5vZGUiLCJkZWZhdWx0U3BlY2lmaWVyIiwic3BlY2lmaWVycyIsImZpbmQiLCJzcGVjaWZpZXIiLCJpbXBvcnRzIiwiRXhwb3J0cyIsImdldCIsInNvdXJjZSIsInZhbHVlIiwiZXJyb3JzIiwibGVuZ3RoIiwicmVwb3J0RXJyb3JzIiwidW5kZWZpbmVkIiwicmVwb3J0IiwibWVzc2FnZSIsImJpbmQiXSwibWFwcGluZ3MiOiI7O0FBQUE7Ozs7QUFDQTs7Ozs7O0FBRUFBLE9BQU9DLE9BQVAsR0FBaUI7QUFDZkMsUUFBTTtBQUNKQyxVQUFNLFNBREY7QUFFSkMsVUFBTTtBQUNKQyxXQUFLLHVCQUFRLFNBQVI7QUFERCxLQUZGO0FBS0pDLFlBQVE7QUFMSixHQURTOztBQVNmQyxVQUFRLFVBQVVDLE9BQVYsRUFBbUI7O0FBRXpCLGFBQVNDLFlBQVQsQ0FBc0JDLGFBQXRCLEVBQXFDQyxJQUFyQyxFQUEyQzs7QUFFekMsWUFBTUMsbUJBQW1CRCxLQUFLRSxVQUFMLENBQWdCQyxJQUFoQixDQUN2QkMsYUFBYUEsVUFBVVosSUFBVixLQUFtQk8sYUFEVCxDQUF6Qjs7QUFJQSxVQUFJLENBQUNFLGdCQUFMLEVBQXVCO0FBQ3ZCLFVBQUlJLFVBQVVDLG9CQUFRQyxHQUFSLENBQVlQLEtBQUtRLE1BQUwsQ0FBWUMsS0FBeEIsRUFBK0JaLE9BQS9CLENBQWQ7QUFDQSxVQUFJUSxXQUFXLElBQWYsRUFBcUI7O0FBRXJCLFVBQUlBLFFBQVFLLE1BQVIsQ0FBZUMsTUFBbkIsRUFBMkI7QUFDekJOLGdCQUFRTyxZQUFSLENBQXFCZixPQUFyQixFQUE4QkcsSUFBOUI7QUFDRCxPQUZELE1BRU8sSUFBSUssUUFBUUUsR0FBUixDQUFZLFNBQVosTUFBMkJNLFNBQS9CLEVBQTBDO0FBQy9DaEIsZ0JBQVFpQixNQUFSLENBQWU7QUFDYmQsZ0JBQU1DLGdCQURPO0FBRWJjLG1CQUFVLCtDQUE4Q2YsS0FBS1EsTUFBTCxDQUFZQyxLQUFNO0FBRjdELFNBQWY7QUFJRDtBQUNGOztBQUVELFdBQU87QUFDTCwyQkFBcUJYLGFBQWFrQixJQUFiLENBQWtCLElBQWxCLEVBQXdCLHdCQUF4QixDQURoQjtBQUVMLGdDQUEwQmxCLGFBQWFrQixJQUFiLENBQWtCLElBQWxCLEVBQXdCLHdCQUF4QjtBQUZyQixLQUFQO0FBSUQ7QUFuQ2MsQ0FBakIiLCJmaWxlIjoiZGVmYXVsdC5qcyIsInNvdXJjZXNDb250ZW50IjpbImltcG9ydCBFeHBvcnRzIGZyb20gJy4uL0V4cG9ydE1hcCdcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnXG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3Byb2JsZW0nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnZGVmYXVsdCcpLFxuICAgIH0sXG4gICAgc2NoZW1hOiBbXSxcbiAgfSxcblxuICBjcmVhdGU6IGZ1bmN0aW9uIChjb250ZXh0KSB7XG5cbiAgICBmdW5jdGlvbiBjaGVja0RlZmF1bHQoc3BlY2lmaWVyVHlwZSwgbm9kZSkge1xuXG4gICAgICBjb25zdCBkZWZhdWx0U3BlY2lmaWVyID0gbm9kZS5zcGVjaWZpZXJzLmZpbmQoXG4gICAgICAgIHNwZWNpZmllciA9PiBzcGVjaWZpZXIudHlwZSA9PT0gc3BlY2lmaWVyVHlwZVxuICAgICAgKVxuXG4gICAgICBpZiAoIWRlZmF1bHRTcGVjaWZpZXIpIHJldHVyblxuICAgICAgdmFyIGltcG9ydHMgPSBFeHBvcnRzLmdldChub2RlLnNvdXJjZS52YWx1ZSwgY29udGV4dClcbiAgICAgIGlmIChpbXBvcnRzID09IG51bGwpIHJldHVyblxuXG4gICAgICBpZiAoaW1wb3J0cy5lcnJvcnMubGVuZ3RoKSB7XG4gICAgICAgIGltcG9ydHMucmVwb3J0RXJyb3JzKGNvbnRleHQsIG5vZGUpXG4gICAgICB9IGVsc2UgaWYgKGltcG9ydHMuZ2V0KCdkZWZhdWx0JykgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgICAgbm9kZTogZGVmYXVsdFNwZWNpZmllcixcbiAgICAgICAgICBtZXNzYWdlOiBgTm8gZGVmYXVsdCBleHBvcnQgZm91bmQgaW4gaW1wb3J0ZWQgbW9kdWxlIFwiJHtub2RlLnNvdXJjZS52YWx1ZX1cIi5gLFxuICAgICAgICB9KVxuICAgICAgfVxuICAgIH1cblxuICAgIHJldHVybiB7XG4gICAgICAnSW1wb3J0RGVjbGFyYXRpb24nOiBjaGVja0RlZmF1bHQuYmluZChudWxsLCAnSW1wb3J0RGVmYXVsdFNwZWNpZmllcicpLFxuICAgICAgJ0V4cG9ydE5hbWVkRGVjbGFyYXRpb24nOiBjaGVja0RlZmF1bHQuYmluZChudWxsLCAnRXhwb3J0RGVmYXVsdFNwZWNpZmllcicpLFxuICAgIH1cbiAgfSxcbn1cbiJdfQ==