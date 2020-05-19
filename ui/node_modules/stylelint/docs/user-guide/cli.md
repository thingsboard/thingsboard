# Command Line Interface (CLI)

## Installation

stylelint is an [npm package](https://www.npmjs.com/package/stylelint). Install it using:

```shell
npm install stylelint --save-dev
```

## Usage

`stylelint --help` prints the CLI documentation.

The CLI outputs formatted results into `process.stdout`, which you can read with your human eyes or pipe elsewhere (e.g. write the information to a file).

### Examples

When you run commands similar to the examples below, be sure to include the quotation marks around file globs. This ensures that you can use the powers of [globby](https://github.com/sindresorhus/globby) (like the `**` globstar) regardless of your shell.

Looking for `.stylelintrc` and linting all `.css` files in the `foo` directory:

```shell
stylelint "foo/*.css"
```

Looking for `.stylelintrc` and linting all `<style>` blocks within the `.html` files in the `bar` directory:

```shell
stylelint "bar/*.html"
```

Looking for `.stylelintrc` and linting `stdin`:

```shell
echo "a { color: pink; }" | stylelint
```

Using `bar/mySpecialConfig.json` as config to lint all `.css` files in the `foo` directory, then writing the output to `myTestReport.txt`:

```shell
stylelint "foo/*.css" --config bar/mySpecialConfig.json > myTestReport.txt
```

Using `bar/mySpecialConfig.json` as config, with quiet mode on, to lint all `.css` files in the `foo` directory and any of its subdirectories and also all `.css` files in the `bar directory`:

```shell
stylelint "foo/**/*.css" "bar/*.css" -q -f json --config bar/mySpecialConfig.json
```

Linting all `.css` files except those within `docker` subfolders, using negation in the input glob:

```shell
stylelint "**/*.css" "!**/docker/**"
```

Caching processed `.scss` files in order to operate only on changed ones in the `foo` directory, using the `cache` and `cache-location` options:

```shell
stylelint "foo/**/*.scss" --cache --cache-location "/Users/user/.stylelintcache/"
```

stylelint will [automatically infer the syntax](css-processors.md#parsing-non-standard-syntax). You can, however, force a specific syntax using the  `--syntax` option. For example, linting all the `.css` files in the `foo` directory _as Scss_:

```shell
stylelint "foo/**/*.css" --syntax scss
```

stylelint can also accept a custom [PostCSS-compatible syntax](https://github.com/postcss/postcss#syntaxes). To use a custom syntax, supply a syntax module name or path to the syntax file: `--custom-syntax custom-syntax` or `--custom-syntax ./path/to/custom-syntax`.

### Recursively linting a directory

To recursively lint a directory, using the `**` globstar:

```shell
stylelint "foo/**/*.scss"
```

The quotation marks around the glob are important because they will allow stylelint to interpret the glob, using globby, instead of your shell, which might not support all the same features.

### Autofixing errors

With `--fix` option stylelint will fix as many errors as possible. The fixes are made to the actual source files. All unfixed errors will be reported.

Linting all `.css` files in the `foo` directory. And fixing source files if violated rules support autofixing:

```shell
stylelint "foo/*.css" --fix
```

**Note:** It's an _experimental_ feature. It currently does not respect special comments for disabling stylelint within sources (e. g. `/* stylelint-disable */`). Autofixing will be applied regardless of these comments.

If you're using both these special comments and autofixing, please run stylelint twice as a temporary solution. On the first run, some violations could be missed, or some violations might be reported incorrectly.

For CSS with standard syntax, stylelint will use [postcss-safe-parser](https://github.com/postcss/postcss-safe-parser) to fix syntax errors.

### Write Report to a File

With the `--output-file filename` option, stylelint will output the report to the specified `filename` in addition to the standard output.

Logging the stylelint output to `stylelint.log`:

```shell
stylelint "foo/*.css" --output-file stylelint.log
```

### Troubleshooting configurations

With the `--print-config` option, stylelint outputs the configuration to be used for the file passed. When present, no linting is performed and only config-related options are valid.

## Options

### `--config`

Path to a specific configuration file (JSON, YAML, or CommonJS), or the name of a module in `node_modules` that points to one. If no `--config` argument is provided, stylelint will search for configuration files in
the following places, in this order:

-   a stylelint property in `package.json`;
-   a `.stylelintrc` file (with or without filename extension: `.json`, `.yaml`, `.yml`, and `.js` are available);
-   a `stylelint.config.js` file exporting a JS object.

The search will begin in the working directory and move up the directory tree until a configuration file is found.

### `--config-basedir`

An absolute path to the directory that relative paths defining "extends" and "plugins" are _relative to_. Only necessary if these values are relative paths.

### `--print-config`

Print the configuration for the given path.

### `--ignore-path, -i`

Path to a file containing patterns that describe files to ignore. The path can be absolute or relative to `process.cwd()`. By default, stylelint looks for `.stylelintignore` in `process.cwd()`.

### `--ignore-pattern, --ip`

Pattern of files to ignore (in addition to those in `.stylelintignore`).

### `--syntax, -s`

Specify a syntax. Options:

-   `css`
-   `css-in-js`
-   `html`
-   `less`
-   `markdown`
-   `sass`
-   `scss`
-   `sugarss`

If you do not specify a syntax, syntaxes will be automatically inferred by the file extensions and file content.

### `--fix`

Automatically fix violations of certain rules.

### `--custom-syntax`

Module name or path to a JS file exporting a PostCSS-compatible syntax.

### `--stdin-filename`

A filename to assign stdin input.

### `--ignore-disables, --id`

Ignore `styleline-disable` comments.

### `--disable-default-ignores, --di`

Allow linting of `node_modules`.

### `--cache`

Store the info about processed files in order to only operate on the changed ones the next time you run stylelint. By default, the cache is stored in `./.stylelintcache`. To adjust this, use `--cache-location`.

Default: `false`.

### `--cache-location`

Path to a file or directory to be used for the cache location.

Default is `"./.stylelintcache"`. If a directory is specified, a cache file will be created inside the specified folder, with a name derived from a hash of the current working directory.

If the directory for the cache does not exist, make sure you add a trailing `/` on *nix systems or `\\` on Windows. Otherwise the path will be assumed to be a file.

### `--formatter, -f`

The output formatter. Options are:

-   `compact`
-   `json`
-   `string` (default)
-   `unix`
-   `verbose`

### `--custom-formatter`

Path to a JS file exporting a custom formatting function.

### `--quiet, -q`

Only register violations for rules with an "error"-level severity (ignore "warning"-level).

### `--color, --no-color`

Force enabling/disabling of color.

### `--report-needless-disables, --rd`

Also report errors for stylelint-disable comments that are not blocking a lint warning.

The process will exit with code `2` if needless disables are found.

### `--report-invalid-scope-disables, --risd`

Report stylelint-disable comments that used for rules that don't exist within the configuration object.

The process will exit with code `2` if invalid scope disables are found.

### `--max-warnings, --mw`

Number of warnings above which the process will exit with code `2`.

Useful when setting `defaultSeverity` to `"warning"` and expecting the process to fail on warnings (e.g. CI build).

### `--output-file, -o`

Path of file to write report.

### `--version, -v`

Show the currently installed version of stylelint.

### `--allow-empty-input, --aei`

When glob pattern matches no files, the process will exit without throwing an error.

## Syntax errors

The CLI informs you about syntax errors in your CSS.
It uses the same format as it uses for linting violations.
The error name is `CssSyntaxError`.

## Exit codes

The CLI can exit the process with the following exit codes:

-   1: Something unknown went wrong.
-   2: At least one rule with an "error"-level severity triggered at least one violations.
-   78: There was some problem with the configuration file.
-   80: A file glob was passed, but it found no files.
