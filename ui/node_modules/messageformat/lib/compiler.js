var reserved = require('reserved-words');
var parse = require('messageformat-parser').parse;


/** Creates a new message compiler. Called internally from {@link MessageFormat#compile}.
 *
 * @class
 * @param {MessageFormat} mf - A MessageFormat instance
 * @property {object} locales - The locale identifiers that are used by the compiled functions
 * @property {object} runtime - Names of the core runtime functions that are used by the compiled functions
 * @property {object} formatters - The formatter functions that are used by the compiled functions 
 */
function Compiler(mf) {
    this.mf = mf;
    this.lc = null;
    this.locales = {};
    this.runtime = {};
    this.formatters = {};
}

module.exports = Compiler;


/** Utility function for quoting an Object's key value iff required
 *
 *  Quotes the key if it contains invalid characters or is an
 *  ECMAScript 3rd Edition reserved word (for IE8).
 */
Compiler.propname = function(key, obj) {
  if (/^[A-Z_$][0-9A-Z_$]*$/i.test(key) &&
     ['break', 'continue', 'delete', 'else', 'for', 'function', 'if', 'in', 'new',
      'return', 'this', 'typeof', 'var', 'void', 'while', 'with', 'case', 'catch',
      'default', 'do', 'finally', 'instanceof', 'switch', 'throw', 'try'].indexOf(key) < 0) {
    return obj ? obj + '.' + key : key;
  } else {
    var jkey = JSON.stringify(key);
    return obj ? obj + '[' + jkey + ']' : jkey;
  }
}


/** Utility function for escaping a function name iff required
 */
Compiler.funcname = function(key) {
  var fn = key.trim().replace(/\W+/g, '_');
  return reserved.check(fn, 'es2015', true) || /^\d/.test(fn) ? '_' + fn : fn;
}


/** Utility formatter function for enforcing Bidi Structured Text by using UCC
 *
 *  List inlined from data extracted from CLDR v27 & v28
 *  To verify/recreate, use the following:
 *
 *     git clone https://github.com/unicode-cldr/cldr-misc-full.git
 *     cd cldr-misc-full/main/
 *     grep characterOrder -r . | tr '"/' '\t' | cut -f2,6 | grep -C4 right-to-left
 */
Compiler.bidiMarkText = function(text, locale) {
  function isLocaleRTL(locale) {
    var rtlLanguages = ['ar', 'ckb', 'fa', 'he', 'ks($|[^bfh])', 'lrc', 'mzn',
                        'pa-Arab', 'ps', 'ug', 'ur', 'uz-Arab', 'yi'];
    return new RegExp('^' + rtlLanguages.join('|^')).test(locale);
  }
  var mark = JSON.stringify(isLocaleRTL(locale) ? '\u200F' : '\u200E');
  return mark + ' + ' + text + ' + ' + mark;
}


/** @private */
Compiler.prototype.cases = function(token, plural) {
  var needOther = true;
  var r = token.cases.map(function(c) {
    if (c.key === 'other') needOther = false;
    var s = c.tokens.map(function(tok) { return this.token(tok, plural); }, this);
    return Compiler.propname(c.key) + ': ' + (s.join(' + ') || '""');
  }, this);
  if (needOther) throw new Error("No 'other' form found in " + JSON.stringify(token));
  return '{ ' + r.join(', ') + ' }';
}


/** @private */
Compiler.prototype.token = function(token, plural) {
  if (typeof token == 'string') return JSON.stringify(token);

  var fn, args = [ Compiler.propname(token.arg, 'd') ];
  switch (token.type) {
    case 'argument':
      return this.mf.bidiSupport ? Compiler.bidiMarkText(args[0], this.lc) : args[0];

    case 'select':
      fn = 'select';
      args.push(this.cases(token, this.mf.strictNumberSign ? null : plural));
      this.runtime.select = true;
      break;

    case 'selectordinal':
      fn = 'plural';
      args.push(0, Compiler.funcname(this.lc), this.cases(token, token), 1);
      this.locales[this.lc] = true;
      this.runtime.plural = true;
      break;

    case 'plural':
      fn = 'plural';
      args.push(token.offset || 0, Compiler.funcname(this.lc), this.cases(token, token));
      this.locales[this.lc] = true;
      this.runtime.plural = true;
      break;

    case 'function':
      if (this.mf.intlSupport && !(token.key in this.mf.fmt) && (token.key in this.mf.constructor.formatters)) {
        var fmt = this.mf.constructor.formatters[token.key];
        this.mf.fmt[token.key] = (typeof fmt(this.mf) == 'function') ? fmt(this.mf) : fmt;
      }
      if (!this.mf.fmt[token.key]) throw new Error('Formatting function ' + JSON.stringify(token.key) + ' not found!');
      args.push(JSON.stringify(this.lc));
      if (token.params) switch (token.params.length) {
          case 0:   break;
          case 1:   args.push(JSON.stringify(token.params[0])); break;
          default:  args.push(JSON.stringify(token.params)); break;
      }
      fn = Compiler.propname(token.key, 'fmt');
      this.formatters[token.key] = true;
      break;

    case 'octothorpe':
      if (!plural) return '"#"';
      fn = 'number';
      args = [ Compiler.propname(plural.arg, 'd'), JSON.stringify(plural.arg) ];
      if (plural.offset) args.push(plural.offset);
      this.runtime.number = true;
      break;
  }

  if (!fn) throw new Error('Parser error for token ' + JSON.stringify(token));
  return fn + '(' + args.join(', ') + ')';
};


/** Recursively compile a string or a tree of strings to JavaScript function sources
 *
 *  If `src` is an object with a key that is also present in `plurals`, the key
 *  in question will be used as the locale identifier for its value. To disable
 *  the compile-time checks for plural & selectordinal keys while maintaining
 *  multi-locale support, use falsy values in `plurals`.
 *
 * @param {string|object} src - the source for which the JS code should be generated
 * @param {string} lc - the default locale
 * @param {object} plurals - a map of pluralization keys for all available locales
 */
Compiler.prototype.compile = function(src, lc, plurals) {
  if (typeof src != 'object') {
    this.lc = lc;
    var pc = plurals[lc] || { cardinal: [], ordinal: [] };
    var r = parse(src, pc).map(function(token) { return this.token(token); }, this);
    return 'function(d) { return ' + (r.join(' + ') || '""') + '; }';
  } else {
    var result = {};
    for (var key in src) {
      var lcKey = plurals.hasOwnProperty(key) ? key : lc;
      result[key] = this.compile(src[key], lcKey, plurals);
    }
    return result;
  }
}
