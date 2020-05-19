"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var regexp_to_ast_1 = require("regexp-to-ast");
var regExpAstCache = {};
var regExpParser = new regexp_to_ast_1.RegExpParser();
function getRegExpAst(regExp) {
    var regExpStr = regExp.toString();
    if (regExpAstCache.hasOwnProperty(regExpStr)) {
        return regExpAstCache[regExpStr];
    }
    else {
        var regExpAst = regExpParser.pattern(regExpStr);
        regExpAstCache[regExpStr] = regExpAst;
        return regExpAst;
    }
}
exports.getRegExpAst = getRegExpAst;
function clearRegExpParserCache() {
    regExpAstCache = {};
}
exports.clearRegExpParserCache = clearRegExpParserCache;
//# sourceMappingURL=reg_exp_parser.js.map