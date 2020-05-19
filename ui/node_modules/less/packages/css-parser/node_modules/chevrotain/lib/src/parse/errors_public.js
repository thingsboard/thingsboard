"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var tokens_public_1 = require("../scan/tokens_public");
var utils = require("../utils/utils");
var utils_1 = require("../utils/utils");
var gast_public_1 = require("./grammar/gast/gast_public");
var gast_1 = require("./grammar/gast/gast");
var checks_1 = require("./grammar/checks");
exports.defaultParserErrorProvider = {
    buildMismatchTokenMessage: function (_a) {
        var expected = _a.expected, actual = _a.actual, previous = _a.previous, ruleName = _a.ruleName;
        var hasLabel = tokens_public_1.hasTokenLabel(expected);
        var expectedMsg = hasLabel
            ? "--> " + tokens_public_1.tokenLabel(expected) + " <--"
            : "token of type --> " + expected.name + " <--";
        var msg = "Expecting " + expectedMsg + " but found --> '" + actual.image + "' <--";
        return msg;
    },
    buildNotAllInputParsedMessage: function (_a) {
        var firstRedundant = _a.firstRedundant, ruleName = _a.ruleName;
        return ("Redundant input, expecting EOF but found: " + firstRedundant.image);
    },
    buildNoViableAltMessage: function (_a) {
        var expectedPathsPerAlt = _a.expectedPathsPerAlt, actual = _a.actual, previous = _a.previous, customUserDescription = _a.customUserDescription, ruleName = _a.ruleName;
        var errPrefix = "Expecting: ";
        // TODO: issue: No Viable Alternative Error may have incomplete details. #502
        var actualText = utils_1.first(actual).image;
        var errSuffix = "\nbut found: '" + actualText + "'";
        if (customUserDescription) {
            return errPrefix + customUserDescription + errSuffix;
        }
        else {
            var allLookAheadPaths = utils_1.reduce(expectedPathsPerAlt, function (result, currAltPaths) { return result.concat(currAltPaths); }, []);
            var nextValidTokenSequences = utils_1.map(allLookAheadPaths, function (currPath) {
                return "[" + utils_1.map(currPath, function (currTokenType) {
                    return tokens_public_1.tokenLabel(currTokenType);
                }).join(", ") + "]";
            });
            var nextValidSequenceItems = utils_1.map(nextValidTokenSequences, function (itemMsg, idx) { return "  " + (idx + 1) + ". " + itemMsg; });
            var calculatedDescription = "one of these possible Token sequences:\n" + nextValidSequenceItems.join("\n");
            return errPrefix + calculatedDescription + errSuffix;
        }
    },
    buildEarlyExitMessage: function (_a) {
        var expectedIterationPaths = _a.expectedIterationPaths, actual = _a.actual, customUserDescription = _a.customUserDescription, ruleName = _a.ruleName;
        var errPrefix = "Expecting: ";
        // TODO: issue: No Viable Alternative Error may have incomplete details. #502
        var actualText = utils_1.first(actual).image;
        var errSuffix = "\nbut found: '" + actualText + "'";
        if (customUserDescription) {
            return errPrefix + customUserDescription + errSuffix;
        }
        else {
            var nextValidTokenSequences = utils_1.map(expectedIterationPaths, function (currPath) {
                return "[" + utils_1.map(currPath, function (currTokenType) {
                    return tokens_public_1.tokenLabel(currTokenType);
                }).join(",") + "]";
            });
            var calculatedDescription = "expecting at least one iteration which starts with one of these possible Token sequences::\n  " +
                ("<" + nextValidTokenSequences.join(" ,") + ">");
            return errPrefix + calculatedDescription + errSuffix;
        }
    }
};
Object.freeze(exports.defaultParserErrorProvider);
exports.defaultGrammarResolverErrorProvider = {
    buildRuleNotFoundError: function (topLevelRule, undefinedRule) {
        var msg = "Invalid grammar, reference to a rule which is not defined: ->" +
            undefinedRule.nonTerminalName +
            "<-\n" +
            "inside top level rule: ->" +
            topLevelRule.name +
            "<-";
        return msg;
    }
};
exports.defaultGrammarValidatorErrorProvider = {
    buildDuplicateFoundError: function (topLevelRule, duplicateProds) {
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
        var topLevelName = topLevelRule.name;
        var duplicateProd = utils_1.first(duplicateProds);
        var index = duplicateProd.idx;
        var dslName = gast_1.getProductionDslName(duplicateProd);
        var extraArgument = getExtraProductionArgument(duplicateProd);
        var hasExplicitIndex = index > 0;
        var msg = "->" + dslName + (hasExplicitIndex ? index : "") + "<- " + (extraArgument ? "with argument: ->" + extraArgument + "<-" : "") + "\n                  appears more than once (" + duplicateProds.length + " times) in the top level rule: ->" + topLevelName + "<-.                  \n                  For further details see: https://sap.github.io/chevrotain/docs/FAQ.html#NUMERICAL_SUFFIXES \n                  ";
        // white space trimming time! better to trim afterwards as it allows to use WELL formatted multi line template strings...
        msg = msg.replace(/[ \t]+/g, " ");
        msg = msg.replace(/\s\s+/g, "\n");
        return msg;
    },
    buildInvalidNestedRuleNameError: function (topLevelRule, nestedProd) {
        var msg = "Invalid nested rule name: ->" + nestedProd.name + "<- inside rule: ->" + topLevelRule.name + "<-\n" +
            ("it must match the pattern: ->" + checks_1.validNestedRuleName.toString() + "<-.\n") +
            "Note that this means a nested rule name must start with the '$'(dollar) sign.";
        return msg;
    },
    buildDuplicateNestedRuleNameError: function (topLevelRule, nestedProd) {
        var duplicateName = utils_1.first(nestedProd).name;
        var errMsg = "Duplicate nested rule name: ->" + duplicateName + "<- inside rule: ->" + topLevelRule.name + "<-\n" +
            "A nested name must be unique in the scope of a top level grammar rule.";
        return errMsg;
    },
    buildNamespaceConflictError: function (rule) {
        var errMsg = "Namespace conflict found in grammar.\n" +
            ("The grammar has both a Terminal(Token) and a Non-Terminal(Rule) named: <" + rule.name + ">.\n") +
            "To resolve this make sure each Terminal and Non-Terminal names are unique\n" +
            "This is easy to accomplish by using the convention that Terminal names start with an uppercase letter\n" +
            "and Non-Terminal names start with a lower case letter.";
        return errMsg;
    },
    buildAlternationPrefixAmbiguityError: function (options) {
        var pathMsg = utils_1.map(options.prefixPath, function (currTok) {
            return tokens_public_1.tokenLabel(currTok);
        }).join(", ");
        var occurrence = options.alternation.idx === 0 ? "" : options.alternation.idx;
        var errMsg = "Ambiguous alternatives: <" + options.ambiguityIndices.join(" ,") + "> due to common lookahead prefix\n" +
            ("in <OR" + occurrence + "> inside <" + options.topLevelRule.name + "> Rule,\n") +
            ("<" + pathMsg + "> may appears as a prefix path in all these alternatives.\n") +
            "See: https://sap.github.io/chevrotain/docs/guide/resolving_grammar_errors.html#COMMON_PREFIX\n" +
            "For Further details.";
        return errMsg;
    },
    buildAlternationAmbiguityError: function (options) {
        var pathMsg = utils_1.map(options.prefixPath, function (currtok) {
            return tokens_public_1.tokenLabel(currtok);
        }).join(", ");
        var occurrence = options.alternation.idx === 0 ? "" : options.alternation.idx;
        var currMessage = "Ambiguous Alternatives Detected: <" + options.ambiguityIndices.join(" ,") + "> in <OR" + occurrence + ">" +
            (" inside <" + options.topLevelRule.name + "> Rule,\n") +
            ("<" + pathMsg + "> may appears as a prefix path in all these alternatives.\n");
        currMessage =
            currMessage +
                "See: https://sap.github.io/chevrotain/docs/guide/resolving_grammar_errors.html#AMBIGUOUS_ALTERNATIVES\n" +
                "For Further details.";
        return currMessage;
    },
    buildEmptyRepetitionError: function (options) {
        var dslName = gast_1.getProductionDslName(options.repetition);
        if (options.repetition.idx !== 0) {
            dslName += options.repetition.idx;
        }
        var errMsg = "The repetition <" + dslName + "> within Rule <" + options.topLevelRule.name + "> can never consume any tokens.\n" +
            "This could lead to an infinite loop.";
        return errMsg;
    },
    buildTokenNameError: function (options) {
        var tokTypeName = options.tokenType.name;
        var errMsg = "Invalid Grammar Token name: ->" + tokTypeName + "<- it must match the pattern: ->" + options.expectedPattern.toString() + "<-";
        return errMsg;
    },
    buildEmptyAlternationError: function (options) {
        var errMsg = "Ambiguous empty alternative: <" + (options.emptyChoiceIdx + 1) + ">" +
            (" in <OR" + options.alternation.idx + "> inside <" + options.topLevelRule.name + "> Rule.\n") +
            "Only the last alternative may be an empty alternative.";
        return errMsg;
    },
    buildTooManyAlternativesError: function (options) {
        var errMsg = "An Alternation cannot have more than 256 alternatives:\n" +
            ("<OR" + options.alternation.idx + "> inside <" + options.topLevelRule.name + "> Rule.\n has " + (options.alternation.definition.length +
                1) + " alternatives.");
        return errMsg;
    },
    buildLeftRecursionError: function (options) {
        var ruleName = options.topLevelRule.name;
        var pathNames = utils.map(options.leftRecursionPath, function (currRule) { return currRule.name; });
        var leftRecursivePath = ruleName + " --> " + pathNames
            .concat([ruleName])
            .join(" --> ");
        var errMsg = "Left Recursion found in grammar.\n" +
            ("rule: <" + ruleName + "> can be invoked from itself (directly or indirectly)\n") +
            ("without consuming any Tokens. The grammar path that causes this is: \n " + leftRecursivePath + "\n") +
            " To fix this refactor your grammar to remove the left recursion.\n" +
            "see: https://en.wikipedia.org/wiki/LL_parser#Left_Factoring.";
        return errMsg;
    },
    buildInvalidRuleNameError: function (options) {
        var ruleName = options.topLevelRule.name;
        var expectedPatternString = options.expectedPattern.toString();
        var errMsg = "Invalid grammar rule name: ->" + ruleName + "<- it must match the pattern: ->" + expectedPatternString + "<-";
        return errMsg;
    },
    buildDuplicateRuleNameError: function (options) {
        var ruleName;
        if (options.topLevelRule instanceof gast_public_1.Rule) {
            ruleName = options.topLevelRule.name;
        }
        else {
            ruleName = options.topLevelRule;
        }
        var errMsg = "Duplicate definition, rule: ->" + ruleName + "<- is already defined in the grammar: ->" + options.grammarName + "<-";
        return errMsg;
    }
};
//# sourceMappingURL=errors_public.js.map