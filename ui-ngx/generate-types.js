/*
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
