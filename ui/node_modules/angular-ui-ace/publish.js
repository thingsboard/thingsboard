/* jshint node:true */

'use strict';

var fs = require('fs');
var path = require('path');

module.exports = function() {

  var js_dependencies =[
    'bower_components/ace-builds/src-min-noconflict/ace.js',
    'bower_components/ace-builds/src-min-noconflict/theme-twilight.js',
    'bower_components/ace-builds/src-min-noconflict/mode-markdown.js',
    'bower_components/ace-builds/src-min-noconflict/mode-scheme.js',
    'bower_components/ace-builds/src-min-noconflict/worker-javascript.js'
  ];

  function putThemInVendorDir (filepath) {
    return 'vendor/' + path.basename(filepath);
  }

  return {
    humaName : 'UI.Ace',
    repoName : 'ui-ace',
    inlineHTML : fs.readFileSync(__dirname + '/demo/demo.html'),
    inlineJS : fs.readFileSync(__dirname + '/demo/demo.js'),
    css: ['demo/demo.css'],
    js : js_dependencies.map(putThemInVendorDir).concat(['dist/ui-ace.min.js']),
    tocopy : js_dependencies,

    bowerData : { main: './ui-ace.js'}
  };
};
