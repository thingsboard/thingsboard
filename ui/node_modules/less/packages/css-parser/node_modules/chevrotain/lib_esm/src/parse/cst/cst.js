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
import { forEach, isUndefined } from "../../utils/utils";
import { AT_LEAST_ONE_IDX, AT_LEAST_ONE_SEP_IDX, getKeyForAltIndex, getKeyForAutomaticLookahead, MANY_IDX, MANY_SEP_IDX, OPTION_IDX, OR_IDX } from "../grammar/keys";
import { Alternation, Option, Repetition, RepetitionMandatory, RepetitionMandatoryWithSeparator, RepetitionWithSeparator } from "../grammar/gast/gast_public";
import { GAstVisitor } from "../grammar/gast/gast_visitor_public";
/**
 * This nodeLocation tracking is not efficient and should only be used
 * when error recovery is enabled or the Token Vector contains virtual Tokens
 * (e.g, Python Indent/Outdent)
 * As it executes the calculation for every single terminal/nonTerminal
 * and does not rely on the fact the token vector is **sorted**
 */
export function setNodeLocationOnlyOffset(currNodeLocation, newLocationInfo) {
    // First (valid) update for this cst node
    if (isNaN(currNodeLocation.startOffset) === true) {
        // assumption1: Token location information is either NaN or a valid number
        // assumption2: Token location information is fully valid if it exist
        // (both start/end offsets exist and are numbers).
        currNodeLocation.startOffset = newLocationInfo.startOffset;
        currNodeLocation.endOffset = newLocationInfo.endOffset;
    }
    // Once the startOffset has been updated with a valid number it should never receive
    // any farther updates as the Token vector is sorted.
    // We still have to check this this condition for every new possible location info
    // because with error recovery enabled we may encounter invalid tokens (NaN location props)
    else if (currNodeLocation.endOffset < newLocationInfo.endOffset === true) {
        currNodeLocation.endOffset = newLocationInfo.endOffset;
    }
}
/**
 * This nodeLocation tracking is not efficient and should only be used
 * when error recovery is enabled or the Token Vector contains virtual Tokens
 * (e.g, Python Indent/Outdent)
 * As it executes the calculation for every single terminal/nonTerminal
 * and does not rely on the fact the token vector is **sorted**
 */
export function setNodeLocationFull(currNodeLocation, newLocationInfo) {
    // First (valid) update for this cst node
    if (isNaN(currNodeLocation.startOffset) === true) {
        // assumption1: Token location information is either NaN or a valid number
        // assumption2: Token location information is fully valid if it exist
        // (all start/end props exist and are numbers).
        currNodeLocation.startOffset = newLocationInfo.startOffset;
        currNodeLocation.startColumn = newLocationInfo.startColumn;
        currNodeLocation.startLine = newLocationInfo.startLine;
        currNodeLocation.endOffset = newLocationInfo.endOffset;
        currNodeLocation.endColumn = newLocationInfo.endColumn;
        currNodeLocation.endLine = newLocationInfo.endLine;
    }
    // Once the start props has been updated with a valid number it should never receive
    // any farther updates as the Token vector is sorted.
    // We still have to check this this condition for every new possible location info
    // because with error recovery enabled we may encounter invalid tokens (NaN location props)
    else if (currNodeLocation.endOffset < newLocationInfo.endOffset === true) {
        currNodeLocation.endOffset = newLocationInfo.endOffset;
        currNodeLocation.endColumn = newLocationInfo.endColumn;
        currNodeLocation.endLine = newLocationInfo.endLine;
    }
}
export function addTerminalToCst(node, token, tokenTypeName) {
    if (node.children[tokenTypeName] === undefined) {
        node.children[tokenTypeName] = [token];
    }
    else {
        node.children[tokenTypeName].push(token);
    }
}
export function addNoneTerminalToCst(node, ruleName, ruleResult) {
    if (node.children[ruleName] === undefined) {
        node.children[ruleName] = [ruleResult];
    }
    else {
        node.children[ruleName].push(ruleResult);
    }
}
var NamedDSLMethodsCollectorVisitor = /** @class */ (function (_super) {
    __extends(NamedDSLMethodsCollectorVisitor, _super);
    function NamedDSLMethodsCollectorVisitor(ruleIdx) {
        var _this = _super.call(this) || this;
        _this.result = [];
        _this.ruleIdx = ruleIdx;
        return _this;
    }
    NamedDSLMethodsCollectorVisitor.prototype.collectNamedDSLMethod = function (node, newNodeConstructor, methodIdx) {
        // TODO: better hack to copy what we need here...
        if (!isUndefined(node.name)) {
            // copy without name so this will indeed be processed later.
            var nameLessNode 
            /* istanbul ignore else */
            = void 0;
            /* istanbul ignore else */
            if (node instanceof Option ||
                node instanceof Repetition ||
                node instanceof RepetitionMandatory ||
                node instanceof Alternation) {
                nameLessNode = new newNodeConstructor({
                    definition: node.definition,
                    idx: node.idx
                });
            }
            else if (node instanceof RepetitionMandatoryWithSeparator ||
                node instanceof RepetitionWithSeparator) {
                nameLessNode = new newNodeConstructor({
                    definition: node.definition,
                    idx: node.idx,
                    separator: node.separator
                });
            }
            else {
                throw Error("non exhaustive match");
            }
            var def = [nameLessNode];
            var key = getKeyForAutomaticLookahead(this.ruleIdx, methodIdx, node.idx);
            this.result.push({ def: def, key: key, name: node.name, orgProd: node });
        }
    };
    NamedDSLMethodsCollectorVisitor.prototype.visitOption = function (node) {
        this.collectNamedDSLMethod(node, Option, OPTION_IDX);
    };
    NamedDSLMethodsCollectorVisitor.prototype.visitRepetition = function (node) {
        this.collectNamedDSLMethod(node, Repetition, MANY_IDX);
    };
    NamedDSLMethodsCollectorVisitor.prototype.visitRepetitionMandatory = function (node) {
        this.collectNamedDSLMethod(node, RepetitionMandatory, AT_LEAST_ONE_IDX);
    };
    NamedDSLMethodsCollectorVisitor.prototype.visitRepetitionMandatoryWithSeparator = function (node) {
        this.collectNamedDSLMethod(node, RepetitionMandatoryWithSeparator, AT_LEAST_ONE_SEP_IDX);
    };
    NamedDSLMethodsCollectorVisitor.prototype.visitRepetitionWithSeparator = function (node) {
        this.collectNamedDSLMethod(node, RepetitionWithSeparator, MANY_SEP_IDX);
    };
    NamedDSLMethodsCollectorVisitor.prototype.visitAlternation = function (node) {
        var _this = this;
        this.collectNamedDSLMethod(node, Alternation, OR_IDX);
        var hasMoreThanOneAlternative = node.definition.length > 1;
        forEach(node.definition, function (currFlatAlt, altIdx) {
            if (!isUndefined(currFlatAlt.name)) {
                var def = currFlatAlt.definition;
                if (hasMoreThanOneAlternative) {
                    def = [new Option({ definition: currFlatAlt.definition })];
                }
                else {
                    // mandatory
                    def = currFlatAlt.definition;
                }
                var key = getKeyForAltIndex(_this.ruleIdx, OR_IDX, node.idx, altIdx);
                _this.result.push({
                    def: def,
                    key: key,
                    name: currFlatAlt.name,
                    orgProd: currFlatAlt
                });
            }
        });
    };
    return NamedDSLMethodsCollectorVisitor;
}(GAstVisitor));
export { NamedDSLMethodsCollectorVisitor };
export function expandAllNestedRuleNames(topRules, fullToShortName) {
    var result = {
        allRuleNames: []
    };
    forEach(topRules, function (currTopRule) {
        var currTopRuleShortName = fullToShortName[currTopRule.name];
        result.allRuleNames.push(currTopRule.name);
        var namedCollectorVisitor = new NamedDSLMethodsCollectorVisitor(currTopRuleShortName);
        currTopRule.accept(namedCollectorVisitor);
        forEach(namedCollectorVisitor.result, function (_a) {
            var def = _a.def, key = _a.key, name = _a.name;
            result.allRuleNames.push(currTopRule.name + name);
        });
    });
    return result;
}
//# sourceMappingURL=cst.js.map