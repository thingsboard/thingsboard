# AngularJS Template loader for [webpack](http://webpack.github.io/)

Includes your AngularJS templates into your webpack Javascript Bundle. Pre-loads the AngularJS template cache
to remove initial load times of templates.

ngTemplate loader does not minify or process your HTML at all, and instead uses the standard loaders such as [html-loader](https://github.com/webpack-contrib/html-loader)
or [raw-loader](https://github.com/webpack-contrib/raw-loader). This gives you the flexibility to pick and choose your HTML loaders.

## Install

`npm install ngtemplate-loader --save-dev`

## Usage

[Documentation: Using loaders](http://webpack.github.io/docs/using-loaders.html)

ngTemplate loader will export the path of the HTML file, so you can use require directly AngularJS with templateUrl parameters e.g. 

``` javascript
var templateUrl = require('ngtemplate!html!./test.html');

app.directive('testDirective', function() {
    return {
        restrict: 'E',
        templateUrl: templateUrl
    }
});
```


To remove the extra `require`, check out the [Baggage Example](#baggage-example) below.

ngTemplate creates a JS module that initialises the $templateCache with the HTML under the file path e.g. 

``` javascript
require('!ngtemplate?relativeTo=/projects/test/app!html!file.html');
// => generates the javascript:
// angular.module('ng').run(['$templateCache', function(c) { c.put('file.html', '<file.html processed by html-loader>') }]);
```


## Beware of requiring from the directive definition

The following code is wrong, Because it'll operate only after angular bootstraps:
```
app.directive('testDirective', function() {
    return {
        restrict: 'E',
        templateUrl: require('ngtemplate!html!./test.html') // <- WRONG !
    }
});
```

### `relativeTo` and `prefix`

You can set the base path of your templates using `relativeTo` and `prefix` parameters. `relativeTo` is used
to strip a matching prefix from the absolute path of the input html file. `prefix` is then appended to path.

The prefix of the path up to and including the first `relativeTo` match is stripped, e.g.

``` javascript
require('!ngtemplate?relativeTo=/src/!html!/test/src/test.html');
// c.put('test.html', ...)
```

To match the from the start of the absolute path prefix a '//', e.g.

``` javascript
require('!ngtemplate?relativeTo=//Users/WearyMonkey/project/test/!html!/test/src/test.html');
// c.put('src/test.html', ...)
```

You can combine `relativeTo` and `prefix` to replace the prefix in the absolute path, e.g.

``` javascript
require('!ngtemplate?relativeTo=src/&prefix=build/!html!/test/src/test.html');
// c.put('build/test.html', ...)
```

### `module`

By default ngTemplate loader adds a run method to the global 'ng' module which does not need to explicitly required by your app.
You can override this by setting the `module` parameter, e.g.

``` javascript
require('!ngtemplate?module=myTemplates&relativeTo=/projects/test/app!html!file.html');
// => returns the javascript:
// angular.module('myTemplates').run(['$templateCache', function(c) { c.put('file.html', '<file.html processed by html-loader>') }]);
```

### Parameter Interpolation

`module`, `relativeTo` and `prefix` parameters are interpolated using 
[Webpack's standard interpolation rules](https://github.com/webpack/loader-utils#interpolatename).
Interpolation regular expressions can be passed using the extra parameters `moduleRegExp`, `relativeToRegExp` 
and `prefixRegExp` which apply to single parameters, or `regExp` which will apply to all three parameters. 


### Path Separators (Or using on Windows)

 By default, ngTemplate loader will assume you are using unix style path separators '/' for html paths in your project.
 e.g. `templateUrl: '/views/app.html'`. If however you want to use Window's style path separators '\'
 e.g. `templateUrl: '\\views\\app.html'` you can override the separator by providing the pathSep parameter.

 ```javascript
 require('ngtemplate?pathSep=\\!html!.\\test.html')
 ```

 Make sure you use the same path separator for the `prefix` and `relativeTo` parameters, all templateUrls and in your webpack.config.js file.

### Using with npm requires

This module relies on angular being available on `window` object. However, in cases angular is connected from `node_modules` via `require('angular')`, option to force this module to get the angular should be used:

```javascript
require('!ngtemplate?requireAngular!html!file.html');

// => generates the javascript:
// var angular = require('angular');
// angular.module('ng').run(['$templateCache', function(c) { c.put('file.html', '<file.html processed by html-loader>') }]);
```

## Webpack Config

It's recommended to adjust your `webpack.config` so `ngtemplate!html!` is applied automatically on all files ending with `.html`:

``` javascript
module.exports = {
  module: {
    loaders: [
      {
        test: /\.html$/,
        loader: 'ngtemplate?relativeTo=' + (path.resolve(__dirname, './app')) + '/!html'
      }
    ]
  }
};
```

Then you only need to write: `require('file.html')`.

## Dynamic Requires

Webpack's dynamic requires do not implicitly call the IIFE wrapping each
call to `window.angular.module('ng').run(...)`, so if you use them to
require a folder full of partials, you must manually iterate through the
resulting object and resolve each dependency in order to accomodate angular's
side-effects oriented module system:

``` javascript
var templates = require.context('.', false, /\.html$/);

templates.keys().forEach(function(key) {
  templates(key);
});

```

## Baggage Example

ngTemplate loader works well with the [Baggage Loader](https://github.com/deepsweet/baggage-loader) to remove all those 
extra HTML and CSS requires. See an example of a directive and webpack.config.js below. Or take a look at more complete
example in the examples/baggage folder.

With a folder structure:

```
app/
├── app.js
├── index.html
├── webpack.config.js
└── my-directive/
    ├── my-directive.js
    ├── my-directive.css
    └── my-directive.html
```

and a webpack.config.js like:

``` javascript
module.exports = {
  module: {
    preLoaders: [
      { 
        test: /\.js$/, 
        loader: 'baggage?[file].html&[file].css' 
      }
    ],
    loaders: [
      {
        test: /\.html$/,
        loader: 'ngtemplate?relativeTo=' + __dirname + '/!html'
      }
    ]
  }
};
```

You can now skip the initial require of html and css like so:

``` javascript
app.directive('myDirective', function() {
    return {
        restrict: 'E',
        templateUrl: require('./my-directive.html')
    }
});
```

## License

MIT (http://www.opensource.org/licenses/mit-license.php)
