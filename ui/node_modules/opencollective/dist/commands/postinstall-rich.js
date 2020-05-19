'use strict';

var _minimist = require('minimist');

var _minimist2 = _interopRequireDefault(_minimist);

var _utils = require('../lib/utils');

var _print = require('../lib/print');

var _fetchData = require('../lib/fetchData');

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var collective = (0, _utils.getCollective)();

function init() {
  var promises = [];
  promises.push((0, _fetchData.fetchStats)(collective.url));
  if (collective.logo) {
    promises.push((0, _fetchData.fetchLogo)(collective.logo));
  }

  Promise.all(promises).then(function (results) {
    collective.stats = results[0];
    var logotxt = results[1];

    if (logotxt) {
      (0, _print.printLogo)(logotxt);
    }
    (0, _print.printFooter)(collective);
    process.exit(0);
  }).catch(function (e) {
    (0, _utils.debug)("Error caught: ", e);
    (0, _print.printFooter)(collective);
    process.exit(0);
  });
}

(0, _utils.debug)("process.env", process.env);
if (collective.url) {
  init();
} else {
  console.log("Usage: opencollective postinstall --collective=webpack");
  process.exit(0);
}