# ng-annotate [![Build Status](https://travis-ci.org/olov/ng-annotate.svg?branch=master)](https://travis-ci.org/olov/ng-annotate)
ng-annotate adds and removes AngularJS dependency injection annotations.

Write your code without annotations and mark-up functions to be annotated 
with the `"ngInject"` directive prologue, just like you would 
`"use strict"`. This must be at the beginning of your function.

```js
$ cat source.js
angular.module("MyMod").controller("MyCtrl", function($scope, $timeout) {
    "ngInject";
    ...
});
```

Then run ng-annotate as a build-step to produce this intermediary,
annotated, result (later sent to the minifier of choice):

```js
$ ng-annotate -a source.js
angular.module("MyMod").controller("MyCtrl", ["$scope", "$timeout", function($scope, $timeout) {
    "ngInject";
    ...
}]);
```

Your minifier will most likely retain the `"ngInject"` prologues so use `sed`
or a regexp in your build toolchain to get rid of those on the ng-annotate output.
`sed` example: `ng-annotate -a source.js | sed "s/[\"']ngInject[\"'];*//g"`.
JavaScript regexp example: `source.replace(/["']ngInject["'];*/g, "")`.

You can also use ng-annotate to rebuild or remove existing annotations.
Rebuilding is useful if you like to check-in the annotated version of your
source code. When refactoring, just change parameter names once and let
ng-annotate rebuild the annotations. Removing is useful if you want to
de-annotate an existing codebase that came with checked-in annotations


## Installation and usage

```bash
npm install -g ng-annotate
```

Then run it as `ng-annotate OPTIONS <file>`. The errors (if any) will go to stderr,
the transpiled output to stdout.

The simplest usage is `ng-annotate -a infile.js > outfile.js`.
See [OPTIONS.md](OPTIONS.md) for command-line documentation.

ng-annotate can be used as a library, see [OPTIONS.md](OPTIONS.md) for its API.


## Implicit matching of common code forms
ng-annotate uses static analysis to detect common AngularJS code patterns. When
this works it means that you do not need to mark-up functions with `"ngInject"`.
For a lot of code bases this works very well (use `ng-strict-di` to simplify 
debugging when it doesn't) but for others it is less reliable and you may prefer 
to use `"ngInject"` instead. For more information about implicit matching see 
[IMPLICIT.md](IMPLICIT.md).


## Explicit annotations with ngInject
The recommended `function foo($scope) { "ngInject"; ... }` can be exchanged
for `/*@ngInject*/ function foo($scope) { ... }` or
`ngInject(function foo($scope) { ... })`. If you use the latter form then
then add `function ngInject(v) { return v }` somewhere in your codebase or process
away the `ngInject` function call in your build step.


### Suppressing false positives with ngNoInject
The `/*@ngInject*/`, `ngInject(..)` and `"ngInject"` siblings have three cousins that
are used for the opposite purpose, suppressing an annotation that ng-annotate added
incorrectly (a "false positive"). They are called `/*@ngNoInject*/`, `ngNoInject(..)`
and `"ngNoInject"` and do exactly what you think they do.


## ES6 and TypeScript support
ng-annotate supports ES5 as input so run it with the output from Babel, Traceur,
TypeScript (tsc) and the likes. Use `"ngInject"` on functions you want annotated.
Your transpiler should preserve directive prologues, if not please file a bug on it.


## Highly recommended: enable ng-strict-di
`<div ng-app="myApp" ng-strict-di>`

Do that in your ng-annotate processed (but not minified) builds and AngularJS will
let you know if there are any missing dependency injection annotations.
[ng-strict-di](https://docs.angularjs.org/api/ng/directive/ngApp) is available in 
AngularJS 1.3 or later.


## Tools support
* [Grunt](http://gruntjs.com/): [grunt-ng-annotate](https://www.npmjs.org/package/grunt-ng-annotate) by [Michał Gołębiowski](https://github.com/mzgol)
* [Browserify](http://browserify.org/): [browserify-ngannotate](https://www.npmjs.org/package/browserify-ngannotate) by [Owen Smith](https://github.com/omsmith)
* [Brunch](http://brunch.io/): [ng-annotate-uglify-js-brunch](https://www.npmjs.org/package/ng-annotate-uglify-js-brunch) by [Kagami Hiiragi](https://github.com/Kagami)
* [Gulp](http://gulpjs.com/): [gulp-ng-annotate](https://www.npmjs.org/package/gulp-ng-annotate/) by [Kagami Hiiragi](https://github.com/Kagami)
* [Broccoli](https://github.com/broccolijs/broccoli): [broccoli-ng-annotate](https://www.npmjs.org/package/broccoli-ng-annotate) by [Gilad Peleg](https://github.com/pgilad)
* [Rails asset pipeline](http://guides.rubyonrails.org/asset_pipeline.html): [ngannotate-rails](https://rubygems.org/gems/ngannotate-rails) by [Kari Ikonen](https://github.com/kikonen)
* [Grails asset pipeline](https://github.com/bertramdev/asset-pipeline): [angular-annotate-asset-pipeline](https://github.com/craigburke/angular-annotate-asset-pipeline) by [Craig Burke](https://github.com/craigburke)
* [Webpack](http://webpack.github.io/): [ng-annotate-webpack-plugin](https://www.npmjs.org/package/ng-annotate-webpack-plugin) by [Chris Liechty](https://github.com/cliechty), [ng-annotate-loader](https://www.npmjs.org/package/ng-annotate-loader) by [Andrey Skladchikov](https://github.com/huston007)
* [Middleman](http://middlemanapp.com/): [middleman-ngannotate](http://rubygems.org/gems/middleman-ngannotate) by [Michael Siebert](https://github.com/siebertm)
* [ENB](http://enb-make.info/) (Russian): [enb-ng-techs](https://www.npmjs.org/package/enb-ng-techs#ng-annotate) by [Alexey Gurianov](https://github.com/guria)


## Changes
See [CHANGES.md](CHANGES.md).


## Build and test
ng-annotate is written in ES6 constlet style and uses [defs.js](https://github.com/olov/defs)
to transpile to ES5. See [BUILD.md](BUILD.md) for build and test instructions.


## Issues and contributions
Please provide issues in the form of input, expected output, actual output. Include 
the version of ng-annotate and node that you are using. With pull requests, please 
include changes to the tests as well (tests/original.js, tests/with_annotations.js).


## License
`MIT`, see [LICENSE](LICENSE) file.

ng-annotate is written by [Olov Lassus](https://github.com/olov) with the kind help by
[contributors](https://github.com/olov/ng-annotate/graphs/contributors).
[Follow @olov](https://twitter.com/olov) on Twitter for updates about ng-annotate.
