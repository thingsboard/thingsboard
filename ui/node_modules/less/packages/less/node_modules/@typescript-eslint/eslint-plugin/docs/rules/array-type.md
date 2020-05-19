# Requires using either `T[]` or `Array<T>` for arrays (array-type)

Using the same style for array definitions across your codebase makes it easier for your developers to read and understand the types.

## Rule Details

This rule aims to standardise usage of array types within your codebase.

## Options

This rule accepts one option - a single string

- `"array"` enforces use of `T[]` for all types `T`.
- `"generic"` enforces use of `Array<T>` for all types `T`.
- `"array-simple"` enforces use of `T[]` if `T` is a simple type.

Without providing an option, by default the rule will enforce `"array"`.

### `"array"`

Always use `T[]` or `readonly T[]` for all array types.

Incorrect code for `"array"`:

```ts
const x: Array<string> = ['a', 'b'];
const y: ReadonlyArray<string> = ['a', 'b'];
```

Correct code for `"array"`:

```ts
const x: string[] = ['a', 'b'];
const y: readonly string[] = ['a', 'b'];
```

### `"generic"`

Always use `Array<T>` or `ReadonlyArray<T>` for all array types.

Incorrect code for `"generic"`:

```ts
const x: string[] = ['a', 'b'];
const y: readonly string[] = ['a', 'b'];
```

Correct code for `"generic"`:

```ts
const x: Array<string> = ['a', 'b'];
const y: ReadonlyArray<string> = ['a', 'b'];
```

### `"array-simple"`

Use `T[]` or `readonly T[]` for simple types (i.e. types which are just primitive names or type references).
Use `Array<T>` or `ReadonlyArray<T>` for all other types (union types, intersection types, object types, function types, etc).

Incorrect code for `"array-simple"`:

```ts
const a: (string | number)[] = ['a', 'b'];
const b: ({ prop: string })[] = [{ prop: 'a' }];
const c: (() => void)[] = [() => {}];
const d: Array<MyType> = ['a', 'b'];
const e: Array<string> = ['a', 'b'];
const f: ReadonlyArray<string> = ['a', 'b'];
```

Correct code for `"array-simple"`:

```ts
const a: Array<string | number> = ['a', 'b'];
const b: Array<{ prop: string }> = [{ prop: 'a' }];
const c: Array<() => void> = [() => {}];
const d: MyType[] = ['a', 'b'];
const e: string[] = ['a', 'b'];
const f: readonly string[] = ['a', 'b'];
```

## Related to

- TSLint: [array-type](https://palantir.github.io/tslint/rules/array-type/)
