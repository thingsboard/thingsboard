// package metadata file for Meteor.js
var packageName = 'digimet:ng-flow';
var where = 'client'; // where to install: 'client' or 'server'. For both, pass nothing.
var version = '2.7.7';
var summary = 'Flow.js html5 file upload extension on angular.js framework';
var gitLink = 'https://github.com/flowjs/ng-flow.git';
var documentationFile = 'README.md';

// Meta-data
Package.describe({
  name: packageName,
  version: version,
  summary: summary,
  git: gitLink,
  documentation: documentationFile
});

Package.onUse(function(api) {
  api.versionsFrom(['METEOR@0.9.0', 'METEOR@1.0']); // Meteor versions

  api.use('angular:angular@1.2.0', where); // Dependencies
  api.use('digimet:flowjs@2.13.0', where);

  api.addFiles('./dist/ng-flow.js', where); // Files in use
});
