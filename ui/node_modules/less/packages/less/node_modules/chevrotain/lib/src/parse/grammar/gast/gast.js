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
var utils_1 = require("../../../utils/utils");
var gast_public_1 = require("./gast_public");
var gast_visitor_public_1 = require("./gast_visitor_public");
function isSequenceProd(prod) {
    return (prod instanceof gast_public_1.Flat ||
        prod instanceof gast_public_1.Option ||
        prod instanceof gast_public_1.Repetition ||
        prod instanceof gast_public_1.RepetitionMandatory ||
        prod instanceof gast_public_1.RepetitionMandatoryWithSeparator ||
        prod instanceof gast_public_1.RepetitionWithSeparator ||
        prod instanceof gast_public_1.Terminal ||
        prod instanceof gast_public_1.Rule);
}
exports.isSequenceProd = isSequenceProd;
function isOptionalProd(prod, alreadyVisited) {
    if (alreadyVisited === void 0) { alreadyVisited = []; }
    var isDirectlyOptional = prod instanceof gast_public_1.Option ||
        prod instanceof gast_public_1.Repetition ||
        prod instanceof gast_public_1.RepetitionWithSeparator;
    if (isDirectlyOptional) {
        return true;
    }
    // note that this can cause infinite loop if one optional empty TOP production has a cyclic dependency with another
    // empty optional top rule
    // may be indirectly optional ((A?B?C?) | (D?E?F?))
    if (prod instanceof gast_public_1.Alternation) {
        // for OR its enough for just one of the alternatives to be optional
        return utils_1.some(prod.definition, function (subProd) {
            return isOptionalProd(subProd, alreadyVisited);
        });
    }
    else if (prod instanceof gast_public_1.NonTerminal && utils_1.contains(alreadyVisited, prod)) {
        // avoiding stack overflow due to infinite recursion
        return false;
    }
    else if (prod instanceof gast_public_1.AbstractProduction) {
        if (prod instanceof gast_public_1.NonTerminal) {
            alreadyVisited.push(prod);
        }
        return utils_1.every(prod.definition, function (subProd) {
            return isOptionalProd(subProd, alreadyVisited);
        });
    }
    else {
        return false;
    }
}
exports.isOptionalProd = isOptionalProd;
function isBranchingProd(prod) {
    return prod instanceof gast_public_1.Alternation;
}
exports.isBranchingProd = isBranchingProd;
function getProductionDslName(prod) {
    /* istanbul ignore else */
    if (prod instanceof gast_public_1.NonTerminal) {
        return "SUBRULE";
    }
    else if (prod instanceof gast_public_1.Option) {
        return "OPTION";
    }
    else if (prod instanceof gast_public_1.Alternation) {
        return "OR";
    }
    else if (prod instanceof gast_public_1.RepetitionMandatory) {
        return "AT_LEAST_ONE";
    }
    else if (prod instanceof gast_public_1.RepetitionMandatoryWithSeparator) {
        return "AT_LEAST_ONE_SEP";
    }
    else if (prod instanceof gast_public_1.RepetitionWithSeparator) {
        return "MANY_SEP";
    }
    else if (prod instanceof gast_public_1.Repetition) {
        return "MANY";
    }
    else if (prod instanceof gast_public_1.Terminal) {
        return "CONSUME";
    }
    else {
        throw Error("non exhaustive match");
    }
}
exports.getProductionDslName = getProductionDslName;
var DslMethodsCollectorVisitor = /** @class */ (function (_super) {
    __extends(DslMethodsCollectorVisitor, _super);
    function DslMethodsCollectorVisitor() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        // A minus is never valid in an identifier name
        _this.separator = "-";
        _this.dslMethods = {
            option: [],
            alternation: [],
            repetition: [],
            repetitionWithSeparator: [],
            repetitionMandatory: [],
            repetitionMandatoryWithSeparator: []
        };
        return _this;
    }
    DslMethodsCollectorVisitor.prototype.reset = function () {
        this.dslMethods = {
            option: [],
            alternation: [],
            repetition: [],
            repetitionWithSeparator: [],
            repetitionMandatory: [],
            repetitionMandatoryWithSeparator: []
        };
    };
    DslMethodsCollectorVisitor.prototype.visitTerminal = function (terminal) {
        var key = terminal.terminalType.name + this.separator + "Terminal";
        if (!utils_1.has(this.dslMethods, key)) {
            this.dslMethods[key] = [];
        }
        this.dslMethods[key].push(terminal);
    };
    DslMethodsCollectorVisitor.prototype.visitNonTerminal = function (subrule) {
        var key = subrule.nonTerminalName + this.separator + "Terminal";
        if (!utils_1.has(this.dslMethods, key)) {
            this.dslMethods[key] = [];
        }
        this.dslMethods[key].push(subrule);
    };
    DslMethodsCollectorVisitor.prototype.visitOption = function (option) {
        this.dslMethods.option.push(option);
    };
    DslMethodsCollectorVisitor.prototype.visitRepetitionWithSeparator = function (manySep) {
        this.dslMethods.repetitionWithSeparator.push(manySep);
    };
    DslMethodsCollectorVisitor.prototype.visitRepetitionMandatory = function (atLeastOne) {
        this.dslMethods.repetitionMandatory.push(atLeastOne);
    };
    DslMethodsCollectorVisitor.prototype.visitRepetitionMandatoryWithSeparator = function (atLeastOneSep) {
        this.dslMethods.repetitionMandatoryWithSeparator.push(atLeastOneSep);
    };
    DslMethodsCollectorVisitor.prototype.visitRepetition = function (many) {
        this.dslMethods.repetition.push(many);
    };
    DslMethodsCollectorVisitor.prototype.visitAlternation = function (or) {
        this.dslMethods.alternation.push(or);
    };
    return DslMethodsCollectorVisitor;
}(gast_visitor_public_1.GAstVisitor));
exports.DslMethodsCollectorVisitor = DslMethodsCollectorVisitor;
var collectorVisitor = new DslMethodsCollectorVisitor();
function collectMethods(rule) {
    collectorVisitor.reset();
    rule.accept(collectorVisitor);
    var dslMethods = collectorVisitor.dslMethods;
    // avoid uncleaned references
    collectorVisitor.reset();
    return dslMethods;
}
exports.collectMethods = collectMethods;
//# sourceMappingURL=gast.js.map