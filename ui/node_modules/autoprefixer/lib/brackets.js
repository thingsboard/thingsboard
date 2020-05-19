"use strict";

function last(array) {
  return array[array.length - 1];
}

var brackets = {
  /**
     * Parse string to nodes tree
     */
  parse: function parse(str) {
    var current = [''];
    var stack = [current];

    for (var _iterator = str, _isArray = Array.isArray(_iterator), _i = 0, _iterator = _isArray ? _iterator : _iterator[Symbol.iterator]();;) {
      var _ref;

      if (_isArray) {
        if (_i >= _iterator.length) break;
        _ref = _iterator[_i++];
      } else {
        _i = _iterator.next();
        if (_i.done) break;
        _ref = _i.value;
      }

      var sym = _ref;

      if (sym === '(') {
        current = [''];
        last(stack).push(current);
        stack.push(current);
        continue;
      }

      if (sym === ')') {
        stack.pop();
        current = last(stack);
        current.push('');
        continue;
      }

      current[current.length - 1] += sym;
    }

    return stack[0];
  },

  /**
     * Generate output string by nodes tree
     */
  stringify: function stringify(ast) {
    var result = '';

    for (var _iterator2 = ast, _isArray2 = Array.isArray(_iterator2), _i2 = 0, _iterator2 = _isArray2 ? _iterator2 : _iterator2[Symbol.iterator]();;) {
      var _ref2;

      if (_isArray2) {
        if (_i2 >= _iterator2.length) break;
        _ref2 = _iterator2[_i2++];
      } else {
        _i2 = _iterator2.next();
        if (_i2.done) break;
        _ref2 = _i2.value;
      }

      var i = _ref2;

      if (typeof i === 'object') {
        result += "(" + brackets.stringify(i) + ")";
        continue;
      }

      result += i;
    }

    return result;
  }
};
module.exports = brackets;