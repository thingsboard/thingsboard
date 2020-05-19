import { buildAlternativesLookAheadFunc, buildLookaheadFuncForOptionalProd, buildLookaheadFuncForOr, buildSingleAlternativeLookaheadFunction, PROD_TYPE } from "../../grammar/lookahead";
import { forEach, has, isES2015MapSupported } from "../../../utils/utils";
import { DEFAULT_PARSER_CONFIG } from "../parser";
import { AT_LEAST_ONE_IDX, AT_LEAST_ONE_SEP_IDX, getKeyForAutomaticLookahead, MANY_IDX, MANY_SEP_IDX, OPTION_IDX, OR_IDX } from "../../grammar/keys";
import { collectMethods, getProductionDslName } from "../../grammar/gast/gast";
/**
 * Trait responsible for the lookahead related utilities and optimizations.
 */
var LooksAhead = /** @class */ (function () {
    function LooksAhead() {
    }
    LooksAhead.prototype.initLooksAhead = function (config) {
        this.dynamicTokensEnabled = has(config, "dynamicTokensEnabled")
            ? config.dynamicTokensEnabled
            : DEFAULT_PARSER_CONFIG.dynamicTokensEnabled;
        this.maxLookahead = has(config, "maxLookahead")
            ? config.maxLookahead
            : DEFAULT_PARSER_CONFIG.maxLookahead;
        /* istanbul ignore next - Using plain array as dictionary will be tested on older node.js versions and IE11 */
        this.lookAheadFuncsCache = isES2015MapSupported() ? new Map() : [];
        // Performance optimization on newer engines that support ES6 Map
        // For larger Maps this is slightly faster than using a plain object (array in our case).
        /* istanbul ignore else - The else branch will be tested on older node.js versions and IE11 */
        if (isES2015MapSupported()) {
            this.getLaFuncFromCache = this.getLaFuncFromMap;
            this.setLaFuncCache = this.setLaFuncCacheUsingMap;
        }
        else {
            this.getLaFuncFromCache = this.getLaFuncFromObj;
            this.setLaFuncCache = this.setLaFuncUsingObj;
        }
    };
    LooksAhead.prototype.preComputeLookaheadFunctions = function (rules) {
        var _this = this;
        forEach(rules, function (currRule) {
            _this.TRACE_INIT(currRule.name + " Rule Lookahead", function () {
                var _a = collectMethods(currRule), alternation = _a.alternation, repetition = _a.repetition, option = _a.option, repetitionMandatory = _a.repetitionMandatory, repetitionMandatoryWithSeparator = _a.repetitionMandatoryWithSeparator, repetitionWithSeparator = _a.repetitionWithSeparator;
                forEach(alternation, function (currProd) {
                    var prodIdx = currProd.idx === 0 ? "" : currProd.idx;
                    _this.TRACE_INIT("" + getProductionDslName(currProd) + prodIdx, function () {
                        var laFunc = buildLookaheadFuncForOr(currProd.idx, currRule, currProd.maxLookahead || _this.maxLookahead, currProd.hasPredicates, _this.dynamicTokensEnabled, _this.lookAheadBuilderForAlternatives);
                        var key = getKeyForAutomaticLookahead(_this.fullRuleNameToShort[currRule.name], OR_IDX, currProd.idx);
                        _this.setLaFuncCache(key, laFunc);
                    });
                });
                forEach(repetition, function (currProd) {
                    _this.computeLookaheadFunc(currRule, currProd.idx, MANY_IDX, PROD_TYPE.REPETITION, currProd.maxLookahead, getProductionDslName(currProd));
                });
                forEach(option, function (currProd) {
                    _this.computeLookaheadFunc(currRule, currProd.idx, OPTION_IDX, PROD_TYPE.OPTION, currProd.maxLookahead, getProductionDslName(currProd));
                });
                forEach(repetitionMandatory, function (currProd) {
                    _this.computeLookaheadFunc(currRule, currProd.idx, AT_LEAST_ONE_IDX, PROD_TYPE.REPETITION_MANDATORY, currProd.maxLookahead, getProductionDslName(currProd));
                });
                forEach(repetitionMandatoryWithSeparator, function (currProd) {
                    _this.computeLookaheadFunc(currRule, currProd.idx, AT_LEAST_ONE_SEP_IDX, PROD_TYPE.REPETITION_MANDATORY_WITH_SEPARATOR, currProd.maxLookahead, getProductionDslName(currProd));
                });
                forEach(repetitionWithSeparator, function (currProd) {
                    _this.computeLookaheadFunc(currRule, currProd.idx, MANY_SEP_IDX, PROD_TYPE.REPETITION_WITH_SEPARATOR, currProd.maxLookahead, getProductionDslName(currProd));
                });
            });
        });
    };
    LooksAhead.prototype.computeLookaheadFunc = function (rule, prodOccurrence, prodKey, prodType, prodMaxLookahead, dslMethodName) {
        var _this = this;
        this.TRACE_INIT("" + dslMethodName + (prodOccurrence === 0 ? "" : prodOccurrence), function () {
            var laFunc = buildLookaheadFuncForOptionalProd(prodOccurrence, rule, prodMaxLookahead || _this.maxLookahead, _this.dynamicTokensEnabled, prodType, _this.lookAheadBuilderForOptional);
            var key = getKeyForAutomaticLookahead(_this.fullRuleNameToShort[rule.name], prodKey, prodOccurrence);
            _this.setLaFuncCache(key, laFunc);
        });
    };
    LooksAhead.prototype.lookAheadBuilderForOptional = function (alt, tokenMatcher, dynamicTokensEnabled) {
        return buildSingleAlternativeLookaheadFunction(alt, tokenMatcher, dynamicTokensEnabled);
    };
    LooksAhead.prototype.lookAheadBuilderForAlternatives = function (alts, hasPredicates, tokenMatcher, dynamicTokensEnabled) {
        return buildAlternativesLookAheadFunc(alts, hasPredicates, tokenMatcher, dynamicTokensEnabled);
    };
    // this actually returns a number, but it is always used as a string (object prop key)
    LooksAhead.prototype.getKeyForAutomaticLookahead = function (dslMethodIdx, occurrence) {
        var currRuleShortName = this.getLastExplicitRuleShortName();
        return getKeyForAutomaticLookahead(currRuleShortName, dslMethodIdx, occurrence);
    };
    /* istanbul ignore next */
    LooksAhead.prototype.getLaFuncFromCache = function (key) {
        return undefined;
    };
    LooksAhead.prototype.getLaFuncFromMap = function (key) {
        return this.lookAheadFuncsCache.get(key);
    };
    /* istanbul ignore next - Using plain array as dictionary will be tested on older node.js versions and IE11 */
    LooksAhead.prototype.getLaFuncFromObj = function (key) {
        return this.lookAheadFuncsCache[key];
    };
    /* istanbul ignore next */
    LooksAhead.prototype.setLaFuncCache = function (key, value) { };
    LooksAhead.prototype.setLaFuncCacheUsingMap = function (key, value) {
        this.lookAheadFuncsCache.set(key, value);
    };
    /* istanbul ignore next - Using plain array as dictionary will be tested on older node.js versions and IE11 */
    LooksAhead.prototype.setLaFuncUsingObj = function (key, value) {
        this.lookAheadFuncsCache[key] = value;
    };
    return LooksAhead;
}());
export { LooksAhead };
//# sourceMappingURL=looksahead.js.map