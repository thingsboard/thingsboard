"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = _default;

var _isWhitespace = _interopRequireDefault(require("./isWhitespace"));

var _isSingleLineString = _interopRequireDefault(require("./isSingleLineString"));

var _configurationError = _interopRequireDefault(require("./configurationError"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

/**
 * Create a whitespaceChecker, which exposes the following functions:
 * - `before()`
 * - `beforeAllowingIndentation()`
 * - `after()`
 * - `afterOneOnly()`
 *
 * @param {"space"|"newline"} targetWhitespace - This is a keyword instead
 *   of the actual character (e.g. " ") in order to accommodate
 *   different styles of newline ("\n" vs "\r\n")
 * @param {
 *     "always"|"never"
 *     |"always-single-line"|"always-multi-line"
 *     | "never-single-line"|"never-multi-line"
 *   } expectation
 * @param {object} messages - An object of message functions;
 *   calling `before*()` or `after*()` and the `expectation` that is passed
 *   determines which message functions are required
 * @param {function} [messages.expectedBefore]
 * @param {function} [messages.rejectedBefore]
 * @param {function} [messages.expectedAfter]
 * @param {function} [messages.rejectedAfter]
 * @param {function} [messages.expectedBeforeSingleLine]
 * @param {function} [messages.rejectedBeforeSingleLine]
 * @param {function} [messages.expectedBeforeMultiLine]
 * @param {function} [messages.rejectedBeforeMultiLine]
 * @return {object} The checker, with its exposed checking functions
 */
function _default(targetWhitespace, expectation, messages) {
  // Keep track of active arguments in order to avoid passing
  // too much stuff around, making signatures long and confusing.
  // This variable gets reset anytime a checking function is called.
  var activeArgs;
  /**
   * Check for whitespace *before* a character.
   *
   * @param {object} args - Named arguments object
   * @param {string} args.source - The source string
   * @param {number} args.index - The index of the character to check before
   * @param {function} args.err - If a violation is found, this callback
   *   will be invoked with the relevant warning message.
   *   Typically this callback will report() the violation.
   * @param {function} args.errTarget - If a violation is found, this string
   *   will be sent to the relevant warning message.
   * @param {string} [args.lineCheckStr] - Single- and multi-line checkers
   *   will use this string to determine whether they should proceed,
   *   i.e. if this string is one line only, single-line checkers will check,
   *   multi-line checkers will ignore.
   *   If none is passed, they will use `source`.
   * @param {boolean} [args.onlyOneChar=false] - Only check *one* character before.
   *   By default, "always-*" checks will look for the `targetWhitespace` one
   *   before and then ensure there is no whitespace two before. This option
   *   bypasses that second check.
   * @param {boolean} [args.allowIndentation=false] - Allow arbitrary indentation
   *   between the `targetWhitespace` (almost definitely a newline) and the `index`.
   *   With this option, the checker will see if a newline *begins* the whitespace before
   *   the `index`.
   */

  function before(_ref) {
    var source = _ref.source,
        index = _ref.index,
        err = _ref.err,
        errTarget = _ref.errTarget,
        lineCheckStr = _ref.lineCheckStr,
        _ref$onlyOneChar = _ref.onlyOneChar,
        onlyOneChar = _ref$onlyOneChar === void 0 ? false : _ref$onlyOneChar,
        _ref$allowIndentation = _ref.allowIndentation,
        allowIndentation = _ref$allowIndentation === void 0 ? false : _ref$allowIndentation;
    activeArgs = {
      source: source,
      index: index,
      err: err,
      errTarget: errTarget,
      onlyOneChar: onlyOneChar,
      allowIndentation: allowIndentation
    };

    switch (expectation) {
      case "always":
        expectBefore();
        break;

      case "never":
        rejectBefore();
        break;

      case "always-single-line":
        if (!(0, _isSingleLineString["default"])(lineCheckStr || source)) {
          return;
        }

        expectBefore(messages.expectedBeforeSingleLine);
        break;

      case "never-single-line":
        if (!(0, _isSingleLineString["default"])(lineCheckStr || source)) {
          return;
        }

        rejectBefore(messages.rejectedBeforeSingleLine);
        break;

      case "always-multi-line":
        if ((0, _isSingleLineString["default"])(lineCheckStr || source)) {
          return;
        }

        expectBefore(messages.expectedBeforeMultiLine);
        break;

      case "never-multi-line":
        if ((0, _isSingleLineString["default"])(lineCheckStr || source)) {
          return;
        }

        rejectBefore(messages.rejectedBeforeMultiLine);
        break;

      default:
        throw (0, _configurationError["default"])("Unknown expectation \"".concat(expectation, "\""));
    }
  }
  /**
   * Check for whitespace *after* a character.
   *
   * Parameters are pretty much the same as for `before()`, above, just substitute
   * the word "after" for "before".
   */


  function after(_ref2) {
    var source = _ref2.source,
        index = _ref2.index,
        err = _ref2.err,
        errTarget = _ref2.errTarget,
        lineCheckStr = _ref2.lineCheckStr,
        _ref2$onlyOneChar = _ref2.onlyOneChar,
        onlyOneChar = _ref2$onlyOneChar === void 0 ? false : _ref2$onlyOneChar;
    activeArgs = {
      source: source,
      index: index,
      err: err,
      errTarget: errTarget,
      onlyOneChar: onlyOneChar
    };

    switch (expectation) {
      case "always":
        expectAfter();
        break;

      case "never":
        rejectAfter();
        break;

      case "always-single-line":
        if (!(0, _isSingleLineString["default"])(lineCheckStr || source)) {
          return;
        }

        expectAfter(messages.expectedAfterSingleLine);
        break;

      case "never-single-line":
        if (!(0, _isSingleLineString["default"])(lineCheckStr || source)) {
          return;
        }

        rejectAfter(messages.rejectedAfterSingleLine);
        break;

      case "always-multi-line":
        if ((0, _isSingleLineString["default"])(lineCheckStr || source)) {
          return;
        }

        expectAfter(messages.expectedAfterMultiLine);
        break;

      case "never-multi-line":
        if ((0, _isSingleLineString["default"])(lineCheckStr || source)) {
          return;
        }

        rejectAfter(messages.rejectedAfterMultiLine);
        break;

      case "at-least-one-space":
        expectAfter(messages.expectedAfterAtLeast);
        break;

      default:
        throw (0, _configurationError["default"])("Unknown expectation \"".concat(expectation, "\""));
    }
  }

  function beforeAllowingIndentation(obj) {
    before(Object.assign({}, obj, {
      allowIndentation: true
    }));
  }

  function expectBefore() {
    var messageFunc = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : messages.expectedBefore;

    if (activeArgs.allowIndentation) {
      expectBeforeAllowingIndentation(messageFunc);
      return;
    }

    var _activeArgs = activeArgs,
        source = _activeArgs.source,
        index = _activeArgs.index;
    var oneCharBefore = source[index - 1];
    var twoCharsBefore = source[index - 2];

    if (!isValue(oneCharBefore)) {
      return;
    }

    if (targetWhitespace === "newline") {
      // If index is preceeded by a Windows CR-LF ...
      if (oneCharBefore === "\n" && twoCharsBefore === "\r") {
        if (activeArgs.onlyOneChar || !(0, _isWhitespace["default"])(source[index - 3])) {
          return;
        }
      } // If index is followed by a Unix LF ...


      if (oneCharBefore === "\n" && twoCharsBefore !== "\r") {
        if (activeArgs.onlyOneChar || !(0, _isWhitespace["default"])(twoCharsBefore)) {
          return;
        }
      }
    }

    if (targetWhitespace === "space" && oneCharBefore === " ") {
      if (activeArgs.onlyOneChar || !(0, _isWhitespace["default"])(twoCharsBefore)) {
        return;
      }
    }

    activeArgs.err(messageFunc(activeArgs.errTarget ? activeArgs.errTarget : source[index]));
  }

  function expectBeforeAllowingIndentation() {
    var messageFunc = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : messages.expectedBefore;
    var _activeArgs2 = activeArgs,
        source = _activeArgs2.source,
        index = _activeArgs2.index,
        err = _activeArgs2.err;

    var expectedChar = function () {
      if (targetWhitespace === "newline") {
        return "\n";
      }

      if (targetWhitespace === "space") {
        return " ";
      }
    }();

    var i = index - 1;

    while (source[i] !== expectedChar) {
      if (source[i] === "\t" || source[i] === " ") {
        i--;
        continue;
      }

      err(messageFunc(activeArgs.errTarget ? activeArgs.errTarget : source[index]));
      return;
    }
  }

  function rejectBefore() {
    var messageFunc = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : messages.rejectedBefore;
    var _activeArgs3 = activeArgs,
        source = _activeArgs3.source,
        index = _activeArgs3.index;
    var oneCharBefore = source[index - 1];

    if (isValue(oneCharBefore) && (0, _isWhitespace["default"])(oneCharBefore)) {
      activeArgs.err(messageFunc(activeArgs.errTarget ? activeArgs.errTarget : source[index]));
    }
  }

  function afterOneOnly(obj) {
    after(Object.assign({}, obj, {
      onlyOneChar: true
    }));
  }

  function expectAfter() {
    var messageFunc = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : messages.expectedAfter;
    var _activeArgs4 = activeArgs,
        source = _activeArgs4.source,
        index = _activeArgs4.index;
    var oneCharAfter = index + 1 < source.length ? source[index + 1] : "";
    var twoCharsAfter = index + 2 < source.length ? source[index + 2] : "";

    if (!isValue(oneCharAfter)) {
      return;
    }

    if (targetWhitespace === "newline") {
      // If index is followed by a Windows CR-LF ...
      if (oneCharAfter === "\r" && twoCharsAfter === "\n") {
        var threeCharsAfter = index + 3 < source.length ? source[index + 3] : "";

        if (activeArgs.onlyOneChar || !(0, _isWhitespace["default"])(threeCharsAfter)) {
          return;
        }
      } // If index is followed by a Unix LF ...


      if (oneCharAfter === "\n") {
        if (activeArgs.onlyOneChar || !(0, _isWhitespace["default"])(twoCharsAfter)) {
          return;
        }
      }
    }

    if (targetWhitespace === "space" && oneCharAfter === " ") {
      if (expectation === "at-least-one-space" || activeArgs.onlyOneChar || !(0, _isWhitespace["default"])(twoCharsAfter)) {
        return;
      }
    }

    activeArgs.err(messageFunc(activeArgs.errTarget ? activeArgs.errTarget : source[index]));
  }

  function rejectAfter() {
    var messageFunc = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : messages.rejectedAfter;
    var _activeArgs5 = activeArgs,
        source = _activeArgs5.source,
        index = _activeArgs5.index;
    var oneCharAfter = index + 1 < source.length ? source[index + 1] : "";

    if (isValue(oneCharAfter) && (0, _isWhitespace["default"])(oneCharAfter)) {
      activeArgs.err(messageFunc(activeArgs.errTarget ? activeArgs.errTarget : source[index]));
    }
  }

  return {
    before: before,
    beforeAllowingIndentation: beforeAllowingIndentation,
    after: after,
    afterOneOnly: afterOneOnly
  };
}

function isValue(x) {
  return x !== undefined && x !== null;
}