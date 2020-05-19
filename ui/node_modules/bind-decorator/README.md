# bind-decorator

Context method binding decorator.

[![npm version](https://badge.fury.io/js/bind-decorator.svg)](https://badge.fury.io/js/bind-decorator)
[![license](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/NoHomey/bind-decorator)
[![Build Status](https://semaphoreci.com/api/v1/nohomey/bind-decorator/branches/master/badge.svg)](https://semaphoreci.com/nohomey/bind-decorator)
[![Code Climate](https://codeclimate.com/github/NoHomey/bind-decorator/badges/gpa.svg)](https://codeclimate.com/github/NoHomey/bind-decorator)
[![Test Coverage](https://codeclimate.com/github/NoHomey/bind-decorator/badges/coverage.svg)](https://codeclimate.com/github/NoHomey/bind-decorator/coverage)
[![Issue Count](https://codeclimate.com/github/NoHomey/bind-decorator/badges/issue_count.svg)](https://codeclimate.com/github/NoHomey/bind-decorator)
![TypeScript](https://img.shields.io/badge/%3C%20%2F%3E-TypeScript-blue.svg)
![Typings](https://img.shields.io/badge/typings-%E2%9C%93-brightgreen.svg)

`@bind` is just a little faster version of [`@autobind`](https://github.com/andreypopp/autobind-decorator/blob/master/src/index.js) for decorating methods only, by binding them to the current context. It is written in TypeScript and follows the latest `decorator`s [proposal](http://tc39.github.io/proposal-decorators/).

- It will `throw` exceptions if decorating anything other than `function`;
- Since the implementation follows the latest `decorator`s [proposal](http://tc39.github.io/proposal-decorators/) where compartion betweeen `this` and `target` can not be trusted, `@bind` will always `return` a `configurable`, `get accessor propertyDescriptor` which will memomize the result of `descriptor.value.bind(this)` by re-defining the property descriptor of the method beeing decorated (Credits goes to [autobind-decorator](https://github.com/andreypopp/autobind-decorator/blob/master/src/index.js) for memoizing the result).

If you are looking for not just method decorator but rather full class bounding decorator check [`@autobind`](https://github.com/andreypopp/autobind-decorator/blob/master/src/index.js).

# Install

Install with npm:

```bash
$ npm install bind-decorator
```

[![NPM](https://nodei.co/npm/bind-decorator.png?downloads=true&stars=true)](https://nodei.co/npm/bind-decorator/)

# Usage

## In JavaScript

```javascript
import bind from 'bind-decorator';

class Test {
    static what = 'static';
    
    @bind
    static test() {
        console.log(this.what);
    }

    constructor(what) {
        this.what = what;
    }

    @bind
    test() {
        console.warn(this.what);
    }
}

const tester = new Test('bind');
const { test } = tester;
tester.test(); // warns 'bind'.
test(); // warns 'bind'.
Test.test(); // logs 'static'.
```

## In TypeScript

```typescript
import bind from 'bind-decorator';

class Test {
    public static what: string = 'static';
    
    @bind
    public static test(): void {
        console.log(this.what);
    }

    public constructor(public what: string) {
        this.what = what;
    }

    @bind
    public test(): void {
        console.warn(this.what);
    }
}

const tester: Test = new Test('bind');
const { test } = tester;
tester.test(); // warns 'bind'.
test(); // warns 'bind'.
Test.test(); // logs 'static'.
```

# Testing

1. `npm install`

2. `npm test`

# Contributing

1. `npm install`

2. Make changes

3. If necessary add some tests to `__tests__`

4. `npm test`

5. Make a Pull Request