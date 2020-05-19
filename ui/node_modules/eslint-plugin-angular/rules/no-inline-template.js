/**
 * disallow the use of inline templates
 *
 * Instead of using inline HTML templates, it is better to load the HTML from an external file.
 * Simple HTML templates are accepted by default.
 * ('no-inline-template': [0, {allowSimple: true}])
 *
 * @version 0.12.0
 * @category bestPractice
 * @sinceAngularVersion 1.x
 */
'use strict';

module.exports = {
    meta: {
        docs: {
            url: 'https://github.com/Gillespie59/eslint-plugin-angular/blob/master/docs/rules/no-inline-template.md'
        },
        schema: [{
            allowSimple: {
                type: 'boolean'
            }
        }]
    },
    create: function(context) {
        // Extracts any HTML tags.
        var regularTagPattern = /<(.+?)>/g;
        // Extracts self closing HTML tags.
        var selfClosingTagPattern = /<(.+?)\/>/g;

        var allowSimple = (context.options[0] && context.options[0].allowSimple) !== false;

        function reportComplex(node) {
            context.report(node, 'Inline template is too complex. Use an external template instead');
        }

        return {
            Property: function(node) {
                if (node.key.name !== 'template' || node.value.type !== 'Literal') {
                    return;
                }
                if (!allowSimple) {
                    context.report(node, 'Inline templates are not allowed. Use an external template instead');
                }
                if ((node.value.value && node.value.value.match(regularTagPattern) || []).length > 2) {
                    return reportComplex(node);
                }
                if ((node.value.value && node.value.value.match(selfClosingTagPattern) || []).length > 1) {
                    return reportComplex(node);
                }
                if (node.value && node.value.raw.indexOf('\\') !== -1) {
                    reportComplex(node);
                }
            }
        };
    }
};
