// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
const child_process = require("child_process");
const fs = require('fs');
const path = require('path');

const typeDir = path.join('.', 'target', 'types');
const srcDir = path.join('.', 'target', 'types', 'src');
const stylesCss = path.join(srcDir, 'styles.css');
const moduleMapPath = path.join('src', 'app', 'modules', 'common', 'modules-map.ts');
const ngcPath = path.join('.', 'node_modules', '.bin', 'ngc');
const tailwindcss = path.join('.', 'node_modules', '.bin', 'tailwindcss');
const tsconfigPath = path.join('src', 'tsconfig.app.json');

console.log(`Remove directory: ${typeDir}`);
try {
  fs.rmSync(typeDir, {recursive: true, force: true,});
} catch (err) {
  console.error(`Remove directory error: ${err}`);
}

const cliCommand = `${ngcPath} --p ${tsconfigPath} --declaration --outDir ${srcDir}`;
executeCliCommand(cliCommand, 'Build types');

fromDir(srcDir, /(\.js|\.js\.map)$/, function (filename) {
  try {
    fs.rmSync(filename);
  } catch (err) {
    console.error(`Remove file error ${filename}: ${err}`);
  }
});
fs.cpSync(moduleMapPath, `${typeDir}/${moduleMapPath}`);

const generateStyleCssCommand = `${tailwindcss} -o ${stylesCss} --minify`;
executeCliCommand(generateStyleCssCommand, 'Generate styles.css');

function executeCliCommand(cliCommand, description) {
  console.log(cliCommand);
  try {
    child_process.execSync(cliCommand);
  } catch (err) {
    console.error(description, err);
    process.exit(1);
  }
}

function fromDir(startPath, filter, callback) {
  if (!fs.existsSync(startPath)) {
    console.log("not dirs", startPath);
    process.exit(1);
  }

  const files = fs.readdirSync(startPath);
  for (let i = 0; i < files.length; i++) {
    const filename = path.join(startPath, files[i]);
    const stat = fs.lstatSync(filename);
    if (stat.isDirectory()) {
      fromDir(filename, filter, callback);
    } else if (filter.test(filename)) {
      callback(filename)
    }
  }
}
