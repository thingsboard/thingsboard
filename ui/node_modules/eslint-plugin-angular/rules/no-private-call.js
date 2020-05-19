/**
 * disallow use of internal angular properties prefixed with $$
 *
 * All scope's properties/methods starting with $$ are used internally by AngularJS.
 * You should not use them directly.
 * Exception can be allowed with this option: {allow:['$$watchers']}
 *
 * @version 0.1.0
 * @category possibleError
 * @sinceAngularVersion 1.x
 */
'use strict';

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/no-private-call.md'
        },
        schema: [
            {
                type: 'object',
                properties: {
                    allow: {
                        type: 'array',
                        items: {
                            type: 'string'
                        }
                    }
                },
                additionalProperties: false
            }
        ]
    },
    create: function(context) {
        var options = context.options[0] || {};
        var allowed = options.allow || [];

        function check(node, name) {
            if (name.slice(0, 2) === '$$' && allowed.indexOf(name) < 0) {
                context.report(node, 'Using $$-prefixed Angular objects/methods are not recommended', {});
            }
        }
        return {

            Identifier: function(node) {
                check(node, node.name);
            }
        };
    }
};
