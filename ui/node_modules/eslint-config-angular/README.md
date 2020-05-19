# eslint-config-angular
[![NPM version](https://badge.fury.io/js/eslint-config-angular.svg)](https://badge.fury.io/js/eslint-config-angular) [![Build Status](https://travis-ci.org/dustinspecker/eslint-config-angular.svg)](https://travis-ci.org/dustinspecker/eslint-config-angular) [![Coverage Status](https://img.shields.io/coveralls/dustinspecker/eslint-config-angular.svg)](https://coveralls.io/r/dustinspecker/eslint-config-angular?branch=master)

[![Code Climate](https://codeclimate.com/github/dustinspecker/eslint-config-angular/badges/gpa.svg)](https://codeclimate.com/github/dustinspecker/eslint-config-angular) [![Dependencies](https://david-dm.org/dustinspecker/eslint-config-angular.svg)](https://david-dm.org/dustinspecker/eslint-config-angular/#info=dependencies&view=table) [![DevDependencies](https://david-dm.org/dustinspecker/eslint-config-angular/dev-status.svg)](https://david-dm.org/dustinspecker/eslint-config-angular/#info=devDependencies&view=table)

> ESLint [shareable](http://eslint.org/docs/developer-guide/shareable-configs.html) config for [Angular plugin](https://github.com/Gillespie59/eslint-plugin-angular)

## Install
```
npm install --save-dev eslint-config-angular
```
**Also, need to install [eslint-plugin-angular](https://github.com/Gillespie59/eslint-plugin-angular).**

## Usage
In your .eslintrc file:
```javascript
{
  "extends": "angular"
}
```

This config will
 - enable [eslint-plugin-angular](https://github.com/Gillespie59/eslint-plugin-angular)
 - add `angular` as a global variable
 - disable the `no-use-before-define` rule as recommended by [eslint-plugin-angular](https://github.com/Gillespie59/eslint-plugin-angular)

All options from [index.js](index.js) may be overridden in your .eslintrc file.

## LICENSE
MIT © [Dustin Specker](https://github.com/dustinspecker)