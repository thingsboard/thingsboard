# stylelint-config-recommended-scss

[![npm version](http://img.shields.io/npm/v/stylelint-config-recommended-scss.svg)](https://www.npmjs.org/package/stylelint-config-recommended-scss)
[![Build Status](https://github.com/kristerkari/stylelint-config-recommended-scss/workflows/Tests/badge.svg)](https://github.com/kristerkari/stylelint-config-recommended-scss/actions?workflow=Tests)
[![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://egghead.io/courses/how-to-contribute-to-an-open-source-project-on-github)
[![Downloads per month](https://img.shields.io/npm/dm/stylelint-config-recommended-scss.svg)](https://npmcharts.com/compare/stylelint-config-recommended-scss)
[![Greenkeeper badge](https://badges.greenkeeper.io/kristerkari/stylelint-config-recommended-scss.svg)](https://greenkeeper.io/)

> The recommended shareable SCSS config for stylelint.

It turns on all the [_possible errors_](https://github.com/stylelint/stylelint/blob/master/docs/user-guide/rules.md#possible-errors) rules within stylelint.

Use it as is or as a foundation for your own config.

## Installation

First, install stylelint-scss and stylelint, if you haven't done so yet via npm:

```shell
npm install stylelint stylelint-scss --save-dev
```

and then you can install the config:

```shell
npm install stylelint-config-recommended-scss --save-dev
```

## Usage

If you've installed `stylelint-config-recommended-scss` locally within your project, just set your `stylelint` config to:

```json
{
  "extends": "stylelint-config-recommended-scss"
}
```

If you've globally installed `stylelint-config-recommended-scss` using the `-g` flag, then you'll need to use the absolute path to `stylelint-config-recommended-scss` in your config e.g.

```json
{
  "extends": "/absolute/path/to/stylelint-config-recommended-scss"
}
```

### Extending the config

Simply add a `"rules"` key to your config, then add your overrides and additions there.

For example, to turn off the `block-no-empty` rule, and add the `unit-whitelist` rule:

```json
{
  "extends": "stylelint-config-recommended-scss",
  "rules": {
    "block-no-empty": null,
    "unit-whitelist": ["em", "rem", "s"]
  }
}
```

## [Changelog](CHANGELOG.md)

## [License](LICENSE)
