/**
 * Avoid mistakes when naming methods defined on the scope object
 *
 * For example, you want to use $scope.$watch instead of $scope.watch
 *
 * @version 2.3.0
 * @category possibleError
 * @sinceAngularVersion 1.x
 */
'use strict';

const bad = ['new', 'watch', 'watchGroup', 'watchCollection',
    'digest', 'destroy', 'eval', 'evalAsync', 'apply',
    'applyAsync', 'on', 'emit', 'broadcast'];

const scope = ['scope', '$scope', '$rootScope'];

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/avoid-scope-typos.md'
        },
        schema: [ ]
    },
    create: function(context) {
        function check(node, name) {
            if (bad.indexOf(name) >= 0 && node && node.parent && node.parent.object && scope.indexOf(node.parent.object.name) >= 0) {
                context.report(node, `The ${name} method should be replaced by $${name}, or you should rename it in order to avoid confusions`, {});
            }
        }
        return {

            Identifier: function(node) {
                check(node, node.name);
            }
        };
    }
};
