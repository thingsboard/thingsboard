[![Build Status](https://travis-ci.org/ngReact/ngReact.svg?branch=master)](https://travis-ci.org/ngReact/ngReact) [![Pair on this](https://tf-assets-staging.s3.amazonaws.com/badges/thinkful_repo_badge.svg)](http://start.thinkful.com/react/?utm_source=github&utm_medium=badge&utm_campaign=ngReact)
# ngReact

The [React.js](http://facebook.github.io/react/) library can be used as a view component in web applications. ngReact is an Angular module that allows React Components to be used in [AngularJS](https://angularjs.org/) applications.

Motivation for this could be any of the following:

- You need greater performance than Angular can offer (two way data binding, Object.observe, too many scope watchers on the page) and React is typically more performant due to the Virtual DOM and other optimizations it can make

- React offers an easier way to think about the state of your UI; instead of data flowing both ways between controller and view as in two way data binding, React typically eschews this for a more unidirectional/reactive paradigm

- Someone in the React community released a component that you would like to try out

- You're already deep into an Angular application and can't move away, but would like to experiment with React

# Installation

Install via Bower:

```bash
bower install ngReact
```

or via npm:

```bash
npm install ngreact
```

# Usage

Then, just make sure Angular, React, and ngReact are on the page,
```html
<script src="bower_components/angular/angular.js"></script>
<script src="bower_components/react/react.js"></script>
<script src="bower_components/react/react-dom.js"></script>
<script src="bower_components/ngReact/ngReact.min.js"></script>
```

and include the 'react' Angular module as a dependency for your new app

```html
<script>
    angular.module('app', ['react']);
</script>
```

and you're good to go.

# Features

Specifically, ngReact is composed of:

- `react-component`, an Angular directive that delegates off to a React Component
- `reactDirective`, a service for converting React components into the `react-component` Angular directive

**ngReact** can be used in existing angular applications, to replace entire or partial views with react components.

## The react-component directive

The reactComponent directive is a generic wrapper for embedding your React components.

With an Angular app and controller declaration like this:

```javascript
angular.module('app', ['react'])
  .controller('helloController', function($scope) {
    $scope.person = { fname: 'Clark', lname: 'Kent' };
  });
```

And a React Component like this

```javascript
/** @jsx React.DOM */
var HelloComponent = React.createClass({
  propTypes: {
    fname : React.PropTypes.string.isRequired,
    lname : React.PropTypes.string.isRequired
  },
  render: function() {
    return <span>Hello {this.props.fname} {this.props.lname}</span>;
  }
})
app.value('HelloComponent', HelloComponent);
```

The component can be used in an Angular view using the react-component directive like so:

```html
<body ng-app="app">
  <div ng-controller="helloController">
    <react-component name="HelloComponent" props="person" watch-depth="reference"/>
  </div>
</body>
```

Here:

- `name` attribute checks for an Angular injectable of that name and falls back to a globally exposed variable of the same name, and
- `props` attribute indicates what scope properties should be exposed to the React component
- `watch-depth` attribute indicates what watch strategy to use to detect changes on scope properties.  The possible values for react-component are `reference`, `collection` and `value` (default)

## The reactDirective service

The reactDirective factory, in contrast to the reactComponent directive, is meant to create specific directives corresponding to React components. In the background, this actually creates and sets up directives specifically bound to the specified React component.

If, for example, you wanted to use the same React component in multiple places, you'd have to specify `<react-component name="yourComponent" props="props"></react-component>` repeatedly, but if you used reactDirective factory, you could create a `<your-component></your-component>` directive and simply use that everywhere.

The service takes the React component as the argument.

```javascript
app.directive('helloComponent', function(reactDirective) {
  return reactDirective(HelloComponent);
});
```

Alternatively you can provide the name of the component

```javascript
app.directive('helloComponent', function(reactDirective) {
  return reactDirective('HelloComponent');
});
```

This creates a directive that can be used like this:

```html
<body ng-app="app">
  <div ng-controller="helloController">
    <hello-component fname="person.fname" lname="person.lname" watch-depth="reference"></hello-component>
  </div>
</body>
```

The `reactDirective` service will read the React component `propTypes` and watch attributes with these names. If your react component doesn't have `propTypes` defined you can pass in an array of attribute names to watch.  By default, attributes will be watched by value however you can also choose to watch by reference or collection by supplying the watch-depth attribute.  Possible values are `reference`, `collection` and `value` (default).

```javascript
app.directive('hello', function(reactDirective) {
  return reactDirective(HelloComponent, ['fname', 'lname']);
});
```

If you want to change the configuration of the directive created the `reactDirective` service, e.g. change `restrict: 'E'` to `restrict: 'C'`, you can do so by passing in an object literal with the desired configuration.

```javascript
app.directive('hello', function(reactDirective) {
  return reactDirective(HelloComponent, undefined, {restrict: 'C'});
});
```

### Minification
A lot of automatic annotation libraries including ng-annotate skip implicit annotations of directives. Because of that you might get the following error when using directive in minified code:
```
Unknown provider: eProvider <- e <- helloDirective
```
To fix it add explicit annotation of dependency
```javascript
var helloDirective = function(reactDirective) {
  return reactDirective('HelloComponent');
};
helloDirective.$inject = ['reactDirective'];
app.directive('hello', helloDirective);
```


## Reusing Angular Injectables

In an existing Angular application, you'll often have existing services or filters that you wish to access from your React component. These can be retrieved using Angular's dependency injection. The React component will still be render-able as aforementioned, using the react-component directive.

It's also possible to pass Angular injectables and other variables as fourth parameter straight to the reactDirective, which will then attach them to the props

```javascript
app.directive('helloComponent', function(reactDirective, $ngRedux) {
  return reactDirective(HelloComponent, undefined, {}, {store: $ngRedux});
});
```

Be aware that you can not inject Angular directives into JSX.

```javascript
app.filter('hero', function() {
  return function(person) {
    if (person.fname === 'Clark' && person.lname === 'Kent') {
      return 'Superman';
    }
    return person.fname + ' ' + person.lname;
  };
});

/** @jsx React.DOM */
app.factory('HelloComponent', function($filter) {
  return React.createClass({
    propTypes: {
      person: React.PropTypes.object.isRequired,
    },
    render: function() {
      return <span>Hello $filter('hero')(this.props.person)</span>;
    }
  });
});
```

```html
<body ng-app="app">
  <div ng-controller="helloController">
    <react-component name="HelloComponent" props="person" />
  </div>
</body>
```

## Jsx Transformation in the browser
During testing you may want to run the `JSXTransformer` in the browser. For this to work with angular you need to make sure that the jsx code has been transformed before the angular application is bootstrapped. To do so you can [manually bootstrap](https://docs.angularjs.org/guide/bootstrap#manual-initialization) the angular application. For a working example see the [jsx-transformer example](https://github.com/davidchang/ngReact/tree/master/examples/jsx-transformer).

NOTE: The workaround for this is hacky as the angular bootstap is postponed in with a `setTimeout`, so consider [transforming jsx in a build step](http://facebook.github.io/react/docs/getting-started.html#offline-transform).

## Usage with [webpack](https://webpack.github.io/) and AngularJS < 1.3.14

CommonJS support was [added to AngularJS in version 1.3.14](https://github.com/angular/angular.js/blob/master/CHANGELOG.md#1314-instantaneous-browserification-2015-02-24). If you use webpack and need to support AngularJS < 1.3.14, you should use webpack's [exports-loader](https://github.com/webpack/exports-loader) so that `require('angular')` returns the correct value. Your webpack configuration should include the following loader config:

```js
...
module: {
  loaders: [
    {
      test: path.resolve(__dirname, 'node_modules/angular/angular.js'),
      loader: 'exports?window.angular'
    }
  ]
},
...
```

## Developing
Before starting development run

```bash
npm install
bower install
```

Build minified version and run tests with

```bash
grunt
```

Continually run test during development with

```bash
grunt karma:background watch
```

# Community

## Maintainers

- Kasper Bøgebjerg Pedersen (@kasperp)
- David Chang (@davidchang)

## Contributors

- Tihomir Kit (@pootzko)
- Alexander Beletsky (@alexanderbeletsky)
- @matthieu-ravey
- @ethul
- Devin Jett (@djett41)
- Marek Kalnik (@marekkalnik)
- @oriweingart
- Basarat Ali Syed (@basarat)
- Rene Bischoff (@Fjandin)
- Zach Pratt (@zpratt)
- Alex Abenoja (@aabenoja)
