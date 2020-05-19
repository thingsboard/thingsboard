var config = require('config');

function getTestFiles() {
  var files = [];

  ['thirdParty', 'tests', 'production'].forEach(function (testDependency) {
    config.scripts[testDependency].forEach(function (script) {
      files.push(script);
    });
  });

  return files;
}

function getPreprocessors() {
  var preprocessors = {};

  ['production', 'tests'].forEach(function (testDependency) {
    config.scripts[testDependency].forEach(function (script) {
      preprocessors[script] = ['webpack', 'sourcemap'];
    });
  });

  return preprocessors;
}

module.exports = function (karmaConfig) {
  var files = getTestFiles(),
    preprocessors = getPreprocessors(files);

  karmaConfig.set({
    basePath: '',

    frameworks: ['mocha', 'chai', 'sinon'],

    files: files,

    exclude: [],

    preprocessors: preprocessors,

    webpack: {
      devtool: 'inline-source-map',
      module: {
        loaders: [
          {
            test: /\.js$/, loader: 'babel-loader',
            exclude: /node_modules/
          }
        ]
      }
    },

    webpackServer: {
      noInfo: true
    },

    reporters: ['dots'],

    port: 9876,

    colors: true,

    logLevel: karmaConfig.LOG_INFO,

    autoWatch: false,

    browsers: ['PhantomJS2'],

    singleRun: config.ci
  });
};
