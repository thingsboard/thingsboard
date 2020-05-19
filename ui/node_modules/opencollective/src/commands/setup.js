const utils = require("../lib/utils");
const debug = utils.debug;

if (!utils.isDevEnvironment()) {
  debug("Not in dev environment");
  process.exit(0);
}

// In some CI environment, NODE_ENV might not be defined.
// We exit if `inquirer` module is not installed
try {
  require.resolve("inquirer");
} catch(e) {
  debug("Can't find inquirer module, exiting");
  process.exit(0);
}

const inquirer = require("inquirer");
if (typeof inquirer.prompt([]).then !== "function") {
  debug("Wrong version of inquirer, exiting");
  process.exit(0);
}

const fs = require("fs");
const path = require("path");
const fetchData = require("../lib/fetchData");
const print = require("../lib/print");

const fetchLogo = fetchData.fetchLogo;
const fetchBanner = fetchData.fetchBanner;
const printLogo = print.printLogo;

const projectPackageJSON = path.normalize("./package.json");
const projectREADME = path.normalize("./README.md");

var pkg;
try {
  pkg = JSON.parse(fs.readFileSync(projectPackageJSON, "utf8"));
  debug("package.json successfully loaded for " + pkg.name);
} catch(e) {
  debug("Unable to load " + process.cwd() + "/" + projectPackageJSON, e);
}
if (!pkg) {
  console.log("Cannot load the `package.json` of your project");
  console.log("Please make sure you are running `opencollective postinstall` from the root directory of your project.")
  console.log("");
  process.exit(0);
} else if(pkg.collective && pkg.collective.url) {
  debug("Open Collective already configured 👌");
  process.exit(0);
}

const askQuestions = function() {

  if (process.env.OC_POSTINSTALL_TEST) {
    return {
      collectiveSlug: pkg.name,
      logo: "https://opencollective.com/opencollective/logo.txt"
    };
  }

  const questions = [
    {
      type: "input",
      name: "collectiveSlug",
      message: "Enter the slug of your collective (https://opencollective.com/:slug)",
      default: pkg.name,
      validate: function(str) {
        if(str.match(/^[a-zA-Z\-0-9]+$/)) return true;
        else return "Please enter a valid slug (e.g. https://opencollective.com/webpack)";
      }
    },
    {
      type: "list",
      name: "showLogo",
      message: "What logo should we use?",
      choices: function(answers) {
        return [
          { name: "Open Collective logo (see above)", value: "https://opencollective.com/opencollective/logo.txt" },
          { name: "The logo of your Collective (https://opencollective.com/" + answers.collectiveSlug + "/logo.txt)", value: "https://opencollective.com/" + answers.collectiveSlug + "/logo.txt" },
          { name: "Custom URL", value: "custom"},
          { name: "No logo", value: null }
        ];
      }
    },
    {
      type: "input",
      name: "logo",
      message: "URL of your logo in ASCII art",
      default: function(answers) {
        "https://opencollective.com/" + answers.collectiveSlug + "/logo.txt"
      },
      validate: function(str) {
        if(str.match(/^https?:\/\/[^\/]+\/.+$/)) return true;
        else return "Please enter a valid url (e.g. https://opencollective.com/webpack/logo.txt)";
      },
      when: function(answers) {
        return (answers.showLogo === "custom");
      }
    }
  ];

  console.log("");
  console.log("You don't have any collective set in your package.json");
  console.log("Let's fix this, shall we?");
  console.log("");
  return inquirer.prompt(questions).catch(function(e) {
    debug("Error while running the prompt", e);
    process.exit(0);
  });
}

const ProcessAnswers = function(answers) {
  console.log("> Updating your package.json");
  pkg.collective = {
    type: "opencollective",
    url: "https://opencollective.com/" + answers.collectiveSlug
  }
  const logo = answers.logo || answers.showLogo;
  if (logo) {
    pkg.collective.logo = logo;
  } else {
    delete pkg.collective.logo;
  }
  var postinstall = "opencollective postinstall";
  pkg.scripts = pkg.scripts || {};
  if (pkg.scripts.postinstall && pkg.scripts.postinstall.indexOf(postinstall) === -1) {
    pkg.scripts.postinstall = pkg.scripts.postinstall + " && " + postinstall;
  } else {
    pkg.scripts.postinstall = postinstall;
  }
  fs.writeFileSync(projectPackageJSON, JSON.stringify(pkg, null, 2), "utf8");
  return updateREADME(answers.collectiveSlug);
}

const updateREADME = function(collectiveSlug) {
  const badgesmd = "[![Backers on Open Collective](https://opencollective.com/" + collectiveSlug + "/backers/badge.svg)](#backers) [![Sponsors on Open Collective](https://opencollective.com/" + collectiveSlug + "/sponsors/badge.svg)](#sponsors)";
  const badgeshtml = "<a href=\"#backers\" alt=\"sponsors on Open Collective\"><img src=\"https://opencollective.com/" + collectiveSlug + "/backers/badge.svg\" /></a> <a href=\"#sponsors\" alt=\"Sponsors on Open Collective\"><img src=\"https://opencollective.com/" + collectiveSlug + "/sponsors/badge.svg\" /></a>";

  var readme;
  try {
    readme = fs.readFileSync(projectREADME, "utf8");

    if (readme.indexOf("https://opencollective.com/" + collectiveSlug + "/backers/badge.svg") !== -1) {
      console.log("Looks like you already have Open Collective added to your README.md, skipping this step.")
      return;
    }

    const lines = readme.split("\n");
    const newLines = [];

    var firstBadgeDetected = false;
    lines.forEach(function(line) {
      if (!firstBadgeDetected && (line.match(/badge.svg/) || line.match(/img.shields.io/))) {
        firstBadgeDetected = true;
        newLines.push(line.match(/<img src/) ? badgeshtml : badgesmd);
      }
      newLines.push(line);
    })

    return fetchBanner(collectiveSlug).then(function(banner) {
      newLines.push(banner);
      console.log("> Adding badges and placeholders for backers and sponsors on your README.md");
      return fs.writeFileSync(projectREADME, newLines.join("\n"), "utf8");
    });
  } catch(e) {
    console.log("> Unable to open your README.md file");
    return;
  }
}

console.log("");
fetchLogo("https://opencollective.com/opencollective/logo.txt")
  .then(printLogo)
  .then(askQuestions)
  .then(ProcessAnswers)
  .then(function() {
    console.log("Done.");
    console.log("");
    console.log("Please double check your new updated README.md to make sure everything looks 👌.");
    console.log("");
    console.log("Protip: You can also suggest a donation amount.");
    console.log("See the docs for more options: https://github.com/opencollective/opencollective-cli");
    console.log("");
    console.log("Have a great day!");
    return process.exit(0);
  })
  .catch(function(e) {
    debug("Error while trying to fetch the open collective logo or running the prompt", e);
    process.exit(0)
  });
