import { defaults, forEach } from "../../../utils/utils";
import { resolveGrammar as orgResolveGrammar } from "../resolver";
import { validateGrammar as orgValidateGrammar } from "../checks";
import { defaultGrammarResolverErrorProvider, defaultGrammarValidatorErrorProvider } from "../../errors_public";
import { DslMethodsCollectorVisitor } from "./gast";
export function resolveGrammar(options) {
    options = defaults(options, {
        errMsgProvider: defaultGrammarResolverErrorProvider
    });
    var topRulesTable = {};
    forEach(options.rules, function (rule) {
        topRulesTable[rule.name] = rule;
    });
    return orgResolveGrammar(topRulesTable, options.errMsgProvider);
}
export function validateGrammar(options) {
    options = defaults(options, {
        errMsgProvider: defaultGrammarValidatorErrorProvider,
        ignoredIssues: {}
    });
    return orgValidateGrammar(options.rules, options.maxLookahead, options.tokenTypes, options.ignoredIssues, options.errMsgProvider, options.grammarName);
}
export function assignOccurrenceIndices(options) {
    forEach(options.rules, function (currRule) {
        var methodsCollector = new DslMethodsCollectorVisitor();
        currRule.accept(methodsCollector);
        forEach(methodsCollector.dslMethods, function (methods) {
            forEach(methods, function (currMethod, arrIdx) {
                currMethod.idx = arrIdx + 1;
            });
        });
    });
}
//# sourceMappingURL=gast_resolver_public.js.map