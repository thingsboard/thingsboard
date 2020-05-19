import chalk from 'chalk';

console.log(
  `
${chalk.bold(`opencollective`)} donate [collective] [amount] [frequency]

  Open the donate page of [collective] (default: collective defined in ${chalk.bold(`package.json`)})

${chalk.dim('Arguments:')}

  collective                      Slug of the collective (e.g. webpack)
  amount                          Amount to give to the collective
  frequency                       one-time, monthly, yearly (default: one-time)

${chalk.dim('Options:')}

  -h, --help                      Output usage information

${chalk.dim('Examples:')}

${chalk.gray('–')} Opens the default donate page:

    ${chalk.cyan('$ opencollective donate webpack')}

${chalk.gray('–')} Opens the donate page to donate $5 USD per month to Webpack:

    ${chalk.cyan(`$ opencollective donate webpack 5 monthly`)}

`
);