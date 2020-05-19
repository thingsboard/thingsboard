# eslint plugin angular [![Npm version](https://img.shields.io/npm/v/eslint-plugin-angular.svg)](https://www.npmjs.com/package/eslint-plugin-angular) [![Npm downloads per month](https://img.shields.io/npm/dm/eslint-plugin-angular.svg)](https://www.npmjs.com/package/eslint-plugin-angular)

[![Greenkeeper badge](https://badges.greenkeeper.io/Gillespie59/eslint-plugin-angular.svg)](https://greenkeeper.io/)

> ESLint rules for your angular project with checks for best-practices, conventions or potential errors.

[![Build Status](https://img.shields.io/travis/Gillespie59/eslint-plugin-angular/master.svg)](https://travis-ci.org/Gillespie59/eslint-plugin-angular)
[![Npm dependencies](https://img.shields.io/david/Gillespie59/eslint-plugin-angular.svg)](https://david-dm.org/Gillespie59/eslint-plugin-angular)
[![devDependency Status](https://img.shields.io/david/dev/Gillespie59/eslint-plugin-angular.svg)](https://david-dm.org/Gillespie59/eslint-plugin-angular#info=devDependencies)
[![Join the chat at https://gitter.im/Gillespie59/eslint-plugin-angular](https://img.shields.io/gitter/room/nwjs/nw.js.svg)](https://gitter.im/Gillespie59/eslint-plugin-angular?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Coverage Status](https://img.shields.io/coveralls/Gillespie59/eslint-plugin-angular/master.svg)](https://coveralls.io/r/Gillespie59/eslint-plugin-angular?branch=master)

## Summary

This repository will give access to new rules for the ESLint tool. You should use it only if you are developing an AngularJS application.

Since the 0.0.4 release, some rules defined in [John Papa's Guideline](https://github.com/johnpapa/angular-styleguide/blob/master/a1) have been implemented. In the description below, you will have a link to the corresponding part of the guideline, in order to have more information.



## Contents

- [Usage with shareable config](#usage-with-shareable-config)
- [Usage without shareable config](#usage-without-shareable-config)
- [Rules](#rules)
- [Need your help](#need-your-help)
- [How to create a new rule](#how-to-create-a-new-rule)
- [Default ESLint configuration file](#default-eslint-configuration-file)
- [Who uses it?](#who-uses-it)
- [Team](#team)



## Usage with [shareable](http://eslint.org/docs/developer-guide/shareable-configs.html) config

1. Install `eslint` as a dev-dependency:

    ```shell
    npm install --save-dev eslint
    ```

2. Install `eslint-plugin-angular` as a dev-dependency:

    ```shell
    npm install --save-dev eslint-plugin-angular
    ```

3. Use the shareable config by adding it to your `.eslintrc`:

    ```yaml
    extends: plugin:angular/johnpapa
    ```



## Usage without shareable config

1. Install `eslint` as a dev-dependency:

    ```shell
    npm install --save-dev eslint
    ```

2. Install `eslint-plugin-angular` as a dev-dependency:

    ```shell
    npm install --save-dev eslint-plugin-angular
    ```

3. Enable the plugin by adding it to your `.eslintrc`:

    ```yaml
    plugins:
      - angular
    ```
4. You can also configure these rules in your `.eslintrc`. All rules defined in this plugin have to be prefixed by 'angular/'

    ```yaml
    plugins:
      - angular
    rules:
      - angular/controller_name: 0
    ```

----


## Rules

Rules in eslint-plugin-angular are divided into several categories to help you better understand their value.


### Possible Errors

The following rules detect patterns that can lead to errors.

 * [avoid-scope-typos](docs/rules/avoid-scope-typos.md) - Avoid mistakes when naming methods defined on the scope object
 * [module-getter](docs/rules/module-getter.md) - disallow to reference modules with variables and require to use the getter syntax instead `angular.module('myModule')` ([y022](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y022))
 * [module-setter](docs/rules/module-setter.md) - disallow to assign modules to variables (linked to [module-getter](docs/module-getter.md) ([y021](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y021))
 * [no-private-call](docs/rules/no-private-call.md) - disallow use of internal angular properties prefixed with $$

### Best Practices

These are rules designed to prevent you from making mistakes. They either prescribe a better way of doing something or help you avoid footguns..

 * [component-limit](docs/rules/component-limit.md) - limit the number of angular components per file ([y001](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y001))
 * [controller-as-route](docs/rules/controller-as-route.md) - require the use of controllerAs in routes or states ([y031](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y031))
 * [controller-as-vm](docs/rules/controller-as-vm.md) - require and specify a capture variable for `this` in controllers ([y032](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y032))
 * [controller-as](docs/rules/controller-as.md) - disallow assignments to `$scope` in controllers ([y031](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y031))
 * [deferred](docs/rules/deferred.md) - use `$q(function(resolve, reject){})` instead of `$q.deferred`
 * [di-unused](docs/rules/di-unused.md) - disallow unused DI parameters
 * [directive-restrict](docs/rules/directive-restrict.md) - disallow any other directive restrict than 'A' or 'E' ([y074](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y074))
 * [empty-controller](docs/rules/empty-controller.md) - disallow empty controllers
 * [no-controller](docs/rules/no-controller.md) - disallow use of controllers (according to the component first pattern)
 * [no-inline-template](docs/rules/no-inline-template.md) - disallow the use of inline templates
 * [no-run-logic](docs/rules/no-run-logic.md) - keep run functions clean and simple ([y171](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y171))
 * [no-services](docs/rules/no-services.md) - disallow DI of specified services for other angular components (`$http` for controllers, filters and directives)
 * [on-watch](docs/rules/on-watch.md) - require `$on` and `$watch` deregistration callbacks to be saved in a variable
 * [prefer-component](docs/rules/prefer-component.md) - 

### Deprecated Angular Features

These rules prevent you from using deprecated angular features.

 * [no-cookiestore](docs/rules/no-cookiestore.md) - use `$cookies` instead of `$cookieStore`
 * [no-directive-replace](docs/rules/no-directive-replace.md) - disallow the deprecated directive replace property
 * [no-http-callback](docs/rules/no-http-callback.md) - disallow the `$http` methods `success()` and `error()`

### Naming

These rules help you to specify several naming conventions.

 * [component-name](docs/rules/component-name.md) - require and specify a prefix for all component names
 * [constant-name](docs/rules/constant-name.md) - require and specify a prefix for all constant names ([y125](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y125))
 * [controller-name](docs/rules/controller-name.md) - require and specify a prefix for all controller names ([y123](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y123), [y124](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y124))
 * [directive-name](docs/rules/directive-name.md) - require and specify a prefix for all directive names ([y073](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y073), [y126](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y126))
 * [factory-name](docs/rules/factory-name.md) - require and specify a prefix for all factory names ([y125](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y125))
 * [file-name](docs/rules/file-name.md) - require and specify a consistent component name pattern ([y120](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y120), [y121](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y121))
 * [filter-name](docs/rules/filter-name.md) - require and specify a prefix for all filter names
 * [module-name](docs/rules/module-name.md) - require and specify a prefix for all module names ([y127](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y127))
 * [provider-name](docs/rules/provider-name.md) - require and specify a prefix for all provider names ([y125](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y125))
 * [service-name](docs/rules/service-name.md) - require and specify a prefix for all service names ([y125](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y125))
 * [value-name](docs/rules/value-name.md) - require and specify a prefix for all value names ([y125](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y125))

### Conventions

Angular often provide multi ways to to something. These rules help you to define convention for your project.

 * [di-order](docs/rules/di-order.md) - require DI parameters to be sorted alphabetically
 * [di](docs/rules/di.md) - require a consistent DI syntax
 * [dumb-inject](docs/rules/dumb-inject.md) - unittest `inject` functions should only consist of assignments from injected values to describe block variables
 * [function-type](docs/rules/function-type.md) - require and specify a consistent function style for components ('named' or 'anonymous') ([y024](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y024))
 * [module-dependency-order](docs/rules/module-dependency-order.md) - require a consistent order of module dependencies
 * [no-service-method](docs/rules/no-service-method.md) - use `factory()` instead of `service()` ([y040](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y040))
 * [one-dependency-per-line](docs/rules/one-dependency-per-line.md) - require all DI parameters to be located in their own line
 * [rest-service](docs/rules/rest-service.md) - disallow different rest service and specify one of '$http', '$resource', 'Restangular'
 * [watchers-execution](docs/rules/watchers-execution.md) - require and specify consistent use `$scope.digest()` or `$scope.apply()`

### Angular Wrappers

These rules help you to enforce the usage of angular wrappers.

 * [angularelement](docs/rules/angularelement.md) - use `angular.element` instead of `$` or `jQuery`
 * [definedundefined](docs/rules/definedundefined.md) - use `angular.isDefined` and `angular.isUndefined` instead of other undefined checks
 * [document-service](docs/rules/document-service.md) - use `$document` instead of `document` ([y180](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y180))
 * [foreach](docs/rules/foreach.md) - use `angular.forEach` instead of native `Array.prototype.forEach`
 * [interval-service](docs/rules/interval-service.md) - use `$interval` instead of `setInterval` ([y181](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y181))
 * [json-functions](docs/rules/json-functions.md) - use `angular.fromJson` and 'angular.toJson' instead of `JSON.parse` and `JSON.stringify`
 * [log](docs/rules/log.md) - use the `$log` service instead of the `console` methods
 * [no-angular-mock](docs/rules/no-angular-mock.md) - require to use `angular.mock` methods directly
 * [no-jquery-angularelement](docs/rules/no-jquery-angularelement.md) - disallow to wrap `angular.element` objects with `jQuery` or `$`
 * [timeout-service](docs/rules/timeout-service.md) - use `$timeout` instead of `setTimeout` ([y181](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y181))
 * [typecheck-array](docs/rules/typecheck-array.md) - use `angular.isArray` instead of `typeof` comparisons
 * [typecheck-date](docs/rules/typecheck-date.md) - use `angular.isDate` instead of `typeof` comparisons
 * [typecheck-function](docs/rules/typecheck-function.md) - use `angular.isFunction` instead of `typeof` comparisons
 * [typecheck-number](docs/rules/typecheck-number.md) - use `angular.isNumber` instead of `typeof` comparisons
 * [typecheck-object](docs/rules/typecheck-object.md) - use `angular.isObject` instead of `typeof` comparisons
 * [typecheck-string](docs/rules/typecheck-string.md) - use `angular.isString` instead of `typeof` comparisons
 * [window-service](docs/rules/window-service.md) - use `$window` instead of `window` ([y180](https://github.com/johnpapa/angular-styleguide/blob/master/a1/README.md#style-y180))

### Misspelling

These rules help you avoiding misspellings.

 * [on-destroy](docs/rules/on-destroy.md) - Check for common misspelling $on('destroy', ...).


----


## Need your help

It is an opensource project. Any help will be very useful. You can :
- Create issue
- Send Pull Request
- Write Documentation
- Add new Features
- Add new Rules
- Improve the quality
- Reply to issues

All development happens on the `development` branch. This means all pull requests should be made to the `development` branch.

If it is time to release, @Gillespie59 will bump the version in `package.json`, create a Git tag and merge the `development` branch into `master`. @Gillespie59 will then publish the new release to the npm registry.



## How to create a new rule

We appreciate contributions and the following notes will help you before you open a Pull Request.

### Check the issues

Have a look at the existing issues. There may exist similar issues with useful information.

### Read the documentation

There are some useful references for creating new rules. Specificly useful are:

* [The Context Object](http://eslint.org/docs/developer-guide/working-with-rules#the-context-object) - This is the most basic understanding needed for adding or modifying a rule.
* [Options Schemas](http://eslint.org/docs/developer-guide/working-with-rules#options-schemas) - This is the preferred way for validating configuration options.
* [Scope](http://estools.github.io/escope/Scope.html) - This is the scope object returned by `context.getScope()`.

### Files you have to create

* `rules/<your-rule>.js`
    * JavaScript file with the new rule
    * The filename `<your-rule>` is exactly the usage name in eslint configs `angular/<your-rule>`
    * Have a look at the `angularRule` wrapper and the `utils` (both in `rules/utils/`) - they probably make things easier for you
    * Add a documentation comment to generate a markdown documentation with the `gulp docs` task
* `test/<your-rule>.js`
    * Write some tests and execute them with `gulp test`
    * Have a look at the coverage reports `coverage/lcov-report/index.html`
* `examples/<your-rule>.js`
    * Add some examples for the documentation
    * Run the `gulp docs` task to test the examples and update the markdown documentation
* `docs/<your-rule>.md`
    * Generated by the `gulp docs` task

### Files you have to touch

* `index.js`
   * Add your rule `rulesConfiguration.addRule('<your-rule>', [0, {someConfig: 'someValue'}])`

### Before you open your PR

* Check that the `gulp` task is working
* Commit generated changes in `README.md` and `docs/<your-rule>.md`
* Open your PR to the `development` branch NOT `master`

### Rules specific for Angular 1 or 2

We can use a property, defined in the ESLint configuration file, in order to know which version is used : Angular 1 or Angular 2. based on this property, you can create rules for each version.

```yaml
plugins:
  - angular

rules:
    angular/controller-name:
      - 2
      - '/[A-Z].*Controller$/'

globals:
    angular: true

settings:
    angular: 2
```

And in your rule, you can access to this property thanks to the `context` object :

```javascript
//If Angular 2 is used, we disabled the rule
if(context.settings.angular === 2){
    return {};
}

return {

    'CallExpression': function(node) {
    }
};
```



## Default ESLint configuration file

Here is the basic configuration for the rules defined in the ESLint plugin, in order to be compatible with the guideline provided by @johnpapa :

```yaml
rules:
    no-use-before-define:
      - 0
```



## Who uses it?

- [argo](https://github.com/albertosantini/argo)
- [generator-gillespie59-angular](https://github.com/Gillespie59/generator-gillespie59-angular/)
- [generator-ng-poly](https://github.com/dustinspecker/generator-ng-poly)
- [JHipster](http://jhipster.github.io/)
- [generator-gulp-angular](https://github.com/Swiip/generator-gulp-angular)


## Team

[![Emmanuel Demey](https://avatars.githubusercontent.com/u/555768?s=117)](http://gillespie59.github.io/) | [![Tilman Potthof](https://avatars.githubusercontent.com/u/157532?s=117)](https://github.com/tilmanpotthof) | [![Remco Haszing](https://avatars.githubusercontent.com/u/779047?s=117)](https://github.com/remcohaszing) |
:---:|:---:|:---:|
[Emmanuel Demey](http://gillespie59.github.io/) | [Tilman Potthof](https://github.com/tilmanpotthof) | [Remco Haszing](https://github.com/remcohaszing) |
