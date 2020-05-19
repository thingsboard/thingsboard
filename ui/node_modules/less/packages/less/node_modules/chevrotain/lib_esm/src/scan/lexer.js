var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
import { BaseRegExpVisitor } from "regexp-to-ast";
import { Lexer, LexerDefinitionErrorType } from "./lexer_public";
import { compact, contains, defaults, difference, filter, find, first, flatten, forEach, has, indexOf, isArray, isEmpty, isFunction, isRegExp, isString, isUndefined, keys, map, mapValues, packArray, PRINT_ERROR, reduce, reject } from "../utils/utils";
import { canMatchCharCode, failedOptimizationPrefixMsg, getOptimizedStartCodesIndices } from "./reg_exp";
import { getRegExpAst } from "./reg_exp_parser";
var PATTERN = "PATTERN";
export var DEFAULT_MODE = "defaultMode";
export var MODES = "modes";
export var SUPPORT_STICKY = typeof new RegExp("(?:)").sticky === "boolean";
export function disableSticky() {
    SUPPORT_STICKY = false;
}
export function enableSticky() {
    SUPPORT_STICKY = true;
}
export function analyzeTokenTypes(tokenTypes, options) {
    options = defaults(options, {
        useSticky: SUPPORT_STICKY,
        debug: false,
        safeMode: false,
        positionTracking: "full",
        lineTerminatorCharacters: ["\r", "\n"],
        tracer: function (msg, action) { return action(); }
    });
    var tracer = options.tracer;
    tracer("initCharCodeToOptimizedIndexMap", function () {
        initCharCodeToOptimizedIndexMap();
    });
    var onlyRelevantTypes;
    tracer("Reject Lexer.NA", function () {
        onlyRelevantTypes = reject(tokenTypes, function (currType) {
            return currType[PATTERN] === Lexer.NA;
        });
    });
    var hasCustom = false;
    var allTransformedPatterns;
    tracer("Transform Patterns", function () {
        hasCustom = false;
        allTransformedPatterns = map(onlyRelevantTypes, function (currType) {
            var currPattern = currType[PATTERN];
            /* istanbul ignore else */
            if (isRegExp(currPattern)) {
                var regExpSource = currPattern.source;
                if (regExpSource.length === 1 &&
                    // only these regExp meta characters which can appear in a length one regExp
                    regExpSource !== "^" &&
                    regExpSource !== "$" &&
                    regExpSource !== ".") {
                    return regExpSource;
                }
                else if (regExpSource.length === 2 &&
                    regExpSource[0] === "\\" &&
                    // not a meta character
                    !contains([
                        "d",
                        "D",
                        "s",
                        "S",
                        "t",
                        "r",
                        "n",
                        "t",
                        "0",
                        "c",
                        "b",
                        "B",
                        "f",
                        "v",
                        "w",
                        "W"
                    ], regExpSource[1])) {
                    // escaped meta Characters: /\+/ /\[/
                    // or redundant escaping: /\a/
                    // without the escaping "\"
                    return regExpSource[1];
                }
                else {
                    return options.useSticky
                        ? addStickyFlag(currPattern)
                        : addStartOfInput(currPattern);
                }
            }
            else if (isFunction(currPattern)) {
                hasCustom = true;
                // CustomPatternMatcherFunc - custom patterns do not require any transformations, only wrapping in a RegExp Like object
                return { exec: currPattern };
            }
            else if (has(currPattern, "exec")) {
                hasCustom = true;
                // ICustomPattern
                return currPattern;
            }
            else if (typeof currPattern === "string") {
                if (currPattern.length === 1) {
                    return currPattern;
                }
                else {
                    var escapedRegExpString = currPattern.replace(/[\\^$.*+?()[\]{}|]/g, "\\$&");
                    var wrappedRegExp = new RegExp(escapedRegExpString);
                    return options.useSticky
                        ? addStickyFlag(wrappedRegExp)
                        : addStartOfInput(wrappedRegExp);
                }
            }
            else {
                throw Error("non exhaustive match");
            }
        });
    });
    var patternIdxToType;
    var patternIdxToGroup;
    var patternIdxToLongerAltIdx;
    var patternIdxToPushMode;
    var patternIdxToPopMode;
    tracer("misc mapping", function () {
        patternIdxToType = map(onlyRelevantTypes, function (currType) { return currType.tokenTypeIdx; });
        patternIdxToGroup = map(onlyRelevantTypes, function (clazz) {
            var groupName = clazz.GROUP;
            /* istanbul ignore next */
            if (groupName === Lexer.SKIPPED) {
                return undefined;
            }
            else if (isString(groupName)) {
                return groupName;
            }
            else if (isUndefined(groupName)) {
                return false;
            }
            else {
                throw Error("non exhaustive match");
            }
        });
        patternIdxToLongerAltIdx = map(onlyRelevantTypes, function (clazz) {
            var longerAltType = clazz.LONGER_ALT;
            if (longerAltType) {
                var longerAltIdx = indexOf(onlyRelevantTypes, longerAltType);
                return longerAltIdx;
            }
        });
        patternIdxToPushMode = map(onlyRelevantTypes, function (clazz) { return clazz.PUSH_MODE; });
        patternIdxToPopMode = map(onlyRelevantTypes, function (clazz) {
            return has(clazz, "POP_MODE");
        });
    });
    var patternIdxToCanLineTerminator;
    tracer("Line Terminator Handling", function () {
        var lineTerminatorCharCodes = getCharCodes(options.lineTerminatorCharacters);
        patternIdxToCanLineTerminator = map(onlyRelevantTypes, function (tokType) { return false; });
        if (options.positionTracking !== "onlyOffset") {
            patternIdxToCanLineTerminator = map(onlyRelevantTypes, function (tokType) {
                if (has(tokType, "LINE_BREAKS")) {
                    return tokType.LINE_BREAKS;
                }
                else {
                    if (checkLineBreaksIssues(tokType, lineTerminatorCharCodes) === false) {
                        return canMatchCharCode(lineTerminatorCharCodes, tokType.PATTERN);
                    }
                }
            });
        }
    });
    var patternIdxToIsCustom;
    var patternIdxToShort;
    var emptyGroups;
    var patternIdxToConfig;
    tracer("Misc Mapping #2", function () {
        patternIdxToIsCustom = map(onlyRelevantTypes, isCustomPattern);
        patternIdxToShort = map(allTransformedPatterns, isShortPattern);
        emptyGroups = reduce(onlyRelevantTypes, function (acc, clazz) {
            var groupName = clazz.GROUP;
            if (isString(groupName) && !(groupName === Lexer.SKIPPED)) {
                acc[groupName] = [];
            }
            return acc;
        }, {});
        patternIdxToConfig = map(allTransformedPatterns, function (x, idx) {
            return {
                pattern: allTransformedPatterns[idx],
                longerAlt: patternIdxToLongerAltIdx[idx],
                canLineTerminator: patternIdxToCanLineTerminator[idx],
                isCustom: patternIdxToIsCustom[idx],
                short: patternIdxToShort[idx],
                group: patternIdxToGroup[idx],
                push: patternIdxToPushMode[idx],
                pop: patternIdxToPopMode[idx],
                tokenTypeIdx: patternIdxToType[idx],
                tokenType: onlyRelevantTypes[idx]
            };
        });
    });
    var canBeOptimized = true;
    var charCodeToPatternIdxToConfig = [];
    if (!options.safeMode) {
        tracer("First Char Optimization", function () {
            charCodeToPatternIdxToConfig = reduce(onlyRelevantTypes, function (result, currTokType, idx) {
                if (typeof currTokType.PATTERN === "string") {
                    var charCode = currTokType.PATTERN.charCodeAt(0);
                    var optimizedIdx = charCodeToOptimizedIndex(charCode);
                    addToMapOfArrays(result, optimizedIdx, patternIdxToConfig[idx]);
                }
                else if (isArray(currTokType.START_CHARS_HINT)) {
                    var lastOptimizedIdx_1;
                    forEach(currTokType.START_CHARS_HINT, function (charOrInt) {
                        var charCode = typeof charOrInt === "string"
                            ? charOrInt.charCodeAt(0)
                            : charOrInt;
                        var currOptimizedIdx = charCodeToOptimizedIndex(charCode);
                        // Avoid adding the config multiple times
                        if (lastOptimizedIdx_1 !== currOptimizedIdx) {
                            lastOptimizedIdx_1 = currOptimizedIdx;
                            addToMapOfArrays(result, currOptimizedIdx, patternIdxToConfig[idx]);
                        }
                    });
                }
                else if (isRegExp(currTokType.PATTERN)) {
                    if (currTokType.PATTERN.unicode) {
                        canBeOptimized = false;
                        if (options.ensureOptimizations) {
                            PRINT_ERROR("" + failedOptimizationPrefixMsg +
                                ("\tUnable to analyze < " + currTokType.PATTERN.toString() + " > pattern.\n") +
                                "\tThe regexp unicode flag is not currently supported by the regexp-to-ast library.\n" +
                                "\tThis will disable the lexer's first char optimizations.\n" +
                                "\tFor details See: https://sap.github.io/chevrotain/docs/guide/resolving_lexer_errors.html#UNICODE_OPTIMIZE");
                        }
                    }
                    else {
                        var optimizedCodes = getOptimizedStartCodesIndices(currTokType.PATTERN, options.ensureOptimizations);
                        /* istanbul ignore if */
                        // start code will only be empty given an empty regExp or failure of regexp-to-ast library
                        // the first should be a different validation and the second cannot be tested.
                        if (isEmpty(optimizedCodes)) {
                            // we cannot understand what codes may start possible matches
                            // The optimization correctness requires knowing start codes for ALL patterns.
                            // Not actually sure this is an error, no debug message
                            canBeOptimized = false;
                        }
                        forEach(optimizedCodes, function (code) {
                            addToMapOfArrays(result, code, patternIdxToConfig[idx]);
                        });
                    }
                }
                else {
                    if (options.ensureOptimizations) {
                        PRINT_ERROR("" + failedOptimizationPrefixMsg +
                            ("\tTokenType: <" + currTokType.name + "> is using a custom token pattern without providing <start_chars_hint> parameter.\n") +
                            "\tThis will disable the lexer's first char optimizations.\n" +
                            "\tFor details See: https://sap.github.io/chevrotain/docs/guide/resolving_lexer_errors.html#CUSTOM_OPTIMIZE");
                    }
                    canBeOptimized = false;
                }
                return result;
            }, []);
        });
    }
    tracer("ArrayPacking", function () {
        charCodeToPatternIdxToConfig = packArray(charCodeToPatternIdxToConfig);
    });
    return {
        emptyGroups: emptyGroups,
        patternIdxToConfig: patternIdxToConfig,
        charCodeToPatternIdxToConfig: charCodeToPatternIdxToConfig,
        hasCustom: hasCustom,
        canBeOptimized: canBeOptimized
    };
}
export function validatePatterns(tokenTypes, validModesNames) {
    var errors = [];
    var missingResult = findMissingPatterns(tokenTypes);
    errors = errors.concat(missingResult.errors);
    var invalidResult = findInvalidPatterns(missingResult.valid);
    var validTokenTypes = invalidResult.valid;
    errors = errors.concat(invalidResult.errors);
    errors = errors.concat(validateRegExpPattern(validTokenTypes));
    errors = errors.concat(findInvalidGroupType(validTokenTypes));
    errors = errors.concat(findModesThatDoNotExist(validTokenTypes, validModesNames));
    errors = errors.concat(findUnreachablePatterns(validTokenTypes));
    return errors;
}
function validateRegExpPattern(tokenTypes) {
    var errors = [];
    var withRegExpPatterns = filter(tokenTypes, function (currTokType) {
        return isRegExp(currTokType[PATTERN]);
    });
    errors = errors.concat(findEndOfInputAnchor(withRegExpPatterns));
    errors = errors.concat(findStartOfInputAnchor(withRegExpPatterns));
    errors = errors.concat(findUnsupportedFlags(withRegExpPatterns));
    errors = errors.concat(findDuplicatePatterns(withRegExpPatterns));
    errors = errors.concat(findEmptyMatchRegExps(withRegExpPatterns));
    return errors;
}
export function findMissingPatterns(tokenTypes) {
    var tokenTypesWithMissingPattern = filter(tokenTypes, function (currType) {
        return !has(currType, PATTERN);
    });
    var errors = map(tokenTypesWithMissingPattern, function (currType) {
        return {
            message: "Token Type: ->" +
                currType.name +
                "<- missing static 'PATTERN' property",
            type: LexerDefinitionErrorType.MISSING_PATTERN,
            tokenTypes: [currType]
        };
    });
    var valid = difference(tokenTypes, tokenTypesWithMissingPattern);
    return { errors: errors, valid: valid };
}
export function findInvalidPatterns(tokenTypes) {
    var tokenTypesWithInvalidPattern = filter(tokenTypes, function (currType) {
        var pattern = currType[PATTERN];
        return (!isRegExp(pattern) &&
            !isFunction(pattern) &&
            !has(pattern, "exec") &&
            !isString(pattern));
    });
    var errors = map(tokenTypesWithInvalidPattern, function (currType) {
        return {
            message: "Token Type: ->" +
                currType.name +
                "<- static 'PATTERN' can only be a RegExp, a" +
                " Function matching the {CustomPatternMatcherFunc} type or an Object matching the {ICustomPattern} interface.",
            type: LexerDefinitionErrorType.INVALID_PATTERN,
            tokenTypes: [currType]
        };
    });
    var valid = difference(tokenTypes, tokenTypesWithInvalidPattern);
    return { errors: errors, valid: valid };
}
var end_of_input = /[^\\][\$]/;
export function findEndOfInputAnchor(tokenTypes) {
    var EndAnchorFinder = /** @class */ (function (_super) {
        __extends(EndAnchorFinder, _super);
        function EndAnchorFinder() {
            var _this = _super !== null && _super.apply(this, arguments) || this;
            _this.found = false;
            return _this;
        }
        EndAnchorFinder.prototype.visitEndAnchor = function (node) {
            this.found = true;
        };
        return EndAnchorFinder;
    }(BaseRegExpVisitor));
    var invalidRegex = filter(tokenTypes, function (currType) {
        var pattern = currType[PATTERN];
        try {
            var regexpAst = getRegExpAst(pattern);
            var endAnchorVisitor = new EndAnchorFinder();
            endAnchorVisitor.visit(regexpAst);
            return endAnchorVisitor.found;
        }
        catch (e) {
            // old behavior in case of runtime exceptions with regexp-to-ast.
            /* istanbul ignore next - cannot ensure an error in regexp-to-ast*/
            return end_of_input.test(pattern.source);
        }
    });
    var errors = map(invalidRegex, function (currType) {
        return {
            message: "Unexpected RegExp Anchor Error:\n" +
                "\tToken Type: ->" +
                currType.name +
                "<- static 'PATTERN' cannot contain end of input anchor '$'\n" +
                "\tSee sap.github.io/chevrotain/docs/guide/resolving_lexer_errors.html#ANCHORS" +
                "\tfor details.",
            type: LexerDefinitionErrorType.EOI_ANCHOR_FOUND,
            tokenTypes: [currType]
        };
    });
    return errors;
}
export function findEmptyMatchRegExps(tokenTypes) {
    var matchesEmptyString = filter(tokenTypes, function (currType) {
        var pattern = currType[PATTERN];
        return pattern.test("");
    });
    var errors = map(matchesEmptyString, function (currType) {
        return {
            message: "Token Type: ->" +
                currType.name +
                "<- static 'PATTERN' must not match an empty string",
            type: LexerDefinitionErrorType.EMPTY_MATCH_PATTERN,
            tokenTypes: [currType]
        };
    });
    return errors;
}
var start_of_input = /[^\\[][\^]|^\^/;
export function findStartOfInputAnchor(tokenTypes) {
    var StartAnchorFinder = /** @class */ (function (_super) {
        __extends(StartAnchorFinder, _super);
        function StartAnchorFinder() {
            var _this = _super !== null && _super.apply(this, arguments) || this;
            _this.found = false;
            return _this;
        }
        StartAnchorFinder.prototype.visitStartAnchor = function (node) {
            this.found = true;
        };
        return StartAnchorFinder;
    }(BaseRegExpVisitor));
    var invalidRegex = filter(tokenTypes, function (currType) {
        var pattern = currType[PATTERN];
        try {
            var regexpAst = getRegExpAst(pattern);
            var startAnchorVisitor = new StartAnchorFinder();
            startAnchorVisitor.visit(regexpAst);
            return startAnchorVisitor.found;
        }
        catch (e) {
            // old behavior in case of runtime exceptions with regexp-to-ast.
            /* istanbul ignore next - cannot ensure an error in regexp-to-ast*/
            return start_of_input.test(pattern.source);
        }
    });
    var errors = map(invalidRegex, function (currType) {
        return {
            message: "Unexpected RegExp Anchor Error:\n" +
                "\tToken Type: ->" +
                currType.name +
                "<- static 'PATTERN' cannot contain start of input anchor '^'\n" +
                "\tSee https://sap.github.io/chevrotain/docs/guide/resolving_lexer_errors.html#ANCHORS" +
                "\tfor details.",
            type: LexerDefinitionErrorType.SOI_ANCHOR_FOUND,
            tokenTypes: [currType]
        };
    });
    return errors;
}
export function findUnsupportedFlags(tokenTypes) {
    var invalidFlags = filter(tokenTypes, function (currType) {
        var pattern = currType[PATTERN];
        return (pattern instanceof RegExp && (pattern.multiline || pattern.global));
    });
    var errors = map(invalidFlags, function (currType) {
        return {
            message: "Token Type: ->" +
                currType.name +
                "<- static 'PATTERN' may NOT contain global('g') or multiline('m')",
            type: LexerDefinitionErrorType.UNSUPPORTED_FLAGS_FOUND,
            tokenTypes: [currType]
        };
    });
    return errors;
}
// This can only test for identical duplicate RegExps, not semantically equivalent ones.
export function findDuplicatePatterns(tokenTypes) {
    var found = [];
    var identicalPatterns = map(tokenTypes, function (outerType) {
        return reduce(tokenTypes, function (result, innerType) {
            if (outerType.PATTERN.source === innerType.PATTERN.source &&
                !contains(found, innerType) &&
                innerType.PATTERN !== Lexer.NA) {
                // this avoids duplicates in the result, each Token Type may only appear in one "set"
                // in essence we are creating Equivalence classes on equality relation.
                found.push(innerType);
                result.push(innerType);
                return result;
            }
            return result;
        }, []);
    });
    identicalPatterns = compact(identicalPatterns);
    var duplicatePatterns = filter(identicalPatterns, function (currIdenticalSet) {
        return currIdenticalSet.length > 1;
    });
    var errors = map(duplicatePatterns, function (setOfIdentical) {
        var tokenTypeNames = map(setOfIdentical, function (currType) {
            return currType.name;
        });
        var dupPatternSrc = first(setOfIdentical).PATTERN;
        return {
            message: "The same RegExp pattern ->" + dupPatternSrc + "<-" +
                ("has been used in all of the following Token Types: " + tokenTypeNames.join(", ") + " <-"),
            type: LexerDefinitionErrorType.DUPLICATE_PATTERNS_FOUND,
            tokenTypes: setOfIdentical
        };
    });
    return errors;
}
export function findInvalidGroupType(tokenTypes) {
    var invalidTypes = filter(tokenTypes, function (clazz) {
        if (!has(clazz, "GROUP")) {
            return false;
        }
        var group = clazz.GROUP;
        return group !== Lexer.SKIPPED && group !== Lexer.NA && !isString(group);
    });
    var errors = map(invalidTypes, function (currType) {
        return {
            message: "Token Type: ->" +
                currType.name +
                "<- static 'GROUP' can only be Lexer.SKIPPED/Lexer.NA/A String",
            type: LexerDefinitionErrorType.INVALID_GROUP_TYPE_FOUND,
            tokenTypes: [currType]
        };
    });
    return errors;
}
export function findModesThatDoNotExist(tokenTypes, validModes) {
    var invalidModes = filter(tokenTypes, function (clazz) {
        return (clazz.PUSH_MODE !== undefined &&
            !contains(validModes, clazz.PUSH_MODE));
    });
    var errors = map(invalidModes, function (tokType) {
        var msg = "Token Type: ->" + tokType.name + "<- static 'PUSH_MODE' value cannot refer to a Lexer Mode ->" + tokType.PUSH_MODE + "<-" +
            "which does not exist";
        return {
            message: msg,
            type: LexerDefinitionErrorType.PUSH_MODE_DOES_NOT_EXIST,
            tokenTypes: [tokType]
        };
    });
    return errors;
}
export function findUnreachablePatterns(tokenTypes) {
    var errors = [];
    var canBeTested = reduce(tokenTypes, function (result, tokType, idx) {
        var pattern = tokType.PATTERN;
        if (pattern === Lexer.NA) {
            return result;
        }
        // a more comprehensive validation for all forms of regExps would require
        // deeper regExp analysis capabilities
        if (isString(pattern)) {
            result.push({ str: pattern, idx: idx, tokenType: tokType });
        }
        else if (isRegExp(pattern) && noMetaChar(pattern)) {
            result.push({ str: pattern.source, idx: idx, tokenType: tokType });
        }
        return result;
    }, []);
    forEach(tokenTypes, function (tokType, testIdx) {
        forEach(canBeTested, function (_a) {
            var str = _a.str, idx = _a.idx, tokenType = _a.tokenType;
            if (testIdx < idx && testTokenType(str, tokType.PATTERN)) {
                var msg = "Token: ->" + tokenType.name + "<- can never be matched.\n" +
                    ("Because it appears AFTER the Token Type ->" + tokType.name + "<-") +
                    "in the lexer's definition.\n" +
                    "See https://sap.github.io/chevrotain/docs/guide/resolving_lexer_errors.html#UNREACHABLE";
                errors.push({
                    message: msg,
                    type: LexerDefinitionErrorType.UNREACHABLE_PATTERN,
                    tokenTypes: [tokType, tokenType]
                });
            }
        });
    });
    return errors;
}
function testTokenType(str, pattern) {
    /* istanbul ignore else */
    if (isRegExp(pattern)) {
        var regExpArray = pattern.exec(str);
        return regExpArray !== null && regExpArray.index === 0;
    }
    else if (isFunction(pattern)) {
        // maintain the API of custom patterns
        return pattern(str, 0, [], {});
    }
    else if (has(pattern, "exec")) {
        // maintain the API of custom patterns
        return pattern.exec(str, 0, [], {});
    }
    else if (typeof pattern === "string") {
        return pattern === str;
    }
    else {
        throw Error("non exhaustive match");
    }
}
function noMetaChar(regExp) {
    //https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/RegExp
    var metaChars = [
        ".",
        "\\",
        "[",
        "]",
        "|",
        "^",
        "$",
        "(",
        ")",
        "?",
        "*",
        "+",
        "{"
    ];
    return (find(metaChars, function (char) { return regExp.source.indexOf(char) !== -1; }) ===
        undefined);
}
export function addStartOfInput(pattern) {
    var flags = pattern.ignoreCase ? "i" : "";
    // always wrapping in a none capturing group preceded by '^' to make sure matching can only work on start of input.
    // duplicate/redundant start of input markers have no meaning (/^^^^A/ === /^A/)
    return new RegExp("^(?:" + pattern.source + ")", flags);
}
export function addStickyFlag(pattern) {
    var flags = pattern.ignoreCase ? "iy" : "y";
    // always wrapping in a none capturing group preceded by '^' to make sure matching can only work on start of input.
    // duplicate/redundant start of input markers have no meaning (/^^^^A/ === /^A/)
    return new RegExp("" + pattern.source, flags);
}
export function performRuntimeChecks(lexerDefinition, trackLines, lineTerminatorCharacters) {
    var errors = [];
    // some run time checks to help the end users.
    if (!has(lexerDefinition, DEFAULT_MODE)) {
        errors.push({
            message: "A MultiMode Lexer cannot be initialized without a <" +
                DEFAULT_MODE +
                "> property in its definition\n",
            type: LexerDefinitionErrorType.MULTI_MODE_LEXER_WITHOUT_DEFAULT_MODE
        });
    }
    if (!has(lexerDefinition, MODES)) {
        errors.push({
            message: "A MultiMode Lexer cannot be initialized without a <" +
                MODES +
                "> property in its definition\n",
            type: LexerDefinitionErrorType.MULTI_MODE_LEXER_WITHOUT_MODES_PROPERTY
        });
    }
    if (has(lexerDefinition, MODES) &&
        has(lexerDefinition, DEFAULT_MODE) &&
        !has(lexerDefinition.modes, lexerDefinition.defaultMode)) {
        errors.push({
            message: "A MultiMode Lexer cannot be initialized with a " + DEFAULT_MODE + ": <" + lexerDefinition.defaultMode + ">" +
                "which does not exist\n",
            type: LexerDefinitionErrorType.MULTI_MODE_LEXER_DEFAULT_MODE_VALUE_DOES_NOT_EXIST
        });
    }
    if (has(lexerDefinition, MODES)) {
        forEach(lexerDefinition.modes, function (currModeValue, currModeName) {
            forEach(currModeValue, function (currTokType, currIdx) {
                if (isUndefined(currTokType)) {
                    errors.push({
                        message: "A Lexer cannot be initialized using an undefined Token Type. Mode:" +
                            ("<" + currModeName + "> at index: <" + currIdx + ">\n"),
                        type: LexerDefinitionErrorType.LEXER_DEFINITION_CANNOT_CONTAIN_UNDEFINED
                    });
                }
            });
        });
    }
    return errors;
}
export function performWarningRuntimeChecks(lexerDefinition, trackLines, lineTerminatorCharacters) {
    var warnings = [];
    var hasAnyLineBreak = false;
    var allTokenTypes = compact(flatten(mapValues(lexerDefinition.modes, function (tokTypes) { return tokTypes; })));
    var concreteTokenTypes = reject(allTokenTypes, function (currType) { return currType[PATTERN] === Lexer.NA; });
    var terminatorCharCodes = getCharCodes(lineTerminatorCharacters);
    if (trackLines) {
        forEach(concreteTokenTypes, function (tokType) {
            var currIssue = checkLineBreaksIssues(tokType, terminatorCharCodes);
            if (currIssue !== false) {
                var message = buildLineBreakIssueMessage(tokType, currIssue);
                var warningDescriptor = {
                    message: message,
                    type: currIssue.issue,
                    tokenType: tokType
                };
                warnings.push(warningDescriptor);
            }
            else {
                // we don't want to attempt to scan if the user explicitly specified the line_breaks option.
                if (has(tokType, "LINE_BREAKS")) {
                    if (tokType.LINE_BREAKS === true) {
                        hasAnyLineBreak = true;
                    }
                }
                else {
                    if (canMatchCharCode(terminatorCharCodes, tokType.PATTERN)) {
                        hasAnyLineBreak = true;
                    }
                }
            }
        });
    }
    if (trackLines && !hasAnyLineBreak) {
        warnings.push({
            message: "Warning: No LINE_BREAKS Found.\n" +
                "\tThis Lexer has been defined to track line and column information,\n" +
                "\tBut none of the Token Types can be identified as matching a line terminator.\n" +
                "\tSee https://sap.github.io/chevrotain/docs/guide/resolving_lexer_errors.html#LINE_BREAKS \n" +
                "\tfor details.",
            type: LexerDefinitionErrorType.NO_LINE_BREAKS_FLAGS
        });
    }
    return warnings;
}
export function cloneEmptyGroups(emptyGroups) {
    var clonedResult = {};
    var groupKeys = keys(emptyGroups);
    forEach(groupKeys, function (currKey) {
        var currGroupValue = emptyGroups[currKey];
        /* istanbul ignore else */
        if (isArray(currGroupValue)) {
            clonedResult[currKey] = [];
        }
        else {
            throw Error("non exhaustive match");
        }
    });
    return clonedResult;
}
// TODO: refactor to avoid duplication
export function isCustomPattern(tokenType) {
    var pattern = tokenType.PATTERN;
    /* istanbul ignore else */
    if (isRegExp(pattern)) {
        return false;
    }
    else if (isFunction(pattern)) {
        // CustomPatternMatcherFunc - custom patterns do not require any transformations, only wrapping in a RegExp Like object
        return true;
    }
    else if (has(pattern, "exec")) {
        // ICustomPattern
        return true;
    }
    else if (isString(pattern)) {
        return false;
    }
    else {
        throw Error("non exhaustive match");
    }
}
export function isShortPattern(pattern) {
    if (isString(pattern) && pattern.length === 1) {
        return pattern.charCodeAt(0);
    }
    else {
        return false;
    }
}
/**
 * Faster than using a RegExp for default newline detection during lexing.
 */
export var LineTerminatorOptimizedTester = {
    // implements /\n|\r\n?/g.test
    test: function (text) {
        var len = text.length;
        for (var i = this.lastIndex; i < len; i++) {
            var c = text.charCodeAt(i);
            if (c === 10) {
                this.lastIndex = i + 1;
                return true;
            }
            else if (c === 13) {
                if (text.charCodeAt(i + 1) === 10) {
                    this.lastIndex = i + 2;
                }
                else {
                    this.lastIndex = i + 1;
                }
                return true;
            }
        }
        return false;
    },
    lastIndex: 0
};
function checkLineBreaksIssues(tokType, lineTerminatorCharCodes) {
    if (has(tokType, "LINE_BREAKS")) {
        // if the user explicitly declared the line_breaks option we will respect their choice
        // and assume it is correct.
        return false;
    }
    else {
        /* istanbul ignore else */
        if (isRegExp(tokType.PATTERN)) {
            try {
                canMatchCharCode(lineTerminatorCharCodes, tokType.PATTERN);
            }
            catch (e) {
                /* istanbul ignore next - to test this we would have to mock <canMatchCharCode> to throw an error */
                return {
                    issue: LexerDefinitionErrorType.IDENTIFY_TERMINATOR,
                    errMsg: e.message
                };
            }
            return false;
        }
        else if (isString(tokType.PATTERN)) {
            // string literal patterns can always be analyzed to detect line terminator usage
            return false;
        }
        else if (isCustomPattern(tokType)) {
            // custom token types
            return { issue: LexerDefinitionErrorType.CUSTOM_LINE_BREAK };
        }
        else {
            throw Error("non exhaustive match");
        }
    }
}
export function buildLineBreakIssueMessage(tokType, details) {
    /* istanbul ignore else */
    if (details.issue === LexerDefinitionErrorType.IDENTIFY_TERMINATOR) {
        return ("Warning: unable to identify line terminator usage in pattern.\n" +
            ("\tThe problem is in the <" + tokType.name + "> Token Type\n") +
            ("\t Root cause: " + details.errMsg + ".\n") +
            "\tFor details See: https://sap.github.io/chevrotain/docs/guide/resolving_lexer_errors.html#IDENTIFY_TERMINATOR");
    }
    else if (details.issue === LexerDefinitionErrorType.CUSTOM_LINE_BREAK) {
        return ("Warning: A Custom Token Pattern should specify the <line_breaks> option.\n" +
            ("\tThe problem is in the <" + tokType.name + "> Token Type\n") +
            "\tFor details See: https://sap.github.io/chevrotain/docs/guide/resolving_lexer_errors.html#CUSTOM_LINE_BREAK");
    }
    else {
        throw Error("non exhaustive match");
    }
}
function getCharCodes(charsOrCodes) {
    var charCodes = map(charsOrCodes, function (numOrString) {
        if (isString(numOrString) && numOrString.length > 0) {
            return numOrString.charCodeAt(0);
        }
        else {
            return numOrString;
        }
    });
    return charCodes;
}
function addToMapOfArrays(map, key, value) {
    if (map[key] === undefined) {
        map[key] = [value];
    }
    else {
        map[key].push(value);
    }
}
export var minOptimizationVal = 256;
/**
 * We ae mapping charCode above ASCI (256) into buckets each in the size of 256.
 * This is because ASCI are the most common start chars so each one of those will get its own
 * possible token configs vector.
 *
 * Tokens starting with charCodes "above" ASCI are uncommon, so we can "afford"
 * to place these into buckets of possible token configs, What we gain from
 * this is avoiding the case of creating an optimization 'charCodeToPatternIdxToConfig'
 * which would contain 10,000+ arrays of small size (e.g unicode Identifiers scenario).
 * Our 'charCodeToPatternIdxToConfig' max size will now be:
 * 256 + (2^16 / 2^8) - 1 === 511
 *
 * note the hack for fast division integer part extraction
 * See: https://stackoverflow.com/a/4228528
 */
export function charCodeToOptimizedIndex(charCode) {
    return charCode < minOptimizationVal
        ? charCode
        : charCodeToOptimizedIdxMap[charCode];
}
/**
 * This is a compromise between cold start / hot running performance
 * Creating this array takes ~3ms on a modern machine,
 * But if we perform the computation at runtime as needed the CSS Lexer benchmark
 * performance degrades by ~10%
 *
 * TODO: Perhaps it should be lazy initialized only if a charCode > 255 is used.
 */
var charCodeToOptimizedIdxMap = [];
function initCharCodeToOptimizedIndexMap() {
    if (isEmpty(charCodeToOptimizedIdxMap)) {
        charCodeToOptimizedIdxMap = new Array(65536);
        for (var i = 0; i < 65536; i++) {
            /* tslint:disable */
            charCodeToOptimizedIdxMap[i] = i > 255 ? 255 + ~~(i / 255) : i;
            /* tslint:enable */
        }
    }
}
//# sourceMappingURL=lexer.js.map