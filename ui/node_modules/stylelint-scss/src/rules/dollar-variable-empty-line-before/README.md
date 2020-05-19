# dollar-variable-empty-line-before

Require an empty line or disallow empty lines before `$`-variable declarations.

If the `$`-variable declaration is the first declaration in a file, it's ignored.

```scss
             /* ← */
$width: 10px;   ↑
/**             ↑
 * This empty line */
```

The `--fix` option on the [command line](https://github.com/stylelint/stylelint/blob/master/docs/user-guide/cli.md#autofixing-errors) can automatically fix all of the problems reported by this rule.

## Options

`string`: `"always"|"never"`

### `"always"`

There *must always* be one empty line before a `$`-variable declaration.

The following patterns are considered warnings:

```scss
@import '1.css';
$var2: 200px;
```

```scss
a {
  $var: 1;
}
```

The following patterns are *not* considered warnings:

```scss
$var: 100px; // The first declaration in a stylesheet
```

```scss
a { color: red; }

$var: 1;
```

### `"never"`

There *must never* be an empty line before a `$`-variable declaration.

The following patterns are considered warnings:

```scss
a { color: red; }

$var: 1;
```

The following patterns are *not* considered warnings:

```scss
$var: 100px;
$var2: 200px;
```

```scss
a {
  width: auto;
}
$var: 1;
```

## Optional secondary options

### `except: ["first-nested", "after-comment", "after-dollar-variable"]`

### `"first-nested"`

Reverse the primary option for a `$`-variable declaration if it's the first child of its parent.

For example, with `"always"`:

The following patterns are considered warnings:

```scss
a {

  $var: 1;
  color: red;
}

b {
  color: red;

  $var: 1;
}
```

The following patterns are *not* considered warnings:

```scss
a {
  $var: 1;
  color: red;
}

b {
  color: red;
  $var: 1;
}
```

### `"after-comment"`

Reverse the primary option for `$`-variable declarations that go after comments.

For example, with `"always"`:

The following patterns are considered warnings:

```scss
a {
  // comment

  $var: 1;
}

b {
  /* comment */

  $var: 1;
}
```

The following patterns are *not* considered warnings:

```scss
a {
  // comment
  $var: 1;
}
```

### `"after-dollar-variable"`

Reverse the primary option for `$`-variable declarations that go right after another `$`-variable declaration.

For example, with `"always"`:

The following patterns are considered warnings:

```scss
a {

  $var: 1; // this one is ok

  $var1: 2; // and this one shouldn't have a preceding empty line
}
```

The following patterns are *not* considered warnings:

```scss
a {

  $var: 1;
  $var1: 2;
}
```

### `ignore: ["after-comment", "inside-single-line-block"]`

### `"after-comment"`

Ignore `$`-variables that go after a comment.

For example, with `"always"`:

The following patterns are *not* considered warnings:

```scss
// comment
$var: 1

/* comment */
$var2: 1;
```

### `"inside-single-line-block"`

Ignore `$`-variables that are inside single-line blocks.

For example, with `"always"`:

The following patterns are *not* considered warnings:

```scss
a { $var: 10; }
```

### `disableFix: true`

Disables autofixing for this rule.
