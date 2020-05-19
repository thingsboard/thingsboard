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
import { assign, forEach, isRegExp, map, pick } from "../../../utils/utils";
import { tokenLabel } from "../../../scan/tokens_public";
var AbstractProduction = /** @class */ (function () {
    function AbstractProduction(definition) {
        this.definition = definition;
    }
    AbstractProduction.prototype.accept = function (visitor) {
        visitor.visit(this);
        forEach(this.definition, function (prod) {
            prod.accept(visitor);
        });
    };
    return AbstractProduction;
}());
export { AbstractProduction };
var NonTerminal = /** @class */ (function (_super) {
    __extends(NonTerminal, _super);
    function NonTerminal(options) {
        var _this = _super.call(this, []) || this;
        _this.idx = 1;
        assign(_this, pick(options, function (v) { return v !== undefined; }));
        return _this;
    }
    Object.defineProperty(NonTerminal.prototype, "definition", {
        get: function () {
            if (this.referencedRule !== undefined) {
                return this.referencedRule.definition;
            }
            return [];
        },
        set: function (definition) {
            // immutable
        },
        enumerable: true,
        configurable: true
    });
    NonTerminal.prototype.accept = function (visitor) {
        visitor.visit(this);
        // don't visit children of a reference, we will get cyclic infinite loops if we do so
    };
    return NonTerminal;
}(AbstractProduction));
export { NonTerminal };
var Rule = /** @class */ (function (_super) {
    __extends(Rule, _super);
    function Rule(options) {
        var _this = _super.call(this, options.definition) || this;
        _this.orgText = "";
        assign(_this, pick(options, function (v) { return v !== undefined; }));
        return _this;
    }
    return Rule;
}(AbstractProduction));
export { Rule };
// TODO: is this only used in an Alternation?
//       Perhaps `Flat` should be renamed to `Alternative`?
var Flat = /** @class */ (function (_super) {
    __extends(Flat, _super);
    // A named Flat production is used to indicate a Nested Rule in an alternation
    function Flat(options) {
        var _this = _super.call(this, options.definition) || this;
        _this.ignoreAmbiguities = false;
        assign(_this, pick(options, function (v) { return v !== undefined; }));
        return _this;
    }
    return Flat;
}(AbstractProduction));
export { Flat };
var Option = /** @class */ (function (_super) {
    __extends(Option, _super);
    function Option(options) {
        var _this = _super.call(this, options.definition) || this;
        _this.idx = 1;
        assign(_this, pick(options, function (v) { return v !== undefined; }));
        return _this;
    }
    return Option;
}(AbstractProduction));
export { Option };
var RepetitionMandatory = /** @class */ (function (_super) {
    __extends(RepetitionMandatory, _super);
    function RepetitionMandatory(options) {
        var _this = _super.call(this, options.definition) || this;
        _this.idx = 1;
        assign(_this, pick(options, function (v) { return v !== undefined; }));
        return _this;
    }
    return RepetitionMandatory;
}(AbstractProduction));
export { RepetitionMandatory };
var RepetitionMandatoryWithSeparator = /** @class */ (function (_super) {
    __extends(RepetitionMandatoryWithSeparator, _super);
    function RepetitionMandatoryWithSeparator(options) {
        var _this = _super.call(this, options.definition) || this;
        _this.idx = 1;
        assign(_this, pick(options, function (v) { return v !== undefined; }));
        return _this;
    }
    return RepetitionMandatoryWithSeparator;
}(AbstractProduction));
export { RepetitionMandatoryWithSeparator };
var Repetition = /** @class */ (function (_super) {
    __extends(Repetition, _super);
    function Repetition(options) {
        var _this = _super.call(this, options.definition) || this;
        _this.idx = 1;
        assign(_this, pick(options, function (v) { return v !== undefined; }));
        return _this;
    }
    return Repetition;
}(AbstractProduction));
export { Repetition };
var RepetitionWithSeparator = /** @class */ (function (_super) {
    __extends(RepetitionWithSeparator, _super);
    function RepetitionWithSeparator(options) {
        var _this = _super.call(this, options.definition) || this;
        _this.idx = 1;
        assign(_this, pick(options, function (v) { return v !== undefined; }));
        return _this;
    }
    return RepetitionWithSeparator;
}(AbstractProduction));
export { RepetitionWithSeparator };
var Alternation = /** @class */ (function (_super) {
    __extends(Alternation, _super);
    function Alternation(options) {
        var _this = _super.call(this, options.definition) || this;
        _this.idx = 1;
        _this.ignoreAmbiguities = false;
        _this.hasPredicates = false;
        assign(_this, pick(options, function (v) { return v !== undefined; }));
        return _this;
    }
    return Alternation;
}(AbstractProduction));
export { Alternation };
var Terminal = /** @class */ (function () {
    function Terminal(options) {
        this.idx = 1;
        assign(this, pick(options, function (v) { return v !== undefined; }));
    }
    Terminal.prototype.accept = function (visitor) {
        visitor.visit(this);
    };
    return Terminal;
}());
export { Terminal };
export function serializeGrammar(topRules) {
    return map(topRules, serializeProduction);
}
export function serializeProduction(node) {
    function convertDefinition(definition) {
        return map(definition, serializeProduction);
    }
    /* istanbul ignore else */
    if (node instanceof NonTerminal) {
        return {
            type: "NonTerminal",
            name: node.nonTerminalName,
            idx: node.idx
        };
    }
    else if (node instanceof Flat) {
        return {
            type: "Flat",
            definition: convertDefinition(node.definition)
        };
    }
    else if (node instanceof Option) {
        return {
            type: "Option",
            idx: node.idx,
            definition: convertDefinition(node.definition)
        };
    }
    else if (node instanceof RepetitionMandatory) {
        return {
            type: "RepetitionMandatory",
            name: node.name,
            idx: node.idx,
            definition: convertDefinition(node.definition)
        };
    }
    else if (node instanceof RepetitionMandatoryWithSeparator) {
        return {
            type: "RepetitionMandatoryWithSeparator",
            name: node.name,
            idx: node.idx,
            separator: (serializeProduction(new Terminal({ terminalType: node.separator }))),
            definition: convertDefinition(node.definition)
        };
    }
    else if (node instanceof RepetitionWithSeparator) {
        return {
            type: "RepetitionWithSeparator",
            name: node.name,
            idx: node.idx,
            separator: (serializeProduction(new Terminal({ terminalType: node.separator }))),
            definition: convertDefinition(node.definition)
        };
    }
    else if (node instanceof Repetition) {
        return {
            type: "Repetition",
            name: node.name,
            idx: node.idx,
            definition: convertDefinition(node.definition)
        };
    }
    else if (node instanceof Alternation) {
        return {
            type: "Alternation",
            name: node.name,
            idx: node.idx,
            definition: convertDefinition(node.definition)
        };
    }
    else if (node instanceof Terminal) {
        var serializedTerminal = {
            type: "Terminal",
            name: node.terminalType.name,
            label: tokenLabel(node.terminalType),
            idx: node.idx
        };
        var pattern = node.terminalType.PATTERN;
        if (node.terminalType.PATTERN) {
            serializedTerminal.pattern = isRegExp(pattern)
                ? pattern.source
                : pattern;
        }
        return serializedTerminal;
    }
    else if (node instanceof Rule) {
        return {
            type: "Rule",
            name: node.name,
            orgText: node.orgText,
            definition: convertDefinition(node.definition)
        };
    }
    else {
        throw Error("non exhaustive match");
    }
}
//# sourceMappingURL=gast_public.js.map