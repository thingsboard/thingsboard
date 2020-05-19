'use strict';

var _minimist = require('minimist');

var _minimist2 = _interopRequireDefault(_minimist);

var _path = require('path');

var _utils = require('../lib/utils');

var _fetchData = require('../lib/fetchData');

var _print = require('../lib/print');

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var collective = (0, _utils.getCollective)();

var argv = (0, _minimist2.default)(process.argv.slice(2), {
  alias: {
    help: 'h'
  }
});

if (argv.help || !collective) {
  var bin = (0, _path.resolve)(__dirname, './help.js');
  require(bin, 'may-exclude');
  process.exit(0);
}

var promises = [];
promises.push((0, _fetchData.fetchStats)(collective.url));
if (collective.logo && !argv.plain) {
  promises.push((0, _fetchData.fetchLogo)(collective.logo));
}

Promise.all(promises).then(function (results) {
  collective.stats = results[0];
  var logotxt = results[1];
  var opts = { plain: argv.plain, align: 'left' };
  console.log("");
  if (logotxt) {
    opts.align = 'center';
    (0, _print.printLogo)(logotxt);
  }
  (0, _print.print)(collective.url, Object.assign({}, opts, { color: 'bold' }));
  console.log("");
  (0, _print.printStats)(collective.stats, opts);
  console.log("");
  process.exit(0);
}).catch(function (e) {
  console.error("Error caught: ", e);
  process.exit(0);
});