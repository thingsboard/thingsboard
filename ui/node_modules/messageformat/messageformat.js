(function(f){if(typeof exports==="object"&&typeof module!=="undefined"){module.exports=f()}else if(typeof define==="function"&&define.amd){define([],f)}else{var g;if(typeof window!=="undefined"){g=window}else if(typeof global!=="undefined"){g=global}else if(typeof self!=="undefined"){g=self}else{g=this}g.MessageFormat = f()}})(function(){var define,module,exports;return (function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
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

},{"messageformat-parser":8,"reserved-words":10}],2:[function(require,module,exports){
/** @file messageformat.js - ICU PluralFormat + SelectFormat for JavaScript
 *
 * @author Alex Sexton - @SlexAxton, Eemeli Aro
 * @version 1.0.2
 * @copyright 2012-2016 Alex Sexton, Eemeli Aro, and Contributors
 * @license To use or fork, MIT. To contribute back, Dojo CLA
 */

var Compiler = require('./compiler');
var Runtime = require('./runtime');


/** Utility getter/wrapper for pluralization functions from
 *  {@link http://github.com/eemeli/make-plural.js make-plural}
 *
 * @private
 */
function getPluralFunc(locale, noPluralKeyChecks) {
  var plurals = require('make-plural/plurals');
  var pluralCategories = require('make-plural/pluralCategories');
  for (var l = locale; l; l = l.replace(/[-_]?[^-_]*$/, '')) {
    var pf = plurals[l];
    if (pf) {
      var pc = noPluralKeyChecks ? { cardinal: [], ordinal: [] } : (pluralCategories[l] || {});
      var fn = function() { return pf.apply(this, arguments); };
      fn.toString = function() { return pf.toString(); };
      fn.cardinal = pc.cardinal;
      fn.ordinal = pc.ordinal;
      return fn;
    }
  }
  throw new Error('Localisation function not found for locale ' + JSON.stringify(locale));
}


/** Create a new message formatter
 *
 *  If `locale` is not set, calls to `compile()` will fetch the default locale
 *  each time. A string `locale` will create a single-locale MessageFormat
 *  instance, with pluralisation rules fetched from the Unicode CLDR using
 *  {@link http://github.com/eemeli/make-plural.js make-plural}.
 *
 *  Using an array of strings as `locale` will create a MessageFormat object
 *  with multi-language support, with pluralisation rules fetched as above. To
 *  select which to use, use the second parameter of `compile()`, or use message
 *  keys corresponding to your locales.
 *
 *  Using an object `locale` with all properties of type `function` allows for
 *  the use of custom/externally defined pluralisation rules.
 *
 * @class
 * @param {string|string[]|Object.<string,function>} [locale] - The locale(s) to use
 */
function MessageFormat(locale) {
  this.pluralFuncs = {};
  if (locale) {
    if (typeof locale == 'string') {
      this.pluralFuncs[locale] = getPluralFunc(locale);
    } else if (Array.isArray(locale)) {
      locale.forEach(function(lc) { this.pluralFuncs[lc] = getPluralFunc(lc); }, this);
    } else if (typeof locale == 'object') {
      for (var lc in locale) if (locale.hasOwnProperty(lc)) {
        if (typeof locale[lc] != 'function') throw new Error('Expected function value for locale ' + JSON.stringify(lc));
        this.pluralFuncs[lc] = locale[lc];
      }
    }
  }
  this.fmt = {};
  this.runtime = new Runtime(this);
}


/** The default locale
 *
 *  Read by `compile()` when no locale has been previously set
 *
 * @memberof MessageFormat
 * @default 'en'
 */
MessageFormat.defaultLocale = 'en';


/** Escape special characaters
 *
 *  Prefix the characters `#`, `{`, `}` and `\` in the input string with a `\`.
 *  This will allow those characters to not be considered as MessageFormat
 *  control characters.
 *
 * @param {string} str - The input string
 * @returns {string} The escaped string
 */
MessageFormat.escape = function(str) {
  return str.replace(/[#{}\\]/g, '\\$&');
}


/** Default number formatting functions in the style of ICU's
 *  {@link http://icu-project.org/apiref/icu4j/com/ibm/icu/text/MessageFormat.html simpleArg syntax}
 *  implemented using the
 *  {@link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl Intl}
 *  object defined by ECMA-402.
 *
 *  **Note**: Intl is not defined in default Node until 0.11.15 / 0.12.0, so
 *  earlier versions require a {@link https://www.npmjs.com/package/intl polyfill}.
 *  Therefore {@link MessageFormat.intlSupport} needs to be true for these default
 *  functions to be available for inclusion in the output.
 *
 * @see MessageFormat#setIntlSupport
 *
 * @namespace
 */
MessageFormat.formatters = {


  /** Represent a number as an integer, percent or currency value
   *
   *  Available in MessageFormat strings as `{VAR, number, integer|percent|currency}`.
   *  Internally, calls Intl.NumberFormat with appropriate parameters. `currency` will
   *  default to USD; to change, set `MessageFormat#currency` to the appropriate
   *  three-letter currency code.
   *
   * @param {number} value - The value to operate on
   * @param {string} type - One of `'integer'`, `'percent'` , or `currency`
   *
   * @example
   * var mf = new MessageFormat('en').setIntlSupport(true);
   * mf.currency = 'EUR';  // needs to be set before first compile() call
   *
   * mf.compile('{N} is almost {N, number, integer}')({ N: 3.14 })
   * // '3.14 is almost 3'
   *
   * mf.compile('{P, number, percent} complete')({ P: 0.99 })
   * // '99% complete'
   *
   * mf.compile('The total is {V, number, currency}.')({ V: 5.5 })
   * // 'The total is €5.50.'
   */
  number: function(self) {
    return new Function("v,lc,p",
      "return new Intl.NumberFormat(lc,\n" +
      "    p=='integer' ? {maximumFractionDigits:0}\n" +
      "  : p=='percent' ? {style:'percent'}\n" +
      "  : p=='currency' ? {style:'currency', currency:'" + (self.currency || 'USD') + "', minimumFractionDigits:2, maximumFractionDigits:2}\n" +
      "  : {}).format(v)"
    );
  },


  /** Represent a date as a short/default/long/full string
   *
   * The input value needs to be in a form that the
   * {@link https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Global_Objects/Date Date object}
   * can process using its single-argument form, `new Date(value)`.
   *
   * @param {number|string} value - Either a Unix epoch time in milliseconds, or a string value representing a date
   * @param {string} [type='default'] - One of `'short'`, `'default'`, `'long'` , or `full`
   *
   * @example
   * var mf = new MessageFormat(['en', 'fi']).setIntlSupport(true);
   *
   * mf.compile('Today is {T, date}')({ T: Date.now() })
   * // 'Today is Feb 21, 2016'
   *
   * mf.compile('Tänään on {T, date}', 'fi')({ T: Date.now() })
   * // 'Tänään on 21. helmikuuta 2016'
   *
   * mf.compile('Unix time started on {T, date, full}')({ T: 0 })
   * // 'Unix time started on Thursday, January 1, 1970'
   *
   * var cf = mf.compile('{sys} became operational on {d0, date, short}');
   * cf({ sys: 'HAL 9000', d0: '12 January 1999' })
   * // 'HAL 9000 became operational on 1/12/1999'
   */
  date: function(v,lc,p) {
    var o = {day:'numeric', month:'short', year:'numeric'};
    switch (p) {
      case 'full': o.weekday = 'long';
      case 'long': o.month = 'long'; break;
      case 'short': o.month = 'numeric';
    }
    return (new Date(v)).toLocaleDateString(lc, o)
  },


  /** Represent a time as a short/default/long string
   *
   * The input value needs to be in a form that the
   * {@link https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Global_Objects/Date Date object}
   * can process using its single-argument form, `new Date(value)`.
   *
   * @param {number|string} value - Either a Unix epoch time in milliseconds, or a string value representing a date
   * @param {string} [type='default'] - One of `'short'`, `'default'`, `'long'` , or `full`
   *
   * @example
   * var mf = new MessageFormat(['en', 'fi']).setIntlSupport(true);
   *
   * mf.compile('The time is now {T, time}')({ T: Date.now() })
   * // 'The time is now 11:26:35 PM'
   *
   * mf.compile('Kello on nyt {T, time}', 'fi')({ T: Date.now() })
   * // 'Kello on nyt 23.26.35'
   *
   * var cf = mf.compile('The Eagle landed at {T, time, full} on {T, date, full}');
   * cf({ T: '1969-07-20 20:17:40 UTC' })
   * // 'The Eagle landed at 10:17:40 PM GMT+2 on Sunday, July 20, 1969'
   */
  time: function(v,lc,p) {
    var o = {second:'numeric', minute:'numeric', hour:'numeric'};
    switch (p) {
      case 'full': case 'long': o.timeZoneName = 'short'; break;
      case 'short': delete o.second;
    }
    return (new Date(v)).toLocaleTimeString(lc, o)
  }
};


/** Add custom formatter functions to this MessageFormat instance
 *
 *  The general syntax for calling a formatting function in MessageFormat is
 *  `{var, fn[, args]*}`, where `var` is the variable that will be set by the
 *  user code, `fn` determines the formatting function, and `args` is an
 *  optional comma-separated list of additional arguments.
 *
 *  In JavaScript, each formatting function is called with three parameters;
 *  the variable value `v`, the current locale `lc`, and (if set) `args` as a
 *  single string, or an array of strings. Formatting functions should not have
 *  side effects.
 *
 * @see MessageFormat.formatters
 *
 * @memberof MessageFormat
 * @param {Object.<string,function>} fmt - A map of formatting functions
 * @returns {MessageFormat} The MessageFormat instance, to allow for chaining
 *
 * @example
 * var mf = new MessageFormat('en-GB');
 * mf.addFormatters({
 *   upcase: function(v) { return v.toUpperCase(); },
 *   locale: function(v, lc) { return lc; },
 *   prop: function(v, lc, p) { return v[p] }
 * });
 *
 * mf.compile('This is {VAR, upcase}.')({ VAR: 'big' })
 * // 'This is BIG.'
 *
 * mf.compile('The current locale is {_, locale}.')({ _: '' })
 * // 'The current locale is en-GB.'
 *
 * mf.compile('Answer: {obj, prop, a}')({ obj: {q: 3, a: 42} })
 * // 'Answer: 42'
 */
MessageFormat.prototype.addFormatters = function(fmt) {
  for (var name in fmt) if (fmt.hasOwnProperty(name)) {
    this.fmt[name] = fmt[name];
  }
  return this;
};


/** Disable the validation of plural & selectordinal keys
 *
 *  Previous versions of messageformat.js allowed the use of plural &
 *  selectordinal statements with any keys; now we throw an error when a
 *  statement uses a non-numerical key that will never be matched as a
 *  pluralization category for the current locale.
 *
 *  Use this method to disable the validation and allow usage as previously.
 *  To re-enable, you'll need to create a new MessageFormat instance.
 *
 * @returns {MessageFormat} The MessageFormat instance, to allow for chaining
 *
 * @example
 * var mf = new MessageFormat('en');
 * var msg = '{X, plural, zero{no answers} one{an answer} other{# answers}}';
 *
 * mf.compile(msg);
 * // Error: Invalid key `zero` for argument `X`. Valid plural keys for this
 * //        locale are `one`, `other`, and explicit keys like `=0`.
 *
 * mf.disablePluralKeyChecks();
 * mf.compile(msg)({ X: 0 });
 * // '0 answers'
 */
MessageFormat.prototype.disablePluralKeyChecks = function() {
  this.noPluralKeyChecks = true;
  for (var lc in this.pluralFuncs) if (this.pluralFuncs.hasOwnProperty(lc)) {
    this.pluralFuncs[lc].cardinal = [];
    this.pluralFuncs[lc].ordinal = [];
  }
  return this;
};


/** Enable or disable the addition of Unicode control characters to all input
 *  to preserve the integrity of the output when mixing LTR and RTL text.
 *
 * @see http://cldr.unicode.org/development/development-process/design-proposals/bidi-handling-of-structured-text
 *
 * @memberof MessageFormat
 * @param {boolean} [enable=true]
 * @returns {MessageFormat} The MessageFormat instance, to allow for chaining
 *
 * @example
 * // upper case stands for RTL characters, output is shown as rendered
 * var mf = new MessageFormat('en');
 *
 * mf.compile('{0} >> {1} >> {2}')(['first', 'SECOND', 'THIRD']);
 * // 'first >> THIRD << SECOND'
 *
 * mf.setBiDiSupport(true);
 * mf.compile('{0} >> {1} >> {2}')(['first', 'SECOND', 'THIRD']);
 * // 'first >> SECOND >> THIRD'
 */
MessageFormat.prototype.setBiDiSupport = function(enable) {
    this.bidiSupport = !!enable || (typeof enable == 'undefined');
    return this;
};


/** Enable or disable support for the default formatters, which require the
 *  `Intl` object. Note that this can't be autodetected, as the environment
 *  in which the formatted text is compiled into Javascript functions is not
 *  necessarily the same environment in which they will get executed.
 *
 * @see MessageFormat.formatters
 *
 * @memberof MessageFormat
 * @param {boolean} [enable=true]
 * @returns {MessageFormat} The MessageFormat instance, to allow for chaining
 */
MessageFormat.prototype.setIntlSupport = function(enable) {
    this.intlSupport = !!enable || (typeof enable == 'undefined');
    return this;
};


/** According to the ICU MessageFormat spec, a `#` character directly inside a
 *  `plural` or `selectordinal` statement should be replaced by the number
 *  matching the surrounding statement. By default, messageformat.js will
 *  replace `#` signs with the value of the nearest surrounding `plural` or
 *  `selectordinal` statement.
 *
 *  Set this to true to follow the stricter ICU MessageFormat spec, and to
 *  throw a runtime error if `#` is used with non-numeric input.
 *
 * @memberof MessageFormat
 * @param {boolean} [enable=true]
 * @returns {MessageFormat} The MessageFormat instance, to allow for chaining
 *
 * @example
 * var mf = new MessageFormat('en');
 *
 * var cookieMsg = '#: {X, plural, =0{no cookies} one{a cookie} other{# cookies}}';
 * mf.compile(cookieMsg)({ X: 3 });
 * // '#: 3 cookies'
 *
 * var pastryMsg = '{X, plural, one{{P, select, cookie{a cookie} other{a pie}}} other{{P, select, cookie{# cookies} other{# pies}}}}';
 * mf.compile(pastryMsg)({ X: 3, P: 'pie' });
 * // '3 pies'
 *
 * mf.setStrictNumberSign(true);
 * mf.compile(pastryMsg)({ X: 3, P: 'pie' });
 * // '# pies'
 */
MessageFormat.prototype.setStrictNumberSign = function(enable) {
    this.strictNumberSign = !!enable || (typeof enable == 'undefined');
    this.runtime.setStrictNumber(this.strictNumberSign);
    return this;
};


/** Compile messages into storable functions
 *
 *  If `messages` is a single string including ICU MessageFormat declarations,
 *  the result of `compile()` is a function taking a single Object parameter
 *  `d` representing each of the input's defined variables.
 *
 *  If `messages` is a hierarchical structure of such strings, the output of
 *  `compile()` will match that structure, with each string replaced by its
 *  corresponding JavaScript function.
 *
 *  If the input `messages` -- and therefore the output -- of `compile()` is an
 *  object, the output object will have a `toString(global)` method that may be
 *  used to store or cache the compiled functions to disk, for later inclusion
 *  in any JS environment, without a local MessageFormat instance required. Its
 *  `global` parameters sets the name (if any) of the resulting global variable,
 *  with special handling for `exports`, `module.exports`, and `export default`.
 *  If `global` does not contain a `.`, the output defaults to an UMD pattern.
 *
 *  If `locale` is not set, the first locale set in the object's constructor
 *  will be used by default; using a key at any depth of `messages` that is a
 *  declared locale will set its child elements to use that locale.
 *
 *  If `locale` is set, it is used for all messages. If the constructor
 *  declared any locales, `locale` needs to be one of them.
 *
 * @memberof MessageFormat
 * @param {string|Object} messages - The input message(s) to be compiled, in ICU MessageFormat
 * @param {string} [locale] - A locale to use for the messages
 * @returns {function|Object} The first match found for the given locale(s)
 *
 * @example
 * var mf = new MessageFormat('en');
 * var cf = mf.compile('A {TYPE} example.');
 *
 * cf({ TYPE: 'simple' })
 * // 'A simple example.'
 *
 * @example
 * var mf = new MessageFormat(['en', 'fi']);
 * var cf = mf.compile({
 *   en: { a: 'A {TYPE} example.',
 *         b: 'This is the {COUNT, selectordinal, one{#st} two{#nd} few{#rd} other{#th}} example.' },
 *   fi: { a: '{TYPE} esimerkki.',
 *         b: 'Tämä on {COUNT, selectordinal, other{#.}} esimerkki.' }
 * });
 *
 * cf.en.b({ COUNT: 2 })
 * // 'This is the 2nd example.'
 *
 * cf.fi.b({ COUNT: 2 })
 * // 'Tämä on 2. esimerkki.'
 *
 * @example
 * var fs = require('fs');
 * var mf = new MessageFormat('en').setIntlSupport();
 * var msgSet = {
 *   a: 'A {TYPE} example.',
 *   b: 'This has {COUNT, plural, one{one member} other{# members}}.',
 *   c: 'We have {P, number, percent} code coverage.'
 * };
 * var cfStr = mf.compile(msgSet).toString('module.exports');
 * fs.writeFileSync('messages.js', cfStr);
 * ...
 * var messages = require('./messages');
 *
 * messages.a({ TYPE: 'more complex' })
 * // 'A more complex example.'
 *
 * messages.b({ COUNT: 3 })
 * // 'This has 3 members.'
 */
MessageFormat.prototype.compile = function(messages, locale) {
  function _stringify(obj, level) {
    if (!level) level = 0;
    if (typeof obj != 'object') return obj;
    var o = [], indent = '';
    for (var i = 0; i < level; ++i) indent += '  ';
    for (var k in obj) o.push('\n' + indent + '  ' + Compiler.propname(k) + ': ' + _stringify(obj[k], level + 1));
    return '{' + o.join(',') + '\n' + indent + '}';
  }

  var pf;
  if (Object.keys(this.pluralFuncs).length == 0) {
    if (!locale) locale = MessageFormat.defaultLocale;
    pf = {};
    pf[locale] = getPluralFunc(locale, this.noPluralKeyChecks);
  } else if (locale) {
    pf = {};
    pf[locale] = this.pluralFuncs[locale];
    if (!pf[locale]) throw new Error('Locale ' + JSON.stringify(locale) + 'not found in ' + JSON.stringify(this.pluralFuncs) + '!');
  } else {
    pf = this.pluralFuncs;
    locale = Object.keys(pf)[0];
  }

  var compiler = new Compiler(this);
  var obj = compiler.compile(messages, locale, pf);

  if (typeof messages != 'object') {
    var fn = new Function(
        'number, plural, select, fmt', Compiler.funcname(locale),
        'return ' + obj);
    var rt = this.runtime;
    return fn(rt.number, rt.plural, rt.select, this.fmt, pf[locale]);
  }

  var rtStr = this.runtime.toString(pf, compiler) + '\n';
  var objStr = _stringify(obj);
  var result = new Function(rtStr + 'return ' + objStr)();
  if (result.hasOwnProperty('toString')) throw new Error('The top-level message key `toString` is reserved');

  result.toString = function(global) {
    switch (global || '') {
      case 'exports':
        var o = [];
        for (var k in obj) o.push(Compiler.propname(k, 'exports') + ' = ' + _stringify(obj[k]));
        return rtStr + o.join(';\n');
      case 'module.exports':
        return rtStr + 'module.exports = ' + objStr;
      case 'export default':
        return rtStr +  'export default ' + objStr;
      case '':
        return rtStr + 'return ' + objStr;
      default:
        if (global.indexOf('.') > -1) return rtStr + global + ' = ' + objStr;
        return rtStr + [
          '(function (root, G) {',
          '  if (typeof define === "function" && define.amd) { define(G); }',
          '  else if (typeof exports === "object") { module.exports = G; }',
          '  else { ' + Compiler.propname(global, 'root') + ' = G; }',
          '})(this, ' + objStr + ');'
        ].join('\n');
    }
  }
  return result;
}


module.exports = MessageFormat;

},{"./compiler":1,"./runtime":3,"make-plural/pluralCategories":6,"make-plural/plurals":7}],3:[function(require,module,exports){
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

},{"./compiler":1}],4:[function(require,module,exports){
// http://wiki.commonjs.org/wiki/Unit_Testing/1.0
//
// THIS IS NOT TESTED NOR LIKELY TO WORK OUTSIDE V8!
//
// Originally from narwhal.js (http://narwhaljs.org)
// Copyright (c) 2009 Thomas Robinson <280north.com>
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the 'Software'), to
// deal in the Software without restriction, including without limitation the
// rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
// sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
// ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

// when used in node, this will actually load the util module we depend on
// versus loading the builtin util module as happens otherwise
// this is a bug in node module loading as far as I am concerned
var util = require('util/');

var pSlice = Array.prototype.slice;
var hasOwn = Object.prototype.hasOwnProperty;

// 1. The assert module provides functions that throw
// AssertionError's when particular conditions are not met. The
// assert module must conform to the following interface.

var assert = module.exports = ok;

// 2. The AssertionError is defined in assert.
// new assert.AssertionError({ message: message,
//                             actual: actual,
//                             expected: expected })

assert.AssertionError = function AssertionError(options) {
  this.name = 'AssertionError';
  this.actual = options.actual;
  this.expected = options.expected;
  this.operator = options.operator;
  if (options.message) {
    this.message = options.message;
    this.generatedMessage = false;
  } else {
    this.message = getMessage(this);
    this.generatedMessage = true;
  }
  var stackStartFunction = options.stackStartFunction || fail;

  if (Error.captureStackTrace) {
    Error.captureStackTrace(this, stackStartFunction);
  }
  else {
    // non v8 browsers so we can have a stacktrace
    var err = new Error();
    if (err.stack) {
      var out = err.stack;

      // try to strip useless frames
      var fn_name = stackStartFunction.name;
      var idx = out.indexOf('\n' + fn_name);
      if (idx >= 0) {
        // once we have located the function frame
        // we need to strip out everything before it (and its line)
        var next_line = out.indexOf('\n', idx + 1);
        out = out.substring(next_line + 1);
      }

      this.stack = out;
    }
  }
};

// assert.AssertionError instanceof Error
util.inherits(assert.AssertionError, Error);

function replacer(key, value) {
  if (util.isUndefined(value)) {
    return '' + value;
  }
  if (util.isNumber(value) && !isFinite(value)) {
    return value.toString();
  }
  if (util.isFunction(value) || util.isRegExp(value)) {
    return value.toString();
  }
  return value;
}

function truncate(s, n) {
  if (util.isString(s)) {
    return s.length < n ? s : s.slice(0, n);
  } else {
    return s;
  }
}

function getMessage(self) {
  return truncate(JSON.stringify(self.actual, replacer), 128) + ' ' +
         self.operator + ' ' +
         truncate(JSON.stringify(self.expected, replacer), 128);
}

// At present only the three keys mentioned above are used and
// understood by the spec. Implementations or sub modules can pass
// other keys to the AssertionError's constructor - they will be
// ignored.

// 3. All of the following functions must throw an AssertionError
// when a corresponding condition is not met, with a message that
// may be undefined if not provided.  All assertion methods provide
// both the actual and expected values to the assertion error for
// display purposes.

function fail(actual, expected, message, operator, stackStartFunction) {
  throw new assert.AssertionError({
    message: message,
    actual: actual,
    expected: expected,
    operator: operator,
    stackStartFunction: stackStartFunction
  });
}

// EXTENSION! allows for well behaved errors defined elsewhere.
assert.fail = fail;

// 4. Pure assertion tests whether a value is truthy, as determined
// by !!guard.
// assert.ok(guard, message_opt);
// This statement is equivalent to assert.equal(true, !!guard,
// message_opt);. To test strictly for the value true, use
// assert.strictEqual(true, guard, message_opt);.

function ok(value, message) {
  if (!value) fail(value, true, message, '==', assert.ok);
}
assert.ok = ok;

// 5. The equality assertion tests shallow, coercive equality with
// ==.
// assert.equal(actual, expected, message_opt);

assert.equal = function equal(actual, expected, message) {
  if (actual != expected) fail(actual, expected, message, '==', assert.equal);
};

// 6. The non-equality assertion tests for whether two objects are not equal
// with != assert.notEqual(actual, expected, message_opt);

assert.notEqual = function notEqual(actual, expected, message) {
  if (actual == expected) {
    fail(actual, expected, message, '!=', assert.notEqual);
  }
};

// 7. The equivalence assertion tests a deep equality relation.
// assert.deepEqual(actual, expected, message_opt);

assert.deepEqual = function deepEqual(actual, expected, message) {
  if (!_deepEqual(actual, expected)) {
    fail(actual, expected, message, 'deepEqual', assert.deepEqual);
  }
};

function _deepEqual(actual, expected) {
  // 7.1. All identical values are equivalent, as determined by ===.
  if (actual === expected) {
    return true;

  } else if (util.isBuffer(actual) && util.isBuffer(expected)) {
    if (actual.length != expected.length) return false;

    for (var i = 0; i < actual.length; i++) {
      if (actual[i] !== expected[i]) return false;
    }

    return true;

  // 7.2. If the expected value is a Date object, the actual value is
  // equivalent if it is also a Date object that refers to the same time.
  } else if (util.isDate(actual) && util.isDate(expected)) {
    return actual.getTime() === expected.getTime();

  // 7.3 If the expected value is a RegExp object, the actual value is
  // equivalent if it is also a RegExp object with the same source and
  // properties (`global`, `multiline`, `lastIndex`, `ignoreCase`).
  } else if (util.isRegExp(actual) && util.isRegExp(expected)) {
    return actual.source === expected.source &&
           actual.global === expected.global &&
           actual.multiline === expected.multiline &&
           actual.lastIndex === expected.lastIndex &&
           actual.ignoreCase === expected.ignoreCase;

  // 7.4. Other pairs that do not both pass typeof value == 'object',
  // equivalence is determined by ==.
  } else if (!util.isObject(actual) && !util.isObject(expected)) {
    return actual == expected;

  // 7.5 For all other Object pairs, including Array objects, equivalence is
  // determined by having the same number of owned properties (as verified
  // with Object.prototype.hasOwnProperty.call), the same set of keys
  // (although not necessarily the same order), equivalent values for every
  // corresponding key, and an identical 'prototype' property. Note: this
  // accounts for both named and indexed properties on Arrays.
  } else {
    return objEquiv(actual, expected);
  }
}

function isArguments(object) {
  return Object.prototype.toString.call(object) == '[object Arguments]';
}

function objEquiv(a, b) {
  if (util.isNullOrUndefined(a) || util.isNullOrUndefined(b))
    return false;
  // an identical 'prototype' property.
  if (a.prototype !== b.prototype) return false;
  // if one is a primitive, the other must be same
  if (util.isPrimitive(a) || util.isPrimitive(b)) {
    return a === b;
  }
  var aIsArgs = isArguments(a),
      bIsArgs = isArguments(b);
  if ((aIsArgs && !bIsArgs) || (!aIsArgs && bIsArgs))
    return false;
  if (aIsArgs) {
    a = pSlice.call(a);
    b = pSlice.call(b);
    return _deepEqual(a, b);
  }
  var ka = objectKeys(a),
      kb = objectKeys(b),
      key, i;
  // having the same number of owned properties (keys incorporates
  // hasOwnProperty)
  if (ka.length != kb.length)
    return false;
  //the same set of keys (although not necessarily the same order),
  ka.sort();
  kb.sort();
  //~~~cheap key test
  for (i = ka.length - 1; i >= 0; i--) {
    if (ka[i] != kb[i])
      return false;
  }
  //equivalent values for every corresponding key, and
  //~~~possibly expensive deep test
  for (i = ka.length - 1; i >= 0; i--) {
    key = ka[i];
    if (!_deepEqual(a[key], b[key])) return false;
  }
  return true;
}

// 8. The non-equivalence assertion tests for any deep inequality.
// assert.notDeepEqual(actual, expected, message_opt);

assert.notDeepEqual = function notDeepEqual(actual, expected, message) {
  if (_deepEqual(actual, expected)) {
    fail(actual, expected, message, 'notDeepEqual', assert.notDeepEqual);
  }
};

// 9. The strict equality assertion tests strict equality, as determined by ===.
// assert.strictEqual(actual, expected, message_opt);

assert.strictEqual = function strictEqual(actual, expected, message) {
  if (actual !== expected) {
    fail(actual, expected, message, '===', assert.strictEqual);
  }
};

// 10. The strict non-equality assertion tests for strict inequality, as
// determined by !==.  assert.notStrictEqual(actual, expected, message_opt);

assert.notStrictEqual = function notStrictEqual(actual, expected, message) {
  if (actual === expected) {
    fail(actual, expected, message, '!==', assert.notStrictEqual);
  }
};

function expectedException(actual, expected) {
  if (!actual || !expected) {
    return false;
  }

  if (Object.prototype.toString.call(expected) == '[object RegExp]') {
    return expected.test(actual);
  } else if (actual instanceof expected) {
    return true;
  } else if (expected.call({}, actual) === true) {
    return true;
  }

  return false;
}

function _throws(shouldThrow, block, expected, message) {
  var actual;

  if (util.isString(expected)) {
    message = expected;
    expected = null;
  }

  try {
    block();
  } catch (e) {
    actual = e;
  }

  message = (expected && expected.name ? ' (' + expected.name + ').' : '.') +
            (message ? ' ' + message : '.');

  if (shouldThrow && !actual) {
    fail(actual, expected, 'Missing expected exception' + message);
  }

  if (!shouldThrow && expectedException(actual, expected)) {
    fail(actual, expected, 'Got unwanted exception' + message);
  }

  if ((shouldThrow && actual && expected &&
      !expectedException(actual, expected)) || (!shouldThrow && actual)) {
    throw actual;
  }
}

// 11. Expected to throw an error:
// assert.throws(block, Error_opt, message_opt);

assert.throws = function(block, /*optional*/error, /*optional*/message) {
  _throws.apply(this, [true].concat(pSlice.call(arguments)));
};

// EXTENSION! This is annoying to write outside this module.
assert.doesNotThrow = function(block, /*optional*/message) {
  _throws.apply(this, [false].concat(pSlice.call(arguments)));
};

assert.ifError = function(err) { if (err) {throw err;}};

var objectKeys = Object.keys || function (obj) {
  var keys = [];
  for (var key in obj) {
    if (hasOwn.call(obj, key)) keys.push(key);
  }
  return keys;
};

},{"util/":13}],5:[function(require,module,exports){
if (typeof Object.create === 'function') {
  // implementation from standard node.js 'util' module
  module.exports = function inherits(ctor, superCtor) {
    ctor.super_ = superCtor
    ctor.prototype = Object.create(superCtor.prototype, {
      constructor: {
        value: ctor,
        enumerable: false,
        writable: true,
        configurable: true
      }
    });
  };
} else {
  // old school shim for old browsers
  module.exports = function inherits(ctor, superCtor) {
    ctor.super_ = superCtor
    var TempCtor = function () {}
    TempCtor.prototype = superCtor.prototype
    ctor.prototype = new TempCtor()
    ctor.prototype.constructor = ctor
  }
}

},{}],6:[function(require,module,exports){
var _cc = [
  {cardinal:["other"],ordinal:["other"]},
  {cardinal:["one","other"],ordinal:["other"]},
  {cardinal:["one","other"],ordinal:["one","other"]},
  {cardinal:["one","two","other"],ordinal:["other"]}
];

(function (root, pluralCategories) {
  if (typeof define === 'function' && define.amd) {
    define(pluralCategories);
  } else if (typeof exports === 'object') {
    module.exports = pluralCategories;
  } else {
    root.pluralCategories = pluralCategories;
  }
}(this, {
af: _cc[1],
ak: _cc[1],
am: _cc[1],
ar: {cardinal:["zero","one","two","few","many","other"],ordinal:["other"]},
as: {cardinal:["one","other"],ordinal:["one","two","few","many","other"]},
asa: _cc[1],
ast: _cc[1],
az: {cardinal:["one","other"],ordinal:["one","few","many","other"]},
be: {cardinal:["one","few","many","other"],ordinal:["few","other"]},
bem: _cc[1],
bez: _cc[1],
bg: _cc[1],
bh: _cc[1],
bm: _cc[0],
bn: {cardinal:["one","other"],ordinal:["one","two","few","many","other"]},
bo: _cc[0],
br: {cardinal:["one","two","few","many","other"],ordinal:["other"]},
brx: _cc[1],
bs: {cardinal:["one","few","other"],ordinal:["other"]},
ca: {cardinal:["one","other"],ordinal:["one","two","few","other"]},
ce: _cc[1],
cgg: _cc[1],
chr: _cc[1],
ckb: _cc[1],
cs: {cardinal:["one","few","many","other"],ordinal:["other"]},
cy: {cardinal:["zero","one","two","few","many","other"],ordinal:["zero","one","two","few","many","other"]},
da: _cc[1],
de: _cc[1],
dsb: {cardinal:["one","two","few","other"],ordinal:["other"]},
dv: _cc[1],
dz: _cc[0],
ee: _cc[1],
el: _cc[1],
en: {cardinal:["one","other"],ordinal:["one","two","few","other"]},
eo: _cc[1],
es: _cc[1],
et: _cc[1],
eu: _cc[1],
fa: _cc[1],
ff: _cc[1],
fi: _cc[1],
fil: _cc[2],
fo: _cc[1],
fr: _cc[2],
fur: _cc[1],
fy: _cc[1],
ga: {cardinal:["one","two","few","many","other"],ordinal:["one","other"]},
gd: {cardinal:["one","two","few","other"],ordinal:["other"]},
gl: _cc[1],
gsw: _cc[1],
gu: {cardinal:["one","other"],ordinal:["one","two","few","many","other"]},
guw: _cc[1],
gv: {cardinal:["one","two","few","many","other"],ordinal:["other"]},
ha: _cc[1],
haw: _cc[1],
he: {cardinal:["one","two","many","other"],ordinal:["other"]},
hi: {cardinal:["one","other"],ordinal:["one","two","few","many","other"]},
hr: {cardinal:["one","few","other"],ordinal:["other"]},
hsb: {cardinal:["one","two","few","other"],ordinal:["other"]},
hu: _cc[2],
hy: _cc[2],
id: _cc[0],
ig: _cc[0],
ii: _cc[0],
"in": _cc[0],
is: _cc[1],
it: {cardinal:["one","other"],ordinal:["many","other"]},
iu: _cc[3],
iw: {cardinal:["one","two","many","other"],ordinal:["other"]},
ja: _cc[0],
jbo: _cc[0],
jgo: _cc[1],
ji: _cc[1],
jmc: _cc[1],
jv: _cc[0],
jw: _cc[0],
ka: {cardinal:["one","other"],ordinal:["one","many","other"]},
kab: _cc[1],
kaj: _cc[1],
kcg: _cc[1],
kde: _cc[0],
kea: _cc[0],
kk: {cardinal:["one","other"],ordinal:["many","other"]},
kkj: _cc[1],
kl: _cc[1],
km: _cc[0],
kn: _cc[1],
ko: _cc[0],
ks: _cc[1],
ksb: _cc[1],
ksh: {cardinal:["zero","one","other"],ordinal:["other"]},
ku: _cc[1],
kw: _cc[3],
ky: _cc[1],
lag: {cardinal:["zero","one","other"],ordinal:["other"]},
lb: _cc[1],
lg: _cc[1],
lkt: _cc[0],
ln: _cc[1],
lo: {cardinal:["other"],ordinal:["one","other"]},
lt: {cardinal:["one","few","many","other"],ordinal:["other"]},
lv: {cardinal:["zero","one","other"],ordinal:["other"]},
mas: _cc[1],
mg: _cc[1],
mgo: _cc[1],
mk: {cardinal:["one","other"],ordinal:["one","two","many","other"]},
ml: _cc[1],
mn: _cc[1],
mo: {cardinal:["one","few","other"],ordinal:["one","other"]},
mr: {cardinal:["one","other"],ordinal:["one","two","few","other"]},
ms: {cardinal:["other"],ordinal:["one","other"]},
mt: {cardinal:["one","few","many","other"],ordinal:["other"]},
my: _cc[0],
nah: _cc[1],
naq: _cc[3],
nb: _cc[1],
nd: _cc[1],
ne: _cc[2],
nl: _cc[1],
nn: _cc[1],
nnh: _cc[1],
no: _cc[1],
nqo: _cc[0],
nr: _cc[1],
nso: _cc[1],
ny: _cc[1],
nyn: _cc[1],
om: _cc[1],
or: _cc[1],
os: _cc[1],
pa: _cc[1],
pap: _cc[1],
pl: {cardinal:["one","few","many","other"],ordinal:["other"]},
prg: {cardinal:["zero","one","other"],ordinal:["other"]},
ps: _cc[1],
pt: _cc[1],
"pt-PT": _cc[1],
rm: _cc[1],
ro: {cardinal:["one","few","other"],ordinal:["one","other"]},
rof: _cc[1],
root: _cc[0],
ru: {cardinal:["one","few","many","other"],ordinal:["other"]},
rwk: _cc[1],
sah: _cc[0],
saq: _cc[1],
sdh: _cc[1],
se: _cc[3],
seh: _cc[1],
ses: _cc[0],
sg: _cc[0],
sh: {cardinal:["one","few","other"],ordinal:["other"]},
shi: {cardinal:["one","few","other"],ordinal:["other"]},
si: _cc[1],
sk: {cardinal:["one","few","many","other"],ordinal:["other"]},
sl: {cardinal:["one","two","few","other"],ordinal:["other"]},
sma: _cc[3],
smi: _cc[3],
smj: _cc[3],
smn: _cc[3],
sms: _cc[3],
sn: _cc[1],
so: _cc[1],
sq: {cardinal:["one","other"],ordinal:["one","many","other"]},
sr: {cardinal:["one","few","other"],ordinal:["other"]},
ss: _cc[1],
ssy: _cc[1],
st: _cc[1],
sv: _cc[2],
sw: _cc[1],
syr: _cc[1],
ta: _cc[1],
te: _cc[1],
teo: _cc[1],
th: _cc[0],
ti: _cc[1],
tig: _cc[1],
tk: _cc[1],
tl: _cc[2],
tn: _cc[1],
to: _cc[0],
tr: _cc[1],
ts: _cc[1],
tzm: _cc[1],
ug: _cc[1],
uk: {cardinal:["one","few","many","other"],ordinal:["few","other"]},
ur: _cc[1],
uz: _cc[1],
ve: _cc[1],
vi: {cardinal:["other"],ordinal:["one","other"]},
vo: _cc[1],
vun: _cc[1],
wa: _cc[1],
wae: _cc[1],
wo: _cc[0],
xh: _cc[1],
xog: _cc[1],
yi: _cc[1],
yo: _cc[0],
zh: _cc[0],
zu: _cc[1]
}));

},{}],7:[function(require,module,exports){
var _cp = [
function(n, ord) {
  if (ord) return 'other';
  return 'other';
},
function(n, ord) {
  if (ord) return 'other';
  return (n == 1) ? 'one' : 'other';
},
function(n, ord) {
  if (ord) return 'other';
  return ((n == 0
          || n == 1)) ? 'one' : 'other';
},
function(n, ord) {
  var s = String(n).split('.'), v0 = !s[1];
  if (ord) return 'other';
  return (n == 1 && v0) ? 'one' : 'other';
}
];

(function (root, plurals) {
  if (typeof define === 'function' && define.amd) {
    define(plurals);
  } else if (typeof exports === 'object') {
    module.exports = plurals;
  } else {
    root.plurals = plurals;
  }
}(this, {
af: _cp[1],

ak: _cp[2],

am: function(n, ord) {
  if (ord) return 'other';
  return (n >= 0 && n <= 1) ? 'one' : 'other';
},

ar: function(n, ord) {
  var s = String(n).split('.'), t0 = Number(s[0]) == n,
      n100 = t0 && s[0].slice(-2);
  if (ord) return 'other';
  return (n == 0) ? 'zero'
      : (n == 1) ? 'one'
      : (n == 2) ? 'two'
      : ((n100 >= 3 && n100 <= 10)) ? 'few'
      : ((n100 >= 11 && n100 <= 99)) ? 'many'
      : 'other';
},

as: function(n, ord) {
  if (ord) return ((n == 1 || n == 5 || n == 7 || n == 8 || n == 9
          || n == 10)) ? 'one'
      : ((n == 2
          || n == 3)) ? 'two'
      : (n == 4) ? 'few'
      : (n == 6) ? 'many'
      : 'other';
  return (n >= 0 && n <= 1) ? 'one' : 'other';
},

asa: _cp[1],

ast: _cp[3],

az: function(n, ord) {
  var s = String(n).split('.'), i = s[0], i10 = i.slice(-1),
      i100 = i.slice(-2), i1000 = i.slice(-3);
  if (ord) return ((i10 == 1 || i10 == 2 || i10 == 5 || i10 == 7 || i10 == 8)
          || (i100 == 20 || i100 == 50 || i100 == 70
          || i100 == 80)) ? 'one'
      : ((i10 == 3 || i10 == 4) || (i1000 == 100 || i1000 == 200
          || i1000 == 300 || i1000 == 400 || i1000 == 500 || i1000 == 600 || i1000 == 700
          || i1000 == 800
          || i1000 == 900)) ? 'few'
      : (i == 0 || i10 == 6 || (i100 == 40 || i100 == 60
          || i100 == 90)) ? 'many'
      : 'other';
  return (n == 1) ? 'one' : 'other';
},

be: function(n, ord) {
  var s = String(n).split('.'), t0 = Number(s[0]) == n,
      n10 = t0 && s[0].slice(-1), n100 = t0 && s[0].slice(-2);
  if (ord) return ((n10 == 2
          || n10 == 3) && n100 != 12 && n100 != 13) ? 'few' : 'other';
  return (n10 == 1 && n100 != 11) ? 'one'
      : ((n10 >= 2 && n10 <= 4) && (n100 < 12
          || n100 > 14)) ? 'few'
      : (t0 && n10 == 0 || (n10 >= 5 && n10 <= 9)
          || (n100 >= 11 && n100 <= 14)) ? 'many'
      : 'other';
},

bem: _cp[1],

bez: _cp[1],

bg: _cp[1],

bh: _cp[2],

bm: _cp[0],

bn: function(n, ord) {
  if (ord) return ((n == 1 || n == 5 || n == 7 || n == 8 || n == 9
          || n == 10)) ? 'one'
      : ((n == 2
          || n == 3)) ? 'two'
      : (n == 4) ? 'few'
      : (n == 6) ? 'many'
      : 'other';
  return (n >= 0 && n <= 1) ? 'one' : 'other';
},

bo: _cp[0],

br: function(n, ord) {
  var s = String(n).split('.'), t0 = Number(s[0]) == n,
      n10 = t0 && s[0].slice(-1), n100 = t0 && s[0].slice(-2),
      n1000000 = t0 && s[0].slice(-6);
  if (ord) return 'other';
  return (n10 == 1 && n100 != 11 && n100 != 71 && n100 != 91) ? 'one'
      : (n10 == 2 && n100 != 12 && n100 != 72 && n100 != 92) ? 'two'
      : (((n10 == 3 || n10 == 4) || n10 == 9) && (n100 < 10
          || n100 > 19) && (n100 < 70 || n100 > 79) && (n100 < 90
          || n100 > 99)) ? 'few'
      : (n != 0 && t0 && n1000000 == 0) ? 'many'
      : 'other';
},

brx: _cp[1],

bs: function(n, ord) {
  var s = String(n).split('.'), i = s[0], f = s[1] || '', v0 = !s[1],
      i10 = i.slice(-1), i100 = i.slice(-2), f10 = f.slice(-1), f100 = f.slice(-2);
  if (ord) return 'other';
  return (v0 && i10 == 1 && i100 != 11
          || f10 == 1 && f100 != 11) ? 'one'
      : (v0 && (i10 >= 2 && i10 <= 4) && (i100 < 12 || i100 > 14)
          || (f10 >= 2 && f10 <= 4) && (f100 < 12
          || f100 > 14)) ? 'few'
      : 'other';
},

ca: function(n, ord) {
  var s = String(n).split('.'), v0 = !s[1];
  if (ord) return ((n == 1
          || n == 3)) ? 'one'
      : (n == 2) ? 'two'
      : (n == 4) ? 'few'
      : 'other';
  return (n == 1 && v0) ? 'one' : 'other';
},

ce: _cp[1],

cgg: _cp[1],

chr: _cp[1],

ckb: _cp[1],

cs: function(n, ord) {
  var s = String(n).split('.'), i = s[0], v0 = !s[1];
  if (ord) return 'other';
  return (n == 1 && v0) ? 'one'
      : ((i >= 2 && i <= 4) && v0) ? 'few'
      : (!v0) ? 'many'
      : 'other';
},

cy: function(n, ord) {
  if (ord) return ((n == 0 || n == 7 || n == 8
          || n == 9)) ? 'zero'
      : (n == 1) ? 'one'
      : (n == 2) ? 'two'
      : ((n == 3
          || n == 4)) ? 'few'
      : ((n == 5
          || n == 6)) ? 'many'
      : 'other';
  return (n == 0) ? 'zero'
      : (n == 1) ? 'one'
      : (n == 2) ? 'two'
      : (n == 3) ? 'few'
      : (n == 6) ? 'many'
      : 'other';
},

da: function(n, ord) {
  var s = String(n).split('.'), i = s[0], t0 = Number(s[0]) == n;
  if (ord) return 'other';
  return (n == 1 || !t0 && (i == 0
          || i == 1)) ? 'one' : 'other';
},

de: _cp[3],

dsb: function(n, ord) {
  var s = String(n).split('.'), i = s[0], f = s[1] || '', v0 = !s[1],
      i100 = i.slice(-2), f100 = f.slice(-2);
  if (ord) return 'other';
  return (v0 && i100 == 1
          || f100 == 1) ? 'one'
      : (v0 && i100 == 2
          || f100 == 2) ? 'two'
      : (v0 && (i100 == 3 || i100 == 4) || (f100 == 3
          || f100 == 4)) ? 'few'
      : 'other';
},

dv: _cp[1],

dz: _cp[0],

ee: _cp[1],

el: _cp[1],

en: function(n, ord) {
  var s = String(n).split('.'), v0 = !s[1], t0 = Number(s[0]) == n,
      n10 = t0 && s[0].slice(-1), n100 = t0 && s[0].slice(-2);
  if (ord) return (n10 == 1 && n100 != 11) ? 'one'
      : (n10 == 2 && n100 != 12) ? 'two'
      : (n10 == 3 && n100 != 13) ? 'few'
      : 'other';
  return (n == 1 && v0) ? 'one' : 'other';
},

eo: _cp[1],

es: _cp[1],

et: _cp[3],

eu: _cp[1],

fa: function(n, ord) {
  if (ord) return 'other';
  return (n >= 0 && n <= 1) ? 'one' : 'other';
},

ff: function(n, ord) {
  if (ord) return 'other';
  return (n >= 0 && n < 2) ? 'one' : 'other';
},

fi: _cp[3],

fil: function(n, ord) {
  var s = String(n).split('.'), i = s[0], f = s[1] || '', v0 = !s[1],
      i10 = i.slice(-1), f10 = f.slice(-1);
  if (ord) return (n == 1) ? 'one' : 'other';
  return (v0 && (i == 1 || i == 2 || i == 3)
          || v0 && i10 != 4 && i10 != 6 && i10 != 9
          || !v0 && f10 != 4 && f10 != 6 && f10 != 9) ? 'one' : 'other';
},

fo: _cp[1],

fr: function(n, ord) {
  if (ord) return (n == 1) ? 'one' : 'other';
  return (n >= 0 && n < 2) ? 'one' : 'other';
},

fur: _cp[1],

fy: _cp[3],

ga: function(n, ord) {
  var s = String(n).split('.'), t0 = Number(s[0]) == n;
  if (ord) return (n == 1) ? 'one' : 'other';
  return (n == 1) ? 'one'
      : (n == 2) ? 'two'
      : ((t0 && n >= 3 && n <= 6)) ? 'few'
      : ((t0 && n >= 7 && n <= 10)) ? 'many'
      : 'other';
},

gd: function(n, ord) {
  var s = String(n).split('.'), t0 = Number(s[0]) == n;
  if (ord) return 'other';
  return ((n == 1
          || n == 11)) ? 'one'
      : ((n == 2
          || n == 12)) ? 'two'
      : (((t0 && n >= 3 && n <= 10)
          || (t0 && n >= 13 && n <= 19))) ? 'few'
      : 'other';
},

gl: _cp[3],

gsw: _cp[1],

gu: function(n, ord) {
  if (ord) return (n == 1) ? 'one'
      : ((n == 2
          || n == 3)) ? 'two'
      : (n == 4) ? 'few'
      : (n == 6) ? 'many'
      : 'other';
  return (n >= 0 && n <= 1) ? 'one' : 'other';
},

guw: _cp[2],

gv: function(n, ord) {
  var s = String(n).split('.'), i = s[0], v0 = !s[1], i10 = i.slice(-1),
      i100 = i.slice(-2);
  if (ord) return 'other';
  return (v0 && i10 == 1) ? 'one'
      : (v0 && i10 == 2) ? 'two'
      : (v0 && (i100 == 0 || i100 == 20 || i100 == 40 || i100 == 60
          || i100 == 80)) ? 'few'
      : (!v0) ? 'many'
      : 'other';
},

ha: _cp[1],

haw: _cp[1],

he: function(n, ord) {
  var s = String(n).split('.'), i = s[0], v0 = !s[1], t0 = Number(s[0]) == n,
      n10 = t0 && s[0].slice(-1);
  if (ord) return 'other';
  return (n == 1 && v0) ? 'one'
      : (i == 2 && v0) ? 'two'
      : (v0 && (n < 0
          || n > 10) && t0 && n10 == 0) ? 'many'
      : 'other';
},

hi: function(n, ord) {
  if (ord) return (n == 1) ? 'one'
      : ((n == 2
          || n == 3)) ? 'two'
      : (n == 4) ? 'few'
      : (n == 6) ? 'many'
      : 'other';
  return (n >= 0 && n <= 1) ? 'one' : 'other';
},

hr: function(n, ord) {
  var s = String(n).split('.'), i = s[0], f = s[1] || '', v0 = !s[1],
      i10 = i.slice(-1), i100 = i.slice(-2), f10 = f.slice(-1), f100 = f.slice(-2);
  if (ord) return 'other';
  return (v0 && i10 == 1 && i100 != 11
          || f10 == 1 && f100 != 11) ? 'one'
      : (v0 && (i10 >= 2 && i10 <= 4) && (i100 < 12 || i100 > 14)
          || (f10 >= 2 && f10 <= 4) && (f100 < 12
          || f100 > 14)) ? 'few'
      : 'other';
},

hsb: function(n, ord) {
  var s = String(n).split('.'), i = s[0], f = s[1] || '', v0 = !s[1],
      i100 = i.slice(-2), f100 = f.slice(-2);
  if (ord) return 'other';
  return (v0 && i100 == 1
          || f100 == 1) ? 'one'
      : (v0 && i100 == 2
          || f100 == 2) ? 'two'
      : (v0 && (i100 == 3 || i100 == 4) || (f100 == 3
          || f100 == 4)) ? 'few'
      : 'other';
},

hu: function(n, ord) {
  if (ord) return ((n == 1
          || n == 5)) ? 'one' : 'other';
  return (n == 1) ? 'one' : 'other';
},

hy: function(n, ord) {
  if (ord) return (n == 1) ? 'one' : 'other';
  return (n >= 0 && n < 2) ? 'one' : 'other';
},

id: _cp[0],

ig: _cp[0],

ii: _cp[0],

"in": _cp[0],

is: function(n, ord) {
  var s = String(n).split('.'), i = s[0], t0 = Number(s[0]) == n,
      i10 = i.slice(-1), i100 = i.slice(-2);
  if (ord) return 'other';
  return (t0 && i10 == 1 && i100 != 11
          || !t0) ? 'one' : 'other';
},

it: function(n, ord) {
  var s = String(n).split('.'), v0 = !s[1];
  if (ord) return ((n == 11 || n == 8 || n == 80
          || n == 800)) ? 'many' : 'other';
  return (n == 1 && v0) ? 'one' : 'other';
},

iu: function(n, ord) {
  if (ord) return 'other';
  return (n == 1) ? 'one'
      : (n == 2) ? 'two'
      : 'other';
},

iw: function(n, ord) {
  var s = String(n).split('.'), i = s[0], v0 = !s[1], t0 = Number(s[0]) == n,
      n10 = t0 && s[0].slice(-1);
  if (ord) return 'other';
  return (n == 1 && v0) ? 'one'
      : (i == 2 && v0) ? 'two'
      : (v0 && (n < 0
          || n > 10) && t0 && n10 == 0) ? 'many'
      : 'other';
},

ja: _cp[0],

jbo: _cp[0],

jgo: _cp[1],

ji: _cp[3],

jmc: _cp[1],

jv: _cp[0],

jw: _cp[0],

ka: function(n, ord) {
  var s = String(n).split('.'), i = s[0], i100 = i.slice(-2);
  if (ord) return (i == 1) ? 'one'
      : (i == 0 || ((i100 >= 2 && i100 <= 20) || i100 == 40 || i100 == 60
          || i100 == 80)) ? 'many'
      : 'other';
  return (n == 1) ? 'one' : 'other';
},

kab: function(n, ord) {
  if (ord) return 'other';
  return (n >= 0 && n < 2) ? 'one' : 'other';
},

kaj: _cp[1],

kcg: _cp[1],

kde: _cp[0],

kea: _cp[0],

kk: function(n, ord) {
  var s = String(n).split('.'), t0 = Number(s[0]) == n,
      n10 = t0 && s[0].slice(-1);
  if (ord) return (n10 == 6 || n10 == 9
          || t0 && n10 == 0 && n != 0) ? 'many' : 'other';
  return (n == 1) ? 'one' : 'other';
},

kkj: _cp[1],

kl: _cp[1],

km: _cp[0],

kn: function(n, ord) {
  if (ord) return 'other';
  return (n >= 0 && n <= 1) ? 'one' : 'other';
},

ko: _cp[0],

ks: _cp[1],

ksb: _cp[1],

ksh: function(n, ord) {
  if (ord) return 'other';
  return (n == 0) ? 'zero'
      : (n == 1) ? 'one'
      : 'other';
},

ku: _cp[1],

kw: function(n, ord) {
  if (ord) return 'other';
  return (n == 1) ? 'one'
      : (n == 2) ? 'two'
      : 'other';
},

ky: _cp[1],

lag: function(n, ord) {
  var s = String(n).split('.'), i = s[0];
  if (ord) return 'other';
  return (n == 0) ? 'zero'
      : ((i == 0
          || i == 1) && n != 0) ? 'one'
      : 'other';
},

lb: _cp[1],

lg: _cp[1],

lkt: _cp[0],

ln: _cp[2],

lo: function(n, ord) {
  if (ord) return (n == 1) ? 'one' : 'other';
  return 'other';
},

lt: function(n, ord) {
  var s = String(n).split('.'), f = s[1] || '', t0 = Number(s[0]) == n,
      n10 = t0 && s[0].slice(-1), n100 = t0 && s[0].slice(-2);
  if (ord) return 'other';
  return (n10 == 1 && (n100 < 11
          || n100 > 19)) ? 'one'
      : ((n10 >= 2 && n10 <= 9) && (n100 < 11
          || n100 > 19)) ? 'few'
      : (f != 0) ? 'many'
      : 'other';
},

lv: function(n, ord) {
  var s = String(n).split('.'), f = s[1] || '', v = f.length,
      t0 = Number(s[0]) == n, n10 = t0 && s[0].slice(-1),
      n100 = t0 && s[0].slice(-2), f100 = f.slice(-2), f10 = f.slice(-1);
  if (ord) return 'other';
  return (t0 && n10 == 0 || (n100 >= 11 && n100 <= 19)
          || v == 2 && (f100 >= 11 && f100 <= 19)) ? 'zero'
      : (n10 == 1 && n100 != 11 || v == 2 && f10 == 1 && f100 != 11
          || v != 2 && f10 == 1) ? 'one'
      : 'other';
},

mas: _cp[1],

mg: _cp[2],

mgo: _cp[1],

mk: function(n, ord) {
  var s = String(n).split('.'), i = s[0], f = s[1] || '', v0 = !s[1],
      i10 = i.slice(-1), i100 = i.slice(-2), f10 = f.slice(-1);
  if (ord) return (i10 == 1 && i100 != 11) ? 'one'
      : (i10 == 2 && i100 != 12) ? 'two'
      : ((i10 == 7
          || i10 == 8) && i100 != 17 && i100 != 18) ? 'many'
      : 'other';
  return (v0 && i10 == 1
          || f10 == 1) ? 'one' : 'other';
},

ml: _cp[1],

mn: _cp[1],

mo: function(n, ord) {
  var s = String(n).split('.'), v0 = !s[1], t0 = Number(s[0]) == n,
      n100 = t0 && s[0].slice(-2);
  if (ord) return (n == 1) ? 'one' : 'other';
  return (n == 1 && v0) ? 'one'
      : (!v0 || n == 0
          || n != 1 && (n100 >= 1 && n100 <= 19)) ? 'few'
      : 'other';
},

mr: function(n, ord) {
  if (ord) return (n == 1) ? 'one'
      : ((n == 2
          || n == 3)) ? 'two'
      : (n == 4) ? 'few'
      : 'other';
  return (n >= 0 && n <= 1) ? 'one' : 'other';
},

ms: function(n, ord) {
  if (ord) return (n == 1) ? 'one' : 'other';
  return 'other';
},

mt: function(n, ord) {
  var s = String(n).split('.'), t0 = Number(s[0]) == n,
      n100 = t0 && s[0].slice(-2);
  if (ord) return 'other';
  return (n == 1) ? 'one'
      : (n == 0
          || (n100 >= 2 && n100 <= 10)) ? 'few'
      : ((n100 >= 11 && n100 <= 19)) ? 'many'
      : 'other';
},

my: _cp[0],

nah: _cp[1],

naq: function(n, ord) {
  if (ord) return 'other';
  return (n == 1) ? 'one'
      : (n == 2) ? 'two'
      : 'other';
},

nb: _cp[1],

nd: _cp[1],

ne: function(n, ord) {
  var s = String(n).split('.'), t0 = Number(s[0]) == n;
  if (ord) return ((t0 && n >= 1 && n <= 4)) ? 'one' : 'other';
  return (n == 1) ? 'one' : 'other';
},

nl: _cp[3],

nn: _cp[1],

nnh: _cp[1],

no: _cp[1],

nqo: _cp[0],

nr: _cp[1],

nso: _cp[2],

ny: _cp[1],

nyn: _cp[1],

om: _cp[1],

or: _cp[1],

os: _cp[1],

pa: _cp[2],

pap: _cp[1],

pl: function(n, ord) {
  var s = String(n).split('.'), i = s[0], v0 = !s[1], i10 = i.slice(-1),
      i100 = i.slice(-2);
  if (ord) return 'other';
  return (n == 1 && v0) ? 'one'
      : (v0 && (i10 >= 2 && i10 <= 4) && (i100 < 12
          || i100 > 14)) ? 'few'
      : (v0 && i != 1 && (i10 == 0 || i10 == 1)
          || v0 && (i10 >= 5 && i10 <= 9)
          || v0 && (i100 >= 12 && i100 <= 14)) ? 'many'
      : 'other';
},

prg: function(n, ord) {
  var s = String(n).split('.'), f = s[1] || '', v = f.length,
      t0 = Number(s[0]) == n, n10 = t0 && s[0].slice(-1),
      n100 = t0 && s[0].slice(-2), f100 = f.slice(-2), f10 = f.slice(-1);
  if (ord) return 'other';
  return (t0 && n10 == 0 || (n100 >= 11 && n100 <= 19)
          || v == 2 && (f100 >= 11 && f100 <= 19)) ? 'zero'
      : (n10 == 1 && n100 != 11 || v == 2 && f10 == 1 && f100 != 11
          || v != 2 && f10 == 1) ? 'one'
      : 'other';
},

ps: _cp[1],

pt: function(n, ord) {
  var s = String(n).split('.'), t0 = Number(s[0]) == n;
  if (ord) return 'other';
  return ((t0 && n >= 0 && n <= 2) && n != 2) ? 'one' : 'other';
},

"pt-PT": _cp[3],

rm: _cp[1],

ro: function(n, ord) {
  var s = String(n).split('.'), v0 = !s[1], t0 = Number(s[0]) == n,
      n100 = t0 && s[0].slice(-2);
  if (ord) return (n == 1) ? 'one' : 'other';
  return (n == 1 && v0) ? 'one'
      : (!v0 || n == 0
          || n != 1 && (n100 >= 1 && n100 <= 19)) ? 'few'
      : 'other';
},

rof: _cp[1],

root: _cp[0],

ru: function(n, ord) {
  var s = String(n).split('.'), i = s[0], v0 = !s[1], i10 = i.slice(-1),
      i100 = i.slice(-2);
  if (ord) return 'other';
  return (v0 && i10 == 1 && i100 != 11) ? 'one'
      : (v0 && (i10 >= 2 && i10 <= 4) && (i100 < 12
          || i100 > 14)) ? 'few'
      : (v0 && i10 == 0 || v0 && (i10 >= 5 && i10 <= 9)
          || v0 && (i100 >= 11 && i100 <= 14)) ? 'many'
      : 'other';
},

rwk: _cp[1],

sah: _cp[0],

saq: _cp[1],

sdh: _cp[1],

se: function(n, ord) {
  if (ord) return 'other';
  return (n == 1) ? 'one'
      : (n == 2) ? 'two'
      : 'other';
},

seh: _cp[1],

ses: _cp[0],

sg: _cp[0],

sh: function(n, ord) {
  var s = String(n).split('.'), i = s[0], f = s[1] || '', v0 = !s[1],
      i10 = i.slice(-1), i100 = i.slice(-2), f10 = f.slice(-1), f100 = f.slice(-2);
  if (ord) return 'other';
  return (v0 && i10 == 1 && i100 != 11
          || f10 == 1 && f100 != 11) ? 'one'
      : (v0 && (i10 >= 2 && i10 <= 4) && (i100 < 12 || i100 > 14)
          || (f10 >= 2 && f10 <= 4) && (f100 < 12
          || f100 > 14)) ? 'few'
      : 'other';
},

shi: function(n, ord) {
  var s = String(n).split('.'), t0 = Number(s[0]) == n;
  if (ord) return 'other';
  return (n >= 0 && n <= 1) ? 'one'
      : ((t0 && n >= 2 && n <= 10)) ? 'few'
      : 'other';
},

si: function(n, ord) {
  var s = String(n).split('.'), i = s[0], f = s[1] || '';
  if (ord) return 'other';
  return ((n == 0 || n == 1)
          || i == 0 && f == 1) ? 'one' : 'other';
},

sk: function(n, ord) {
  var s = String(n).split('.'), i = s[0], v0 = !s[1];
  if (ord) return 'other';
  return (n == 1 && v0) ? 'one'
      : ((i >= 2 && i <= 4) && v0) ? 'few'
      : (!v0) ? 'many'
      : 'other';
},

sl: function(n, ord) {
  var s = String(n).split('.'), i = s[0], v0 = !s[1], i100 = i.slice(-2);
  if (ord) return 'other';
  return (v0 && i100 == 1) ? 'one'
      : (v0 && i100 == 2) ? 'two'
      : (v0 && (i100 == 3 || i100 == 4)
          || !v0) ? 'few'
      : 'other';
},

sma: function(n, ord) {
  if (ord) return 'other';
  return (n == 1) ? 'one'
      : (n == 2) ? 'two'
      : 'other';
},

smi: function(n, ord) {
  if (ord) return 'other';
  return (n == 1) ? 'one'
      : (n == 2) ? 'two'
      : 'other';
},

smj: function(n, ord) {
  if (ord) return 'other';
  return (n == 1) ? 'one'
      : (n == 2) ? 'two'
      : 'other';
},

smn: function(n, ord) {
  if (ord) return 'other';
  return (n == 1) ? 'one'
      : (n == 2) ? 'two'
      : 'other';
},

sms: function(n, ord) {
  if (ord) return 'other';
  return (n == 1) ? 'one'
      : (n == 2) ? 'two'
      : 'other';
},

sn: _cp[1],

so: _cp[1],

sq: function(n, ord) {
  var s = String(n).split('.'), t0 = Number(s[0]) == n,
      n10 = t0 && s[0].slice(-1), n100 = t0 && s[0].slice(-2);
  if (ord) return (n == 1) ? 'one'
      : (n10 == 4 && n100 != 14) ? 'many'
      : 'other';
  return (n == 1) ? 'one' : 'other';
},

sr: function(n, ord) {
  var s = String(n).split('.'), i = s[0], f = s[1] || '', v0 = !s[1],
      i10 = i.slice(-1), i100 = i.slice(-2), f10 = f.slice(-1), f100 = f.slice(-2);
  if (ord) return 'other';
  return (v0 && i10 == 1 && i100 != 11
          || f10 == 1 && f100 != 11) ? 'one'
      : (v0 && (i10 >= 2 && i10 <= 4) && (i100 < 12 || i100 > 14)
          || (f10 >= 2 && f10 <= 4) && (f100 < 12
          || f100 > 14)) ? 'few'
      : 'other';
},

ss: _cp[1],

ssy: _cp[1],

st: _cp[1],

sv: function(n, ord) {
  var s = String(n).split('.'), v0 = !s[1], t0 = Number(s[0]) == n,
      n10 = t0 && s[0].slice(-1), n100 = t0 && s[0].slice(-2);
  if (ord) return ((n10 == 1
          || n10 == 2) && n100 != 11 && n100 != 12) ? 'one' : 'other';
  return (n == 1 && v0) ? 'one' : 'other';
},

sw: _cp[3],

syr: _cp[1],

ta: _cp[1],

te: _cp[1],

teo: _cp[1],

th: _cp[0],

ti: _cp[2],

tig: _cp[1],

tk: _cp[1],

tl: function(n, ord) {
  var s = String(n).split('.'), i = s[0], f = s[1] || '', v0 = !s[1],
      i10 = i.slice(-1), f10 = f.slice(-1);
  if (ord) return (n == 1) ? 'one' : 'other';
  return (v0 && (i == 1 || i == 2 || i == 3)
          || v0 && i10 != 4 && i10 != 6 && i10 != 9
          || !v0 && f10 != 4 && f10 != 6 && f10 != 9) ? 'one' : 'other';
},

tn: _cp[1],

to: _cp[0],

tr: _cp[1],

ts: _cp[1],

tzm: function(n, ord) {
  var s = String(n).split('.'), t0 = Number(s[0]) == n;
  if (ord) return 'other';
  return ((n == 0 || n == 1)
          || (t0 && n >= 11 && n <= 99)) ? 'one' : 'other';
},

ug: _cp[1],

uk: function(n, ord) {
  var s = String(n).split('.'), i = s[0], v0 = !s[1], t0 = Number(s[0]) == n,
      n10 = t0 && s[0].slice(-1), n100 = t0 && s[0].slice(-2), i10 = i.slice(-1),
      i100 = i.slice(-2);
  if (ord) return (n10 == 3 && n100 != 13) ? 'few' : 'other';
  return (v0 && i10 == 1 && i100 != 11) ? 'one'
      : (v0 && (i10 >= 2 && i10 <= 4) && (i100 < 12
          || i100 > 14)) ? 'few'
      : (v0 && i10 == 0 || v0 && (i10 >= 5 && i10 <= 9)
          || v0 && (i100 >= 11 && i100 <= 14)) ? 'many'
      : 'other';
},

ur: _cp[3],

uz: _cp[1],

ve: _cp[1],

vi: function(n, ord) {
  if (ord) return (n == 1) ? 'one' : 'other';
  return 'other';
},

vo: _cp[1],

vun: _cp[1],

wa: _cp[2],

wae: _cp[1],

wo: _cp[0],

xh: _cp[1],

xog: _cp[1],

yi: _cp[3],

yo: _cp[0],

zh: _cp[0],

zu: function(n, ord) {
  if (ord) return 'other';
  return (n >= 0 && n <= 1) ? 'one' : 'other';
}
}));

},{}],8:[function(require,module,exports){
/*
 * Generated by PEG.js 0.10.0.
 *
 * http://pegjs.org/
 */

"use strict";

function peg$subclass(child, parent) {
  function ctor() { this.constructor = child; }
  ctor.prototype = parent.prototype;
  child.prototype = new ctor();
}

function peg$SyntaxError(message, expected, found, location) {
  this.message  = message;
  this.expected = expected;
  this.found    = found;
  this.location = location;
  this.name     = "SyntaxError";

  if (typeof Error.captureStackTrace === "function") {
    Error.captureStackTrace(this, peg$SyntaxError);
  }
}

peg$subclass(peg$SyntaxError, Error);

peg$SyntaxError.buildMessage = function(expected, found) {
  var DESCRIBE_EXPECTATION_FNS = {
        literal: function(expectation) {
          return "\"" + literalEscape(expectation.text) + "\"";
        },

        "class": function(expectation) {
          var escapedParts = "",
              i;

          for (i = 0; i < expectation.parts.length; i++) {
            escapedParts += expectation.parts[i] instanceof Array
              ? classEscape(expectation.parts[i][0]) + "-" + classEscape(expectation.parts[i][1])
              : classEscape(expectation.parts[i]);
          }

          return "[" + (expectation.inverted ? "^" : "") + escapedParts + "]";
        },

        any: function(expectation) {
          return "any character";
        },

        end: function(expectation) {
          return "end of input";
        },

        other: function(expectation) {
          return expectation.description;
        }
      };

  function hex(ch) {
    return ch.charCodeAt(0).toString(16).toUpperCase();
  }

  function literalEscape(s) {
    return s
      .replace(/\\/g, '\\\\')
      .replace(/"/g,  '\\"')
      .replace(/\0/g, '\\0')
      .replace(/\t/g, '\\t')
      .replace(/\n/g, '\\n')
      .replace(/\r/g, '\\r')
      .replace(/[\x00-\x0F]/g,          function(ch) { return '\\x0' + hex(ch); })
      .replace(/[\x10-\x1F\x7F-\x9F]/g, function(ch) { return '\\x'  + hex(ch); });
  }

  function classEscape(s) {
    return s
      .replace(/\\/g, '\\\\')
      .replace(/\]/g, '\\]')
      .replace(/\^/g, '\\^')
      .replace(/-/g,  '\\-')
      .replace(/\0/g, '\\0')
      .replace(/\t/g, '\\t')
      .replace(/\n/g, '\\n')
      .replace(/\r/g, '\\r')
      .replace(/[\x00-\x0F]/g,          function(ch) { return '\\x0' + hex(ch); })
      .replace(/[\x10-\x1F\x7F-\x9F]/g, function(ch) { return '\\x'  + hex(ch); });
  }

  function describeExpectation(expectation) {
    return DESCRIBE_EXPECTATION_FNS[expectation.type](expectation);
  }

  function describeExpected(expected) {
    var descriptions = new Array(expected.length),
        i, j;

    for (i = 0; i < expected.length; i++) {
      descriptions[i] = describeExpectation(expected[i]);
    }

    descriptions.sort();

    if (descriptions.length > 0) {
      for (i = 1, j = 1; i < descriptions.length; i++) {
        if (descriptions[i - 1] !== descriptions[i]) {
          descriptions[j] = descriptions[i];
          j++;
        }
      }
      descriptions.length = j;
    }

    switch (descriptions.length) {
      case 1:
        return descriptions[0];

      case 2:
        return descriptions[0] + " or " + descriptions[1];

      default:
        return descriptions.slice(0, -1).join(", ")
          + ", or "
          + descriptions[descriptions.length - 1];
    }
  }

  function describeFound(found) {
    return found ? "\"" + literalEscape(found) + "\"" : "end of input";
  }

  return "Expected " + describeExpected(expected) + " but " + describeFound(found) + " found.";
};

function peg$parse(input, options) {
  options = options !== void 0 ? options : {};

  var peg$FAILED = {},

      peg$startRuleFunctions = { start: peg$parsestart },
      peg$startRuleFunction  = peg$parsestart,

      peg$c0 = "#",
      peg$c1 = peg$literalExpectation("#", false),
      peg$c2 = function() { return { type: 'octothorpe' }; },
      peg$c3 = function(str) { return str.join(''); },
      peg$c4 = "{",
      peg$c5 = peg$literalExpectation("{", false),
      peg$c6 = "}",
      peg$c7 = peg$literalExpectation("}", false),
      peg$c8 = function(arg) {
          return {
            type: 'argument',
            arg: arg
          };
        },
      peg$c9 = ",",
      peg$c10 = peg$literalExpectation(",", false),
      peg$c11 = "select",
      peg$c12 = peg$literalExpectation("select", false),
      peg$c13 = function(arg, cases) {
          return {
            type: 'select',
            arg: arg,
            cases: cases
          };
        },
      peg$c14 = "plural",
      peg$c15 = peg$literalExpectation("plural", false),
      peg$c16 = "selectordinal",
      peg$c17 = peg$literalExpectation("selectordinal", false),
      peg$c18 = function(arg, type, offset, cases) {
          var ls = ((type === 'selectordinal') ? options.ordinal : options.cardinal)
                   || ['zero', 'one', 'two', 'few', 'many', 'other'];
          if (ls && ls.length) cases.forEach(function(c) {
            if (isNaN(c.key) && ls.indexOf(c.key) < 0) throw new Error(
              'Invalid key `' + c.key + '` for argument `' + arg + '`.' +
              ' Valid ' + type + ' keys for this locale are `' + ls.join('`, `') +
              '`, and explicit keys like `=0`.');
          });
          return {
            type: type,
            arg: arg,
            offset: offset || 0,
            cases: cases
          };
        },
      peg$c19 = function(arg, key, params) {
          return {
            type: 'function',
            arg: arg,
            key: key,
            params: params
          };
        },
      peg$c20 = /^[0-9a-zA-Z$_]/,
      peg$c21 = peg$classExpectation([["0", "9"], ["a", "z"], ["A", "Z"], "$", "_"], false, false),
      peg$c22 = /^[^ \t\n\r,.+={}]/,
      peg$c23 = peg$classExpectation([" ", "\t", "\n", "\r", ",", ".", "+", "=", "{", "}"], true, false),
      peg$c24 = function(key, tokens) { return { key: key, tokens: tokens }; },
      peg$c25 = function(tokens) { return tokens; },
      peg$c26 = "offset",
      peg$c27 = peg$literalExpectation("offset", false),
      peg$c28 = ":",
      peg$c29 = peg$literalExpectation(":", false),
      peg$c30 = function(d) { return d; },
      peg$c31 = "=",
      peg$c32 = peg$literalExpectation("=", false),
      peg$c33 = function(p) { return p; },
      peg$c34 = /^[^{}#\\\0-\x08\x0E-\x1F\x7F]/,
      peg$c35 = peg$classExpectation(["{", "}", "#", "\\", ["\0", "\b"], ["\x0E", "\x1F"], "\x7F"], true, false),
      peg$c36 = "\\\\",
      peg$c37 = peg$literalExpectation("\\\\", false),
      peg$c38 = function() { return '\\'; },
      peg$c39 = "\\#",
      peg$c40 = peg$literalExpectation("\\#", false),
      peg$c41 = function() { return '#'; },
      peg$c42 = "\\{",
      peg$c43 = peg$literalExpectation("\\{", false),
      peg$c44 = function() { return '\u007B'; },
      peg$c45 = "\\}",
      peg$c46 = peg$literalExpectation("\\}", false),
      peg$c47 = function() { return '\u007D'; },
      peg$c48 = "\\u",
      peg$c49 = peg$literalExpectation("\\u", false),
      peg$c50 = function(h1, h2, h3, h4) {
            return String.fromCharCode(parseInt('0x' + h1 + h2 + h3 + h4));
          },
      peg$c51 = /^[0-9]/,
      peg$c52 = peg$classExpectation([["0", "9"]], false, false),
      peg$c53 = /^[0-9a-fA-F]/,
      peg$c54 = peg$classExpectation([["0", "9"], ["a", "f"], ["A", "F"]], false, false),
      peg$c55 = /^[ \t\n\r]/,
      peg$c56 = peg$classExpectation([" ", "\t", "\n", "\r"], false, false),

      peg$currPos          = 0,
      peg$savedPos         = 0,
      peg$posDetailsCache  = [{ line: 1, column: 1 }],
      peg$maxFailPos       = 0,
      peg$maxFailExpected  = [],
      peg$silentFails      = 0,

      peg$result;

  if ("startRule" in options) {
    if (!(options.startRule in peg$startRuleFunctions)) {
      throw new Error("Can't start parsing from rule \"" + options.startRule + "\".");
    }

    peg$startRuleFunction = peg$startRuleFunctions[options.startRule];
  }

  function text() {
    return input.substring(peg$savedPos, peg$currPos);
  }

  function location() {
    return peg$computeLocation(peg$savedPos, peg$currPos);
  }

  function expected(description, location) {
    location = location !== void 0 ? location : peg$computeLocation(peg$savedPos, peg$currPos)

    throw peg$buildStructuredError(
      [peg$otherExpectation(description)],
      input.substring(peg$savedPos, peg$currPos),
      location
    );
  }

  function error(message, location) {
    location = location !== void 0 ? location : peg$computeLocation(peg$savedPos, peg$currPos)

    throw peg$buildSimpleError(message, location);
  }

  function peg$literalExpectation(text, ignoreCase) {
    return { type: "literal", text: text, ignoreCase: ignoreCase };
  }

  function peg$classExpectation(parts, inverted, ignoreCase) {
    return { type: "class", parts: parts, inverted: inverted, ignoreCase: ignoreCase };
  }

  function peg$anyExpectation() {
    return { type: "any" };
  }

  function peg$endExpectation() {
    return { type: "end" };
  }

  function peg$otherExpectation(description) {
    return { type: "other", description: description };
  }

  function peg$computePosDetails(pos) {
    var details = peg$posDetailsCache[pos], p;

    if (details) {
      return details;
    } else {
      p = pos - 1;
      while (!peg$posDetailsCache[p]) {
        p--;
      }

      details = peg$posDetailsCache[p];
      details = {
        line:   details.line,
        column: details.column
      };

      while (p < pos) {
        if (input.charCodeAt(p) === 10) {
          details.line++;
          details.column = 1;
        } else {
          details.column++;
        }

        p++;
      }

      peg$posDetailsCache[pos] = details;
      return details;
    }
  }

  function peg$computeLocation(startPos, endPos) {
    var startPosDetails = peg$computePosDetails(startPos),
        endPosDetails   = peg$computePosDetails(endPos);

    return {
      start: {
        offset: startPos,
        line:   startPosDetails.line,
        column: startPosDetails.column
      },
      end: {
        offset: endPos,
        line:   endPosDetails.line,
        column: endPosDetails.column
      }
    };
  }

  function peg$fail(expected) {
    if (peg$currPos < peg$maxFailPos) { return; }

    if (peg$currPos > peg$maxFailPos) {
      peg$maxFailPos = peg$currPos;
      peg$maxFailExpected = [];
    }

    peg$maxFailExpected.push(expected);
  }

  function peg$buildSimpleError(message, location) {
    return new peg$SyntaxError(message, null, null, location);
  }

  function peg$buildStructuredError(expected, found, location) {
    return new peg$SyntaxError(
      peg$SyntaxError.buildMessage(expected, found),
      expected,
      found,
      location
    );
  }

  function peg$parsestart() {
    var s0, s1;

    s0 = [];
    s1 = peg$parsetoken();
    while (s1 !== peg$FAILED) {
      s0.push(s1);
      s1 = peg$parsetoken();
    }

    return s0;
  }

  function peg$parsetoken() {
    var s0, s1, s2;

    s0 = peg$parseargument();
    if (s0 === peg$FAILED) {
      s0 = peg$parseselect();
      if (s0 === peg$FAILED) {
        s0 = peg$parseplural();
        if (s0 === peg$FAILED) {
          s0 = peg$parsefunction();
          if (s0 === peg$FAILED) {
            s0 = peg$currPos;
            if (input.charCodeAt(peg$currPos) === 35) {
              s1 = peg$c0;
              peg$currPos++;
            } else {
              s1 = peg$FAILED;
              if (peg$silentFails === 0) { peg$fail(peg$c1); }
            }
            if (s1 !== peg$FAILED) {
              peg$savedPos = s0;
              s1 = peg$c2();
            }
            s0 = s1;
            if (s0 === peg$FAILED) {
              s0 = peg$currPos;
              s1 = [];
              s2 = peg$parsechar();
              if (s2 !== peg$FAILED) {
                while (s2 !== peg$FAILED) {
                  s1.push(s2);
                  s2 = peg$parsechar();
                }
              } else {
                s1 = peg$FAILED;
              }
              if (s1 !== peg$FAILED) {
                peg$savedPos = s0;
                s1 = peg$c3(s1);
              }
              s0 = s1;
            }
          }
        }
      }
    }

    return s0;
  }

  function peg$parseargument() {
    var s0, s1, s2, s3, s4, s5;

    s0 = peg$currPos;
    if (input.charCodeAt(peg$currPos) === 123) {
      s1 = peg$c4;
      peg$currPos++;
    } else {
      s1 = peg$FAILED;
      if (peg$silentFails === 0) { peg$fail(peg$c5); }
    }
    if (s1 !== peg$FAILED) {
      s2 = peg$parse_();
      if (s2 !== peg$FAILED) {
        s3 = peg$parseid();
        if (s3 !== peg$FAILED) {
          s4 = peg$parse_();
          if (s4 !== peg$FAILED) {
            if (input.charCodeAt(peg$currPos) === 125) {
              s5 = peg$c6;
              peg$currPos++;
            } else {
              s5 = peg$FAILED;
              if (peg$silentFails === 0) { peg$fail(peg$c7); }
            }
            if (s5 !== peg$FAILED) {
              peg$savedPos = s0;
              s1 = peg$c8(s3);
              s0 = s1;
            } else {
              peg$currPos = s0;
              s0 = peg$FAILED;
            }
          } else {
            peg$currPos = s0;
            s0 = peg$FAILED;
          }
        } else {
          peg$currPos = s0;
          s0 = peg$FAILED;
        }
      } else {
        peg$currPos = s0;
        s0 = peg$FAILED;
      }
    } else {
      peg$currPos = s0;
      s0 = peg$FAILED;
    }

    return s0;
  }

  function peg$parseselect() {
    var s0, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13;

    s0 = peg$currPos;
    if (input.charCodeAt(peg$currPos) === 123) {
      s1 = peg$c4;
      peg$currPos++;
    } else {
      s1 = peg$FAILED;
      if (peg$silentFails === 0) { peg$fail(peg$c5); }
    }
    if (s1 !== peg$FAILED) {
      s2 = peg$parse_();
      if (s2 !== peg$FAILED) {
        s3 = peg$parseid();
        if (s3 !== peg$FAILED) {
          s4 = peg$parse_();
          if (s4 !== peg$FAILED) {
            if (input.charCodeAt(peg$currPos) === 44) {
              s5 = peg$c9;
              peg$currPos++;
            } else {
              s5 = peg$FAILED;
              if (peg$silentFails === 0) { peg$fail(peg$c10); }
            }
            if (s5 !== peg$FAILED) {
              s6 = peg$parse_();
              if (s6 !== peg$FAILED) {
                if (input.substr(peg$currPos, 6) === peg$c11) {
                  s7 = peg$c11;
                  peg$currPos += 6;
                } else {
                  s7 = peg$FAILED;
                  if (peg$silentFails === 0) { peg$fail(peg$c12); }
                }
                if (s7 !== peg$FAILED) {
                  s8 = peg$parse_();
                  if (s8 !== peg$FAILED) {
                    if (input.charCodeAt(peg$currPos) === 44) {
                      s9 = peg$c9;
                      peg$currPos++;
                    } else {
                      s9 = peg$FAILED;
                      if (peg$silentFails === 0) { peg$fail(peg$c10); }
                    }
                    if (s9 !== peg$FAILED) {
                      s10 = peg$parse_();
                      if (s10 !== peg$FAILED) {
                        s11 = [];
                        s12 = peg$parseselectCase();
                        if (s12 !== peg$FAILED) {
                          while (s12 !== peg$FAILED) {
                            s11.push(s12);
                            s12 = peg$parseselectCase();
                          }
                        } else {
                          s11 = peg$FAILED;
                        }
                        if (s11 !== peg$FAILED) {
                          s12 = peg$parse_();
                          if (s12 !== peg$FAILED) {
                            if (input.charCodeAt(peg$currPos) === 125) {
                              s13 = peg$c6;
                              peg$currPos++;
                            } else {
                              s13 = peg$FAILED;
                              if (peg$silentFails === 0) { peg$fail(peg$c7); }
                            }
                            if (s13 !== peg$FAILED) {
                              peg$savedPos = s0;
                              s1 = peg$c13(s3, s11);
                              s0 = s1;
                            } else {
                              peg$currPos = s0;
                              s0 = peg$FAILED;
                            }
                          } else {
                            peg$currPos = s0;
                            s0 = peg$FAILED;
                          }
                        } else {
                          peg$currPos = s0;
                          s0 = peg$FAILED;
                        }
                      } else {
                        peg$currPos = s0;
                        s0 = peg$FAILED;
                      }
                    } else {
                      peg$currPos = s0;
                      s0 = peg$FAILED;
                    }
                  } else {
                    peg$currPos = s0;
                    s0 = peg$FAILED;
                  }
                } else {
                  peg$currPos = s0;
                  s0 = peg$FAILED;
                }
              } else {
                peg$currPos = s0;
                s0 = peg$FAILED;
              }
            } else {
              peg$currPos = s0;
              s0 = peg$FAILED;
            }
          } else {
            peg$currPos = s0;
            s0 = peg$FAILED;
          }
        } else {
          peg$currPos = s0;
          s0 = peg$FAILED;
        }
      } else {
        peg$currPos = s0;
        s0 = peg$FAILED;
      }
    } else {
      peg$currPos = s0;
      s0 = peg$FAILED;
    }

    return s0;
  }

  function peg$parseplural() {
    var s0, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14;

    s0 = peg$currPos;
    if (input.charCodeAt(peg$currPos) === 123) {
      s1 = peg$c4;
      peg$currPos++;
    } else {
      s1 = peg$FAILED;
      if (peg$silentFails === 0) { peg$fail(peg$c5); }
    }
    if (s1 !== peg$FAILED) {
      s2 = peg$parse_();
      if (s2 !== peg$FAILED) {
        s3 = peg$parseid();
        if (s3 !== peg$FAILED) {
          s4 = peg$parse_();
          if (s4 !== peg$FAILED) {
            if (input.charCodeAt(peg$currPos) === 44) {
              s5 = peg$c9;
              peg$currPos++;
            } else {
              s5 = peg$FAILED;
              if (peg$silentFails === 0) { peg$fail(peg$c10); }
            }
            if (s5 !== peg$FAILED) {
              s6 = peg$parse_();
              if (s6 !== peg$FAILED) {
                if (input.substr(peg$currPos, 6) === peg$c14) {
                  s7 = peg$c14;
                  peg$currPos += 6;
                } else {
                  s7 = peg$FAILED;
                  if (peg$silentFails === 0) { peg$fail(peg$c15); }
                }
                if (s7 === peg$FAILED) {
                  if (input.substr(peg$currPos, 13) === peg$c16) {
                    s7 = peg$c16;
                    peg$currPos += 13;
                  } else {
                    s7 = peg$FAILED;
                    if (peg$silentFails === 0) { peg$fail(peg$c17); }
                  }
                }
                if (s7 !== peg$FAILED) {
                  s8 = peg$parse_();
                  if (s8 !== peg$FAILED) {
                    if (input.charCodeAt(peg$currPos) === 44) {
                      s9 = peg$c9;
                      peg$currPos++;
                    } else {
                      s9 = peg$FAILED;
                      if (peg$silentFails === 0) { peg$fail(peg$c10); }
                    }
                    if (s9 !== peg$FAILED) {
                      s10 = peg$parse_();
                      if (s10 !== peg$FAILED) {
                        s11 = peg$parseoffset();
                        if (s11 === peg$FAILED) {
                          s11 = null;
                        }
                        if (s11 !== peg$FAILED) {
                          s12 = [];
                          s13 = peg$parsepluralCase();
                          if (s13 !== peg$FAILED) {
                            while (s13 !== peg$FAILED) {
                              s12.push(s13);
                              s13 = peg$parsepluralCase();
                            }
                          } else {
                            s12 = peg$FAILED;
                          }
                          if (s12 !== peg$FAILED) {
                            s13 = peg$parse_();
                            if (s13 !== peg$FAILED) {
                              if (input.charCodeAt(peg$currPos) === 125) {
                                s14 = peg$c6;
                                peg$currPos++;
                              } else {
                                s14 = peg$FAILED;
                                if (peg$silentFails === 0) { peg$fail(peg$c7); }
                              }
                              if (s14 !== peg$FAILED) {
                                peg$savedPos = s0;
                                s1 = peg$c18(s3, s7, s11, s12);
                                s0 = s1;
                              } else {
                                peg$currPos = s0;
                                s0 = peg$FAILED;
                              }
                            } else {
                              peg$currPos = s0;
                              s0 = peg$FAILED;
                            }
                          } else {
                            peg$currPos = s0;
                            s0 = peg$FAILED;
                          }
                        } else {
                          peg$currPos = s0;
                          s0 = peg$FAILED;
                        }
                      } else {
                        peg$currPos = s0;
                        s0 = peg$FAILED;
                      }
                    } else {
                      peg$currPos = s0;
                      s0 = peg$FAILED;
                    }
                  } else {
                    peg$currPos = s0;
                    s0 = peg$FAILED;
                  }
                } else {
                  peg$currPos = s0;
                  s0 = peg$FAILED;
                }
              } else {
                peg$currPos = s0;
                s0 = peg$FAILED;
              }
            } else {
              peg$currPos = s0;
              s0 = peg$FAILED;
            }
          } else {
            peg$currPos = s0;
            s0 = peg$FAILED;
          }
        } else {
          peg$currPos = s0;
          s0 = peg$FAILED;
        }
      } else {
        peg$currPos = s0;
        s0 = peg$FAILED;
      }
    } else {
      peg$currPos = s0;
      s0 = peg$FAILED;
    }

    return s0;
  }

  function peg$parsefunction() {
    var s0, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10;

    s0 = peg$currPos;
    if (input.charCodeAt(peg$currPos) === 123) {
      s1 = peg$c4;
      peg$currPos++;
    } else {
      s1 = peg$FAILED;
      if (peg$silentFails === 0) { peg$fail(peg$c5); }
    }
    if (s1 !== peg$FAILED) {
      s2 = peg$parse_();
      if (s2 !== peg$FAILED) {
        s3 = peg$parseid();
        if (s3 !== peg$FAILED) {
          s4 = peg$parse_();
          if (s4 !== peg$FAILED) {
            if (input.charCodeAt(peg$currPos) === 44) {
              s5 = peg$c9;
              peg$currPos++;
            } else {
              s5 = peg$FAILED;
              if (peg$silentFails === 0) { peg$fail(peg$c10); }
            }
            if (s5 !== peg$FAILED) {
              s6 = peg$parse_();
              if (s6 !== peg$FAILED) {
                s7 = peg$parseid();
                if (s7 !== peg$FAILED) {
                  s8 = peg$parse_();
                  if (s8 !== peg$FAILED) {
                    s9 = [];
                    s10 = peg$parsefunctionParams();
                    while (s10 !== peg$FAILED) {
                      s9.push(s10);
                      s10 = peg$parsefunctionParams();
                    }
                    if (s9 !== peg$FAILED) {
                      if (input.charCodeAt(peg$currPos) === 125) {
                        s10 = peg$c6;
                        peg$currPos++;
                      } else {
                        s10 = peg$FAILED;
                        if (peg$silentFails === 0) { peg$fail(peg$c7); }
                      }
                      if (s10 !== peg$FAILED) {
                        peg$savedPos = s0;
                        s1 = peg$c19(s3, s7, s9);
                        s0 = s1;
                      } else {
                        peg$currPos = s0;
                        s0 = peg$FAILED;
                      }
                    } else {
                      peg$currPos = s0;
                      s0 = peg$FAILED;
                    }
                  } else {
                    peg$currPos = s0;
                    s0 = peg$FAILED;
                  }
                } else {
                  peg$currPos = s0;
                  s0 = peg$FAILED;
                }
              } else {
                peg$currPos = s0;
                s0 = peg$FAILED;
              }
            } else {
              peg$currPos = s0;
              s0 = peg$FAILED;
            }
          } else {
            peg$currPos = s0;
            s0 = peg$FAILED;
          }
        } else {
          peg$currPos = s0;
          s0 = peg$FAILED;
        }
      } else {
        peg$currPos = s0;
        s0 = peg$FAILED;
      }
    } else {
      peg$currPos = s0;
      s0 = peg$FAILED;
    }

    return s0;
  }

  function peg$parseid() {
    var s0, s1, s2, s3, s4;

    s0 = peg$currPos;
    s1 = peg$currPos;
    if (peg$c20.test(input.charAt(peg$currPos))) {
      s2 = input.charAt(peg$currPos);
      peg$currPos++;
    } else {
      s2 = peg$FAILED;
      if (peg$silentFails === 0) { peg$fail(peg$c21); }
    }
    if (s2 !== peg$FAILED) {
      s3 = [];
      if (peg$c22.test(input.charAt(peg$currPos))) {
        s4 = input.charAt(peg$currPos);
        peg$currPos++;
      } else {
        s4 = peg$FAILED;
        if (peg$silentFails === 0) { peg$fail(peg$c23); }
      }
      while (s4 !== peg$FAILED) {
        s3.push(s4);
        if (peg$c22.test(input.charAt(peg$currPos))) {
          s4 = input.charAt(peg$currPos);
          peg$currPos++;
        } else {
          s4 = peg$FAILED;
          if (peg$silentFails === 0) { peg$fail(peg$c23); }
        }
      }
      if (s3 !== peg$FAILED) {
        s2 = [s2, s3];
        s1 = s2;
      } else {
        peg$currPos = s1;
        s1 = peg$FAILED;
      }
    } else {
      peg$currPos = s1;
      s1 = peg$FAILED;
    }
    if (s1 !== peg$FAILED) {
      s0 = input.substring(s0, peg$currPos);
    } else {
      s0 = s1;
    }

    return s0;
  }

  function peg$parseselectCase() {
    var s0, s1, s2, s3, s4;

    s0 = peg$currPos;
    s1 = peg$parse_();
    if (s1 !== peg$FAILED) {
      s2 = peg$parseid();
      if (s2 !== peg$FAILED) {
        s3 = peg$parse_();
        if (s3 !== peg$FAILED) {
          s4 = peg$parsecaseTokens();
          if (s4 !== peg$FAILED) {
            peg$savedPos = s0;
            s1 = peg$c24(s2, s4);
            s0 = s1;
          } else {
            peg$currPos = s0;
            s0 = peg$FAILED;
          }
        } else {
          peg$currPos = s0;
          s0 = peg$FAILED;
        }
      } else {
        peg$currPos = s0;
        s0 = peg$FAILED;
      }
    } else {
      peg$currPos = s0;
      s0 = peg$FAILED;
    }

    return s0;
  }

  function peg$parsepluralCase() {
    var s0, s1, s2, s3, s4;

    s0 = peg$currPos;
    s1 = peg$parse_();
    if (s1 !== peg$FAILED) {
      s2 = peg$parsepluralKey();
      if (s2 !== peg$FAILED) {
        s3 = peg$parse_();
        if (s3 !== peg$FAILED) {
          s4 = peg$parsecaseTokens();
          if (s4 !== peg$FAILED) {
            peg$savedPos = s0;
            s1 = peg$c24(s2, s4);
            s0 = s1;
          } else {
            peg$currPos = s0;
            s0 = peg$FAILED;
          }
        } else {
          peg$currPos = s0;
          s0 = peg$FAILED;
        }
      } else {
        peg$currPos = s0;
        s0 = peg$FAILED;
      }
    } else {
      peg$currPos = s0;
      s0 = peg$FAILED;
    }

    return s0;
  }

  function peg$parsecaseTokens() {
    var s0, s1, s2, s3, s4, s5;

    s0 = peg$currPos;
    if (input.charCodeAt(peg$currPos) === 123) {
      s1 = peg$c4;
      peg$currPos++;
    } else {
      s1 = peg$FAILED;
      if (peg$silentFails === 0) { peg$fail(peg$c5); }
    }
    if (s1 !== peg$FAILED) {
      s2 = peg$currPos;
      s3 = peg$parse_();
      if (s3 !== peg$FAILED) {
        s4 = peg$currPos;
        peg$silentFails++;
        if (input.charCodeAt(peg$currPos) === 123) {
          s5 = peg$c4;
          peg$currPos++;
        } else {
          s5 = peg$FAILED;
          if (peg$silentFails === 0) { peg$fail(peg$c5); }
        }
        peg$silentFails--;
        if (s5 !== peg$FAILED) {
          peg$currPos = s4;
          s4 = void 0;
        } else {
          s4 = peg$FAILED;
        }
        if (s4 !== peg$FAILED) {
          s3 = [s3, s4];
          s2 = s3;
        } else {
          peg$currPos = s2;
          s2 = peg$FAILED;
        }
      } else {
        peg$currPos = s2;
        s2 = peg$FAILED;
      }
      if (s2 === peg$FAILED) {
        s2 = null;
      }
      if (s2 !== peg$FAILED) {
        s3 = [];
        s4 = peg$parsetoken();
        while (s4 !== peg$FAILED) {
          s3.push(s4);
          s4 = peg$parsetoken();
        }
        if (s3 !== peg$FAILED) {
          s4 = peg$parse_();
          if (s4 !== peg$FAILED) {
            if (input.charCodeAt(peg$currPos) === 125) {
              s5 = peg$c6;
              peg$currPos++;
            } else {
              s5 = peg$FAILED;
              if (peg$silentFails === 0) { peg$fail(peg$c7); }
            }
            if (s5 !== peg$FAILED) {
              peg$savedPos = s0;
              s1 = peg$c25(s3);
              s0 = s1;
            } else {
              peg$currPos = s0;
              s0 = peg$FAILED;
            }
          } else {
            peg$currPos = s0;
            s0 = peg$FAILED;
          }
        } else {
          peg$currPos = s0;
          s0 = peg$FAILED;
        }
      } else {
        peg$currPos = s0;
        s0 = peg$FAILED;
      }
    } else {
      peg$currPos = s0;
      s0 = peg$FAILED;
    }

    return s0;
  }

  function peg$parseoffset() {
    var s0, s1, s2, s3, s4, s5, s6, s7;

    s0 = peg$currPos;
    s1 = peg$parse_();
    if (s1 !== peg$FAILED) {
      if (input.substr(peg$currPos, 6) === peg$c26) {
        s2 = peg$c26;
        peg$currPos += 6;
      } else {
        s2 = peg$FAILED;
        if (peg$silentFails === 0) { peg$fail(peg$c27); }
      }
      if (s2 !== peg$FAILED) {
        s3 = peg$parse_();
        if (s3 !== peg$FAILED) {
          if (input.charCodeAt(peg$currPos) === 58) {
            s4 = peg$c28;
            peg$currPos++;
          } else {
            s4 = peg$FAILED;
            if (peg$silentFails === 0) { peg$fail(peg$c29); }
          }
          if (s4 !== peg$FAILED) {
            s5 = peg$parse_();
            if (s5 !== peg$FAILED) {
              s6 = peg$parsedigits();
              if (s6 !== peg$FAILED) {
                s7 = peg$parse_();
                if (s7 !== peg$FAILED) {
                  peg$savedPos = s0;
                  s1 = peg$c30(s6);
                  s0 = s1;
                } else {
                  peg$currPos = s0;
                  s0 = peg$FAILED;
                }
              } else {
                peg$currPos = s0;
                s0 = peg$FAILED;
              }
            } else {
              peg$currPos = s0;
              s0 = peg$FAILED;
            }
          } else {
            peg$currPos = s0;
            s0 = peg$FAILED;
          }
        } else {
          peg$currPos = s0;
          s0 = peg$FAILED;
        }
      } else {
        peg$currPos = s0;
        s0 = peg$FAILED;
      }
    } else {
      peg$currPos = s0;
      s0 = peg$FAILED;
    }

    return s0;
  }

  function peg$parsepluralKey() {
    var s0, s1, s2;

    s0 = peg$parseid();
    if (s0 === peg$FAILED) {
      s0 = peg$currPos;
      if (input.charCodeAt(peg$currPos) === 61) {
        s1 = peg$c31;
        peg$currPos++;
      } else {
        s1 = peg$FAILED;
        if (peg$silentFails === 0) { peg$fail(peg$c32); }
      }
      if (s1 !== peg$FAILED) {
        s2 = peg$parsedigits();
        if (s2 !== peg$FAILED) {
          peg$savedPos = s0;
          s1 = peg$c30(s2);
          s0 = s1;
        } else {
          peg$currPos = s0;
          s0 = peg$FAILED;
        }
      } else {
        peg$currPos = s0;
        s0 = peg$FAILED;
      }
    }

    return s0;
  }

  function peg$parsefunctionParams() {
    var s0, s1, s2, s3, s4, s5;

    s0 = peg$currPos;
    s1 = peg$parse_();
    if (s1 !== peg$FAILED) {
      if (input.charCodeAt(peg$currPos) === 44) {
        s2 = peg$c9;
        peg$currPos++;
      } else {
        s2 = peg$FAILED;
        if (peg$silentFails === 0) { peg$fail(peg$c10); }
      }
      if (s2 !== peg$FAILED) {
        s3 = peg$parse_();
        if (s3 !== peg$FAILED) {
          s4 = peg$parseid();
          if (s4 !== peg$FAILED) {
            s5 = peg$parse_();
            if (s5 !== peg$FAILED) {
              peg$savedPos = s0;
              s1 = peg$c33(s4);
              s0 = s1;
            } else {
              peg$currPos = s0;
              s0 = peg$FAILED;
            }
          } else {
            peg$currPos = s0;
            s0 = peg$FAILED;
          }
        } else {
          peg$currPos = s0;
          s0 = peg$FAILED;
        }
      } else {
        peg$currPos = s0;
        s0 = peg$FAILED;
      }
    } else {
      peg$currPos = s0;
      s0 = peg$FAILED;
    }

    return s0;
  }

  function peg$parsechar() {
    var s0, s1, s2, s3, s4, s5;

    if (peg$c34.test(input.charAt(peg$currPos))) {
      s0 = input.charAt(peg$currPos);
      peg$currPos++;
    } else {
      s0 = peg$FAILED;
      if (peg$silentFails === 0) { peg$fail(peg$c35); }
    }
    if (s0 === peg$FAILED) {
      s0 = peg$currPos;
      if (input.substr(peg$currPos, 2) === peg$c36) {
        s1 = peg$c36;
        peg$currPos += 2;
      } else {
        s1 = peg$FAILED;
        if (peg$silentFails === 0) { peg$fail(peg$c37); }
      }
      if (s1 !== peg$FAILED) {
        peg$savedPos = s0;
        s1 = peg$c38();
      }
      s0 = s1;
      if (s0 === peg$FAILED) {
        s0 = peg$currPos;
        if (input.substr(peg$currPos, 2) === peg$c39) {
          s1 = peg$c39;
          peg$currPos += 2;
        } else {
          s1 = peg$FAILED;
          if (peg$silentFails === 0) { peg$fail(peg$c40); }
        }
        if (s1 !== peg$FAILED) {
          peg$savedPos = s0;
          s1 = peg$c41();
        }
        s0 = s1;
        if (s0 === peg$FAILED) {
          s0 = peg$currPos;
          if (input.substr(peg$currPos, 2) === peg$c42) {
            s1 = peg$c42;
            peg$currPos += 2;
          } else {
            s1 = peg$FAILED;
            if (peg$silentFails === 0) { peg$fail(peg$c43); }
          }
          if (s1 !== peg$FAILED) {
            peg$savedPos = s0;
            s1 = peg$c44();
          }
          s0 = s1;
          if (s0 === peg$FAILED) {
            s0 = peg$currPos;
            if (input.substr(peg$currPos, 2) === peg$c45) {
              s1 = peg$c45;
              peg$currPos += 2;
            } else {
              s1 = peg$FAILED;
              if (peg$silentFails === 0) { peg$fail(peg$c46); }
            }
            if (s1 !== peg$FAILED) {
              peg$savedPos = s0;
              s1 = peg$c47();
            }
            s0 = s1;
            if (s0 === peg$FAILED) {
              s0 = peg$currPos;
              if (input.substr(peg$currPos, 2) === peg$c48) {
                s1 = peg$c48;
                peg$currPos += 2;
              } else {
                s1 = peg$FAILED;
                if (peg$silentFails === 0) { peg$fail(peg$c49); }
              }
              if (s1 !== peg$FAILED) {
                s2 = peg$parsehexDigit();
                if (s2 !== peg$FAILED) {
                  s3 = peg$parsehexDigit();
                  if (s3 !== peg$FAILED) {
                    s4 = peg$parsehexDigit();
                    if (s4 !== peg$FAILED) {
                      s5 = peg$parsehexDigit();
                      if (s5 !== peg$FAILED) {
                        peg$savedPos = s0;
                        s1 = peg$c50(s2, s3, s4, s5);
                        s0 = s1;
                      } else {
                        peg$currPos = s0;
                        s0 = peg$FAILED;
                      }
                    } else {
                      peg$currPos = s0;
                      s0 = peg$FAILED;
                    }
                  } else {
                    peg$currPos = s0;
                    s0 = peg$FAILED;
                  }
                } else {
                  peg$currPos = s0;
                  s0 = peg$FAILED;
                }
              } else {
                peg$currPos = s0;
                s0 = peg$FAILED;
              }
            }
          }
        }
      }
    }

    return s0;
  }

  function peg$parsedigits() {
    var s0, s1, s2;

    s0 = peg$currPos;
    s1 = [];
    if (peg$c51.test(input.charAt(peg$currPos))) {
      s2 = input.charAt(peg$currPos);
      peg$currPos++;
    } else {
      s2 = peg$FAILED;
      if (peg$silentFails === 0) { peg$fail(peg$c52); }
    }
    if (s2 !== peg$FAILED) {
      while (s2 !== peg$FAILED) {
        s1.push(s2);
        if (peg$c51.test(input.charAt(peg$currPos))) {
          s2 = input.charAt(peg$currPos);
          peg$currPos++;
        } else {
          s2 = peg$FAILED;
          if (peg$silentFails === 0) { peg$fail(peg$c52); }
        }
      }
    } else {
      s1 = peg$FAILED;
    }
    if (s1 !== peg$FAILED) {
      s0 = input.substring(s0, peg$currPos);
    } else {
      s0 = s1;
    }

    return s0;
  }

  function peg$parsehexDigit() {
    var s0;

    if (peg$c53.test(input.charAt(peg$currPos))) {
      s0 = input.charAt(peg$currPos);
      peg$currPos++;
    } else {
      s0 = peg$FAILED;
      if (peg$silentFails === 0) { peg$fail(peg$c54); }
    }

    return s0;
  }

  function peg$parse_() {
    var s0, s1, s2;

    s0 = peg$currPos;
    s1 = [];
    if (peg$c55.test(input.charAt(peg$currPos))) {
      s2 = input.charAt(peg$currPos);
      peg$currPos++;
    } else {
      s2 = peg$FAILED;
      if (peg$silentFails === 0) { peg$fail(peg$c56); }
    }
    while (s2 !== peg$FAILED) {
      s1.push(s2);
      if (peg$c55.test(input.charAt(peg$currPos))) {
        s2 = input.charAt(peg$currPos);
        peg$currPos++;
      } else {
        s2 = peg$FAILED;
        if (peg$silentFails === 0) { peg$fail(peg$c56); }
      }
    }
    if (s1 !== peg$FAILED) {
      s0 = input.substring(s0, peg$currPos);
    } else {
      s0 = s1;
    }

    return s0;
  }

  peg$result = peg$startRuleFunction();

  if (peg$result !== peg$FAILED && peg$currPos === input.length) {
    return peg$result;
  } else {
    if (peg$result !== peg$FAILED && peg$currPos < input.length) {
      peg$fail(peg$endExpectation());
    }

    throw peg$buildStructuredError(
      peg$maxFailExpected,
      peg$maxFailPos < input.length ? input.charAt(peg$maxFailPos) : null,
      peg$maxFailPos < input.length
        ? peg$computeLocation(peg$maxFailPos, peg$maxFailPos + 1)
        : peg$computeLocation(peg$maxFailPos, peg$maxFailPos)
    );
  }
}

module.exports = {
  SyntaxError: peg$SyntaxError,
  parse:       peg$parse
};

},{}],9:[function(require,module,exports){
// shim for using process in browser
var process = module.exports = {};

// cached from whatever global is present so that test runners that stub it
// don't break things.  But we need to wrap it in a try catch in case it is
// wrapped in strict mode code which doesn't define any globals.  It's inside a
// function because try/catches deoptimize in certain engines.

var cachedSetTimeout;
var cachedClearTimeout;

function defaultSetTimout() {
    throw new Error('setTimeout has not been defined');
}
function defaultClearTimeout () {
    throw new Error('clearTimeout has not been defined');
}
(function () {
    try {
        if (typeof setTimeout === 'function') {
            cachedSetTimeout = setTimeout;
        } else {
            cachedSetTimeout = defaultSetTimout;
        }
    } catch (e) {
        cachedSetTimeout = defaultSetTimout;
    }
    try {
        if (typeof clearTimeout === 'function') {
            cachedClearTimeout = clearTimeout;
        } else {
            cachedClearTimeout = defaultClearTimeout;
        }
    } catch (e) {
        cachedClearTimeout = defaultClearTimeout;
    }
} ())
function runTimeout(fun) {
    if (cachedSetTimeout === setTimeout) {
        //normal enviroments in sane situations
        return setTimeout(fun, 0);
    }
    // if setTimeout wasn't available but was latter defined
    if ((cachedSetTimeout === defaultSetTimout || !cachedSetTimeout) && setTimeout) {
        cachedSetTimeout = setTimeout;
        return setTimeout(fun, 0);
    }
    try {
        // when when somebody has screwed with setTimeout but no I.E. maddness
        return cachedSetTimeout(fun, 0);
    } catch(e){
        try {
            // When we are in I.E. but the script has been evaled so I.E. doesn't trust the global object when called normally
            return cachedSetTimeout.call(null, fun, 0);
        } catch(e){
            // same as above but when it's a version of I.E. that must have the global object for 'this', hopfully our context correct otherwise it will throw a global error
            return cachedSetTimeout.call(this, fun, 0);
        }
    }


}
function runClearTimeout(marker) {
    if (cachedClearTimeout === clearTimeout) {
        //normal enviroments in sane situations
        return clearTimeout(marker);
    }
    // if clearTimeout wasn't available but was latter defined
    if ((cachedClearTimeout === defaultClearTimeout || !cachedClearTimeout) && clearTimeout) {
        cachedClearTimeout = clearTimeout;
        return clearTimeout(marker);
    }
    try {
        // when when somebody has screwed with setTimeout but no I.E. maddness
        return cachedClearTimeout(marker);
    } catch (e){
        try {
            // When we are in I.E. but the script has been evaled so I.E. doesn't  trust the global object when called normally
            return cachedClearTimeout.call(null, marker);
        } catch (e){
            // same as above but when it's a version of I.E. that must have the global object for 'this', hopfully our context correct otherwise it will throw a global error.
            // Some versions of I.E. have different rules for clearTimeout vs setTimeout
            return cachedClearTimeout.call(this, marker);
        }
    }



}
var queue = [];
var draining = false;
var currentQueue;
var queueIndex = -1;

function cleanUpNextTick() {
    if (!draining || !currentQueue) {
        return;
    }
    draining = false;
    if (currentQueue.length) {
        queue = currentQueue.concat(queue);
    } else {
        queueIndex = -1;
    }
    if (queue.length) {
        drainQueue();
    }
}

function drainQueue() {
    if (draining) {
        return;
    }
    var timeout = runTimeout(cleanUpNextTick);
    draining = true;

    var len = queue.length;
    while(len) {
        currentQueue = queue;
        queue = [];
        while (++queueIndex < len) {
            if (currentQueue) {
                currentQueue[queueIndex].run();
            }
        }
        queueIndex = -1;
        len = queue.length;
    }
    currentQueue = null;
    draining = false;
    runClearTimeout(timeout);
}

process.nextTick = function (fun) {
    var args = new Array(arguments.length - 1);
    if (arguments.length > 1) {
        for (var i = 1; i < arguments.length; i++) {
            args[i - 1] = arguments[i];
        }
    }
    queue.push(new Item(fun, args));
    if (queue.length === 1 && !draining) {
        runTimeout(drainQueue);
    }
};

// v8 likes predictible objects
function Item(fun, array) {
    this.fun = fun;
    this.array = array;
}
Item.prototype.run = function () {
    this.fun.apply(null, this.array);
};
process.title = 'browser';
process.browser = true;
process.env = {};
process.argv = [];
process.version = ''; // empty string to avoid regexp issues
process.versions = {};

function noop() {}

process.on = noop;
process.addListener = noop;
process.once = noop;
process.off = noop;
process.removeListener = noop;
process.removeAllListeners = noop;
process.emit = noop;

process.binding = function (name) {
    throw new Error('process.binding is not supported');
};

process.cwd = function () { return '/' };
process.chdir = function (dir) {
    throw new Error('process.chdir is not supported');
};
process.umask = function() { return 0; };

},{}],10:[function(require,module,exports){
module.exports = require('./reserved-words');

},{"./reserved-words":11}],11:[function(require,module,exports){
var assert = require('assert');

/**
 * Structure for storing keywords.
 *
 * @typedef {Object.<String,Boolean>} KeywordsHash
 */

/**
 * ECMAScript dialects.
 *
 * @const
 * @type {Object.<String,Number|String>} - keys as readable names and values as versions
 */
var DIALECTS = {
    es3: 3,
    es5: 5,
    es2015: 6,
    es7: 7,

    // aliases
    es6: 6,
    'default': 5,
    next: 6
};

/**
 * ECMAScript reserved words.
 *
 * @type {Object.<String,KeywordsHash>}
 */
var KEYWORDS = exports.KEYWORDS = {};

/**
 * Check word for being an reserved word.
 *
 * @param {String} word - word to check
 * @param {String|Number} [dialect] - dialect or version
 * @param {Boolean} [strict] - strict mode
 * @returns {?Boolean}
 */
exports.check = function check(word, dialect, strict) {
    dialect = dialect || DIALECTS.default;
    var version = DIALECTS[dialect] || dialect;

    if (strict && version >= 5) {
        version += '-strict';
    }

    assert(KEYWORDS[version], 'Unknown dialect');

    return KEYWORDS[version][word];
};

/**
 * Reserved Words for ES3
 *
 * ECMA-262 3rd: 7.5.1
 * http://www.ecma-international.org/publications/files/ECMA-ST-ARCH/ECMA-262,%203rd%20edition,%20December%201999.pdf
 *
 * @type {KeywordsHash}
 */
KEYWORDS['3'] = _hash(
    // Keyword, ECMA-262 3rd: 7.5.2
    'break    else       new     var',
    'case     finally    return  void',
    'catch    for        switch  while',
    'continue function   this    with',
    'default  if         throw',
    'delete   in         try',
    'do       instanceof typeof',
    // FutureReservedWord, ECMA-262 3rd 7.5.3
    'abstract enum       int        short',
    'boolean  export     interface  static',
    'byte     extends    long       super',
    'char     final      native     synchronized',
    'class    float      package    throws',
    'const    goto       private    transient',
    'debugger implements protected  volatile',
    'double   import     public',
    // NullLiteral & BooleanLiteral
    'null true false'
);

/**
 * Reserved Words for ES5.
 *
 * http://es5.github.io/#x7.6.1
 *
 * @type {KeywordsHash}
 */
KEYWORDS['5'] = _hash(
    // Keyword
    'break    do       instanceof typeof',
    'case     else     new        var',
    'catch    finally  return     void',
    'continue for      switch     while',
    'debugger function this       with',
    'default  if       throw',
    'delete   in       try',
    // FutureReservedWord
    'class enum extends super',
    'const export import',
    // NullLiteral & BooleanLiteral
    'null true false'
);

/**
 * Reserved Words for ES5 in strict mode.
 *
 * @type {KeywordsHash}
 */
KEYWORDS['5-strict'] = _hash(
    KEYWORDS['5'],
    // FutureReservedWord, strict mode. http://es5.github.io/#x7.6.1.2
    'implements let     private   public yield',
    'interface  package protected static'
);

/**
 * Reserved Words for ES6.
 *
 * 11.6.2
 * http://www.ecma-international.org/ecma-262/6.0/index.html#sec-reserved-words
 *
 * @type {KeywordsHash}
 */
KEYWORDS['6'] = _hash(
    // Keywords, ES6 11.6.2.1, http://www.ecma-international.org/ecma-262/6.0/index.html#sec-keywords
    'break    do       in         typeof',
    'case     else     instanceof var',
    'catch    export   new        void',
    'class    extends  return     while',
    'const    finally  super      with',
    'continue for      switch     yield',
    'debugger function this',
    'default  if       throw',
    'delete   import   try',
    // Future Reserved Words, ES6 11.6.2.2
    // http://www.ecma-international.org/ecma-262/6.0/index.html#sec-future-reserved-words
    'enum await',
    // NullLiteral & BooleanLiteral
    'null true false'
);

/**
 * Reserved Words for ES6 in strict mode.
 *
 * @type {KeywordsHash}
 */
KEYWORDS['6-strict'] = _hash(
    KEYWORDS['6'],
    // Keywords, ES6 11.6.2.1
    'let static',
    // Future Reserved Words, ES6 11.6.2.2
    'implements package protected',
    'interface private public'
);

/**
 * Generates hash from strings
 *
 * @private
 * @param {...String|KeywordsHash} keywords - Space-delimited string or previous result of _hash
 * @return {KeywordsHash} - Object with keywords in keys and true in values
 */
function _hash() {
    var set = Array.prototype.map.call(arguments, function(v) {
        return typeof v === 'string' ? v : Object.keys(v).join(' ');
    }).join(' ');

    return set.split(/\s+/)
        .reduce(function(res, keyword) {
            res[keyword] = true;
            return res;
        }, {});
}

},{"assert":4}],12:[function(require,module,exports){
module.exports = function isBuffer(arg) {
  return arg && typeof arg === 'object'
    && typeof arg.copy === 'function'
    && typeof arg.fill === 'function'
    && typeof arg.readUInt8 === 'function';
}
},{}],13:[function(require,module,exports){
(function (process,global){
// Copyright Joyent, Inc. and other Node contributors.
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions:
//
// The above copyright notice and this permission notice shall be included
// in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.

var formatRegExp = /%[sdj%]/g;
exports.format = function(f) {
  if (!isString(f)) {
    var objects = [];
    for (var i = 0; i < arguments.length; i++) {
      objects.push(inspect(arguments[i]));
    }
    return objects.join(' ');
  }

  var i = 1;
  var args = arguments;
  var len = args.length;
  var str = String(f).replace(formatRegExp, function(x) {
    if (x === '%%') return '%';
    if (i >= len) return x;
    switch (x) {
      case '%s': return String(args[i++]);
      case '%d': return Number(args[i++]);
      case '%j':
        try {
          return JSON.stringify(args[i++]);
        } catch (_) {
          return '[Circular]';
        }
      default:
        return x;
    }
  });
  for (var x = args[i]; i < len; x = args[++i]) {
    if (isNull(x) || !isObject(x)) {
      str += ' ' + x;
    } else {
      str += ' ' + inspect(x);
    }
  }
  return str;
};


// Mark that a method should not be used.
// Returns a modified function which warns once by default.
// If --no-deprecation is set, then it is a no-op.
exports.deprecate = function(fn, msg) {
  // Allow for deprecating things in the process of starting up.
  if (isUndefined(global.process)) {
    return function() {
      return exports.deprecate(fn, msg).apply(this, arguments);
    };
  }

  if (process.noDeprecation === true) {
    return fn;
  }

  var warned = false;
  function deprecated() {
    if (!warned) {
      if (process.throwDeprecation) {
        throw new Error(msg);
      } else if (process.traceDeprecation) {
        console.trace(msg);
      } else {
        console.error(msg);
      }
      warned = true;
    }
    return fn.apply(this, arguments);
  }

  return deprecated;
};


var debugs = {};
var debugEnviron;
exports.debuglog = function(set) {
  if (isUndefined(debugEnviron))
    debugEnviron = process.env.NODE_DEBUG || '';
  set = set.toUpperCase();
  if (!debugs[set]) {
    if (new RegExp('\\b' + set + '\\b', 'i').test(debugEnviron)) {
      var pid = process.pid;
      debugs[set] = function() {
        var msg = exports.format.apply(exports, arguments);
        console.error('%s %d: %s', set, pid, msg);
      };
    } else {
      debugs[set] = function() {};
    }
  }
  return debugs[set];
};


/**
 * Echos the value of a value. Trys to print the value out
 * in the best way possible given the different types.
 *
 * @param {Object} obj The object to print out.
 * @param {Object} opts Optional options object that alters the output.
 */
/* legacy: obj, showHidden, depth, colors*/
function inspect(obj, opts) {
  // default options
  var ctx = {
    seen: [],
    stylize: stylizeNoColor
  };
  // legacy...
  if (arguments.length >= 3) ctx.depth = arguments[2];
  if (arguments.length >= 4) ctx.colors = arguments[3];
  if (isBoolean(opts)) {
    // legacy...
    ctx.showHidden = opts;
  } else if (opts) {
    // got an "options" object
    exports._extend(ctx, opts);
  }
  // set default options
  if (isUndefined(ctx.showHidden)) ctx.showHidden = false;
  if (isUndefined(ctx.depth)) ctx.depth = 2;
  if (isUndefined(ctx.colors)) ctx.colors = false;
  if (isUndefined(ctx.customInspect)) ctx.customInspect = true;
  if (ctx.colors) ctx.stylize = stylizeWithColor;
  return formatValue(ctx, obj, ctx.depth);
}
exports.inspect = inspect;


// http://en.wikipedia.org/wiki/ANSI_escape_code#graphics
inspect.colors = {
  'bold' : [1, 22],
  'italic' : [3, 23],
  'underline' : [4, 24],
  'inverse' : [7, 27],
  'white' : [37, 39],
  'grey' : [90, 39],
  'black' : [30, 39],
  'blue' : [34, 39],
  'cyan' : [36, 39],
  'green' : [32, 39],
  'magenta' : [35, 39],
  'red' : [31, 39],
  'yellow' : [33, 39]
};

// Don't use 'blue' not visible on cmd.exe
inspect.styles = {
  'special': 'cyan',
  'number': 'yellow',
  'boolean': 'yellow',
  'undefined': 'grey',
  'null': 'bold',
  'string': 'green',
  'date': 'magenta',
  // "name": intentionally not styling
  'regexp': 'red'
};


function stylizeWithColor(str, styleType) {
  var style = inspect.styles[styleType];

  if (style) {
    return '\u001b[' + inspect.colors[style][0] + 'm' + str +
           '\u001b[' + inspect.colors[style][1] + 'm';
  } else {
    return str;
  }
}


function stylizeNoColor(str, styleType) {
  return str;
}


function arrayToHash(array) {
  var hash = {};

  array.forEach(function(val, idx) {
    hash[val] = true;
  });

  return hash;
}


function formatValue(ctx, value, recurseTimes) {
  // Provide a hook for user-specified inspect functions.
  // Check that value is an object with an inspect function on it
  if (ctx.customInspect &&
      value &&
      isFunction(value.inspect) &&
      // Filter out the util module, it's inspect function is special
      value.inspect !== exports.inspect &&
      // Also filter out any prototype objects using the circular check.
      !(value.constructor && value.constructor.prototype === value)) {
    var ret = value.inspect(recurseTimes, ctx);
    if (!isString(ret)) {
      ret = formatValue(ctx, ret, recurseTimes);
    }
    return ret;
  }

  // Primitive types cannot have properties
  var primitive = formatPrimitive(ctx, value);
  if (primitive) {
    return primitive;
  }

  // Look up the keys of the object.
  var keys = Object.keys(value);
  var visibleKeys = arrayToHash(keys);

  if (ctx.showHidden) {
    keys = Object.getOwnPropertyNames(value);
  }

  // IE doesn't make error fields non-enumerable
  // http://msdn.microsoft.com/en-us/library/ie/dww52sbt(v=vs.94).aspx
  if (isError(value)
      && (keys.indexOf('message') >= 0 || keys.indexOf('description') >= 0)) {
    return formatError(value);
  }

  // Some type of object without properties can be shortcutted.
  if (keys.length === 0) {
    if (isFunction(value)) {
      var name = value.name ? ': ' + value.name : '';
      return ctx.stylize('[Function' + name + ']', 'special');
    }
    if (isRegExp(value)) {
      return ctx.stylize(RegExp.prototype.toString.call(value), 'regexp');
    }
    if (isDate(value)) {
      return ctx.stylize(Date.prototype.toString.call(value), 'date');
    }
    if (isError(value)) {
      return formatError(value);
    }
  }

  var base = '', array = false, braces = ['{', '}'];

  // Make Array say that they are Array
  if (isArray(value)) {
    array = true;
    braces = ['[', ']'];
  }

  // Make functions say that they are functions
  if (isFunction(value)) {
    var n = value.name ? ': ' + value.name : '';
    base = ' [Function' + n + ']';
  }

  // Make RegExps say that they are RegExps
  if (isRegExp(value)) {
    base = ' ' + RegExp.prototype.toString.call(value);
  }

  // Make dates with properties first say the date
  if (isDate(value)) {
    base = ' ' + Date.prototype.toUTCString.call(value);
  }

  // Make error with message first say the error
  if (isError(value)) {
    base = ' ' + formatError(value);
  }

  if (keys.length === 0 && (!array || value.length == 0)) {
    return braces[0] + base + braces[1];
  }

  if (recurseTimes < 0) {
    if (isRegExp(value)) {
      return ctx.stylize(RegExp.prototype.toString.call(value), 'regexp');
    } else {
      return ctx.stylize('[Object]', 'special');
    }
  }

  ctx.seen.push(value);

  var output;
  if (array) {
    output = formatArray(ctx, value, recurseTimes, visibleKeys, keys);
  } else {
    output = keys.map(function(key) {
      return formatProperty(ctx, value, recurseTimes, visibleKeys, key, array);
    });
  }

  ctx.seen.pop();

  return reduceToSingleString(output, base, braces);
}


function formatPrimitive(ctx, value) {
  if (isUndefined(value))
    return ctx.stylize('undefined', 'undefined');
  if (isString(value)) {
    var simple = '\'' + JSON.stringify(value).replace(/^"|"$/g, '')
                                             .replace(/'/g, "\\'")
                                             .replace(/\\"/g, '"') + '\'';
    return ctx.stylize(simple, 'string');
  }
  if (isNumber(value))
    return ctx.stylize('' + value, 'number');
  if (isBoolean(value))
    return ctx.stylize('' + value, 'boolean');
  // For some reason typeof null is "object", so special case here.
  if (isNull(value))
    return ctx.stylize('null', 'null');
}


function formatError(value) {
  return '[' + Error.prototype.toString.call(value) + ']';
}


function formatArray(ctx, value, recurseTimes, visibleKeys, keys) {
  var output = [];
  for (var i = 0, l = value.length; i < l; ++i) {
    if (hasOwnProperty(value, String(i))) {
      output.push(formatProperty(ctx, value, recurseTimes, visibleKeys,
          String(i), true));
    } else {
      output.push('');
    }
  }
  keys.forEach(function(key) {
    if (!key.match(/^\d+$/)) {
      output.push(formatProperty(ctx, value, recurseTimes, visibleKeys,
          key, true));
    }
  });
  return output;
}


function formatProperty(ctx, value, recurseTimes, visibleKeys, key, array) {
  var name, str, desc;
  desc = Object.getOwnPropertyDescriptor(value, key) || { value: value[key] };
  if (desc.get) {
    if (desc.set) {
      str = ctx.stylize('[Getter/Setter]', 'special');
    } else {
      str = ctx.stylize('[Getter]', 'special');
    }
  } else {
    if (desc.set) {
      str = ctx.stylize('[Setter]', 'special');
    }
  }
  if (!hasOwnProperty(visibleKeys, key)) {
    name = '[' + key + ']';
  }
  if (!str) {
    if (ctx.seen.indexOf(desc.value) < 0) {
      if (isNull(recurseTimes)) {
        str = formatValue(ctx, desc.value, null);
      } else {
        str = formatValue(ctx, desc.value, recurseTimes - 1);
      }
      if (str.indexOf('\n') > -1) {
        if (array) {
          str = str.split('\n').map(function(line) {
            return '  ' + line;
          }).join('\n').substr(2);
        } else {
          str = '\n' + str.split('\n').map(function(line) {
            return '   ' + line;
          }).join('\n');
        }
      }
    } else {
      str = ctx.stylize('[Circular]', 'special');
    }
  }
  if (isUndefined(name)) {
    if (array && key.match(/^\d+$/)) {
      return str;
    }
    name = JSON.stringify('' + key);
    if (name.match(/^"([a-zA-Z_][a-zA-Z_0-9]*)"$/)) {
      name = name.substr(1, name.length - 2);
      name = ctx.stylize(name, 'name');
    } else {
      name = name.replace(/'/g, "\\'")
                 .replace(/\\"/g, '"')
                 .replace(/(^"|"$)/g, "'");
      name = ctx.stylize(name, 'string');
    }
  }

  return name + ': ' + str;
}


function reduceToSingleString(output, base, braces) {
  var numLinesEst = 0;
  var length = output.reduce(function(prev, cur) {
    numLinesEst++;
    if (cur.indexOf('\n') >= 0) numLinesEst++;
    return prev + cur.replace(/\u001b\[\d\d?m/g, '').length + 1;
  }, 0);

  if (length > 60) {
    return braces[0] +
           (base === '' ? '' : base + '\n ') +
           ' ' +
           output.join(',\n  ') +
           ' ' +
           braces[1];
  }

  return braces[0] + base + ' ' + output.join(', ') + ' ' + braces[1];
}


// NOTE: These type checking functions intentionally don't use `instanceof`
// because it is fragile and can be easily faked with `Object.create()`.
function isArray(ar) {
  return Array.isArray(ar);
}
exports.isArray = isArray;

function isBoolean(arg) {
  return typeof arg === 'boolean';
}
exports.isBoolean = isBoolean;

function isNull(arg) {
  return arg === null;
}
exports.isNull = isNull;

function isNullOrUndefined(arg) {
  return arg == null;
}
exports.isNullOrUndefined = isNullOrUndefined;

function isNumber(arg) {
  return typeof arg === 'number';
}
exports.isNumber = isNumber;

function isString(arg) {
  return typeof arg === 'string';
}
exports.isString = isString;

function isSymbol(arg) {
  return typeof arg === 'symbol';
}
exports.isSymbol = isSymbol;

function isUndefined(arg) {
  return arg === void 0;
}
exports.isUndefined = isUndefined;

function isRegExp(re) {
  return isObject(re) && objectToString(re) === '[object RegExp]';
}
exports.isRegExp = isRegExp;

function isObject(arg) {
  return typeof arg === 'object' && arg !== null;
}
exports.isObject = isObject;

function isDate(d) {
  return isObject(d) && objectToString(d) === '[object Date]';
}
exports.isDate = isDate;

function isError(e) {
  return isObject(e) &&
      (objectToString(e) === '[object Error]' || e instanceof Error);
}
exports.isError = isError;

function isFunction(arg) {
  return typeof arg === 'function';
}
exports.isFunction = isFunction;

function isPrimitive(arg) {
  return arg === null ||
         typeof arg === 'boolean' ||
         typeof arg === 'number' ||
         typeof arg === 'string' ||
         typeof arg === 'symbol' ||  // ES6 symbol
         typeof arg === 'undefined';
}
exports.isPrimitive = isPrimitive;

exports.isBuffer = require('./support/isBuffer');

function objectToString(o) {
  return Object.prototype.toString.call(o);
}


function pad(n) {
  return n < 10 ? '0' + n.toString(10) : n.toString(10);
}


var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep',
              'Oct', 'Nov', 'Dec'];

// 26 Feb 16:19:34
function timestamp() {
  var d = new Date();
  var time = [pad(d.getHours()),
              pad(d.getMinutes()),
              pad(d.getSeconds())].join(':');
  return [d.getDate(), months[d.getMonth()], time].join(' ');
}


// log is just a thin wrapper to console.log that prepends a timestamp
exports.log = function() {
  console.log('%s - %s', timestamp(), exports.format.apply(exports, arguments));
};


/**
 * Inherit the prototype methods from one constructor into another.
 *
 * The Function.prototype.inherits from lang.js rewritten as a standalone
 * function (not on Function.prototype). NOTE: If this file is to be loaded
 * during bootstrapping this function needs to be rewritten using some native
 * functions as prototype setup using normal JavaScript does not work as
 * expected during bootstrapping (see mirror.js in r114903).
 *
 * @param {function} ctor Constructor function which needs to inherit the
 *     prototype.
 * @param {function} superCtor Constructor function to inherit prototype from.
 */
exports.inherits = require('inherits');

exports._extend = function(origin, add) {
  // Don't do anything if add isn't an object
  if (!add || !isObject(add)) return origin;

  var keys = Object.keys(add);
  var i = keys.length;
  while (i--) {
    origin[keys[i]] = add[keys[i]];
  }
  return origin;
};

function hasOwnProperty(obj, prop) {
  return Object.prototype.hasOwnProperty.call(obj, prop);
}

}).call(this,require('_process'),typeof global !== "undefined" ? global : typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {})
},{"./support/isBuffer":12,"_process":9,"inherits":5}]},{},[2])(2)
});