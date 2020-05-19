import minimist from 'minimist';

import { debug, getCollective, padding, getPackageJSON } from '../lib/utils';
import { printLogo, printFooter, printStats} from '../lib/print';
import { fetchStats, fetchLogo } from '../lib/fetchData';

const collective = getCollective();

function init() {
  const promises = [];
  promises.push(fetchStats(collective.url));
  if (collective.logo) {
    promises.push(fetchLogo(collective.logo));
  }

  Promise.all(promises)
    .then(function(results) {
      collective.stats = results[0];
      const logotxt = results[1];

      if (logotxt) {
        printLogo(logotxt);
      }
      printFooter(collective);
      process.exit(0);     
    })
    .catch(function(e) {
      debug("Error caught: ", e);
      printFooter(collective);
      process.exit(0);
    })
}

debug("process.env", process.env);
if (collective.url) {
  init();
} else {
  console.log("Usage: opencollective postinstall --collective=webpack");
  process.exit(0);
}