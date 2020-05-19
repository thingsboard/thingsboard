import { Alternation, Flat, NonTerminal, Option, Repetition, RepetitionMandatory, RepetitionMandatoryWithSeparator, RepetitionWithSeparator, Rule, Terminal } from "./gast_public";
var GAstVisitor = /** @class */ (function () {
    function GAstVisitor() {
    }
    GAstVisitor.prototype.visit = function (node) {
        var nodeAny = node;
        switch (nodeAny.constructor) {
            case NonTerminal:
                return this.visitNonTerminal(nodeAny);
            case Flat:
                return this.visitFlat(nodeAny);
            case Option:
                return this.visitOption(nodeAny);
            case RepetitionMandatory:
                return this.visitRepetitionMandatory(nodeAny);
            case RepetitionMandatoryWithSeparator:
                return this.visitRepetitionMandatoryWithSeparator(nodeAny);
            case RepetitionWithSeparator:
                return this.visitRepetitionWithSeparator(nodeAny);
            case Repetition:
                return this.visitRepetition(nodeAny);
            case Alternation:
                return this.visitAlternation(nodeAny);
            case Terminal:
                return this.visitTerminal(nodeAny);
            case Rule:
                return this.visitRule(nodeAny);
            /* istanbul ignore next */
            default:
                throw Error("non exhaustive match");
        }
    };
    GAstVisitor.prototype.visitNonTerminal = function (node) { };
    GAstVisitor.prototype.visitFlat = function (node) { };
    GAstVisitor.prototype.visitOption = function (node) { };
    GAstVisitor.prototype.visitRepetition = function (node) { };
    GAstVisitor.prototype.visitRepetitionMandatory = function (node) { };
    GAstVisitor.prototype.visitRepetitionMandatoryWithSeparator = function (node) { };
    GAstVisitor.prototype.visitRepetitionWithSeparator = function (node) { };
    GAstVisitor.prototype.visitAlternation = function (node) { };
    GAstVisitor.prototype.visitTerminal = function (node) { };
    GAstVisitor.prototype.visitRule = function (node) { };
    return GAstVisitor;
}());
export { GAstVisitor };
//# sourceMappingURL=gast_visitor_public.js.map