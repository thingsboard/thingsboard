# Disallow Empty Functions (@typescript-eslint/no-empty-function)

Empty functions can reduce readability because readers need to guess whether it’s intentional or not. So writing a clear comment for empty functions is a good practice.

## Rule Details

The `@typescript-eslint/no-empty-function` rule extends the `no-empty-function` rule from ESLint core, and adds support for handling Typescript specific code that would otherwise trigger the rule.

One example of valid Typescript specific code that would otherwise trigger the `no-empty-function` rule is the use of [parameter properties](https://www.typescriptlang.org/docs/handbook/classes.html#parameter-properties) in constructor functions:

```typescript
class Person {
  constructor(private firstName: string, private surname: string) {}
}
```

The above code is functionally equivalent to:

```typescript
class Person {
  private firstName: string;
  private surname: string;

  constructor(firstName: string, surname: string) {
    this.firstName = firstName;
    this.surname = surname;
  }
}
```

Parameter properties enable both the _declaration_ and _initialization_ of member properties in a single location, avoiding the boilerplate & duplication that is common when initializing member properties from parameter values in a constructor function.

In these cases, although the constructor has an empty function body, it is technically valid and should not trigger an error.

See the [ESLint documentation](https://eslint.org/docs/rules/no-empty-function) for more details on the `no-empty-function` rule.

## Rule Changes

```cjson
{
    // note you must disable the base rule as it can report incorrect errors
    "no-empty-function": "off",
    "@typescript-eslint/no-empty-function": "error"
}
```

<sup>Taken with ❤️ [from ESLint core](https://github.com/eslint/eslint/blob/master/docs/rules/no-empty-function.md)</sup>
