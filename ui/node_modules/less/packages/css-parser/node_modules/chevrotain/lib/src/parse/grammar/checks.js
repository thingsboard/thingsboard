"use strict";
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
Object.defineProperty(exports, "__esModule", { value: true });
var utils = require("../../utils/utils");
var utils_1 = require("../../utils/utils");
var parser_1 = require("../parser/parser");
var gast_1 = require("./gast/gast");
var lookahead_1 = require("./lookahead");
var cst_1 = require("../cst/cst");
var interpreter_1 = require("./interpreter");
var gast_public_1 = require("./gast/gast_public");
var gast_visitor_public_1 = require("./gast/gast_visitor_public");
function validateGrammar(topLevels, globalMaxLookahead, tokenTypes, ignoredIssues, errMsgProvider, grammarName) {
    var duplicateErrors = utils.map(topLevels, function (currTopLevel) {
        return validateDuplicateProductions(currTopLevel, errMsgProvider);
    });
    var leftRecursionErrors = utils.map(topLevels, function (currTopRule) {
        return validateNoLeftRecursion(currTopRule, currTopRule, errMsgProvider);
    });
    var emptyAltErrors = [];
    var ambiguousAltsErrors = [];
    var emptyRepetitionErrors = [];
    // left recursion could cause infinite loops in the following validations.
    // It is safest to first have the user fix the left recursion errors first and only then examine Further issues.
    if (utils_1.every(leftRecursionErrors, utils_1.isEmpty)) {
        emptyAltErrors = utils_1.map(topLevels, function (currTopRule) {
            return validateEmptyOrAlternative(currTopRule, errMsgProvider);
        });
        ambiguousAltsErrors = utils_1.map(topLevels, function (currTopRule) {
            return validateAmbiguousAlternationAlternatives(currTopRule, globalMaxLookahead, ignoredIssues, errMsgProvider);
        });
        emptyRepetitionErrors = validateSomeNonEmptyLookaheadPath(topLevels, globalMaxLookahead, errMsgProvider);
    }
    var termsNamespaceConflictErrors = checkTerminalAndNoneTerminalsNameSpace(topLevels, tokenTypes, errMsgProvider);
    var tokenNameErrors = utils.map(tokenTypes, function (currTokType) {
        return validateTokenName(currTokType, errMsgProvider);
    });
    var nestedRulesNameErrors = validateNestedRulesNames(topLevels, errMsgProvider);
    var nestedRulesDuplicateErrors = validateDuplicateNestedRules(topLevels, errMsgProvider);
    var tooManyAltsErrors = utils_1.map(topLevels, function (curRule) {
        return validateTooManyAlts(curRule, errMsgProvider);
    });
    var ruleNameErrors = utils_1.map(topLevels, function (curRule) {
        return validateRuleName(curRule, errMsgProvider);
    });
    var duplicateRulesError = utils_1.map(topLevels, function (curRule) {
        return validateRuleDoesNotAlreadyExist(curRule, topLevels, grammarName, errMsgProvider);
    });
    return (utils.flatten(duplicateErrors.concat(tokenNameErrors, nestedRulesNameErrors, nestedRulesDuplicateErrors, emptyRepetitionErrors, leftRecursionErrors, emptyAltErrors, ambiguousAltsErrors, termsNamespaceConflictErrors, tooManyAltsErrors, ruleNameErrors, duplicateRulesError)));
}
exports.validateGrammar = validateGrammar;
function validateNestedRulesNames(topLevels, errMsgProvider) {
    var result = [];
    utils_1.forEach(topLevels, function (curTopLevel) {
        var namedCollectorVisitor = new cst_1.NamedDSLMethodsCollectorVisitor("");
        curTopLevel.accept(namedCollectorVisitor);
        var nestedProds = utils_1.map(namedCollectorVisitor.result, function (currItem) { return currItem.orgProd; });
        result.push(utils_1.map(nestedProds, function (currNestedProd) {
            return validateNestedRuleName(curTopLevel, currNestedProd, errMsgProvider);
        }));
    });
    return utils_1.flatten(result);
}
function validateDuplicateProductions(topLevelRule, errMsgProvider) {
    var collectorVisitor = new OccurrenceValidationCollector();
    topLevelRule.accept(collectorVisitor);
    var allRuleProductions = collectorVisitor.allProductions;
    var productionGroups = utils.groupBy(allRuleProductions, identifyProductionForDuplicates);
    var duplicates = utils.pick(productionGroups, function (currGroup) {
        return currGroup.length > 1;
    });
    var errors = utils.map(utils.values(duplicates), function (currDuplicates) {
        var firstProd = utils.first(currDuplicates);
        var msg = errMsgProvider.buildDuplicateFoundError(topLevelRule, currDuplicates);
        var dslName = gast_1.getProductionDslName(firstProd);
        var defError = {
            message: msg,
            type: parser_1.ParserDefinitionErrorType.DUPLICATE_PRODUCTIONS,
            ruleName: topLevelRule.name,
            dslName: dslName,
            occurrence: firstProd.idx
        };
        var param = getExtraProductionArgument(firstProd);
        if (param) {
            defError.parameter = param;
        }
        return defError;
    });
    return errors;
}
function identifyProductionForDuplicates(prod) {
    return gast_1.getProductionDslName(prod) + "_#_" + prod.idx + "_#_" + getExtraProductionArgument(prod);
}
exports.identifyProductionForDuplicates = identifyProductionForDuplicates;
function getExtraProductionArgument(prod) {
    if (prod instanceof gast_public_1.Terminal) {
        return prod.terminalType.name;
    }
    else if (prod instanceof gast_public_1.NonTerminal) {
        return prod.nonTerminalName;
    }
    else {
        return "";
    }
}
var OccurrenceValidationCollector = /** @class */ (function (_super) {
    __extends(OccurrenceValidationCollector, _super);
    function OccurrenceValidationCollector() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.allProductions = [];
        return _this;
    }
    OccurrenceValidationCollector.prototype.visitNonTerminal = function (subrule) {
        this.allProductions.push(subrule);
    };
    OccurrenceValidationCollector.prototype.visitOption = function (option) {
        this.allProductions.push(option);
    };
    OccurrenceValidationCollector.prototype.visitRepetitionWithSeparator = function (manySep) {
        this.allProductions.push(manySep);
    };
    OccurrenceValidationCollector.prototype.visitRepetitionMandatory = function (atLeastOne) {
        this.allProductions.push(atLeastOne);
    };
    OccurrenceValidationCollector.prototype.visitRepetitionMandatoryWithSeparator = function (atLeastOneSep) {
        this.allProductions.push(atLeastOneSep);
    };
    OccurrenceValidationCollector.prototype.visitRepetition = function (many) {
        this.allProductions.push(many);
    };
    OccurrenceValidationCollector.prototype.visitAlternation = function (or) {
        this.allProductions.push(or);
    };
    OccurrenceValidationCollector.prototype.visitTerminal = function (terminal) {
        this.allProductions.push(terminal);
    };
    return OccurrenceValidationCollector;
}(gast_visitor_public_1.GAstVisitor));
exports.OccurrenceValidationCollector = OccurrenceValidationCollector;
exports.validTermsPattern = /^[a-zA-Z_]\w*$/;
exports.validNestedRuleName = new RegExp(exports.validTermsPattern.source.replace("^", "^\\$"));
// TODO: remove this limitation now that we use recorders
function validateRuleName(rule, errMsgProvider) {
    var errors = [];
    var ruleName = rule.name;
    if (!ruleName.match(exports.validTermsPattern)) {
        errors.push({
            message: errMsgProvider.buildInvalidRuleNameError({
                topLevelRule: rule,
                expectedPattern: exports.validTermsPattern
            }),
            type: parser_1.ParserDefinitionErrorType.INVALID_RULE_NAME,
            ruleName: ruleName
        });
    }
    return errors;
}
exports.validateRuleName = validateRuleName;
// TODO: did the nested rule name regExp now change?
function validateNestedRuleName(topLevel, nestedProd, errMsgProvider) {
    var errors = [];
    var errMsg;
    if (!nestedProd.name.match(exports.validNestedRuleName)) {
        errMsg = errMsgProvider.buildInvalidNestedRuleNameError(topLevel, nestedProd);
        errors.push({
            message: errMsg,
            type: parser_1.ParserDefinitionErrorType.INVALID_NESTED_RULE_NAME,
            ruleName: topLevel.name
        });
    }
    return errors;
}
exports.validateNestedRuleName = validateNestedRuleName;
// TODO: remove this limitation now that we use recorders
function validateTokenName(tokenType, errMsgProvider) {
    var errors = [];
    var tokTypeName = tokenType.name;
    if (!tokTypeName.match(exports.validTermsPattern)) {
        errors.push({
            message: errMsgProvider.buildTokenNameError({
                tokenType: tokenType,
                expectedPattern: exports.validTermsPattern
            }),
            type: parser_1.ParserDefinitionErrorType.INVALID_TOKEN_NAME
        });
    }
    return errors;
}
exports.validateTokenName = validateTokenName;
function validateRuleDoesNotAlreadyExist(rule, allRules, className, errMsgProvider) {
    var errors = [];
    var occurrences = utils_1.reduce(allRules, function (result, curRule) {
        if (curRule.name === rule.name) {
            return result + 1;
        }
        return result;
    }, 0);
    if (occurrences > 1) {
        var errMsg = errMsgProvider.buildDuplicateRuleNameError({
            topLevelRule: rule,
            grammarName: className
        });
        errors.push({
            message: errMsg,
            type: parser_1.ParserDefinitionErrorType.DUPLICATE_RULE_NAME,
            ruleName: rule.name
        });
    }
    return errors;
}
exports.validateRuleDoesNotAlreadyExist = validateRuleDoesNotAlreadyExist;
// TODO: is there anyway to get only the rule names of rules inherited from the super grammars?
// This is not part of the IGrammarErrorProvider because the validation cannot be performed on
// The grammar structure, only at runtime.
function validateRuleIsOverridden(ruleName, definedRulesNames, className) {
    var errors = [];
    var errMsg;
    if (!utils.contains(definedRulesNames, ruleName)) {
        errMsg =
            "Invalid rule override, rule: ->" + ruleName + "<- cannot be overridden in the grammar: ->" + className + "<-" +
                "as it is not defined in any of the super grammars ";
        errors.push({
            message: errMsg,
            type: parser_1.ParserDefinitionErrorType.INVALID_RULE_OVERRIDE,
            ruleName: ruleName
        });
    }
    return errors;
}
exports.validateRuleIsOverridden = validateRuleIsOverridden;
function validateNoLeftRecursion(topRule, currRule, errMsgProvider, path) {
    if (path === void 0) { path = []; }
    var errors = [];
    var nextNonTerminals = getFirstNoneTerminal(currRule.definition);
    if (utils.isEmpty(nextNonTerminals)) {
        return [];
    }
    else {
        var ruleName = topRule.name;
        var foundLeftRecursion = utils.contains(nextNonTerminals, topRule);
        if (foundLeftRecursion) {
            errors.push({
                message: errMsgProvider.buildLeftRecursionError({
                    topLevelRule: topRule,
                    leftRecursionPath: path
                }),
                type: parser_1.ParserDefinitionErrorType.LEFT_RECURSION,
                ruleName: ruleName
            });
        }
        // we are only looking for cyclic paths leading back to the specific topRule
        // other cyclic paths are ignored, we still need this difference to avoid infinite loops...
        var validNextSteps = utils.difference(nextNonTerminals, path.concat([topRule]));
        var errorsFromNextSteps = utils.map(validNextSteps, function (currRefRule) {
            var newPath = utils.cloneArr(path);
            newPath.push(currRefRule);
            return validateNoLeftRecursion(topRule, currRefRule, errMsgProvider, newPath);
        });
        return errors.concat(utils.flatten(errorsFromNextSteps));
    }
}
exports.validateNoLeftRecursion = validateNoLeftRecursion;
function getFirstNoneTerminal(definition) {
    var result = [];
    if (utils.isEmpty(definition)) {
        return result;
    }
    var firstProd = utils.first(definition);
    /* istanbul ignore else */
    if (firstProd instanceof gast_public_1.NonTerminal) {
        result.push(firstProd.referencedRule);
    }
    else if (firstProd instanceof gast_public_1.Flat ||
        firstProd instanceof gast_public_1.Option ||
        firstProd instanceof gast_public_1.RepetitionMandatory ||
        firstProd instanceof gast_public_1.RepetitionMandatoryWithSeparator ||
        firstProd instanceof gast_public_1.RepetitionWithSeparator ||
        firstProd instanceof gast_public_1.Repetition) {
        result = result.concat(getFirstNoneTerminal(firstProd.definition));
    }
    else if (firstProd instanceof gast_public_1.Alternation) {
        // each sub definition in alternation is a FLAT
        result = utils.flatten(utils.map(firstProd.definition, function (currSubDef) {
            return getFirstNoneTerminal(currSubDef.definition);
        }));
    }
    else if (firstProd instanceof gast_public_1.Terminal) {
        // nothing to see, move along
    }
    else {
        throw Error("non exhaustive match");
    }
    var isFirstOptional = gast_1.isOptionalProd(firstProd);
    var hasMore = definition.length > 1;
    if (isFirstOptional && hasMore) {
        var rest = utils.drop(definition);
        return result.concat(getFirstNoneTerminal(rest));
    }
    else {
        return result;
    }
}
exports.getFirstNoneTerminal = getFirstNoneTerminal;
var OrCollector = /** @class */ (function (_super) {
    __extends(OrCollector, _super);
    function OrCollector() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.alternations = [];
        return _this;
    }
    OrCollector.prototype.visitAlternation = function (node) {
        this.alternations.push(node);
    };
    return OrCollector;
}(gast_visitor_public_1.GAstVisitor));
function validateEmptyOrAlternative(topLevelRule, errMsgProvider) {
    var orCollector = new OrCollector();
    topLevelRule.accept(orCollector);
    var ors = orCollector.alternations;
    var errors = utils.reduce(ors, function (errors, currOr) {
        var exceptLast = utils.dropRight(currOr.definition);
        var currErrors = utils.map(exceptLast, function (currAlternative, currAltIdx) {
            var possibleFirstInAlt = interpreter_1.nextPossibleTokensAfter([currAlternative], [], null, 1);
            if (utils.isEmpty(possibleFirstInAlt)) {
                return {
                    message: errMsgProvider.buildEmptyAlternationError({
                        topLevelRule: topLevelRule,
                        alternation: currOr,
                        emptyChoiceIdx: currAltIdx
                    }),
                    type: parser_1.ParserDefinitionErrorType.NONE_LAST_EMPTY_ALT,
                    ruleName: topLevelRule.name,
                    occurrence: currOr.idx,
                    alternative: currAltIdx + 1
                };
            }
            else {
                return null;
            }
        });
        return errors.concat(utils.compact(currErrors));
    }, []);
    return errors;
}
exports.validateEmptyOrAlternative = validateEmptyOrAlternative;
function validateAmbiguousAlternationAlternatives(topLevelRule, globalMaxLookahead, ignoredIssues, errMsgProvider) {
    var orCollector = new OrCollector();
    topLevelRule.accept(orCollector);
    var ors = orCollector.alternations;
    // TODO: this filtering should be deprecated once we remove the ignoredIssues
    //  IParserConfig property
    var ignoredIssuesForCurrentRule = ignoredIssues[topLevelRule.name];
    if (ignoredIssuesForCurrentRule) {
        ors = utils_1.reject(ors, function (currOr) {
            return ignoredIssuesForCurrentRule[gast_1.getProductionDslName(currOr) +
                (currOr.idx === 0 ? "" : currOr.idx)];
        });
    }
    // New Handling of ignoring ambiguities
    // - https://github.com/SAP/chevrotain/issues/869
    ors = utils_1.reject(ors, function (currOr) { return currOr.ignoreAmbiguities === true; });
    var errors = utils.reduce(ors, function (result, currOr) {
        var currOccurrence = currOr.idx;
        var actualMaxLookahead = currOr.maxLookahead || globalMaxLookahead;
        var alternatives = lookahead_1.getLookaheadPathsForOr(currOccurrence, topLevelRule, actualMaxLookahead, currOr);
        var altsAmbiguityErrors = checkAlternativesAmbiguities(alternatives, currOr, topLevelRule, errMsgProvider);
        var altsPrefixAmbiguityErrors = checkPrefixAlternativesAmbiguities(alternatives, currOr, topLevelRule, errMsgProvider);
        return result.concat(altsAmbiguityErrors, altsPrefixAmbiguityErrors);
    }, []);
    return errors;
}
exports.validateAmbiguousAlternationAlternatives = validateAmbiguousAlternationAlternatives;
var RepetionCollector = /** @class */ (function (_super) {
    __extends(RepetionCollector, _super);
    function RepetionCollector() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.allProductions = [];
        return _this;
    }
    RepetionCollector.prototype.visitRepetitionWithSeparator = function (manySep) {
        this.allProductions.push(manySep);
    };
    RepetionCollector.prototype.visitRepetitionMandatory = function (atLeastOne) {
        this.allProductions.push(atLeastOne);
    };
    RepetionCollector.prototype.visitRepetitionMandatoryWithSeparator = function (atLeastOneSep) {
        this.allProductions.push(atLeastOneSep);
    };
    RepetionCollector.prototype.visitRepetition = function (many) {
        this.allProductions.push(many);
    };
    return RepetionCollector;
}(gast_visitor_public_1.GAstVisitor));
exports.RepetionCollector = RepetionCollector;
function validateTooManyAlts(topLevelRule, errMsgProvider) {
    var orCollector = new OrCollector();
    topLevelRule.accept(orCollector);
    var ors = orCollector.alternations;
    var errors = utils.reduce(ors, function (errors, currOr) {
        if (currOr.definition.length > 255) {
            errors.push({
                message: errMsgProvider.buildTooManyAlternativesError({
                    topLevelRule: topLevelRule,
                    alternation: currOr
                }),
                type: parser_1.ParserDefinitionErrorType.TOO_MANY_ALTS,
                ruleName: topLevelRule.name,
                occurrence: currOr.idx
            });
        }
        return errors;
    }, []);
    return errors;
}
exports.validateTooManyAlts = validateTooManyAlts;
function validateSomeNonEmptyLookaheadPath(topLevelRules, maxLookahead, errMsgProvider) {
    var errors = [];
    utils_1.forEach(topLevelRules, function (currTopRule) {
        var collectorVisitor = new RepetionCollector();
        currTopRule.accept(collectorVisitor);
        var allRuleProductions = collectorVisitor.allProductions;
        utils_1.forEach(allRuleProductions, function (currProd) {
            var prodType = lookahead_1.getProdType(currProd);
            var actualMaxLookahead = currProd.maxLookahead || maxLookahead;
            var currOccurrence = currProd.idx;
            var paths = lookahead_1.getLookaheadPathsForOptionalProd(currOccurrence, currTopRule, prodType, actualMaxLookahead);
            var pathsInsideProduction = paths[0];
            if (utils_1.isEmpty(utils_1.flatten(pathsInsideProduction))) {
                var errMsg = errMsgProvider.buildEmptyRepetitionError({
                    topLevelRule: currTopRule,
                    repetition: currProd
                });
                errors.push({
                    message: errMsg,
                    type: parser_1.ParserDefinitionErrorType.NO_NON_EMPTY_LOOKAHEAD,
                    ruleName: currTopRule.name
                });
            }
        });
    });
    return errors;
}
exports.validateSomeNonEmptyLookaheadPath = validateSomeNonEmptyLookaheadPath;
function checkAlternativesAmbiguities(alternatives, alternation, rule, errMsgProvider) {
    var foundAmbiguousPaths = [];
    var identicalAmbiguities = utils_1.reduce(alternatives, function (result, currAlt, currAltIdx) {
        // ignore (skip) ambiguities with this alternative
        if (alternation.definition[currAltIdx].ignoreAmbiguities === true) {
            return result;
        }
        utils_1.forEach(currAlt, function (currPath) {
            var altsCurrPathAppearsIn = [currAltIdx];
            utils_1.forEach(alternatives, function (currOtherAlt, currOtherAltIdx) {
                if (currAltIdx !== currOtherAltIdx &&
                    lookahead_1.containsPath(currOtherAlt, currPath) &&
                    // ignore (skip) ambiguities with this "other" alternative
                    alternation.definition[currOtherAltIdx]
                        .ignoreAmbiguities !== true) {
                    altsCurrPathAppearsIn.push(currOtherAltIdx);
                }
            });
            if (altsCurrPathAppearsIn.length > 1 &&
                !lookahead_1.containsPath(foundAmbiguousPaths, currPath)) {
                foundAmbiguousPaths.push(currPath);
                result.push({
                    alts: altsCurrPathAppearsIn,
                    path: currPath
                });
            }
        });
        return result;
    }, []);
    var currErrors = utils.map(identicalAmbiguities, function (currAmbDescriptor) {
        var ambgIndices = utils_1.map(currAmbDescriptor.alts, function (currAltIdx) { return currAltIdx + 1; });
        var currMessage = errMsgProvider.buildAlternationAmbiguityError({
            topLevelRule: rule,
            alternation: alternation,
            ambiguityIndices: ambgIndices,
            prefixPath: currAmbDescriptor.path
        });
        return {
            message: currMessage,
            type: parser_1.ParserDefinitionErrorType.AMBIGUOUS_ALTS,
            ruleName: rule.name,
            occurrence: alternation.idx,
            alternatives: [currAmbDescriptor.alts]
        };
    });
    return currErrors;
}
function checkPrefixAlternativesAmbiguities(alternatives, alternation, rule, errMsgProvider) {
    var errors = [];
    // flatten
    var pathsAndIndices = utils_1.reduce(alternatives, function (result, currAlt, idx) {
        var currPathsAndIdx = utils_1.map(currAlt, function (currPath) {
            return { idx: idx, path: currPath };
        });
        return result.concat(currPathsAndIdx);
    }, []);
    utils_1.forEach(pathsAndIndices, function (currPathAndIdx) {
        var alternativeGast = alternation.definition[currPathAndIdx.idx];
        // ignore (skip) ambiguities with this alternative
        if (alternativeGast.ignoreAmbiguities === true) {
            return;
        }
        var targetIdx = currPathAndIdx.idx;
        var targetPath = currPathAndIdx.path;
        var prefixAmbiguitiesPathsAndIndices = utils_1.findAll(pathsAndIndices, function (searchPathAndIdx) {
            // prefix ambiguity can only be created from lower idx (higher priority) path
            return (
            // ignore (skip) ambiguities with this "other" alternative
            alternation.definition[searchPathAndIdx.idx]
                .ignoreAmbiguities !== true &&
                searchPathAndIdx.idx < targetIdx &&
                // checking for strict prefix because identical lookaheads
                // will be be detected using a different validation.
                lookahead_1.isStrictPrefixOfPath(searchPathAndIdx.path, targetPath));
        });
        var currPathPrefixErrors = utils_1.map(prefixAmbiguitiesPathsAndIndices, function (currAmbPathAndIdx) {
            var ambgIndices = [currAmbPathAndIdx.idx + 1, targetIdx + 1];
            var occurrence = alternation.idx === 0 ? "" : alternation.idx;
            var message = errMsgProvider.buildAlternationPrefixAmbiguityError({
                topLevelRule: rule,
                alternation: alternation,
                ambiguityIndices: ambgIndices,
                prefixPath: currAmbPathAndIdx.path
            });
            return {
                message: message,
                type: parser_1.ParserDefinitionErrorType.AMBIGUOUS_PREFIX_ALTS,
                ruleName: rule.name,
                occurrence: occurrence,
                alternatives: ambgIndices
            };
        });
        errors = errors.concat(currPathPrefixErrors);
    });
    return errors;
}
exports.checkPrefixAlternativesAmbiguities = checkPrefixAlternativesAmbiguities;
function checkTerminalAndNoneTerminalsNameSpace(topLevels, tokenTypes, errMsgProvider) {
    var errors = [];
    var tokenNames = utils_1.map(tokenTypes, function (currToken) { return currToken.name; });
    utils_1.forEach(topLevels, function (currRule) {
        var currRuleName = currRule.name;
        if (utils_1.contains(tokenNames, currRuleName)) {
            var errMsg = errMsgProvider.buildNamespaceConflictError(currRule);
            errors.push({
                message: errMsg,
                type: parser_1.ParserDefinitionErrorType.CONFLICT_TOKENS_RULES_NAMESPACE,
                ruleName: currRuleName
            });
        }
    });
    return errors;
}
function validateDuplicateNestedRules(topLevelRules, errMsgProvider) {
    var errors = [];
    utils_1.forEach(topLevelRules, function (currTopRule) {
        var namedCollectorVisitor = new cst_1.NamedDSLMethodsCollectorVisitor("");
        currTopRule.accept(namedCollectorVisitor);
        var prodsByGroup = utils_1.groupBy(namedCollectorVisitor.result, function (item) { return item.name; });
        var duplicates = utils_1.pick(prodsByGroup, function (currGroup) {
            return currGroup.length > 1;
        });
        utils_1.forEach(utils_1.values(duplicates), function (currDupGroup) {
            var currDupProds = utils_1.map(currDupGroup, function (dupGroup) { return dupGroup.orgProd; });
            var errMsg = errMsgProvider.buildDuplicateNestedRuleNameError(currTopRule, currDupProds);
            errors.push({
                message: errMsg,
                type: parser_1.ParserDefinitionErrorType.DUPLICATE_NESTED_NAME,
                ruleName: currTopRule.name
            });
        });
    });
    return errors;
}
//# sourceMappingURL=checks.js.map