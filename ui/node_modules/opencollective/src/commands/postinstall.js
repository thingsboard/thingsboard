import { isFancyEnvironment } from '../lib/utils';
import minimist from 'minimist';
import { resolve } from 'path';
/**
 * If we are not on a fancy TTY, just show a barebone message
 * without fancy emoji, centering, fetching data, etc.
 */
const defaultCmd = (isFancyEnvironment()) ? 'rich' : 'plain';

const argv = minimist(process.argv.slice(2), {
  alias: {
    help: 'h'
  }
});

let cmd;
if (argv.help) {
  cmd = 'help'
} else {
  cmd = argv.plain ? 'plain' : defaultCmd;
}

const bin = resolve(__dirname, `./postinstall-${cmd}.js`);
require(bin, 'may-exclude');
