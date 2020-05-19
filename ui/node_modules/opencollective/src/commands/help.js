import chalk from 'chalk';

console.log(
  `
${chalk.bold(`opencollective`)} [collective] <command>

Command line utility to manage your open collectives (work in progress)
Feedback and conversations: https://slack.opencollective.org

${chalk.dim('Commands:')}

  info | stats   Print the stats of the collective (default)
  donate         Donate to [collective] (default: collective defined in ${chalk.bold(`package.json`)})
  open           Open the website of the collective
  postinstall    Print the post install message for the collective (default: current collective)

  ${chalk.dim('Support:')}

    slack                         Open https://slack.opencollective.org
    twitter                       Open https://twitter.com/opencollect
    email                         Open mailto:support@opencollective.com

  ${chalk.dim('Priority support:')}

    sudo opencollective support

${chalk.dim('Options:')}

  -h, --help                                      Output usage information
  -c ${chalk.bold.underline('COLLECTIVE')}, --collective=${chalk.bold.underline('COLLECTIVE')}          Slug of the collective
  -t ${chalk.bold.underline('TOKEN')}, --token=${chalk.bold.underline('TOKEN')}                         Open Collective Login token
  -gt ${chalk.bold.underline('GITHUB_TOKEN')}, --github_token=${chalk.bold.underline('GITHUB_TOKEN')}   Github token

${chalk.dim('Examples:')}


${chalk.gray('–')} Get the latest stats for the Hoodie collective:

    ${chalk.cyan('$ opencollective hoodie')}

${chalk.gray('–')} Donate to webpack:

    ${chalk.cyan('$ opencollective donate webpack')}

${chalk.gray('–')} Run post install:

    ${chalk.cyan('$ opencollective postinstall webpack')}

${chalk.gray('–')} Run post install in plain text mode (no emoji, no ascii art):

    ${chalk.cyan(`$ opencollective postinstall cyclejs --plain`)}


`
);