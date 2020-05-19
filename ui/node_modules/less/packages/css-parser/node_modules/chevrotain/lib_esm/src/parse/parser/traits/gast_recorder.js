import { forEach, has, isArray, isFunction, peek, some } from "../../../utils/utils";
import { Alternation, Flat, NonTerminal, Option, Repetition, RepetitionMandatory, RepetitionMandatoryWithSeparator, RepetitionWithSeparator, Rule, Terminal } from "../../grammar/gast/gast_public";
import { Lexer } from "../../../scan/lexer_public";
import { augmentTokenTypes, hasShortKeyProperty } from "../../../scan/tokens";
import { createToken, createTokenInstance } from "../../../scan/tokens_public";
import { END_OF_FILE } from "../parser";
import { BITS_FOR_OCCURRENCE_IDX } from "../../grammar/keys";
var RECORDING_NULL_OBJECT = {
    description: "This Object indicates the Parser is during Recording Phase"
};
Object.freeze(RECORDING_NULL_OBJECT);
var HANDLE_SEPARATOR = true;
var MAX_METHOD_IDX = Math.pow(2, BITS_FOR_OCCURRENCE_IDX) - 1;
var RFT = createToken({ name: "RECORDING_PHASE_TOKEN", pattern: Lexer.NA });
augmentTokenTypes([RFT]);
var RECORDING_PHASE_TOKEN = createTokenInstance(RFT, "This IToken indicates the Parser is in Recording Phase\n\t" +
    "" +
    "See: https://sap.github.io/chevrotain/docs/guide/internals.html#grammar-recording for details", 
// Using "-1" instead of NaN (as in EOF) because an actual number is less likely to
// cause errors if the output of LA or CONSUME would be (incorrectly) used during the recording phase.
-1, -1, -1, -1, -1, -1);
Object.freeze(RECORDING_PHASE_TOKEN);
var RECORDING_PHASE_CSTNODE = {
    name: "This CSTNode indicates the Parser is in Recording Phase\n\t" +
        "See: https://sap.github.io/chevrotain/docs/guide/internals.html#grammar-recording for details",
    children: {}
};
/**
 * This trait handles the creation of the GAST structure for Chevrotain Grammars
 */
var GastRecorder = /** @class */ (function () {
    function GastRecorder() {
    }
    GastRecorder.prototype.initGastRecorder = function (config) {
        this.recordingProdStack = [];
        this.RECORDING_PHASE = false;
    };
    GastRecorder.prototype.enableRecording = function () {
        var _this = this;
        this.RECORDING_PHASE = true;
        this.TRACE_INIT("Enable Recording", function () {
            var _loop_1 = function (i) {
                var idx = i > 0 ? i : "";
                _this["CONSUME" + idx] = function (arg1, arg2) {
                    return this.consumeInternalRecord(arg1, i, arg2);
                };
                _this["SUBRULE" + idx] = function (arg1, arg2) {
                    return this.subruleInternalRecord(arg1, i, arg2);
                };
                _this["OPTION" + idx] = function (arg1) {
                    return this.optionInternalRecord(arg1, i);
                };
                _this["OR" + idx] = function (arg1) {
                    return this.orInternalRecord(arg1, i);
                };
                _this["MANY" + idx] = function (arg1) {
                    this.manyInternalRecord(i, arg1);
                };
                _this["MANY_SEP" + idx] = function (arg1) {
                    this.manySepFirstInternalRecord(i, arg1);
                };
                _this["AT_LEAST_ONE" + idx] = function (arg1) {
                    this.atLeastOneInternalRecord(i, arg1);
                };
                _this["AT_LEAST_ONE_SEP" + idx] = function (arg1) {
                    this.atLeastOneSepFirstInternalRecord(i, arg1);
                };
            };
            /**
             * Warning Dark Voodoo Magic upcoming!
             * We are "replacing" the public parsing DSL methods API
             * With **new** alternative implementations on the Parser **instance**
             *
             * So far this is the only way I've found to avoid performance regressions during parsing time.
             * - Approx 30% performance regression was measured on Chrome 75 Canary when attempting to replace the "internal"
             *   implementations directly instead.
             */
            for (var i = 0; i < 10; i++) {
                _loop_1(i);
            }
            // DSL methods with the idx(suffix) as an argument
            _this["consume"] = function (idx, arg1, arg2) {
                return this.consumeInternalRecord(arg1, idx, arg2);
            };
            _this["subrule"] = function (idx, arg1, arg2) {
                return this.subruleInternalRecord(arg1, idx, arg2);
            };
            _this["option"] = function (idx, arg1) {
                return this.optionInternalRecord(arg1, idx);
            };
            _this["or"] = function (idx, arg1) {
                return this.orInternalRecord(arg1, idx);
            };
            _this["many"] = function (idx, arg1) {
                this.manyInternalRecord(idx, arg1);
            };
            _this["atLeastOne"] = function (idx, arg1) {
                this.atLeastOneInternalRecord(idx, arg1);
            };
            _this.ACTION = _this.ACTION_RECORD;
            _this.BACKTRACK = _this.BACKTRACK_RECORD;
            _this.LA = _this.LA_RECORD;
        });
    };
    GastRecorder.prototype.disableRecording = function () {
        var _this = this;
        this.RECORDING_PHASE = false;
        // By deleting these **instance** properties, any future invocation
        // will be deferred to the original methods on the **prototype** object
        // This seems to get rid of any incorrect optimizations that V8 may
        // do during the recording phase.
        this.TRACE_INIT("Deleting Recording methods", function () {
            for (var i = 0; i < 10; i++) {
                var idx = i > 0 ? i : "";
                delete _this["CONSUME" + idx];
                delete _this["SUBRULE" + idx];
                delete _this["OPTION" + idx];
                delete _this["OR" + idx];
                delete _this["MANY" + idx];
                delete _this["MANY_SEP" + idx];
                delete _this["AT_LEAST_ONE" + idx];
                delete _this["AT_LEAST_ONE_SEP" + idx];
            }
            delete _this["consume"];
            delete _this["subrule"];
            delete _this["option"];
            delete _this["or"];
            delete _this["many"];
            delete _this["atLeastOne"];
            delete _this.ACTION;
            delete _this.BACKTRACK;
            delete _this.LA;
        });
    };
    // TODO: is there any way to use this method to check no
    //   Parser methods are called inside an ACTION?
    //   Maybe try/catch/finally on ACTIONS while disabling the recorders state changes?
    GastRecorder.prototype.ACTION_RECORD = function (impl) {
        // NO-OP during recording
        return;
    };
    // Executing backtracking logic will break our recording logic assumptions
    GastRecorder.prototype.BACKTRACK_RECORD = function (grammarRule, args) {
        return function () { return true; };
    };
    // LA is part of the official API and may be used for custom lookahead logic
    // by end users who may forget to wrap it in ACTION or inside a GATE
    GastRecorder.prototype.LA_RECORD = function (howMuch) {
        // We cannot use the RECORD_PHASE_TOKEN here because someone may depend
        // On LA return EOF at the end of the input so an infinite loop may occur.
        return END_OF_FILE;
    };
    GastRecorder.prototype.topLevelRuleRecord = function (name, def) {
        try {
            var newTopLevelRule = new Rule({ definition: [], name: name });
            newTopLevelRule.name = name;
            this.recordingProdStack.push(newTopLevelRule);
            def.call(this);
            this.recordingProdStack.pop();
            return newTopLevelRule;
        }
        catch (originalError) {
            if (originalError.KNOWN_RECORDER_ERROR !== true) {
                try {
                    originalError.message =
                        originalError.message +
                            '\n\t This error was thrown during the "grammar recording phase" For more info see:\n\t' +
                            "https://sap.github.io/chevrotain/docs/guide/internals.html#grammar-recording";
                }
                catch (mutabilityError) {
                    // We may not be able to modify the original error object
                    throw originalError;
                }
            }
            throw originalError;
        }
    };
    // Implementation of parsing DSL
    GastRecorder.prototype.optionInternalRecord = function (actionORMethodDef, occurrence) {
        return recordProd.call(this, Option, actionORMethodDef, occurrence);
    };
    GastRecorder.prototype.atLeastOneInternalRecord = function (occurrence, actionORMethodDef) {
        recordProd.call(this, RepetitionMandatory, actionORMethodDef, occurrence);
    };
    GastRecorder.prototype.atLeastOneSepFirstInternalRecord = function (occurrence, options) {
        recordProd.call(this, RepetitionMandatoryWithSeparator, options, occurrence, HANDLE_SEPARATOR);
    };
    GastRecorder.prototype.manyInternalRecord = function (occurrence, actionORMethodDef) {
        recordProd.call(this, Repetition, actionORMethodDef, occurrence);
    };
    GastRecorder.prototype.manySepFirstInternalRecord = function (occurrence, options) {
        recordProd.call(this, RepetitionWithSeparator, options, occurrence, HANDLE_SEPARATOR);
    };
    GastRecorder.prototype.orInternalRecord = function (altsOrOpts, occurrence) {
        return recordOrProd.call(this, altsOrOpts, occurrence);
    };
    GastRecorder.prototype.subruleInternalRecord = function (ruleToCall, occurrence, options) {
        assertMethodIdxIsValid(occurrence);
        if (!ruleToCall || has(ruleToCall, "ruleName") === false) {
            var error = new Error("<SUBRULE" + getIdxSuffix(occurrence) + "> argument is invalid" +
                (" expecting a Parser method reference but got: <" + JSON.stringify(ruleToCall) + ">") +
                ("\n inside top level rule: <" + this.recordingProdStack[0].name + ">"));
            error.KNOWN_RECORDER_ERROR = true;
            throw error;
        }
        var prevProd = peek(this.recordingProdStack);
        var ruleName = ruleToCall["ruleName"];
        var newNoneTerminal = new NonTerminal({
            idx: occurrence,
            nonTerminalName: ruleName,
            // The resolving of the `referencedRule` property will be done once all the Rule's GASTs have been created
            referencedRule: undefined
        });
        prevProd.definition.push(newNoneTerminal);
        return this.outputCst
            ? RECORDING_PHASE_CSTNODE
            : RECORDING_NULL_OBJECT;
    };
    GastRecorder.prototype.consumeInternalRecord = function (tokType, occurrence, options) {
        assertMethodIdxIsValid(occurrence);
        if (!hasShortKeyProperty(tokType)) {
            var error = new Error("<CONSUME" + getIdxSuffix(occurrence) + "> argument is invalid" +
                (" expecting a TokenType reference but got: <" + JSON.stringify(tokType) + ">") +
                ("\n inside top level rule: <" + this.recordingProdStack[0].name + ">"));
            error.KNOWN_RECORDER_ERROR = true;
            throw error;
        }
        var prevProd = peek(this.recordingProdStack);
        var newNoneTerminal = new Terminal({
            idx: occurrence,
            terminalType: tokType
        });
        prevProd.definition.push(newNoneTerminal);
        return RECORDING_PHASE_TOKEN;
    };
    return GastRecorder;
}());
export { GastRecorder };
function recordProd(prodConstructor, mainProdArg, occurrence, handleSep) {
    if (handleSep === void 0) { handleSep = false; }
    assertMethodIdxIsValid(occurrence);
    var prevProd = peek(this.recordingProdStack);
    var grammarAction = isFunction(mainProdArg)
        ? mainProdArg
        : mainProdArg.DEF;
    var newProd = new prodConstructor({ definition: [], idx: occurrence });
    if (has(mainProdArg, "NAME")) {
        newProd.name = mainProdArg.NAME;
    }
    if (handleSep) {
        newProd.separator = mainProdArg.SEP;
    }
    if (has(mainProdArg, "MAX_LOOKAHEAD")) {
        newProd.maxLookahead = mainProdArg.MAX_LOOKAHEAD;
    }
    this.recordingProdStack.push(newProd);
    grammarAction.call(this);
    prevProd.definition.push(newProd);
    this.recordingProdStack.pop();
    return RECORDING_NULL_OBJECT;
}
function recordOrProd(mainProdArg, occurrence) {
    var _this = this;
    assertMethodIdxIsValid(occurrence);
    var prevProd = peek(this.recordingProdStack);
    // Only an array of alternatives
    var hasOptions = isArray(mainProdArg) === false;
    var alts = hasOptions === false ? mainProdArg : mainProdArg.DEF;
    var newOrProd = new Alternation({
        definition: [],
        idx: occurrence,
        ignoreAmbiguities: hasOptions && mainProdArg.IGNORE_AMBIGUITIES === true
    });
    if (has(mainProdArg, "NAME")) {
        newOrProd.name = mainProdArg.NAME;
    }
    if (has(mainProdArg, "MAX_LOOKAHEAD")) {
        newOrProd.maxLookahead = mainProdArg.MAX_LOOKAHEAD;
    }
    var hasPredicates = some(alts, function (currAlt) { return isFunction(currAlt.GATE); });
    newOrProd.hasPredicates = hasPredicates;
    prevProd.definition.push(newOrProd);
    forEach(alts, function (currAlt) {
        var currAltFlat = new Flat({ definition: [] });
        newOrProd.definition.push(currAltFlat);
        if (has(currAlt, "NAME")) {
            currAltFlat.name = currAlt.NAME;
        }
        if (has(currAlt, "IGNORE_AMBIGUITIES")) {
            currAltFlat.ignoreAmbiguities = currAlt.IGNORE_AMBIGUITIES;
        }
        // **implicit** ignoreAmbiguities due to usage of gate
        else if (has(currAlt, "GATE")) {
            currAltFlat.ignoreAmbiguities = true;
        }
        _this.recordingProdStack.push(currAltFlat);
        currAlt.ALT.call(_this);
        _this.recordingProdStack.pop();
    });
    return RECORDING_NULL_OBJECT;
}
function getIdxSuffix(idx) {
    return idx === 0 ? "" : "" + idx;
}
function assertMethodIdxIsValid(idx) {
    if (idx < 0 || idx > MAX_METHOD_IDX) {
        var error = new Error(
        // The stack trace will contain all the needed details
        "Invalid DSL Method idx value: <" + idx + ">\n\t" +
            ("Idx value must be a none negative value smaller than " + (MAX_METHOD_IDX +
                1)));
        error.KNOWN_RECORDER_ERROR = true;
        throw error;
    }
}
//# sourceMappingURL=gast_recorder.js.map