# Requires Promise-like values to be handled appropriately (no-floating-promises)

This rule forbids usage of Promise-like values in statements without handling
their errors appropriately. Unhandled promises can cause several issues, such
as improperly sequenced operations, ignored Promise rejections and more. Valid
ways of handling a Promise-valued statement include `await`ing, returning, and
either calling `.then()` with two arguments or `.catch()` with one argument.

## Rule Details

Examples of **incorrect** code for this rule:

```ts
const promise = new Promise((resolve, reject) => resolve('value'));
promise;

async function returnsPromise() {
  return 'value';
}
returnsPromise().then(() => {});

Promise.reject('value').catch();
```

Examples of **correct** code for this rule:

```ts
const promise = new Promise((resolve, reject) => resolve('value'));
await promise;

async function returnsPromise() {
  return 'value';
}
returnsPromise().then(() => {}, () => {});

Promise.reject('value').catch(() => {});
```

## When Not To Use It

If you do not use Promise-like values in your codebase or want to allow them to
remain unhandled.

## Related to

- Tslint: ['no-floating-promises'](https://palantir.github.io/tslint/rules/no-floating-promises/)
