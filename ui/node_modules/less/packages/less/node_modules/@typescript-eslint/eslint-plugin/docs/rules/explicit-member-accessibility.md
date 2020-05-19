# Require explicit accessibility modifiers on class properties and methods (explicit-member-accessibility)

Leaving off accessibility modifier and making everything public can make
your interface hard to use by others.
If you make all internal pieces private or protected, your interface will
be easier to use.

## Rule Details

This rule aims to make code more readable and explicit about who can use
which properties.

## Options

```ts
type AccessibilityLevel =
  | 'explicit' // require an accessor (including public)
  | 'no-public' // don't require public
  | 'off'; // don't check

interface Config {
  accessibility?: AccessibilityLevel;
  overrides?: {
    accessors?: AccessibilityLevel;
    constructors?: AccessibilityLevel;
    methods?: AccessibilityLevel;
    properties?: AccessibilityLevel;
    parameterProperties?: AccessibilityLevel;
  };
}
```

Default config:

```JSON
{ "accessibility": "explicit" }
```

This rule in it's default state requires no configuration and will enforce that every class member has an accessibility modifier. If you would like to allow for some implicit public members then you have the following options:
A possible configuration could be:

```ts
{
  accessibility: 'explicit',
  overrides: {
    accessors: 'explicit',
    constructors: 'no-public',
    methods: 'explicit',
    properties: 'off',
    parameterProperties: 'explicit'
  }
}
```

The following patterns are considered incorrect code if no options are provided:

```ts
class Animal {
  constructor(name) {
    // No accessibility modifier
    this.animalName = name;
  }
  animalName: string; // No accessibility modifier
  get name(): string {
    // No accessibility modifier
    return this.animalName;
  }
  set name(value: string) {
    // No accessibility modifier
    this.animalName = value;
  }
  walk() {
    // method
  }
}
```

The following patterns are considered correct with the default options `{ accessibility: 'explicit' }`:

```ts
class Animal {
  public constructor(public breed, animalName) {
    // Parameter property and constructor
    this.animalName = name;
  }
  private animalName: string; // Property
  get name(): string {
    // get accessor
    return this.animalName;
  }
  set name(value: string) {
    // set accessor
    this.animalName = value;
  }
  public walk() {
    // method
  }
}
```

The following patterns are considered incorrect with the accessibility set to **no-public** `[{ accessibility: 'no-public' }]`:

```ts
class Animal {
  public constructor(public breed, animalName) {
    // Parameter property and constructor
    this.animalName = name;
  }
  public animalName: string; // Property
  public get name(): string {
    // get accessor
    return this.animalName;
  }
  public set name(value: string) {
    // set accessor
    this.animalName = value;
  }
  public walk() {
    // method
  }
}
```

The following patterns are considered correct with the accessibility set to **no-public** `[{ accessibility: 'no-public' }]`:

```ts
class Animal {
  constructor(protected breed, animalName) {
    // Parameter property and constructor
    this.name = name;
  }
  private animalName: string; // Property
  get name(): string {
    // get accessor
    return this.animalName;
  }
  private set name(value: string) {
    // set accessor
    this.animalName = value;
  }
  protected walk() {
    // method
  }
}
```

### Overrides

There are three ways in which an override can be used.

- To disallow the use of public on a given member.
- To enforce explicit member accessibility when the root has allowed implicit public accessibility
- To disable any checks on given member type

#### Disallow the use of public on a given member

e.g. `[ { overrides: { constructors: 'no-public' } } ]`

The following patterns are considered incorrect with the example override

```ts
class Animal {
  public constructor(protected animalName) {}
  public get name() {
    return this.animalName;
  }
}
```

The following patterns are considered correct with the example override

```ts
class Animal {
  constructor(protected animalName) {}
  public get name() {
    return this.animalName;
  }
}
```

#### Require explicit accessibility for a given member

e.g. `[ { accessibility: 'no-public', overrides: { properties: 'explicit' } } ]`

The following patterns are considered incorrect with the example override

```ts
class Animal {
  constructor(protected animalName) {}
  get name() {
    return this.animalName;
  }
  protected set name(value: string) {
    this.animalName = value;
  }
  legs: number;
  private hasFleas: boolean;
}
```

The following patterns are considered correct with the example override

```ts
class Animal {
  constructor(protected animalName) {}
  get name() {
    return this.animalName;
  }
  protected set name(value: string) {
    this.animalName = value;
  }
  public legs: number;
  private hasFleas: boolean;
}
```

#### Disable any checks on given member type

e.g. `[{ overrides: { accessors : 'off' } } ]`

As no checks on the overridden member type are performed all permutations of visibility are permitted for that member type

The follow pattern is considered incorrect for the given configuration

```ts
class Animal {
  constructor(protected animalName) {}
  public get name() {
    return this.animalName;
  }
  get legs() {
    return this.legCount;
  }
}
```

The following patterns are considered correct with the example override

```ts
class Animal {
  public constructor(protected animalName) {}
  public get name() {
    return this.animalName;
  }
  get legs() {
    return this.legCount;
  }
}
```

## When Not To Use It

If you think defaulting to public is a good default, then you should consider using the `no-public` setting. If you want to mix implicit and explicit public members then disable this rule.

## Further Reading

- TypeScript [Accessibility Modifiers](https://www.typescriptlang.org/docs/handbook/classes.html#public-private-and-protected-modifiers)

## Compatibility

- TSLint: [member-access](http://palantir.github.io/tslint/rules/member-access/)
