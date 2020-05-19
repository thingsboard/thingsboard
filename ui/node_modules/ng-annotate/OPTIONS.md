## ng-annotate command-line options

`ng-annotate OPTIONS <file>`. The errors (if any) will go to stderr,
the transpiled output to stdout.

Use the `--add` (`-a`) option to add annotations where non-existing,
use `--remove` (`-r`) to remove all existing annotations,
use `--add --remove` (`-ar`) to rebuild all annotations.

Use the `-o` option to write output to file.

Provide `-` instead of an input `<file>` to read input from stdin.

Use the `--sourcemap` option to generate an inline sourcemap.

Use the `--sourceroot` option to set the sourceRoot property of the generated sourcemap.

Use the `--single_quotes` option to output `'$scope'` instead of `"$scope"`.

Use the `--regexp` option to restrict matching further or to expand matching.
See description further down.

Use the `--list` option to list optional matchers.

Use the `--enable` option to enable optional matcher.

*experimental* Use the `--rename` option to rename providers (services, factories,
controllers, etc.) with a new name when declared and referenced through annotation.
Use it like this: `--rename oldname1 newname1 oldname2 newname2`

*experimental* Use the `--plugin` option to load a user plugin with the provided path,
1.x may change API). See [plugin-example.js](plugin-example.js) for more info.

*experimental* Use the `--stats` option to print statistics on stderr.


## Library (API)
ng-annotate can be used as a library. See [ng-annotate.js](ng-annotate.js) for further info about
options and return value.

```js
var ngAnnotate = require("ng-annotate");
var somePlugin = require("./some/path/some-plugin");
var res = ngAnnotate(src, {
    add: true,
    plugin: [somePlugin],
    rename: [{from: "generalname", to: "uniquename"}, {from: "alpha", to: "beta"}],
    map: { inline: false, inFile: "source.js", sourceRoot: "/path/to/source/root" },
    enable: ["angular-dashboard-framework"],
});
var errorstringArray = res.errors;
var transformedSource = res.src;
var transformedSourceMap = res.map;
```
