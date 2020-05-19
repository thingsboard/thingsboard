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
import { RestWalker } from "./rest";
import { first } from "./first";
import { assign, forEach } from "../../utils/utils";
import { IN } from "../constants";
import { Flat } from "./gast/gast_public";
// This ResyncFollowsWalker computes all of the follows required for RESYNC
// (skipping reference production).
var ResyncFollowsWalker = /** @class */ (function (_super) {
    __extends(ResyncFollowsWalker, _super);
    function ResyncFollowsWalker(topProd) {
        var _this = _super.call(this) || this;
        _this.topProd = topProd;
        _this.follows = {};
        return _this;
    }
    ResyncFollowsWalker.prototype.startWalking = function () {
        this.walk(this.topProd);
        return this.follows;
    };
    ResyncFollowsWalker.prototype.walkTerminal = function (terminal, currRest, prevRest) {
        // do nothing! just like in the public sector after 13:00
    };
    ResyncFollowsWalker.prototype.walkProdRef = function (refProd, currRest, prevRest) {
        var followName = buildBetweenProdsFollowPrefix(refProd.referencedRule, refProd.idx) +
            this.topProd.name;
        var fullRest = currRest.concat(prevRest);
        var restProd = new Flat({ definition: fullRest });
        var t_in_topProd_follows = first(restProd);
        this.follows[followName] = t_in_topProd_follows;
    };
    return ResyncFollowsWalker;
}(RestWalker));
export { ResyncFollowsWalker };
export function computeAllProdsFollows(topProductions) {
    var reSyncFollows = {};
    forEach(topProductions, function (topProd) {
        var currRefsFollow = new ResyncFollowsWalker(topProd).startWalking();
        assign(reSyncFollows, currRefsFollow);
    });
    return reSyncFollows;
}
export function buildBetweenProdsFollowPrefix(inner, occurenceInParent) {
    return inner.name + occurenceInParent + IN;
}
export function buildInProdFollowPrefix(terminal) {
    var terminalName = terminal.terminalType.name;
    return terminalName + terminal.idx + IN;
}
//# sourceMappingURL=follow.js.map