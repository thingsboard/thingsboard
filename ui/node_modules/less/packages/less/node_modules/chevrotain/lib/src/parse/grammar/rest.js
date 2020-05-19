"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var utils_1 = require("../../utils/utils");
var gast_public_1 = require("./gast/gast_public");
/**
 *  A Grammar Walker that computes the "remaining" grammar "after" a productions in the grammar.
 */
var RestWalker = /** @class */ (function () {
    function RestWalker() {
    }
    RestWalker.prototype.walk = function (prod, prevRest) {
        var _this = this;
        if (prevRest === void 0) { prevRest = []; }
        utils_1.forEach(prod.definition, function (subProd, index) {
            var currRest = utils_1.drop(prod.definition, index + 1);
            /* istanbul ignore else */
            if (subProd instanceof gast_public_1.NonTerminal) {
                _this.walkProdRef(subProd, currRest, prevRest);
            }
            else if (subProd instanceof gast_public_1.Terminal) {
                _this.walkTerminal(subProd, currRest, prevRest);
            }
            else if (subProd instanceof gast_public_1.Flat) {
                _this.walkFlat(subProd, currRest, prevRest);
            }
            else if (subProd instanceof gast_public_1.Option) {
                _this.walkOption(subProd, currRest, prevRest);
            }
            else if (subProd instanceof gast_public_1.RepetitionMandatory) {
                _this.walkAtLeastOne(subProd, currRest, prevRest);
            }
            else if (subProd instanceof gast_public_1.RepetitionMandatoryWithSeparator) {
                _this.walkAtLeastOneSep(subProd, currRest, prevRest);
            }
            else if (subProd instanceof gast_public_1.RepetitionWithSeparator) {
                _this.walkManySep(subProd, currRest, prevRest);
            }
            else if (subProd instanceof gast_public_1.Repetition) {
                _this.walkMany(subProd, currRest, prevRest);
            }
            else if (subProd instanceof gast_public_1.Alternation) {
                _this.walkOr(subProd, currRest, prevRest);
            }
            else {
                throw Error("non exhaustive match");
            }
        });
    };
    RestWalker.prototype.walkTerminal = function (terminal, currRest, prevRest) { };
    RestWalker.prototype.walkProdRef = function (refProd, currRest, prevRest) { };
    RestWalker.prototype.walkFlat = function (flatProd, currRest, prevRest) {
        // ABCDEF => after the D the rest is EF
        var fullOrRest = currRest.concat(prevRest);
        this.walk(flatProd, fullOrRest);
    };
    RestWalker.prototype.walkOption = function (optionProd, currRest, prevRest) {
        // ABC(DE)?F => after the (DE)? the rest is F
        var fullOrRest = currRest.concat(prevRest);
        this.walk(optionProd, fullOrRest);
    };
    RestWalker.prototype.walkAtLeastOne = function (atLeastOneProd, currRest, prevRest) {
        // ABC(DE)+F => after the (DE)+ the rest is (DE)?F
        var fullAtLeastOneRest = [
            new gast_public_1.Option({ definition: atLeastOneProd.definition })
        ].concat(currRest, prevRest);
        this.walk(atLeastOneProd, fullAtLeastOneRest);
    };
    RestWalker.prototype.walkAtLeastOneSep = function (atLeastOneSepProd, currRest, prevRest) {
        // ABC DE(,DE)* F => after the (,DE)+ the rest is (,DE)?F
        var fullAtLeastOneSepRest = restForRepetitionWithSeparator(atLeastOneSepProd, currRest, prevRest);
        this.walk(atLeastOneSepProd, fullAtLeastOneSepRest);
    };
    RestWalker.prototype.walkMany = function (manyProd, currRest, prevRest) {
        // ABC(DE)*F => after the (DE)* the rest is (DE)?F
        var fullManyRest = [
            new gast_public_1.Option({ definition: manyProd.definition })
        ].concat(currRest, prevRest);
        this.walk(manyProd, fullManyRest);
    };
    RestWalker.prototype.walkManySep = function (manySepProd, currRest, prevRest) {
        // ABC (DE(,DE)*)? F => after the (,DE)* the rest is (,DE)?F
        var fullManySepRest = restForRepetitionWithSeparator(manySepProd, currRest, prevRest);
        this.walk(manySepProd, fullManySepRest);
    };
    RestWalker.prototype.walkOr = function (orProd, currRest, prevRest) {
        var _this = this;
        // ABC(D|E|F)G => when finding the (D|E|F) the rest is G
        var fullOrRest = currRest.concat(prevRest);
        // walk all different alternatives
        utils_1.forEach(orProd.definition, function (alt) {
            // wrapping each alternative in a single definition wrapper
            // to avoid errors in computing the rest of that alternative in the invocation to computeInProdFollows
            // (otherwise for OR([alt1,alt2]) alt2 will be considered in 'rest' of alt1
            var prodWrapper = new gast_public_1.Flat({ definition: [alt] });
            _this.walk(prodWrapper, fullOrRest);
        });
    };
    return RestWalker;
}());
exports.RestWalker = RestWalker;
function restForRepetitionWithSeparator(repSepProd, currRest, prevRest) {
    var repSepRest = [
        new gast_public_1.Option({
            definition: [
                new gast_public_1.Terminal({ terminalType: repSepProd.separator })
            ].concat(repSepProd.definition)
        })
    ];
    var fullRepSepRest = repSepRest.concat(currRest, prevRest);
    return fullRepSepRest;
}
//# sourceMappingURL=rest.js.map