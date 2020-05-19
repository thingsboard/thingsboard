import { RegExpParser } from "regexp-to-ast";
var regExpAstCache = {};
var regExpParser = new RegExpParser();
export function getRegExpAst(regExp) {
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
export function clearRegExpParserCache() {
    regExpAstCache = {};
}
//# sourceMappingURL=reg_exp_parser.js.map