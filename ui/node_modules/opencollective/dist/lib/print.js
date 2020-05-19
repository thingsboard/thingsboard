'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.getDonateURL = getDonateURL;
exports.print = print;
exports.printStats = printStats;
exports.printLogo = printLogo;
exports.emoji = emoji;
exports.printFooter = printFooter;

var _child_process = require('child_process');

var _chalk = require('chalk');

var _chalk2 = _interopRequireDefault(_chalk);

var _utils = require('../lib/utils');

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var collective_suggested_donation_amount = process.env.npm_package_collective_suggested_donation_amount;
var collective_suggested_donation_interval = process.env.npm_package_collective_suggested_donation_interval;
var user_agent = process.env.npm_config_user_agent;

function getDonateURL(collective) {
  var donate_url = collective.url;
  if (collective_suggested_donation_amount) {
    donate_url += '/donate/' + collective_suggested_donation_amount;
    if (collective_suggested_donation_interval) {
      donate_url += '/' + collective_suggested_donation_interval;
    }
    donate_url += npm_config_user_agent.match(/yarn/) ? '/yarn' : '/npm';
  } else {
    donate_url += '/donate';
  }
  return donate_url;
}

function print(str, opts) {
  opts = opts || { color: null, align: 'center' };
  if (opts.plain) {
    opts.color = null;
  }
  str = str || '';
  opts.align = opts.align || 'center';
  var terminalCols = process.platform === 'win32' ? 80 : parseInt((0, _child_process.execSync)('tput cols').toString());
  var strLength = str.replace(/\u001b\[[0-9]{2}m/g, '').length;
  var leftPaddingLength = opts.align === 'center' ? Math.floor((terminalCols - strLength) / 2) : 2;
  var leftPadding = (0, _utils.padding)(leftPaddingLength);
  if (opts.color) {
    str = _chalk2.default[opts.color](str);
  }

  console.log(leftPadding, str);
}

function printStats(stats, opts) {
  if (!stats) return;
  print('Number of contributors: ' + stats.contributorsCount, opts);
  print('Number of backers: ' + stats.backersCount, opts);
  print('Annual budget: ' + (0, _utils.formatCurrency)(stats.yearlyIncome, stats.currency), opts);
  print('Current balance: ' + (0, _utils.formatCurrency)(stats.balance, stats.currency), Object.assign({}, { color: 'bold' }, opts));
}

function printLogo(logotxt) {
  if (!logotxt) return;
  logotxt.split('\n').forEach(function (line) {
    return print(line, { color: 'blue' });
  });
}

/**
 * Only show emoji on OSx (Windows shell doesn't like them that much ¯\_(ツ)_/¯ )
 * @param {*} emoji 
 */
function emoji(emoji) {
  if (process.stdout.isTTY && process.platform === 'darwin') {
    return emoji;
  } else {
    return '';
  }
}

function printFooter(collective) {
  console.log("");
  print('Thanks for installing ' + collective.slug + ' ' + emoji('🙏'), { color: 'yellow' });
  print('Please consider donating to our open collective', { color: 'dim' });
  print('to help us maintain this package.', { color: 'dim' });
  console.log("");
  printStats(collective.stats);
  console.log("");
  print(_chalk2.default.bold(emoji('👉 ') + ' Donate:') + ' ' + _chalk2.default.underline(getDonateURL(collective)));
  console.log("");
}