# CHANGES

## 0.2.0

- Upgrade to React 0.14 (Many thanks to @aabenoja) (#109)
- Bug fix to avoid calling $scope.$apply when you're already in an $apply or $digest (which would cause a bug) (#99)
- Improvements to docs, tests, and code styles

## 0.1.7

- Fix/Enhancement: Return the value from Angular functions invoked within a $scope.$apply, if you require that value (#85). There was mixed discussion on this, and a sufficiently compelling use case was not provided, but I am more so of the opinion that returning is trivial and a generally accepted practice. Using it should not be necessary and could be considered an antipattern, but in the case that a valid use case arises down the road, I'd rather ngReact not be in the way.

## 0.1.6

- Fix: Address bug where a changing prop causes a function to no longer be wrapped within scope.apply (#78)
- Enhancement: Utilize $watchGroup in Angular if it exists to avoid superfluous renders (#69)

## 0.1.5

- Feature: Add the watchDepth attribute to be able to control the type of scope watcher utilized by Angular ('collection' to use *watchCollection*, 'reference' to use *watch* and reference comparison, and defaulting to *watch* and value comparison (#47)
- Enhancement: Docs state you cannot use Angular directives from within React components (#50)

## 0.1.4

- Fix: Make it work with Browserify (#37)
- Enhancement: Allow ability to override default configuration for components created via the reactDirective factory (#34)
- Fix/Enhancement: Allow React components on the window to be namespaced (ex: "Views.Components.ThisReactComponentIsNamespaced")
- Fix/Enhancement: Allow for "controller as" syntax (had been broken) (#40)
- Enhancement: Set up Travis CI

## 0.1.2

- Fix: bug where fallback to use globally exposed React was incorrectly using window.react instead of window.React
- Fix: bug where minified code would always expect CommonJS environment due to "exports" always being an object (since we were using wrap=true in Uglify config)

## 0.1.1

- **Breaking**: Enhancement: Upgrading to React v0.12 (breaking as React begins to deprecate their API)
- Fix: Support usage in CommonJS and AMD environments (thanks to @alexanderbeletsky)
- Fix: bug where fallback to look for React component on the window was not reached because the $injector would throw an error
- Enhancement: Modify reactDirective to be able to take the React component itself as the argument (rather than a String for the React component's name, which is also still supported)
- Enhancement: Unit test support