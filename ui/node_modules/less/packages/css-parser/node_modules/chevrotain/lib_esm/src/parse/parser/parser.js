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
import { applyMixins, cloneObj, forEach, has, isEmpty, map, PRINT_WARNING, toFastProperties, values } from "../../utils/utils";
import { computeAllProdsFollows } from "../grammar/follow";
import { createTokenInstance, EOF } from "../../scan/tokens_public";
import { expandAllNestedRuleNames } from "../cst/cst";
import { defaultGrammarValidatorErrorProvider, defaultParserErrorProvider } from "../errors_public";
import { resolveGrammar, validateGrammar } from "../grammar/gast/gast_resolver_public";
import { Recoverable } from "./traits/recoverable";
import { LooksAhead } from "./traits/looksahead";
import { TreeBuilder } from "./traits/tree_builder";
import { LexerAdapter } from "./traits/lexer_adapter";
import { RecognizerApi } from "./traits/recognizer_api";
import { RecognizerEngine } from "./traits/recognizer_engine";
import { ErrorHandler } from "./traits/error_handler";
import { ContentAssist } from "./traits/context_assist";
import { GastRecorder } from "./traits/gast_recorder";
import { PerformanceTracer } from "./traits/perf_tracer";
export var END_OF_FILE = createTokenInstance(EOF, "", NaN, NaN, NaN, NaN, NaN, NaN);
Object.freeze(END_OF_FILE);
export var DEFAULT_PARSER_CONFIG = Object.freeze({
    recoveryEnabled: false,
    maxLookahead: 4,
    ignoredIssues: {},
    dynamicTokensEnabled: false,
    outputCst: true,
    errorMessageProvider: defaultParserErrorProvider,
    nodeLocationTracking: "none",
    traceInitPerf: false,
    skipValidations: false
});
export var DEFAULT_RULE_CONFIG = Object.freeze({
    recoveryValueFunc: function () { return undefined; },
    resyncEnabled: true
});
export var ParserDefinitionErrorType;
(function (ParserDefinitionErrorType) {
    ParserDefinitionErrorType[ParserDefinitionErrorType["INVALID_RULE_NAME"] = 0] = "INVALID_RULE_NAME";
    ParserDefinitionErrorType[ParserDefinitionErrorType["DUPLICATE_RULE_NAME"] = 1] = "DUPLICATE_RULE_NAME";
    ParserDefinitionErrorType[ParserDefinitionErrorType["INVALID_RULE_OVERRIDE"] = 2] = "INVALID_RULE_OVERRIDE";
    ParserDefinitionErrorType[ParserDefinitionErrorType["DUPLICATE_PRODUCTIONS"] = 3] = "DUPLICATE_PRODUCTIONS";
    ParserDefinitionErrorType[ParserDefinitionErrorType["UNRESOLVED_SUBRULE_REF"] = 4] = "UNRESOLVED_SUBRULE_REF";
    ParserDefinitionErrorType[ParserDefinitionErrorType["LEFT_RECURSION"] = 5] = "LEFT_RECURSION";
    ParserDefinitionErrorType[ParserDefinitionErrorType["NONE_LAST_EMPTY_ALT"] = 6] = "NONE_LAST_EMPTY_ALT";
    ParserDefinitionErrorType[ParserDefinitionErrorType["AMBIGUOUS_ALTS"] = 7] = "AMBIGUOUS_ALTS";
    ParserDefinitionErrorType[ParserDefinitionErrorType["CONFLICT_TOKENS_RULES_NAMESPACE"] = 8] = "CONFLICT_TOKENS_RULES_NAMESPACE";
    ParserDefinitionErrorType[ParserDefinitionErrorType["INVALID_TOKEN_NAME"] = 9] = "INVALID_TOKEN_NAME";
    ParserDefinitionErrorType[ParserDefinitionErrorType["INVALID_NESTED_RULE_NAME"] = 10] = "INVALID_NESTED_RULE_NAME";
    ParserDefinitionErrorType[ParserDefinitionErrorType["DUPLICATE_NESTED_NAME"] = 11] = "DUPLICATE_NESTED_NAME";
    ParserDefinitionErrorType[ParserDefinitionErrorType["NO_NON_EMPTY_LOOKAHEAD"] = 12] = "NO_NON_EMPTY_LOOKAHEAD";
    ParserDefinitionErrorType[ParserDefinitionErrorType["AMBIGUOUS_PREFIX_ALTS"] = 13] = "AMBIGUOUS_PREFIX_ALTS";
    ParserDefinitionErrorType[ParserDefinitionErrorType["TOO_MANY_ALTS"] = 14] = "TOO_MANY_ALTS";
})(ParserDefinitionErrorType || (ParserDefinitionErrorType = {}));
export function EMPTY_ALT(value) {
    if (value === void 0) { value = undefined; }
    return function () {
        return value;
    };
}
var Parser = /** @class */ (function () {
    function Parser(tokenVocabulary, config) {
        if (config === void 0) { config = DEFAULT_PARSER_CONFIG; }
        this.ignoredIssues = DEFAULT_PARSER_CONFIG.ignoredIssues;
        this.definitionErrors = [];
        this.selfAnalysisDone = false;
        var that = this;
        that.initErrorHandler(config);
        that.initLexerAdapter();
        that.initLooksAhead(config);
        that.initRecognizerEngine(tokenVocabulary, config);
        that.initRecoverable(config);
        that.initTreeBuilder(config);
        that.initContentAssist();
        that.initGastRecorder(config);
        that.initPerformanceTracer(config);
        /* istanbul ignore if - complete over-kill to test this, we should only add a test when we actually hard deprecate it and throw an error... */
        if (has(config, "ignoredIssues") &&
            config.ignoredIssues !== DEFAULT_PARSER_CONFIG.ignoredIssues) {
            PRINT_WARNING("The <ignoredIssues> IParserConfig property is soft-deprecated and will be removed in future versions.\n\t" +
                "Please use the <IGNORE_AMBIGUITIES> flag on the relevant DSL method instead.");
        }
        this.ignoredIssues = has(config, "ignoredIssues")
            ? config.ignoredIssues
            : DEFAULT_PARSER_CONFIG.ignoredIssues;
        this.skipValidations = has(config, "skipValidations")
            ? config.skipValidations
            : DEFAULT_PARSER_CONFIG.skipValidations;
    }
    /**
     *  @deprecated use the **instance** method with the same name instead
     */
    Parser.performSelfAnalysis = function (parserInstance) {
        ;
        parserInstance.performSelfAnalysis();
    };
    Parser.prototype.performSelfAnalysis = function () {
        var _this = this;
        this.TRACE_INIT("performSelfAnalysis", function () {
            var defErrorsMsgs;
            _this.selfAnalysisDone = true;
            var className = _this.className;
            _this.TRACE_INIT("toFastProps", function () {
                // Without this voodoo magic the parser would be x3-x4 slower
                // It seems it is better to invoke `toFastProperties` **before**
                // Any manipulations of the `this` object done during the recording phase.
                toFastProperties(_this);
            });
            _this.TRACE_INIT("Grammar Recording", function () {
                try {
                    _this.enableRecording();
                    // Building the GAST
                    forEach(_this.definedRulesNames, function (currRuleName) {
                        var wrappedRule = _this[currRuleName];
                        var originalGrammarAction = wrappedRule["originalGrammarAction"];
                        var recordedRuleGast = undefined;
                        _this.TRACE_INIT(currRuleName + " Rule", function () {
                            recordedRuleGast = _this.topLevelRuleRecord(currRuleName, originalGrammarAction);
                        });
                        _this.gastProductionsCache[currRuleName] = recordedRuleGast;
                    });
                }
                finally {
                    _this.disableRecording();
                }
            });
            var resolverErrors = [];
            _this.TRACE_INIT("Grammar Resolving", function () {
                resolverErrors = resolveGrammar({
                    rules: values(_this.gastProductionsCache)
                });
                _this.definitionErrors.push.apply(_this.definitionErrors, resolverErrors); // mutability for the win?
            });
            _this.TRACE_INIT("Grammar Validations", function () {
                // only perform additional grammar validations IFF no resolving errors have occurred.
                // as unresolved grammar may lead to unhandled runtime exceptions in the follow up validations.
                if (isEmpty(resolverErrors) && _this.skipValidations === false) {
                    var validationErrors = validateGrammar({
                        rules: values(_this.gastProductionsCache),
                        maxLookahead: _this.maxLookahead,
                        tokenTypes: values(_this.tokensMap),
                        ignoredIssues: _this.ignoredIssues,
                        errMsgProvider: defaultGrammarValidatorErrorProvider,
                        grammarName: className
                    });
                    _this.definitionErrors.push.apply(_this.definitionErrors, validationErrors); // mutability for the win?
                }
            });
            // this analysis may fail if the grammar is not perfectly valid
            if (isEmpty(_this.definitionErrors)) {
                // The results of these computations are not needed unless error recovery is enabled.
                if (_this.recoveryEnabled) {
                    _this.TRACE_INIT("computeAllProdsFollows", function () {
                        var allFollows = computeAllProdsFollows(values(_this.gastProductionsCache));
                        _this.resyncFollows = allFollows;
                    });
                }
                _this.TRACE_INIT("ComputeLookaheadFunctions", function () {
                    _this.preComputeLookaheadFunctions(values(_this.gastProductionsCache));
                });
            }
            _this.TRACE_INIT("expandAllNestedRuleNames", function () {
                // TODO: is this needed for EmbeddedActionsParser?
                var cstAnalysisResult = expandAllNestedRuleNames(values(_this.gastProductionsCache), _this.fullRuleNameToShort);
                _this.allRuleNames = cstAnalysisResult.allRuleNames;
            });
            if (!Parser.DEFER_DEFINITION_ERRORS_HANDLING &&
                !isEmpty(_this.definitionErrors)) {
                defErrorsMsgs = map(_this.definitionErrors, function (defError) { return defError.message; });
                throw new Error("Parser Definition Errors detected:\n " + defErrorsMsgs.join("\n-------------------------------\n"));
            }
        });
    };
    // Set this flag to true if you don't want the Parser to throw error when problems in it's definition are detected.
    // (normally during the parser's constructor).
    // This is a design time flag, it will not affect the runtime error handling of the parser, just design time errors,
    // for example: duplicate rule names, referencing an unresolved subrule, ect...
    // This flag should not be enabled during normal usage, it is used in special situations, for example when
    // needing to display the parser definition errors in some GUI(online playground).
    Parser.DEFER_DEFINITION_ERRORS_HANDLING = false;
    return Parser;
}());
export { Parser };
applyMixins(Parser, [
    Recoverable,
    LooksAhead,
    TreeBuilder,
    LexerAdapter,
    RecognizerEngine,
    RecognizerApi,
    ErrorHandler,
    ContentAssist,
    GastRecorder,
    PerformanceTracer
]);
var CstParser = /** @class */ (function (_super) {
    __extends(CstParser, _super);
    function CstParser(tokenVocabulary, config) {
        if (config === void 0) { config = DEFAULT_PARSER_CONFIG; }
        var _this = this;
        var configClone = cloneObj(config);
        configClone.outputCst = true;
        _this = _super.call(this, tokenVocabulary, configClone) || this;
        return _this;
    }
    return CstParser;
}(Parser));
export { CstParser };
var EmbeddedActionsParser = /** @class */ (function (_super) {
    __extends(EmbeddedActionsParser, _super);
    function EmbeddedActionsParser(tokenVocabulary, config) {
        if (config === void 0) { config = DEFAULT_PARSER_CONFIG; }
        var _this = this;
        var configClone = cloneObj(config);
        configClone.outputCst = false;
        _this = _super.call(this, tokenVocabulary, configClone) || this;
        return _this;
    }
    return EmbeddedActionsParser;
}(Parser));
export { EmbeddedActionsParser };
//# sourceMappingURL=parser.js.map