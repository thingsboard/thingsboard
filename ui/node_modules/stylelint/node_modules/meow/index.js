'use strict';
const path = require('path');
const buildMinimistOptions = require('minimist-options');
const yargs = require('yargs-parser');
const camelcaseKeys = require('camelcase-keys');
const decamelizeKeys = require('decamelize-keys');
const trimNewlines = require('trim-newlines');
const redent = require('redent');
const readPkgUp = require('read-pkg-up');
const hardRejection = require('hard-rejection');
const normalizePackageData = require('normalize-package-data');

// Prevent caching of this module so module.parent is always accurate
delete require.cache[__filename];
const parentDir = path.dirname(module.parent.filename);

const meow = (helpText, options) => {
	if (typeof helpText !== 'string') {
		options = helpText;
		helpText = '';
	}

	options = {
		pkg: readPkgUp.sync({
			cwd: parentDir,
			normalize: false
		}).packageJson || {},
		argv: process.argv.slice(2),
		inferType: false,
		input: 'string',
		help: helpText,
		autoHelp: true,
		autoVersion: true,
		booleanDefault: false,
		hardRejection: true,
		...options
	};

	if (options.hardRejection) {
		hardRejection();
	}

	const minimistFlags = options.flags && typeof options.booleanDefault !== 'undefined' ? Object.keys(options.flags).reduce(
		(flags, flag) => {
			if (flags[flag].type === 'boolean' && !Object.prototype.hasOwnProperty.call(flags[flag], 'default')) {
				flags[flag].default = options.booleanDefault;
			}

			return flags;
		},
		options.flags
	) : options.flags;

	let minimistoptions = {
		arguments: options.input,
		...minimistFlags
	};

	minimistoptions = decamelizeKeys(minimistoptions, '-', {exclude: ['stopEarly', '--']});

	if (options.inferType) {
		delete minimistoptions.arguments;
	}

	minimistoptions = buildMinimistOptions(minimistoptions);

	if (minimistoptions['--']) {
		minimistoptions.configuration = {
			...minimistoptions.configuration,
			'populate--': true
		};
	}

	const {pkg} = options;
	const argv = yargs(options.argv, minimistoptions);
	let help = redent(trimNewlines((options.help || '').replace(/\t+\n*$/, '')), 2);

	normalizePackageData(pkg);

	process.title = pkg.bin ? Object.keys(pkg.bin)[0] : pkg.name;

	let {description} = options;
	if (!description && description !== false) {
		({description} = pkg);
	}

	help = (description ? `\n  ${description}\n` : '') + (help ? `\n${help}\n` : '\n');

	const showHelp = code => {
		console.log(help);
		process.exit(typeof code === 'number' ? code : 2);
	};

	const showVersion = () => {
		console.log(typeof options.version === 'string' ? options.version : pkg.version);
		process.exit();
	};

	if (argv._.length === 0 && options.argv.length === 1) {
		if (argv.version === true && options.autoVersion) {
			showVersion();
		}

		if (argv.help === true && options.autoHelp) {
			showHelp(0);
		}
	}

	const input = argv._;
	delete argv._;

	const flags = camelcaseKeys(argv, {exclude: ['--', /^\w$/]});
	const unnormalizedFlags = {...flags};

	if (options.flags !== undefined) {
		for (const flagValue of Object.values(options.flags)) {
			delete flags[flagValue.alias];
		}
	}

	return {
		input,
		flags,
		unnormalizedFlags,
		pkg,
		help,
		showHelp,
		showVersion
	};
};

module.exports = meow;
