import chalk from 'chalk';

console.log(
  `
${chalk.bold(`opencollective`)} postinstall [collective]
pwd
  Print the post install message for [collective] (default: collective defined in ${chalk.bold(`package.json`)})

${chalk.dim('Environment variables:')}

  npm_package_name                Slug of the collective (e.g. webpack)
  npm_package_collective_url      Url of the collective (e.g. https://opencollective.com/webpack)
  npm_package_collective_logo     Url of the logo of the collective (in ascii art)

  Those environment variables are automatically set by npm if you run this command via an npm script (e.g. npm run postinstall)
  Make sure you have defined your collective in your ${chalk.bold(`package.json`)} as follows:
    {
      "collective": {
        "type": "opencollective",
        "url": "https://opencollective.com/[collective]",
        "logo": "[logo]"
      }
    }


${chalk.dim('Options:')}

  -h, --help                      Output usage information

${chalk.dim('Examples:')}

${chalk.gray('–')} Run post install:

    ${chalk.cyan('$ opencollective postinstall webpack')}

${chalk.gray('–')} Run post install in plain text mode (no emoji, no ascii art):

    ${chalk.cyan(`$ opencollective postinstall cyclejs --plain`)}

`
);