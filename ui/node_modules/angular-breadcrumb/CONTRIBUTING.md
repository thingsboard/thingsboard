# Contributing to angular-breadcrumb

I am very glad to see this project living with PR from contributors who trust in it. Here is some guidelines to keep the contributions useful and efficient.

## Development hints

### Installation
- Checkout the repository
- Run `npm install`
- Run `bower install`

### Test running
This module uses the classic AngularJS stack with:

- Karma (test runner)
- Jasmine (assertion framework)
- angular-mocks (AngularJS module for testing)

Run the test with the grunt task `grunt test`. It runs the tests with different versions of AngularJS.

### Test developing
Tests are build around modules with a specific `$stateProvider` configuration:

- [Basic configuration](https://github.com/ncuillery/angular-breadcrumb/blob/master/test/mock/test-modules.js#L6): Basic definitions (no template, no controller)
- [Interpolation configuration](https://github.com/ncuillery/angular-breadcrumb/blob/master/test/mock/test-modules.js#L21): States with bindings in `ncyBreadcrumbLabel`
- [HTML configuration](https://github.com/ncuillery/angular-breadcrumb/blob/master/test/mock/test-modules.js#L36): States with HTML in `ncyBreadcrumbLabel`
- [Sample configuration](https://github.com/ncuillery/angular-breadcrumb/blob/master/test/mock/test-modules.js#L41): Bridge towards the sample app configuration for using in tests
- [UI-router's configuration](https://github.com/ncuillery/angular-breadcrumb/blob/master/test/mock/test-ui-router-sample.js#L9): Clone of the UI-router sample app (complemented with breadcrumb configuration)

Theses modules are loaded by Karma and they are available in test specifications.

Specifications are generally related to the directive `ncyBreadcrumb` or the service `$breadcrumb`.

### Sample
If you are not familiar with JS testing. You can run the [sample](http://ncuillery.github.io/angular-breadcrumb/#/sample) locally for testing purposes by using `grunt sample`. Sources are live-reloaded after each changes.

## Submitting a Pull Request
- Fork the [repository](https://github.com/ncuillery/angular-breadcrumb/)
- Make your changes in a new git branch following the coding rules below.
- Run the grunt default task (by typing `grunt` or `grunt default`): it will run the tests and build the module in `dist` directory)
- Commit the changes (including the `dist` directory) by using the commit conventions explained below.
- Push and make the PR


## Coding rules
- When making changes on the source file, please check that your changes are covered by the tests. If not, create a new test case.


## Commit conventions
angular-breadcrumb uses the same strict conventions as AngularJS and UI-router. These conventions are explained [here](https://github.com/angular/angular.js/blob/master/CONTRIBUTING.md#-git-commit-guidelines).

It is very important to fit these conventions especially for types `fix` and `feature` which are used by the CHANGELOG.md generation (it uses the [grunt-conventional-changelog](https://github.com/btford/grunt-conventional-changelog)).
