import { has, isString, isUndefined } from "../utils/utils";
import { Lexer } from "./lexer_public";
import { augmentTokenTypes, tokenStructuredMatcher } from "./tokens";
export function tokenLabel(tokType) {
    if (hasTokenLabel(tokType)) {
        return tokType.LABEL;
    }
    else {
        return tokType.name;
    }
}
export function tokenName(tokType) {
    return tokType.name;
}
export function hasTokenLabel(obj) {
    return isString(obj.LABEL) && obj.LABEL !== "";
}
var PARENT = "parent";
var CATEGORIES = "categories";
var LABEL = "label";
var GROUP = "group";
var PUSH_MODE = "push_mode";
var POP_MODE = "pop_mode";
var LONGER_ALT = "longer_alt";
var LINE_BREAKS = "line_breaks";
var START_CHARS_HINT = "start_chars_hint";
export function createToken(config) {
    return createTokenInternal(config);
}
function createTokenInternal(config) {
    var pattern = config.pattern;
    var tokenType = {};
    tokenType.name = config.name;
    if (!isUndefined(pattern)) {
        tokenType.PATTERN = pattern;
    }
    if (has(config, PARENT)) {
        throw "The parent property is no longer supported.\n" +
            "See: https://github.com/SAP/chevrotain/issues/564#issuecomment-349062346 for details.";
    }
    if (has(config, CATEGORIES)) {
        // casting to ANY as this will be fixed inside `augmentTokenTypes``
        tokenType.CATEGORIES = config[CATEGORIES];
    }
    augmentTokenTypes([tokenType]);
    if (has(config, LABEL)) {
        tokenType.LABEL = config[LABEL];
    }
    if (has(config, GROUP)) {
        tokenType.GROUP = config[GROUP];
    }
    if (has(config, POP_MODE)) {
        tokenType.POP_MODE = config[POP_MODE];
    }
    if (has(config, PUSH_MODE)) {
        tokenType.PUSH_MODE = config[PUSH_MODE];
    }
    if (has(config, LONGER_ALT)) {
        tokenType.LONGER_ALT = config[LONGER_ALT];
    }
    if (has(config, LINE_BREAKS)) {
        tokenType.LINE_BREAKS = config[LINE_BREAKS];
    }
    if (has(config, START_CHARS_HINT)) {
        tokenType.START_CHARS_HINT = config[START_CHARS_HINT];
    }
    return tokenType;
}
export var EOF = createToken({ name: "EOF", pattern: Lexer.NA });
augmentTokenTypes([EOF]);
export function createTokenInstance(tokType, image, startOffset, endOffset, startLine, endLine, startColumn, endColumn) {
    return {
        image: image,
        startOffset: startOffset,
        endOffset: endOffset,
        startLine: startLine,
        endLine: endLine,
        startColumn: startColumn,
        endColumn: endColumn,
        tokenTypeIdx: tokType.tokenTypeIdx,
        tokenType: tokType
    };
}
export function tokenMatcher(token, tokType) {
    return tokenStructuredMatcher(token, tokType);
}
//# sourceMappingURL=tokens_public.js.map