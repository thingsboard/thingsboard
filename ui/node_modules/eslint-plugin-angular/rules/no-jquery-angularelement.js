/**
 * disallow to wrap `angular.element` objects with `jQuery` or `$`
 *
 * You should not wrap angular.element object into jQuery(), because angular.element already return jQLite element
 * @version 0.1.0
 * @category angularWrapper
 * @sinceAngularVersion 1.x
 */
'use strict';

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/no-jquery-angularelement.md'
        },
        schema: []
    },
    create: function(context) {
        return {

            MemberExpression: function(node) {
                if (node.object.name === 'angular' && node.property.name === 'element') {
                    if (node.parent !== undefined && node.parent.parent !== undefined &&
                        node.parent.parent.type === 'CallExpression' &&
                        node.parent.parent.callee.type === 'Identifier' &&
                        (node.parent.parent.callee.name === 'jQuery' || node.parent.parent.callee.name === '$')) {
                        context.report(node, 'angular.element returns already a jQLite element. No need to wrap with the jQuery object', {});
                    }
                }
            }
        };
    }
};
