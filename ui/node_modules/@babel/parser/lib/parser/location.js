"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _location = require("../util/location");

var _comments = _interopRequireDefault(require("./comments"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

class LocationParser extends _comments.default {
  getLocationForPosition(pos) {
    let loc;
    if (pos === this.state.start) loc = this.state.startLoc;else if (pos === this.state.lastTokStart) loc = this.state.lastTokStartLoc;else if (pos === this.state.end) loc = this.state.endLoc;else if (pos === this.state.lastTokEnd) loc = this.state.lastTokEndLoc;else loc = (0, _location.getLineInfo)(this.input, pos);
    return loc;
  }

  raise(pos, message, {
    missingPluginNames,
    code
  } = {}) {
    const loc = this.getLocationForPosition(pos);
    message += ` (${loc.line}:${loc.column})`;
    const err = new SyntaxError(message);
    err.pos = pos;
    err.loc = loc;

    if (missingPluginNames) {
      err.missingPlugin = missingPluginNames;
    }

    if (code !== undefined) {
      err.code = code;
    }

    if (this.options.errorRecovery) {
      if (!this.isLookahead) this.state.errors.push(err);
      return err;
    } else {
      throw err;
    }
  }

}

exports.default = LocationParser;