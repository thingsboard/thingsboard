'use strict';

var fs = require('fs');
var _ = require('lodash');

var templates = {
    ruleSourcePath: _.template('rules/<%= ruleName %>.js'),
    ruleDocumentationPath: _.template('docs/rules/<%= ruleName %>.md'),
    ruleExamplesPath: _.template('examples/<%= ruleName %>.js'),
    styleguide: _.template('[<%= name %> by <%= type %> - <%= description %>](<%= link %>)'),
    styleguideShort: _.template('[<%= name %>](<%= link %>)'),
    styleguideLinks: {
        johnpapa: _.template('https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-<%= name %>')
    }
};

var templatesDir = './scripts/templates/';
var templateSettings = {
    imports: {
        formatStyleguideReference: function(styleRef) {
            return templates.styleguide(styleguideReferenceTemplateContext(styleRef));
        },
        formatStyleguideReferenceListShort: function(rule) {
            if (!rule.styleguideReferences || rule.styleguideReferences.length === 0) {
                return '';
            }
            return ' (' + rule.styleguideReferences
                .map(styleguideReferenceTemplateContext)
                .map(templates.styleguideShort).join(', ') +
            ')';
        },
        formatConfigAsJson: function(examples) {
            var config = examples[0].displayOptions;
            if (!config) {
                return 2;
            }
            return JSON.stringify([2].concat(config));
        },
        formatConfigAsMarkdown: function(examples) {
            var config = examples[0].displayOptions;
            if (!config) {
                return '';
            }
            return '`' + config.map(JSON.stringify).join('` and `') + '`';
        },
        indent: function(content, indentation) {
            var spaces = new Array(indentation + 1).join(' ');
            return content.replace(/\n/g, '\n' + spaces);
        }
    }
};

fs.readdirSync(templatesDir).forEach(function(templateFilename) {
    var templateName = templateFilename.split('.')[0];
    if (templates[templateName] !== undefined) {
        throw new Error('Can not create from template "' + templateFilename + '" because template key "' +
            templateName + '" already exists.');
    }

    templates[templateName] = _.template(fs.readFileSync(templatesDir + templateFilename), templateSettings);
});

module.exports = templates;

function styleguideReferenceTemplateContext(styleRef) {
    var linkTemplate = templates.styleguideLinks[styleRef.type];
    if (!linkTemplate) {
        throw new Error('No styleguide link template for styleguide type: "' + styleRef.type);
    }
    return _.extend({
        link: linkTemplate(styleRef)
    }, styleRef);
}
