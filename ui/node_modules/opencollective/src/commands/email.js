import opn from 'opn';

const url = 'mailto:support@opencollective.com?subject=opencollective-cli%20support';
console.log("Opening", "mailto:support@opencollective.com");
opn(url);

process.exit(0);