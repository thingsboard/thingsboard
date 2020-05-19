"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

class StylelintError extends Error {
  constructor(messages) {
    super(messages);
    this.name = 'StylelintError';
    this.stack = false;
  }

  static format({
    formatter
  }, messages) {
    return new StylelintError(formatter(messages));
  }

}

exports.default = StylelintError;