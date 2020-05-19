import opn from 'opn';
import { getCollective, getArgs } from '../lib/utils';
import { resolve } from 'path';

const collective = getCollective();

const args = getArgs('collective', 'amount', 'frequency');

if (!collective) {
  const bin = resolve(__dirname, `./donate-help.js`);
  require(bin, 'may-exclude');
  process.exit(0);
}

let url = `${collective.url}/donate`;
if (args.amount) {
  url += `/${args.amount}`;
  if (args.frequency) {
    url += `/${args.frequency}`;
  }
}
console.log("Opening", url);
opn(url);

process.exit(0);