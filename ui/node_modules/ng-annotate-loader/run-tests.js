'use strict';

const test = require('tape');
const webpack = require('webpack');
const fs = require('fs');
const SourceMapConsumer = require('source-map').SourceMapConsumer;

test.createStream()
  .pipe(require('tap-spec')())
  .pipe(process.stdout);

const cases = ['typescript', 'simple', 'babel', 'uglifyjs'];

for (let testCase of cases) {
  test('Acceptance tests. Case ' + testCase, (t) => {
    const folder = './cases/' + testCase;

    webpack(require(folder + '/webpack.config.js'), (err, stats) => {
      if (err) {
        throw err; // hard error
      }

      if (stats.hasErrors()) {
        console.error(stats.toString({
          version: false,
          hash: false,
          assets: true,
          chunks: false,
          colors: true,
        }));
      }

      const actualSource = fs.readFileSync(folder + '/dist/build.js', 'utf8');
      const expectedSource = fs.readFileSync(folder + '/reference/build.js', 'utf8');

      t.equal(actualSource, expectedSource, 'Test annotated source');

      const actualMap = fs.readFileSync(folder + '/dist/build.js.map', 'utf8');
      testMap(t, actualMap, require(folder + '/reference/sourcemap-checkpoints'));
    });

    t.plan(2);
  });
}

/**
 *
 * @param t
 * @param content
 * @param {array<{original, generated}>} checkpoints
 */
function testMap(t, content, checkpoints){
  t.test('Check source map cases', (t) => {
    const rawMap = JSON.parse(content);
    const map = new SourceMapConsumer(rawMap);

    const sources = rawMap.sources.map((source) => {
      const matches = source.match(/[^/]+\..+$/);
      return matches ?  matches[0] : source;
    });

    t.equal(sources.length, uniq(sources).length, 'No duplicates in sourcemap sources');

    for(let point of checkpoints) {
      const result = map.generatedPositionFor(point.original);

      t.deepEqual({ line: result.line, column: result.column }, point.generated);
    }
  });

  t.end();
}

function uniq(a) {
  return Array.from(new Set(a));
}
