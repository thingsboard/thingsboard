import minimist from 'minimist';
import { resolve } from 'path';
import { getCollective } from '../lib/utils';
import { fetchLogo, fetchStats } from '../lib/fetchData';
import { print, printStats, printLogo } from '../lib/print';

const collective = getCollective();

const argv = minimist(process.argv.slice(2), {
  alias: {
    help: 'h'
  }
});

if (argv.help || !collective) {
  const bin = resolve(__dirname, `./help.js`);
  require(bin, 'may-exclude');
  process.exit(0);
}

const promises = [];
promises.push(fetchStats(collective.url));
if (collective.logo && !argv.plain) {
  promises.push(fetchLogo(collective.logo));
}

Promise.all(promises)
  .then(function(results) {
    collective.stats = results[0];
    const logotxt = results[1];
    const opts = { plain: argv.plain, align: 'left' };
    console.log("");
    if (logotxt) {
      opts.align = 'center';
      printLogo(logotxt);
    }
    print(collective.url, Object.assign({}, opts, { color: 'bold' }));
    console.log("");
    printStats(collective.stats, opts);
    console.log("");
    process.exit(0);     
  })
  .catch(function(e) {
    console.error("Error caught: ", e);
    process.exit(0);
  });