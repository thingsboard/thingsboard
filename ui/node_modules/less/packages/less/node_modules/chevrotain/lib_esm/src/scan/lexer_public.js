import { analyzeTokenTypes, charCodeToOptimizedIndex, cloneEmptyGroups, DEFAULT_MODE, LineTerminatorOptimizedTester, performRuntimeChecks, performWarningRuntimeChecks, SUPPORT_STICKY, validatePatterns } from "./lexer";
import { cloneArr, cloneObj, forEach, IDENTITY, isArray, isEmpty, isUndefined, keys, last, map, merge, NOOP, PRINT_WARNING, reduce, reject, timer, toFastProperties } from "../utils/utils";
import { augmentTokenTypes } from "./tokens";
import { defaultLexerErrorProvider } from "../scan/lexer_errors_public";
import { clearRegExpParserCache } from "./reg_exp_parser";
export var LexerDefinitionErrorType;
(function (LexerDefinitionErrorType) {
    LexerDefinitionErrorType[LexerDefinitionErrorType["MISSING_PATTERN"] = 0] = "MISSING_PATTERN";
    LexerDefinitionErrorType[LexerDefinitionErrorType["INVALID_PATTERN"] = 1] = "INVALID_PATTERN";
    LexerDefinitionErrorType[LexerDefinitionErrorType["EOI_ANCHOR_FOUND"] = 2] = "EOI_ANCHOR_FOUND";
    LexerDefinitionErrorType[LexerDefinitionErrorType["UNSUPPORTED_FLAGS_FOUND"] = 3] = "UNSUPPORTED_FLAGS_FOUND";
    LexerDefinitionErrorType[LexerDefinitionErrorType["DUPLICATE_PATTERNS_FOUND"] = 4] = "DUPLICATE_PATTERNS_FOUND";
    LexerDefinitionErrorType[LexerDefinitionErrorType["INVALID_GROUP_TYPE_FOUND"] = 5] = "INVALID_GROUP_TYPE_FOUND";
    LexerDefinitionErrorType[LexerDefinitionErrorType["PUSH_MODE_DOES_NOT_EXIST"] = 6] = "PUSH_MODE_DOES_NOT_EXIST";
    LexerDefinitionErrorType[LexerDefinitionErrorType["MULTI_MODE_LEXER_WITHOUT_DEFAULT_MODE"] = 7] = "MULTI_MODE_LEXER_WITHOUT_DEFAULT_MODE";
    LexerDefinitionErrorType[LexerDefinitionErrorType["MULTI_MODE_LEXER_WITHOUT_MODES_PROPERTY"] = 8] = "MULTI_MODE_LEXER_WITHOUT_MODES_PROPERTY";
    LexerDefinitionErrorType[LexerDefinitionErrorType["MULTI_MODE_LEXER_DEFAULT_MODE_VALUE_DOES_NOT_EXIST"] = 9] = "MULTI_MODE_LEXER_DEFAULT_MODE_VALUE_DOES_NOT_EXIST";
    LexerDefinitionErrorType[LexerDefinitionErrorType["LEXER_DEFINITION_CANNOT_CONTAIN_UNDEFINED"] = 10] = "LEXER_DEFINITION_CANNOT_CONTAIN_UNDEFINED";
    LexerDefinitionErrorType[LexerDefinitionErrorType["SOI_ANCHOR_FOUND"] = 11] = "SOI_ANCHOR_FOUND";
    LexerDefinitionErrorType[LexerDefinitionErrorType["EMPTY_MATCH_PATTERN"] = 12] = "EMPTY_MATCH_PATTERN";
    LexerDefinitionErrorType[LexerDefinitionErrorType["NO_LINE_BREAKS_FLAGS"] = 13] = "NO_LINE_BREAKS_FLAGS";
    LexerDefinitionErrorType[LexerDefinitionErrorType["UNREACHABLE_PATTERN"] = 14] = "UNREACHABLE_PATTERN";
    LexerDefinitionErrorType[LexerDefinitionErrorType["IDENTIFY_TERMINATOR"] = 15] = "IDENTIFY_TERMINATOR";
    LexerDefinitionErrorType[LexerDefinitionErrorType["CUSTOM_LINE_BREAK"] = 16] = "CUSTOM_LINE_BREAK";
})(LexerDefinitionErrorType || (LexerDefinitionErrorType = {}));
var DEFAULT_LEXER_CONFIG = {
    deferDefinitionErrorsHandling: false,
    positionTracking: "full",
    lineTerminatorsPattern: /\n|\r\n?/g,
    lineTerminatorCharacters: ["\n", "\r"],
    ensureOptimizations: false,
    safeMode: false,
    errorMessageProvider: defaultLexerErrorProvider,
    traceInitPerf: false,
    skipValidations: false
};
Object.freeze(DEFAULT_LEXER_CONFIG);
var Lexer = /** @class */ (function () {
    function Lexer(lexerDefinition, config) {
        var _this = this;
        if (config === void 0) { config = DEFAULT_LEXER_CONFIG; }
        this.lexerDefinition = lexerDefinition;
        this.lexerDefinitionErrors = [];
        this.lexerDefinitionWarning = [];
        this.patternIdxToConfig = {};
        this.charCodeToPatternIdxToConfig = {};
        this.modes = [];
        this.emptyGroups = {};
        this.config = undefined;
        this.trackStartLines = true;
        this.trackEndLines = true;
        this.hasCustom = false;
        this.canModeBeOptimized = {};
        if (typeof config === "boolean") {
            throw Error("The second argument to the Lexer constructor is now an ILexerConfig Object.\n" +
                "a boolean 2nd argument is no longer supported");
        }
        // todo: defaults func?
        this.config = merge(DEFAULT_LEXER_CONFIG, config);
        var traceInitVal = this.config.traceInitPerf;
        if (traceInitVal === true) {
            this.traceInitMaxIdent = Infinity;
            this.traceInitPerf = true;
        }
        else if (typeof traceInitVal === "number") {
            this.traceInitMaxIdent = traceInitVal;
            this.traceInitPerf = true;
        }
        this.traceInitIndent = -1;
        this.TRACE_INIT("Lexer Constructor", function () {
            var actualDefinition;
            var hasOnlySingleMode = true;
            _this.TRACE_INIT("Lexer Config handling", function () {
                if (_this.config.lineTerminatorsPattern ===
                    DEFAULT_LEXER_CONFIG.lineTerminatorsPattern) {
                    // optimized built-in implementation for the defaults definition of lineTerminators
                    _this.config.lineTerminatorsPattern = LineTerminatorOptimizedTester;
                }
                else {
                    if (_this.config.lineTerminatorCharacters ===
                        DEFAULT_LEXER_CONFIG.lineTerminatorCharacters) {
                        throw Error("Error: Missing <lineTerminatorCharacters> property on the Lexer config.\n" +
                            "\tFor details See: https://sap.github.io/chevrotain/docs/guide/resolving_lexer_errors.html#MISSING_LINE_TERM_CHARS");
                    }
                }
                if (config.safeMode && config.ensureOptimizations) {
                    throw Error('"safeMode" and "ensureOptimizations" flags are mutually exclusive.');
                }
                _this.trackStartLines = /full|onlyStart/i.test(_this.config.positionTracking);
                _this.trackEndLines = /full/i.test(_this.config.positionTracking);
                // Convert SingleModeLexerDefinition into a IMultiModeLexerDefinition.
                if (isArray(lexerDefinition)) {
                    actualDefinition = { modes: {} };
                    actualDefinition.modes[DEFAULT_MODE] = cloneArr(lexerDefinition);
                    actualDefinition[DEFAULT_MODE] = DEFAULT_MODE;
                }
                else {
                    // no conversion needed, input should already be a IMultiModeLexerDefinition
                    hasOnlySingleMode = false;
                    actualDefinition = cloneObj((lexerDefinition));
                }
            });
            if (_this.config.skipValidations === false) {
                _this.TRACE_INIT("performRuntimeChecks", function () {
                    _this.lexerDefinitionErrors = _this.lexerDefinitionErrors.concat(performRuntimeChecks(actualDefinition, _this.trackStartLines, _this.config.lineTerminatorCharacters));
                });
                _this.TRACE_INIT("performWarningRuntimeChecks", function () {
                    _this.lexerDefinitionWarning = _this.lexerDefinitionWarning.concat(performWarningRuntimeChecks(actualDefinition, _this.trackStartLines, _this.config.lineTerminatorCharacters));
                });
            }
            // for extra robustness to avoid throwing an none informative error message
            actualDefinition.modes = actualDefinition.modes
                ? actualDefinition.modes
                : {};
            // an error of undefined TokenTypes will be detected in "performRuntimeChecks" above.
            // this transformation is to increase robustness in the case of partially invalid lexer definition.
            forEach(actualDefinition.modes, function (currModeValue, currModeName) {
                actualDefinition.modes[currModeName] = reject(currModeValue, function (currTokType) { return isUndefined(currTokType); });
            });
            var allModeNames = keys(actualDefinition.modes);
            forEach(actualDefinition.modes, function (currModDef, currModName) {
                _this.TRACE_INIT("Mode: <" + currModName + "> processing", function () {
                    _this.modes.push(currModName);
                    if (_this.config.skipValidations === false) {
                        _this.TRACE_INIT("validatePatterns", function () {
                            _this.lexerDefinitionErrors = _this.lexerDefinitionErrors.concat(validatePatterns(currModDef, allModeNames));
                        });
                    }
                    // If definition errors were encountered, the analysis phase may fail unexpectedly/
                    // Considering a lexer with definition errors may never be used, there is no point
                    // to performing the analysis anyhow...
                    if (isEmpty(_this.lexerDefinitionErrors)) {
                        augmentTokenTypes(currModDef);
                        var currAnalyzeResult_1;
                        _this.TRACE_INIT("analyzeTokenTypes", function () {
                            currAnalyzeResult_1 = analyzeTokenTypes(currModDef, {
                                lineTerminatorCharacters: _this.config
                                    .lineTerminatorCharacters,
                                positionTracking: config.positionTracking,
                                ensureOptimizations: config.ensureOptimizations,
                                safeMode: config.safeMode,
                                tracer: _this.TRACE_INIT.bind(_this)
                            });
                        });
                        _this.patternIdxToConfig[currModName] =
                            currAnalyzeResult_1.patternIdxToConfig;
                        _this.charCodeToPatternIdxToConfig[currModName] =
                            currAnalyzeResult_1.charCodeToPatternIdxToConfig;
                        _this.emptyGroups = merge(_this.emptyGroups, currAnalyzeResult_1.emptyGroups);
                        _this.hasCustom =
                            currAnalyzeResult_1.hasCustom || _this.hasCustom;
                        _this.canModeBeOptimized[currModName] =
                            currAnalyzeResult_1.canBeOptimized;
                    }
                });
            });
            _this.defaultMode = actualDefinition.defaultMode;
            if (!isEmpty(_this.lexerDefinitionErrors) &&
                !_this.config.deferDefinitionErrorsHandling) {
                var allErrMessages = map(_this.lexerDefinitionErrors, function (error) {
                    return error.message;
                });
                var allErrMessagesString = allErrMessages.join("-----------------------\n");
                throw new Error("Errors detected in definition of Lexer:\n" +
                    allErrMessagesString);
            }
            // Only print warning if there are no errors, This will avoid pl
            forEach(_this.lexerDefinitionWarning, function (warningDescriptor) {
                PRINT_WARNING(warningDescriptor.message);
            });
            _this.TRACE_INIT("Choosing sub-methods implementations", function () {
                // Choose the relevant internal implementations for this specific parser.
                // These implementations should be in-lined by the JavaScript engine
                // to provide optimal performance in each scenario.
                if (SUPPORT_STICKY) {
                    _this.chopInput = IDENTITY;
                    _this.match = _this.matchWithTest;
                }
                else {
                    _this.updateLastIndex = NOOP;
                    _this.match = _this.matchWithExec;
                }
                if (hasOnlySingleMode) {
                    _this.handleModes = NOOP;
                }
                if (_this.trackStartLines === false) {
                    _this.computeNewColumn = IDENTITY;
                }
                if (_this.trackEndLines === false) {
                    _this.updateTokenEndLineColumnLocation = NOOP;
                }
                if (/full/i.test(_this.config.positionTracking)) {
                    _this.createTokenInstance = _this.createFullToken;
                }
                else if (/onlyStart/i.test(_this.config.positionTracking)) {
                    _this.createTokenInstance = _this.createStartOnlyToken;
                }
                else if (/onlyOffset/i.test(_this.config.positionTracking)) {
                    _this.createTokenInstance = _this.createOffsetOnlyToken;
                }
                else {
                    throw Error("Invalid <positionTracking> config option: \"" + _this.config.positionTracking + "\"");
                }
                if (_this.hasCustom) {
                    _this.addToken = _this.addTokenUsingPush;
                    _this.handlePayload = _this.handlePayloadWithCustom;
                }
                else {
                    _this.addToken = _this.addTokenUsingMemberAccess;
                    _this.handlePayload = _this.handlePayloadNoCustom;
                }
            });
            _this.TRACE_INIT("Failed Optimization Warnings", function () {
                var unOptimizedModes = reduce(_this.canModeBeOptimized, function (cannotBeOptimized, canBeOptimized, modeName) {
                    if (canBeOptimized === false) {
                        cannotBeOptimized.push(modeName);
                    }
                    return cannotBeOptimized;
                }, []);
                if (config.ensureOptimizations && !isEmpty(unOptimizedModes)) {
                    throw Error("Lexer Modes: < " + unOptimizedModes.join(", ") + " > cannot be optimized.\n" +
                        '\t Disable the "ensureOptimizations" lexer config flag to silently ignore this and run the lexer in an un-optimized mode.\n' +
                        "\t Or inspect the console log for details on how to resolve these issues.");
                }
            });
            _this.TRACE_INIT("clearRegExpParserCache", function () {
                clearRegExpParserCache();
            });
            _this.TRACE_INIT("toFastProperties", function () {
                toFastProperties(_this);
            });
        });
    }
    Lexer.prototype.tokenize = function (text, initialMode) {
        if (initialMode === void 0) { initialMode = this.defaultMode; }
        if (!isEmpty(this.lexerDefinitionErrors)) {
            var allErrMessages = map(this.lexerDefinitionErrors, function (error) {
                return error.message;
            });
            var allErrMessagesString = allErrMessages.join("-----------------------\n");
            throw new Error("Unable to Tokenize because Errors detected in definition of Lexer:\n" +
                allErrMessagesString);
        }
        var lexResult = this.tokenizeInternal(text, initialMode);
        return lexResult;
    };
    // There is quite a bit of duplication between this and "tokenizeInternalLazy"
    // This is intentional due to performance considerations.
    Lexer.prototype.tokenizeInternal = function (text, initialMode) {
        var _this = this;
        var i, j, matchAltImage, longerAltIdx, matchedImage, payload, altPayload, imageLength, group, tokType, newToken, errLength, droppedChar, msg, match;
        var orgText = text;
        var orgLength = orgText.length;
        var offset = 0;
        var matchedTokensIndex = 0;
        // initializing the tokensArray to the "guessed" size.
        // guessing too little will still reduce the number of array re-sizes on pushes.
        // guessing too large (Tested by guessing x4 too large) may cost a bit more of memory
        // but would still have a faster runtime by avoiding (All but one) array resizing.
        var guessedNumberOfTokens = this.hasCustom
            ? 0 // will break custom token pattern APIs the matchedTokens array will contain undefined elements.
            : Math.floor(text.length / 10);
        var matchedTokens = new Array(guessedNumberOfTokens);
        var errors = [];
        var line = this.trackStartLines ? 1 : undefined;
        var column = this.trackStartLines ? 1 : undefined;
        var groups = cloneEmptyGroups(this.emptyGroups);
        var trackLines = this.trackStartLines;
        var lineTerminatorPattern = this.config.lineTerminatorsPattern;
        var currModePatternsLength = 0;
        var patternIdxToConfig = [];
        var currCharCodeToPatternIdxToConfig = [];
        var modeStack = [];
        var emptyArray = [];
        Object.freeze(emptyArray);
        var getPossiblePatterns = undefined;
        function getPossiblePatternsSlow() {
            return patternIdxToConfig;
        }
        function getPossiblePatternsOptimized(charCode) {
            var optimizedCharIdx = charCodeToOptimizedIndex(charCode);
            var possiblePatterns = currCharCodeToPatternIdxToConfig[optimizedCharIdx];
            if (possiblePatterns === undefined) {
                return emptyArray;
            }
            else {
                return possiblePatterns;
            }
        }
        var pop_mode = function (popToken) {
            // TODO: perhaps avoid this error in the edge case there is no more input?
            if (modeStack.length === 1 &&
                // if we have both a POP_MODE and a PUSH_MODE this is in-fact a "transition"
                // So no error should occur.
                popToken.tokenType.PUSH_MODE === undefined) {
                // if we try to pop the last mode there lexer will no longer have ANY mode.
                // thus the pop is ignored, an error will be created and the lexer will continue parsing in the previous mode.
                var msg_1 = _this.config.errorMessageProvider.buildUnableToPopLexerModeMessage(popToken);
                errors.push({
                    offset: popToken.startOffset,
                    line: popToken.startLine !== undefined
                        ? popToken.startLine
                        : undefined,
                    column: popToken.startColumn !== undefined
                        ? popToken.startColumn
                        : undefined,
                    length: popToken.image.length,
                    message: msg_1
                });
            }
            else {
                modeStack.pop();
                var newMode = last(modeStack);
                patternIdxToConfig = _this.patternIdxToConfig[newMode];
                currCharCodeToPatternIdxToConfig = _this
                    .charCodeToPatternIdxToConfig[newMode];
                currModePatternsLength = patternIdxToConfig.length;
                var modeCanBeOptimized = _this.canModeBeOptimized[newMode] &&
                    _this.config.safeMode === false;
                if (currCharCodeToPatternIdxToConfig && modeCanBeOptimized) {
                    getPossiblePatterns = getPossiblePatternsOptimized;
                }
                else {
                    getPossiblePatterns = getPossiblePatternsSlow;
                }
            }
        };
        function push_mode(newMode) {
            modeStack.push(newMode);
            currCharCodeToPatternIdxToConfig = this
                .charCodeToPatternIdxToConfig[newMode];
            patternIdxToConfig = this.patternIdxToConfig[newMode];
            currModePatternsLength = patternIdxToConfig.length;
            currModePatternsLength = patternIdxToConfig.length;
            var modeCanBeOptimized = this.canModeBeOptimized[newMode] &&
                this.config.safeMode === false;
            if (currCharCodeToPatternIdxToConfig && modeCanBeOptimized) {
                getPossiblePatterns = getPossiblePatternsOptimized;
            }
            else {
                getPossiblePatterns = getPossiblePatternsSlow;
            }
        }
        // this pattern seems to avoid a V8 de-optimization, although that de-optimization does not
        // seem to matter performance wise.
        push_mode.call(this, initialMode);
        var currConfig;
        while (offset < orgLength) {
            matchedImage = null;
            var nextCharCode = orgText.charCodeAt(offset);
            var chosenPatternIdxToConfig = getPossiblePatterns(nextCharCode);
            var chosenPatternsLength = chosenPatternIdxToConfig.length;
            for (i = 0; i < chosenPatternsLength; i++) {
                currConfig = chosenPatternIdxToConfig[i];
                var currPattern = currConfig.pattern;
                payload = null;
                // manually in-lined because > 600 chars won't be in-lined in V8
                var singleCharCode = currConfig.short;
                if (singleCharCode !== false) {
                    if (nextCharCode === singleCharCode) {
                        // single character string
                        matchedImage = currPattern;
                    }
                }
                else if (currConfig.isCustom === true) {
                    match = currPattern.exec(orgText, offset, matchedTokens, groups);
                    if (match !== null) {
                        matchedImage = match[0];
                        if (match.payload !== undefined) {
                            payload = match.payload;
                        }
                    }
                    else {
                        matchedImage = null;
                    }
                }
                else {
                    this.updateLastIndex(currPattern, offset);
                    matchedImage = this.match(currPattern, text, offset);
                }
                if (matchedImage !== null) {
                    // even though this pattern matched we must try a another longer alternative.
                    // this can be used to prioritize keywords over identifiers
                    longerAltIdx = currConfig.longerAlt;
                    if (longerAltIdx !== undefined) {
                        // TODO: micro optimize, avoid extra prop access
                        // by saving/linking longerAlt on the original config?
                        var longerAltConfig = patternIdxToConfig[longerAltIdx];
                        var longerAltPattern = longerAltConfig.pattern;
                        altPayload = null;
                        // single Char can never be a longer alt so no need to test it.
                        // manually in-lined because > 600 chars won't be in-lined in V8
                        if (longerAltConfig.isCustom === true) {
                            match = longerAltPattern.exec(orgText, offset, matchedTokens, groups);
                            if (match !== null) {
                                matchAltImage = match[0];
                                if (match.payload !== undefined) {
                                    altPayload = match.payload;
                                }
                            }
                            else {
                                matchAltImage = null;
                            }
                        }
                        else {
                            this.updateLastIndex(longerAltPattern, offset);
                            matchAltImage = this.match(longerAltPattern, text, offset);
                        }
                        if (matchAltImage &&
                            matchAltImage.length > matchedImage.length) {
                            matchedImage = matchAltImage;
                            payload = altPayload;
                            currConfig = longerAltConfig;
                        }
                    }
                    break;
                }
            }
            // successful match
            if (matchedImage !== null) {
                imageLength = matchedImage.length;
                group = currConfig.group;
                if (group !== undefined) {
                    tokType = currConfig.tokenTypeIdx;
                    // TODO: "offset + imageLength" and the new column may be computed twice in case of "full" location information inside
                    // createFullToken method
                    newToken = this.createTokenInstance(matchedImage, offset, tokType, currConfig.tokenType, line, column, imageLength);
                    this.handlePayload(newToken, payload);
                    // TODO: optimize NOOP in case there are no special groups?
                    if (group === false) {
                        matchedTokensIndex = this.addToken(matchedTokens, matchedTokensIndex, newToken);
                    }
                    else {
                        groups[group].push(newToken);
                    }
                }
                text = this.chopInput(text, imageLength);
                offset = offset + imageLength;
                // TODO: with newlines the column may be assigned twice
                column = this.computeNewColumn(column, imageLength);
                if (trackLines === true &&
                    currConfig.canLineTerminator === true) {
                    var numOfLTsInMatch = 0;
                    var foundTerminator = void 0;
                    var lastLTEndOffset = void 0;
                    lineTerminatorPattern.lastIndex = 0;
                    do {
                        foundTerminator = lineTerminatorPattern.test(matchedImage);
                        if (foundTerminator === true) {
                            lastLTEndOffset =
                                lineTerminatorPattern.lastIndex - 1;
                            numOfLTsInMatch++;
                        }
                    } while (foundTerminator === true);
                    if (numOfLTsInMatch !== 0) {
                        line = line + numOfLTsInMatch;
                        column = imageLength - lastLTEndOffset;
                        this.updateTokenEndLineColumnLocation(newToken, group, lastLTEndOffset, numOfLTsInMatch, line, column, imageLength);
                    }
                }
                // will be NOOP if no modes present
                this.handleModes(currConfig, pop_mode, push_mode, newToken);
            }
            else {
                // error recovery, drop characters until we identify a valid token's start point
                var errorStartOffset = offset;
                var errorLine = line;
                var errorColumn = column;
                var foundResyncPoint = false;
                while (!foundResyncPoint && offset < orgLength) {
                    // drop chars until we succeed in matching something
                    droppedChar = orgText.charCodeAt(offset);
                    // Identity Func (when sticky flag is enabled)
                    text = this.chopInput(text, 1);
                    offset++;
                    for (j = 0; j < currModePatternsLength; j++) {
                        var currConfig_1 = patternIdxToConfig[j];
                        var currPattern = currConfig_1.pattern;
                        // manually in-lined because > 600 chars won't be in-lined in V8
                        var singleCharCode = currConfig_1.short;
                        if (singleCharCode !== false) {
                            if (orgText.charCodeAt(offset) === singleCharCode) {
                                // single character string
                                foundResyncPoint = true;
                            }
                        }
                        else if (currConfig_1.isCustom === true) {
                            foundResyncPoint =
                                currPattern.exec(orgText, offset, matchedTokens, groups) !== null;
                        }
                        else {
                            this.updateLastIndex(currPattern, offset);
                            foundResyncPoint = currPattern.exec(text) !== null;
                        }
                        if (foundResyncPoint === true) {
                            break;
                        }
                    }
                }
                errLength = offset - errorStartOffset;
                // at this point we either re-synced or reached the end of the input text
                msg = this.config.errorMessageProvider.buildUnexpectedCharactersMessage(orgText, errorStartOffset, errLength, errorLine, errorColumn);
                errors.push({
                    offset: errorStartOffset,
                    line: errorLine,
                    column: errorColumn,
                    length: errLength,
                    message: msg
                });
            }
        }
        // if we do have custom patterns which push directly into the
        // TODO: custom tokens should not push directly??
        if (!this.hasCustom) {
            // if we guessed a too large size for the tokens array this will shrink it to the right size.
            matchedTokens.length = matchedTokensIndex;
        }
        return {
            tokens: matchedTokens,
            groups: groups,
            errors: errors
        };
    };
    Lexer.prototype.handleModes = function (config, pop_mode, push_mode, newToken) {
        if (config.pop === true) {
            // need to save the PUSH_MODE property as if the mode is popped
            // patternIdxToPopMode is updated to reflect the new mode after popping the stack
            var pushMode = config.push;
            pop_mode(newToken);
            if (pushMode !== undefined) {
                push_mode.call(this, pushMode);
            }
        }
        else if (config.push !== undefined) {
            push_mode.call(this, config.push);
        }
    };
    Lexer.prototype.chopInput = function (text, length) {
        return text.substring(length);
    };
    Lexer.prototype.updateLastIndex = function (regExp, newLastIndex) {
        regExp.lastIndex = newLastIndex;
    };
    // TODO: decrease this under 600 characters? inspect stripping comments option in TSC compiler
    Lexer.prototype.updateTokenEndLineColumnLocation = function (newToken, group, lastLTIdx, numOfLTsInMatch, line, column, imageLength) {
        var lastCharIsLT, fixForEndingInLT;
        if (group !== undefined) {
            // a none skipped multi line Token, need to update endLine/endColumn
            lastCharIsLT = lastLTIdx === imageLength - 1;
            fixForEndingInLT = lastCharIsLT ? -1 : 0;
            if (!(numOfLTsInMatch === 1 && lastCharIsLT === true)) {
                // if a token ends in a LT that last LT only affects the line numbering of following Tokens
                newToken.endLine = line + fixForEndingInLT;
                // the last LT in a token does not affect the endColumn either as the [columnStart ... columnEnd)
                // inclusive to exclusive range.
                newToken.endColumn = column - 1 + -fixForEndingInLT;
            }
            // else single LT in the last character of a token, no need to modify the endLine/EndColumn
        }
    };
    Lexer.prototype.computeNewColumn = function (oldColumn, imageLength) {
        return oldColumn + imageLength;
    };
    // Place holder, will be replaced by the correct variant according to the locationTracking option at runtime.
    /* istanbul ignore next - place holder */
    Lexer.prototype.createTokenInstance = function () {
        var args = [];
        for (var _i = 0; _i < arguments.length; _i++) {
            args[_i] = arguments[_i];
        }
        return null;
    };
    Lexer.prototype.createOffsetOnlyToken = function (image, startOffset, tokenTypeIdx, tokenType) {
        return {
            image: image,
            startOffset: startOffset,
            tokenTypeIdx: tokenTypeIdx,
            tokenType: tokenType
        };
    };
    Lexer.prototype.createStartOnlyToken = function (image, startOffset, tokenTypeIdx, tokenType, startLine, startColumn) {
        return {
            image: image,
            startOffset: startOffset,
            startLine: startLine,
            startColumn: startColumn,
            tokenTypeIdx: tokenTypeIdx,
            tokenType: tokenType
        };
    };
    Lexer.prototype.createFullToken = function (image, startOffset, tokenTypeIdx, tokenType, startLine, startColumn, imageLength) {
        return {
            image: image,
            startOffset: startOffset,
            endOffset: startOffset + imageLength - 1,
            startLine: startLine,
            endLine: startLine,
            startColumn: startColumn,
            endColumn: startColumn + imageLength - 1,
            tokenTypeIdx: tokenTypeIdx,
            tokenType: tokenType
        };
    };
    // Place holder, will be replaced by the correct variant according to the locationTracking option at runtime.
    /* istanbul ignore next - place holder */
    Lexer.prototype.addToken = function (tokenVector, index, tokenToAdd) {
        return 666;
    };
    Lexer.prototype.addTokenUsingPush = function (tokenVector, index, tokenToAdd) {
        tokenVector.push(tokenToAdd);
        return index;
    };
    Lexer.prototype.addTokenUsingMemberAccess = function (tokenVector, index, tokenToAdd) {
        tokenVector[index] = tokenToAdd;
        index++;
        return index;
    };
    // Place holder, will be replaced by the correct variant according to the hasCustom flag option at runtime.
    /* istanbul ignore next - place holder */
    Lexer.prototype.handlePayload = function (token, payload) { };
    Lexer.prototype.handlePayloadNoCustom = function (token, payload) { };
    Lexer.prototype.handlePayloadWithCustom = function (token, payload) {
        if (payload !== null) {
            token.payload = payload;
        }
    };
    /* istanbul ignore next - place holder to be replaced with chosen alternative at runtime */
    Lexer.prototype.match = function (pattern, text, offset) {
        return null;
    };
    Lexer.prototype.matchWithTest = function (pattern, text, offset) {
        var found = pattern.test(text);
        if (found === true) {
            return text.substring(offset, pattern.lastIndex);
        }
        return null;
    };
    Lexer.prototype.matchWithExec = function (pattern, text) {
        var regExpArray = pattern.exec(text);
        return regExpArray !== null ? regExpArray[0] : regExpArray;
    };
    // Duplicated from the parser's perf trace trait to allow future extraction
    // of the lexer to a separate package.
    Lexer.prototype.TRACE_INIT = function (phaseDesc, phaseImpl) {
        // No need to optimize this using NOOP pattern because
        // It is not called in a hot spot...
        if (this.traceInitPerf === true) {
            this.traceInitIndent++;
            var indent = new Array(this.traceInitIndent + 1).join("\t");
            if (this.traceInitIndent < this.traceInitMaxIdent) {
                console.log(indent + "--> <" + phaseDesc + ">");
            }
            var _a = timer(phaseImpl), time = _a.time, value = _a.value;
            /* istanbul ignore next - Difficult to reproduce specific performance behavior (>10ms) in tests */
            var traceMethod = time > 10 ? console.warn : console.log;
            if (this.traceInitIndent < this.traceInitMaxIdent) {
                traceMethod(indent + "<-- <" + phaseDesc + "> time: " + time + "ms");
            }
            this.traceInitIndent--;
            return value;
        }
        else {
            return phaseImpl();
        }
    };
    Lexer.SKIPPED = "This marks a skipped Token pattern, this means each token identified by it will" +
        "be consumed and then thrown into oblivion, this can be used to for example to completely ignore whitespace.";
    Lexer.NA = /NOT_APPLICABLE/;
    return Lexer;
}());
export { Lexer };
//# sourceMappingURL=lexer_public.js.map