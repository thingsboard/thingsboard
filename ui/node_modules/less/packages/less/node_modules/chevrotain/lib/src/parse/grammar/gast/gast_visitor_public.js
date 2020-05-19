"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var gast_public_1 = require("./gast_public");
var GAstVisitor = /** @class */ (function () {
    function GAstVisitor() {
    }
    GAstVisitor.prototype.visit = function (node) {
        var nodeAny = node;
        switch (nodeAny.constructor) {
            case gast_public_1.NonTerminal:
                return this.visitNonTerminal(nodeAny);
            case gast_public_1.Flat:
                return this.visitFlat(nodeAny);
            case gast_public_1.Option:
                return this.visitOption(nodeAny);
            case gast_public_1.RepetitionMandatory:
                return this.visitRepetitionMandatory(nodeAny);
            case gast_public_1.RepetitionMandatoryWithSeparator:
                return this.visitRepetitionMandatoryWithSeparator(nodeAny);
            case gast_public_1.RepetitionWithSeparator:
                return this.visitRepetitionWithSeparator(nodeAny);
            case gast_public_1.Repetition:
                return this.visitRepetition(nodeAny);
            case gast_public_1.Alternation:
                return this.visitAlternation(nodeAny);
            case gast_public_1.Terminal:
                return this.visitTerminal(nodeAny);
            case gast_public_1.Rule:
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
exports.GAstVisitor = GAstVisitor;
//# sourceMappingURL=gast_visitor_public.js.map