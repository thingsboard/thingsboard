var Compiler = require('./compiler');


/** A set of utility functions that are called by the compiled Javascript
 *  functions, these are included locally in the output of {@link
 *  MessageFormat#compile compile()}.
 *
 * @class
 * @param {MessageFormat} mf - A MessageFormat instance
 */
function Runtime(mf) {
  this.mf = mf;
  this.setStrictNumber(mf.strictNumberSign);
}

module.exports = Runtime;


/** Utility function for `#` in plural rules
 *
 *  Will throw an Error if `value` has a non-numeric value and `offset` is
 *  non-zero or {@link MessageFormat#setStrictNumberSign} is set.
 *
 * @function Runtime#number
 * @param {number} value - The value to operate on
 * @param {string} name - The name of the argument, used for error reporting
 * @param {number} [offset=0] - An optional offset, set by the surrounding context
 * @returns {number|string} The result of applying the offset to the input value
 */
function defaultNumber(value, name, offset) {
  if (!offset) return value;
  if (isNaN(value)) throw new Error('Can\'t apply offset:' + offset + ' to argument `' + name +
                                    '` with non-numerical value ' + JSON.stringify(value) + '.');
  return value - offset;
}


/** @private */
function strictNumber(value, name, offset) {
  if (isNaN(value)) throw new Error('Argument `' + name + '` has non-numerical value ' + JSON.stringify(value) + '.');
  return value - (offset || 0);
}


/** Set how strictly the {@link number} method parses its input.
 *
 *  According to the ICU MessageFormat spec, `#` can only be used to replace a
 *  number input of a `plural` statement. By default, messageformat.js does not
 *  throw a runtime error if you use non-numeric argument with a `plural` rule,
 *  unless rule also includes a non-zero `offset`.
 *
 *  This is called by {@link MessageFormat#setStrictNumberSign} to follow the
 *  stricter ICU MessageFormat spec.
 *
 * @param {boolean} [enable=false]
 */
Runtime.prototype.setStrictNumber = function(enable) {
  this.number = enable ? strictNumber : defaultNumber;
}


/** Utility function for `{N, plural|selectordinal, ...}`
 *
 * @param {number} value - The key to use to find a pluralization rule
 * @param {number} offset - An offset to apply to `value`
 * @param {function} lcfunc - A locale function from `pluralFuncs`
 * @param {Object.<string,string>} data - The object from which results are looked up
 * @param {?boolean} isOrdinal - If true, use ordinal rather than cardinal rules
 * @returns {string} The result of the pluralization
 */
Runtime.prototype.plural = function(value, offset, lcfunc, data, isOrdinal) {
  if ({}.hasOwnProperty.call(data, value)) return data[value];
  if (offset) value -= offset;
  var key = lcfunc(value, isOrdinal);
  if (key in data) return data[key];
  return data.other;
}


/** Utility function for `{N, select, ...}`
 *
 * @param {number} value - The key to use to find a selection
 * @param {Object.<string,string>} data - The object from which results are looked up
 * @returns {string} The result of the select statement
 */
Runtime.prototype.select = function(value, data) {
  if ({}.hasOwnProperty.call(data, value)) return data[value];
  return data.other;
}


/** @private */
Runtime.prototype.toString = function(pluralFuncs, compiler) {
  function _stringify(o, level) {
    if (typeof o != 'object') {
      var funcStr = o.toString().replace(/^(function )\w*/, '$1');
      var indent = /([ \t]*)\S.*$/.exec(funcStr);
      return indent ? funcStr.replace(new RegExp('^' + indent[1], 'mg'), '') : funcStr;
    }
    var s = [];
    for (var i in o) {
      if (level == 0) s.push('var ' + i + ' = ' + _stringify(o[i], level + 1) + ';\n');
      else s.push(Compiler.propname(i) + ': ' + _stringify(o[i], level + 1));
    }
    if (level == 0) return s.join('');
    if (s.length == 0) return '{}';
    var indent = '  '; while (--level) indent += '  ';
    return '{\n' + s.join(',\n').replace(/^/gm, indent) + '\n}';
  }

  var obj = {};
  Object.keys(compiler.locales).forEach(function(lc) { obj[Compiler.funcname(lc)] = pluralFuncs[lc]; });
  Object.keys(compiler.runtime).forEach(function(fn) { obj[fn] = this[fn]; }, this);
  var fmtKeys = Object.keys(compiler.formatters);
  var fmt = this.mf.fmt;
  if (fmtKeys.length) obj.fmt = fmtKeys.reduce(function(o, key) { o[key] = fmt[key]; return o; }, {});
  return _stringify(obj, 0);
}
