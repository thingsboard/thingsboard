/**
 * use `$window` instead of `window`
 *
 * Instead of the default window object, you should prefer the AngularJS wrapper service $window.
 *
 * @styleguideReference {johnpapa} `y180` Angular $ Wrapper Services - $document and $window
 * @version 0.1.0
 * @category angularWrapper
 * @sinceAngularVersion 1.x
 */
'use strict';

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/window-service.md'
        },
        schema: []
    },
    create: function(context) {
        var restrict = ['document', 'setInterval', 'setTimeout'];
        return {

            MemberExpression: function(node) {
                if (node.object.name === 'window' && restrict.indexOf(node.property.name) < 0) {
                    context.report(node, 'You should use the $window service instead of the default window object', {});
                }
            }
        };
    }
};
