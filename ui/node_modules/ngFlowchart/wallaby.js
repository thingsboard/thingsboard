module.exports = function (wallaby) {
  return {

    testFramework: 'jasmine@2.2.1',
    files: [
      'app/bower_components/angular/angular.js',
      'app/bower_components/angular-loader/angular-loader.min.js',
      'app/bower_components/angular-mocks/angular-mocks.js',
      'app/bower_components/angular-route/angular-route.min.js',
      'app/bower_components/bind-polyfill/index.js',
      'app/*.js',
      'dist/onedatastyle.css',
      'app/flowchart/**/*.html',
      'app/flowchart/flowchart.js',
      'app/flowchart/**/*.js',
      '!app/flowchart/**/*_test.js',
      '!app/server.js'
    ],
    tests: [
      'app/flowchart/**/*_test.js'
    ],
    preprocessors: {
      'app/**/*.html': function (file) {
        return require('wallaby-ng-html2js-preprocessor').transform(file, {
          // strip this from the file path
          stripPrefix: 'app/',
          //stripSufix: '.ext',
          // prepend this to the
          //prependPrefix: '/',

          // setting this option will create only a single module that contains templates
          // from all the files, so you can load them all with module('foo')
          moduleName: 'flowchart-templates'
        })
      },
    }
  };
}
;
