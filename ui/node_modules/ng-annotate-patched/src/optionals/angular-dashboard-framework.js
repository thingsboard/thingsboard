"use strict";

let ctx = null;
module.exports = {
    init: function(_ctx) {
        ctx = _ctx;
    },

    match: function(node) {
        // dashboardProvider.widget("name", {
        //   ...
        //   controller: function($scope) {},
        //   resolve: {f: function($scope) {}, ..}
        // })

        const callee = node.callee;
        if (!callee) {
            return false;
        }

        const obj = callee.object;
        if (!obj) {
            return false;
        }

        // identifier or expression
        if (!(obj.$chained === 1 || (obj.type === "Identifier" && obj.name === "dashboardProvider"))) {
            return false;
        }

        node.$chained = 1;

        const method = callee.property; // identifier
        if (method.name !== "widget") {
            return false;
        }

        const args = node.arguments;
        if (args.length !== 2) {
            return false;
        }

        const configArg = ctx.last(args);
        if (configArg.type !== "ObjectExpression") {
            return false;
        }

        const props = configArg.properties;
        const res = [
            ctx.matchProp("controller", props)
        ];
        // {resolve: ..}
        res.push.apply(res, ctx.matchResolve(props));

        // edit: {controller: function(), resolve: {}, apply: function()}
        const edit = ctx.matchProp('edit', props);
        if (edit && edit.type === "ObjectExpression") {
            const editProps = edit.properties;
            res.push(ctx.matchProp('controller', editProps));
            res.push(ctx.matchProp('apply', editProps));
            res.push.apply(res, ctx.matchResolve(editProps));
        }

        const filteredRes = res.filter(Boolean);
        return (filteredRes.length === 0 ? false : filteredRes);
    }
};
