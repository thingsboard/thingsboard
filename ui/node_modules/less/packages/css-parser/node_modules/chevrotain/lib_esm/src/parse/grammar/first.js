import { uniq, map, flatten } from "../../utils/utils";
import { NonTerminal, Terminal } from "./gast/gast_public";
import { isBranchingProd, isOptionalProd, isSequenceProd } from "./gast/gast";
export function first(prod) {
    /* istanbul ignore else */
    if (prod instanceof NonTerminal) {
        // this could in theory cause infinite loops if
        // (1) prod A refs prod B.
        // (2) prod B refs prod A
        // (3) AB can match the empty set
        // in other words a cycle where everything is optional so the first will keep
        // looking ahead for the next optional part and will never exit
        // currently there is no safeguard for this unique edge case because
        // (1) not sure a grammar in which this can happen is useful for anything (productive)
        return first(prod.referencedRule);
    }
    else if (prod instanceof Terminal) {
        return firstForTerminal(prod);
    }
    else if (isSequenceProd(prod)) {
        return firstForSequence(prod);
    }
    else if (isBranchingProd(prod)) {
        return firstForBranching(prod);
    }
    else {
        throw Error("non exhaustive match");
    }
}
export function firstForSequence(prod) {
    var firstSet = [];
    var seq = prod.definition;
    var nextSubProdIdx = 0;
    var hasInnerProdsRemaining = seq.length > nextSubProdIdx;
    var currSubProd;
    // so we enter the loop at least once (if the definition is not empty
    var isLastInnerProdOptional = true;
    // scan a sequence until it's end or until we have found a NONE optional production in it
    while (hasInnerProdsRemaining && isLastInnerProdOptional) {
        currSubProd = seq[nextSubProdIdx];
        isLastInnerProdOptional = isOptionalProd(currSubProd);
        firstSet = firstSet.concat(first(currSubProd));
        nextSubProdIdx = nextSubProdIdx + 1;
        hasInnerProdsRemaining = seq.length > nextSubProdIdx;
    }
    return uniq(firstSet);
}
export function firstForBranching(prod) {
    var allAlternativesFirsts = map(prod.definition, function (innerProd) {
        return first(innerProd);
    });
    return uniq(flatten(allAlternativesFirsts));
}
export function firstForTerminal(terminal) {
    return [terminal.terminalType];
}
//# sourceMappingURL=first.js.map