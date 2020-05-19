# Boolean expressions are limited to booleans (strict-boolean-expressions)

Requires that any boolean expression is limited to true booleans rather than
casting another primitive to a boolean at runtime.

It is useful to be explicit, for example, if you were trying to check if a
number was defined. Doing `if (number)` would evaluate to `false` if `number`
was defined and `0`. This rule forces these expressions to be explicit and to
strictly use booleans.

The following nodes are checked:

- Arguments to the `!`, `&&`, and `||` operators
- The condition in a conditional expression `(cond ? x : y)`
- Conditions for `if`, `for`, `while`, and `do-while` statements.

Examples of **incorrect** code for this rule:

```ts
const number = 0;
if (number) {
  return;
}

let foo = bar || 'foobar';

let undefinedItem;
let foo = undefinedItem ? 'foo' : 'bar';

let str = 'foo';
while (str) {
  break;
}
```

Examples of **correct** code for this rule:

```ts
const number = 0;
if (typeof number !== 'undefined') {
  return;
}

let foo = typeof bar !== 'undefined' ? bar : 'foobar';

let undefinedItem;
let foo = typeof undefinedItem !== 'undefined' ? 'foo' : 'bar';

let str = 'foo';
while (typeof str !== 'undefined') {
  break;
}
```

## Related To

- TSLint: [strict-boolean-expressions](https://palantir.github.io/tslint/rules/strict-boolean-expressions)
