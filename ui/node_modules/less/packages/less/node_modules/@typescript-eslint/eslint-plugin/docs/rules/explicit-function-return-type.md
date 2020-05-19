# Require explicit return types on functions and class methods (explicit-function-return-type)

Explicit types for function return values makes it clear to any calling code what type is returned.
This ensures that the return value is assigned to a variable of the correct type; or in the case
where there is no return value, that the calling code doesn't try to use the undefined value when it
shouldn't.

## Rule Details

This rule aims to ensure that the values returned from functions are of the expected type.

The following patterns are considered warnings:

```ts
// Should indicate that no value is returned (void)
function test() {
  return;
}

// Should indicate that a number is returned
var fn = function() {
  return 1;
};

// Should indicate that a string is returned
var arrowFn = () => 'test';

class Test {
  // Should indicate that no value is returned (void)
  method() {
    return;
  }
}
```

The following patterns are not warnings:

```ts
// No return value should be expected (void)
function test(): void {
  return;
}

// A return value of type number
var fn = function(): number {
  return 1;
};

// A return value of type string
var arrowFn = (): string => 'test';

class Test {
  // No return value should be expected (void)
  method(): void {
    return;
  }
}
```

## Options

The rule accepts an options object with the following properties:

```ts
type Options = {
  // if true, only functions which are part of a declaration will be checked
  allowExpressions?: boolean;
  // if true, type annotations are also allowed on the variable of a function expression rather than on the function directly
  allowTypedFunctionExpressions?: boolean;
  // if true, functions immediately returning another function expression will not be checked
  allowHigherOrderFunctions?: boolean;
};

const defaults = {
  allowExpressions: false,
  allowTypedFunctionExpressions: false,
  allowHigherOrderFunctions: false,
};
```

### allowExpressions

Examples of **incorrect** code for this rule with `{ allowExpressions: true }`:

```ts
function test() {}
```

Examples of **correct** code for this rule with `{ allowExpressions: true }`:

```ts
node.addEventListener('click', () => {});

node.addEventListener('click', function() {});

const foo = arr.map(i => i * i);
```

### allowTypedFunctionExpressions

Examples of **incorrect** code for this rule with `{ allowTypedFunctionExpressions: true }`:

```ts
let arrowFn = () => 'test';

let funcExpr = function() {
  return 'test';
};

let objectProp = {
  foo: () => 1,
};
```

Examples of additional **correct** code for this rule with `{ allowTypedFunctionExpressions: true }`:

```ts
type FuncType = () => string;

let arrowFn: FuncType = () => 'test';

let funcExpr: FuncType = function() {
  return 'test';
};

let asTyped = (() => '') as () => string;
let castTyped = <() => string>(() => '');

interface ObjectType {
  foo(): number;
}
let objectProp: ObjectType = {
  foo: () => 1,
};
let objectPropAs = {
  foo: () => 1,
} as ObjectType;
let objectPropCast = <ObjectType>{
  foo: () => 1,
};

declare functionWithArg(arg: () => number);
functionWithArg(() => 1);
```

### allowHigherOrderFunctions

Examples of **incorrect** code for this rule with `{ allowHigherOrderFunctions: true }`:

```ts
var arrowFn = (x: number) => (y: number) => x + y;

function fn(x: number) {
  return function(y: number) {
    return x + y;
  };
}
```

Examples of **correct** code for this rule with `{ allowHigherOrderFunctions: true }`:

```ts
var arrowFn = (x: number) => (y: number): number => x + y;

function fn(x: number) {
  return function(y: number): number {
    return x + y;
  };
}
```

## When Not To Use It

If you don't wish to prevent calling code from using function return values in unexpected ways, then
you will not need this rule.

## Further Reading

- TypeScript [Functions](https://www.typescriptlang.org/docs/handbook/functions.html#function-types)
