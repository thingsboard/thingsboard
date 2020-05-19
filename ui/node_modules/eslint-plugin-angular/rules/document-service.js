/**
 * use `$document` instead of `document`
 *
 * Instead of the default document object, you should prefer the AngularJS wrapper service $document.
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
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/document-service.md'
        },
        schema: []
    },
    create: function(context) {
        return {
            MemberExpression: function(node) {
                if (node.object.name === 'document' || (node.object.name === 'window' && node.property.name === 'document')) {
                    context.report(node, 'You should use the $document service instead of the default document object', {});
                }
            }
        };
    }
};
