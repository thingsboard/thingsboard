import {PackageJson} from 'type-fest';

declare namespace meow {
	type FlagType = 'string' | 'boolean' | 'number';

	interface Flag<Type extends FlagType, Default> {
		readonly type?: Type;
		readonly alias?: string;
		readonly default?: Default;
	}

	type StringFlag = Flag<'string', string>;
	type BooleanFlag = Flag<'boolean', boolean>;
	type NumberFlag = Flag<'number', number>;

	type AnyFlags = {[key: string]: StringFlag | BooleanFlag | NumberFlag};

	interface Options<Flags extends AnyFlags> {
		/**
		Define argument flags.

		The key is the flag name and the value is an object with any of:

		- `type`: Type of value. (Possible values: `string` `boolean` `number`)
		- `alias`: Usually used to define a short flag alias.
		- `default`: Default value when the flag is not specified.

		@example
		```
		flags: {
			unicorn: {
				type: 'string',
				alias: 'u',
				default: 'rainbow'
			}
		}
		```
		*/
		readonly flags?: Flags;

		/**
		Description to show above the help text. Default: The package.json `"description"` property.

		Set it to `false` to disable it altogether.
		*/
		readonly description?: string | false;

		/**
		The help text you want shown.

		The input is reindented and starting/ending newlines are trimmed which means you can use a [template literal](https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/template_strings) without having to care about using the correct amount of indent.

		The description will be shown above your help text automatically.

		Set it to `false` to disable it altogether.
		*/
		readonly help?: string | false;

		/**
		Set a custom version output. Default: The package.json `"version"` property.

		Set it to `false` to disable it altogether.
		*/
		readonly version?: string | false;

		/**
		Automatically show the help text when the `--help` flag is present. Useful to set this value to `false` when a CLI manages child CLIs with their own help text.

		This option is only considered when there is only one argument in `process.argv`.
		*/
		readonly autoHelp?: boolean;

		/**
		Automatically show the version text when the `--version` flag is present. Useful to set this value to `false` when a CLI manages child CLIs with their own version text.

		This option is only considered when there is only one argument in `process.argv`.
		*/
		readonly autoVersion?: boolean;

		/**
		package.json as an `Object`. Default: Closest package.json upwards.

		_You most likely don't need this option._
		*/
		readonly pkg?: {[key: string]: unknown};

		/**
		Custom arguments object.

		@default process.argv.slice(2)
		*/
		readonly argv?: ReadonlyArray<string>;

		/**
		Infer the argument type.

		By default, the argument `5` in `$ foo 5` becomes a string. Enabling this would infer it as a number.

		@default false
		*/
		readonly inferType?: boolean;

		/**
		Value of `boolean` flags not defined in `argv`. If set to `undefined` the flags not defined in `argv` will be excluded from the result. The `default` value set in `boolean` flags take precedence over `booleanDefault`.

		__Caution: Explicitly specifying undefined for `booleanDefault` has different meaning from omitting key itself.__

		@example
		```
		import meow = require('meow');

		const cli = meow(`
			Usage
				$ foo

			Options
				--rainbow, -r  Include a rainbow
				--unicorn, -u  Include a unicorn
				--no-sparkles  Exclude sparkles

			Examples
				$ foo
				🌈 unicorns✨🌈
		`, {
			booleanDefault: undefined,
			flags: {
				rainbow: {
					type: 'boolean',
					default: true,
					alias: 'r'
				},
					unicorn: {
					type: 'boolean',
					default: false,
					alias: 'u'
				},
				cake: {
					type: 'boolean',
					alias: 'c'
				},
				sparkles: {
					type: 'boolean',
					default: true
				}
			}
		});

		//{
		//	flags: {
		//		rainbow: true,
		//		unicorn: false,
		//		sparkles: true
		//	},
		//	unnormalizedFlags: {
		//		rainbow: true,
		//		r: true,
		//		unicorn: false,
		//		u: false,
		//		sparkles: true
		//	},
		//	…
		//}
		```
		*/
		readonly booleanDefault?: boolean | null | undefined;

		/**
		Whether to use [hard-rejection](https://github.com/sindresorhus/hard-rejection) or not. Disabling this can be useful if you need to handle `process.on('unhandledRejection')` yourself.

		@default true
		*/
		readonly hardRejection?: boolean;
	}

	type TypedFlags<Flags extends AnyFlags> = {
		[F in keyof Flags]: Flags[F] extends {type: 'number'}
			? number
			: Flags[F] extends {type: 'string'}
				? string
				: Flags[F] extends {type: 'boolean'}
					? boolean
					: unknown;
	};

	interface Result<Flags extends AnyFlags> {
		/**
		Non-flag arguments.
		*/
		input: string[];

		/**
		Flags converted to camelCase excluding aliases.
		*/
		flags: TypedFlags<Flags> & {[name: string]: unknown};

		/**
		Flags converted camelCase including aliases.
		*/
		unnormalizedFlags: TypedFlags<Flags> & {[name: string]: unknown};

		/**
		The `package.json` object.
		*/
		pkg: PackageJson;

		/**
		The help text used with `--help`.
		*/
		help: string;

		/**
		Show the help text and exit with code.

		@param exitCode - The exit code to use. Default: `2`.
		*/
		showHelp(exitCode?: number): void;

		/**
		Show the version text and exit.
		*/
		showVersion(): void;
	}
}
/**
@param helpMessage - Shortcut for the `help` option.

@example
```
#!/usr/bin/env node
'use strict';
import meow = require('meow');
import foo = require('.');

const cli = meow(`
	Usage
	  $ foo <input>

	Options
	  --rainbow, -r  Include a rainbow

	Examples
	  $ foo unicorns --rainbow
	  🌈 unicorns 🌈
`, {
	flags: {
		rainbow: {
			type: 'boolean',
			alias: 'r'
		}
	}
});

//{
//	input: ['unicorns'],
//	flags: {rainbow: true},
//	...
//}

foo(cli.input[0], cli.flags);
```
*/
declare function meow<Flags extends meow.AnyFlags>(helpMessage: string, options?: meow.Options<Flags>): meow.Result<Flags>;
declare function meow<Flags extends meow.AnyFlags>(options?: meow.Options<Flags>): meow.Result<Flags>;

export = meow;
