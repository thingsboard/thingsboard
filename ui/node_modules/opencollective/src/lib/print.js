import { execSync } from 'child_process';
import chalk from 'chalk';
import { padding, formatCurrency } from '../lib/utils';

const collective_suggested_donation_amount = process.env.npm_package_collective_suggested_donation_amount;
const collective_suggested_donation_interval = process.env.npm_package_collective_suggested_donation_interval;
const user_agent = process.env.npm_config_user_agent;

export function getDonateURL(collective) {
  var donate_url = collective.url;
  if (collective_suggested_donation_amount) {
    donate_url += `/donate/${collective_suggested_donation_amount}`;
    if (collective_suggested_donation_interval) {
      donate_url += `/${collective_suggested_donation_interval}`;
    }
    donate_url += (npm_config_user_agent.match(/yarn/)) ? '/yarn' : '/npm';
  } else {
    donate_url += '/donate';
  }
  return donate_url;
}

export function print(str, opts) {
  opts = opts || { color: null, align: 'center'};
  if (opts.plain) {
    opts.color = null;
  }
  str = str || '';
  opts.align = opts.align || 'center';
  const terminalCols = process.platform === 'win32' ? 80 : parseInt(execSync(`tput cols`).toString());
  const strLength = str.replace(/\u001b\[[0-9]{2}m/g,'').length;
  const leftPaddingLength = (opts.align === 'center') ? Math.floor((terminalCols - strLength) / 2) : 2; 
  const leftPadding = padding(leftPaddingLength);
  if (opts.color) {
    str = chalk[opts.color](str);
  }

  console.log(leftPadding, str);
}

export function printStats(stats, opts) {
  if (!stats) return;
  print(`Number of contributors: ${stats.contributorsCount}`, opts);
  print(`Number of backers: ${stats.backersCount}`, opts);
  print(`Annual budget: ${formatCurrency(stats.yearlyIncome, stats.currency)}`, opts);
  print(`Current balance: ${formatCurrency(stats.balance, stats.currency)}`, Object.assign({}, { color: 'bold' }, opts));  
}

export function printLogo(logotxt) {
  if (!logotxt) return;
  logotxt.split('\n').forEach(function(line) {
    return print(line, { color: 'blue' });
  });
}

/**
 * Only show emoji on OSx (Windows shell doesn't like them that much ¯\_(ツ)_/¯ )
 * @param {*} emoji 
 */
export function emoji(emoji) {
  if (process.stdout.isTTY && process.platform === 'darwin') {
    return emoji;
  } else {
    return '';
  }
}

export function printFooter(collective) {
  console.log("");
  print(`Thanks for installing ${collective.slug} ${emoji('🙏')}`, { color: 'yellow' });
  print(`Please consider donating to our open collective`, { color: 'dim' });
  print(`to help us maintain this package.`, { color: 'dim' });
  console.log("");
  printStats(collective.stats);
  console.log("");
  print(`${chalk.bold(`${emoji('👉 ')} Donate:`)} ${chalk.underline(getDonateURL(collective))}`);
  console.log("");
}