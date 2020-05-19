import {Opts as MinimistOptions} from 'minimist';

export type OptionType = 'string' | 'boolean' | 'number' | 'array';

export interface BaseOption<
	TypeOptionType extends OptionType,
	DefaultOptionType
> {
	/**
	 * The data type the option should be parsed to.
	 */
	readonly type?: TypeOptionType;

	/**
	 * An alias/list of aliases for the option.
	 */
	readonly alias?: string | ReadonlyArray<string>;

	/**
	 * The default value for the option.
	 */
	readonly default?: DefaultOptionType;
}

export type StringOption = BaseOption<'string', string>;
export type BooleanOption = BaseOption<'boolean', boolean>;
export type NumberOption = BaseOption<'number', number>;
export type ArrayOption<ArrayContentType = unknown> = BaseOption<
	'array',
	ReadonlyArray<ArrayContentType>
>;
type MinimistOption = NonNullable<
	| MinimistOptions['stopEarly']
	| MinimistOptions['unknown']
	| MinimistOptions['--']
>;

export type Options<ArrayOptionContentType = unknown> = {
	[key: string]:
		| OptionType
		| StringOption
		| BooleanOption
		| NumberOption
		| ArrayOption<ArrayOptionContentType>
		| MinimistOption;  // Workaround for https://github.com/microsoft/TypeScript/issues/17867
};

/**
 * Write options for [minimist](https://npmjs.org/package/minimist) in a comfortable way. Support string, boolean, number and array options.
 */
export default function buildOptions(options?: Options): MinimistOptions;
