"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var utils_1 = require("../../../utils/utils");
var resolver_1 = require("../resolver");
var checks_1 = require("../checks");
var errors_public_1 = require("../../errors_public");
var gast_1 = require("./gast");
function resolveGrammar(options) {
    options = utils_1.defaults(options, {
        errMsgProvider: errors_public_1.defaultGrammarResolverErrorProvider
    });
    var topRulesTable = {};
    utils_1.forEach(options.rules, function (rule) {
        topRulesTable[rule.name] = rule;
    });
    return resolver_1.resolveGrammar(topRulesTable, options.errMsgProvider);
}
exports.resolveGrammar = resolveGrammar;
function validateGrammar(options) {
    options = utils_1.defaults(options, {
        errMsgProvider: errors_public_1.defaultGrammarValidatorErrorProvider,
        ignoredIssues: {}
    });
    return checks_1.validateGrammar(options.rules, options.maxLookahead, options.tokenTypes, options.ignoredIssues, options.errMsgProvider, options.grammarName);
}
exports.validateGrammar = validateGrammar;
function assignOccurrenceIndices(options) {
    utils_1.forEach(options.rules, function (currRule) {
        var methodsCollector = new gast_1.DslMethodsCollectorVisitor();
        currRule.accept(methodsCollector);
        utils_1.forEach(methodsCollector.dslMethods, function (methods) {
            utils_1.forEach(methods, function (currMethod, arrIdx) {
                currMethod.idx = arrIdx + 1;
            });
        });
    });
}
exports.assignOccurrenceIndices = assignOccurrenceIndices;
//# sourceMappingURL=gast_resolver_public.js.map