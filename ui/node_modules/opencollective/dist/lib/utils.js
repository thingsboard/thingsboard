'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.debug = debug;
exports.isDevEnvironment = isDevEnvironment;
exports.isFancyEnvironment = isFancyEnvironment;
exports.padding = padding;
exports.formatCurrency = formatCurrency;
exports.getPackageJSON = getPackageJSON;
exports.getCollectiveSlug = getCollectiveSlug;
exports.getCollective = getCollective;
exports.getArgs = getArgs;

var _fs = require('fs');

var _fs2 = _interopRequireDefault(_fs);

var _chalk = require('chalk');

var _chalk2 = _interopRequireDefault(_chalk);

var _minimist = require('minimist');

var _minimist2 = _interopRequireDefault(_minimist);

var _path = require('path');

var _path2 = _interopRequireDefault(_path);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function debug() {
  if (process.env.DEBUG) {
    console.log.apply(this, arguments);
  }
}

function isDevEnvironment() {
  if (process.env.OC_POSTINSTALL_TEST) return true;
  if (process.env.CI || process.env.CONTINUOUS_INTEGRATION) return false;
  return !process.env.NODE_ENV || process.env.NODE_ENV === 'dev' || process.env.NODE_ENV === 'development';
}

function isFancyEnvironment() {
  var npm_config_node_version = process.env.npm_config_node_version;
  return isDevEnvironment() && process.stdout.isTTY && process.platform !== 'win32' && (!npm_config_node_version || parseInt(npm_config_node_version.substr(0, npm_config_node_version.indexOf('.')))) >= 5;
}

function padding(length) {
  var padding = '';
  for (var i = 0; i < length; i++) {
    padding += ' ';
  }
  return padding;
}

function formatCurrency(amount, currency, precision) {
  precision = precision || 0;
  amount = amount / 100; // converting cents

  return amount.toLocaleString(currency, {
    style: 'currency',
    currency: currency,
    minimumFractionDigits: precision,
    maximumFractionDigits: precision
  });
}

var argv = (0, _minimist2.default)(process.argv.slice(2), {
  alias: {
    collective: 'c',
    logo: 'l',
    help: 'h'
  }
});

function getPackageJSON() {
  var packageJSONPath = _path2.default.resolve('./package.json');
  debug("Loading ", packageJSONPath);
  var pkg = void 0;
  try {
    pkg = JSON.parse(_fs2.default.readFileSync(packageJSONPath, "utf8"));
    return pkg;
  } catch (e) {
    debug("error while trying to load ./package.json", "cwd:", process.cwd(), e);
  }
}

function getCollectiveSlug() {
  debug(">>> argv", argv);
  if (argv.collective) return argv.collective;
  if (process.env.npm_package_name) return process.env.npm_package_name;
  if (argv._[0]) return argv._[0];
}

function getCollective() {
  var pkg = void 0;
  var collective = {};
  collective.slug = getCollectiveSlug();
  if (!collective.slug) {
    pkg = getPackageJSON();
    if (pkg && pkg.collective && pkg.collective.url) {
      collective.slug = pkg.collective.url.substr(pkg.collective.url.lastIndexOf('/') + 1).toLowerCase();
    }
  }
  collective.url = process.env.npm_package_collective_url || 'https://opencollective.com/' + collective.slug;
  collective.logo = argv.logo || process.env.npm_package_collective_logo;

  if (!collective.logo) {
    pkg = pkg || getPackageJSON();
    if (pkg.collective) {
      collective.logo = pkg.collective.logo;
    }
  }

  debug(">>> collective", collective);
  return collective;
}

function getArgs() {
  var args = {};
  for (var i in arguments) {
    args[arguments[i]] = argv._[i];
  }
  debug(">>> args", args);
  return args;
}