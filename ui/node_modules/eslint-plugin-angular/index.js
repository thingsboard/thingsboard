'use strict';

/* eslint-disable global-require */

var fs = require('fs');
var path = require('path');

var rules = {};
var ruleDir = path.join(__dirname, 'rules');

fs.readdirSync(ruleDir).forEach(function(name) {
    var match = name.match(/(.+)\.js$/);
    if (match) {
        rules[match[1]] = require(path.join(ruleDir, name));
    }
});

module.exports = {
    rules: rules,
    environments: require('./environments'),
    configs: {
        johnpapa: {
            plugins: [
                'angular'
            ],
            rules: {
                'angular/component-name': 2,
                'angular/constant-name': 2,
                'angular/controller-as-route': 2,
                'angular/controller-as-vm': 2,
                'angular/controller-as': 2,
                'angular/controller-name': 2,
                'angular/directive-name': 2,
                'angular/directive-restrict': 2,
                'angular/document-service': 2,
                'angular/factory-name': 2,
                'angular/file-name': 2,
                'angular/filter-name': 2,
                'angular/function-type': 2,
                'angular/interval-service': 2,
                'angular/module-getter': 2,
                'angular/module-name': 2,
                'angular/module-setter': 2,
                'angular/no-run-logic': 2,
                'angular/no-service-method': 2,
                'angular/provider-name': 2,
                'angular/service-name': 2,
                'angular/timeout-service': 2,
                'angular/value-name': 2,
                'angular/window-service': 2
            }
        },
        bestpractices: {
            plugins: [
                'angular'
            ],
            rules: {
                'angular/component-name': 2,
                'angular/constant-name': 2,
                'angular/controller-as-route': 2,
                'angular/controller-as-vm': 2,
                'angular/controller-as': 2,
                'angular/deferred': 2,
                'angular/di-unused': 2,
                'angular/directive-restrict': 2,
                'angular/empty-controller': 2,
                'angular/no-controller': 2,
                'angular/no-inline-template': 2,
                'angular/no-run-logic': 2,
                'angular/no-service-method': 2,
                'angular/no-services': 2,
                'angular/on-watch': 2,
                'angular/prefer-component': 2
            }
        }
    }
};
