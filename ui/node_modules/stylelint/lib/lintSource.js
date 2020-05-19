'use strict';

const _ = require('lodash');
const assignDisabledRanges = require('./assignDisabledRanges');
const getOsEol = require('./utils/getOsEol');
const path = require('path');
const reportUnknownRuleNames = require('./reportUnknownRuleNames');
const requireRule = require('./requireRule');
const rulesOrder = require('./rules');

/** @typedef {import('stylelint').StylelintInternalApi} StylelintInternalApi */
/** @typedef {import('stylelint').PostcssResult} PostcssResult */
/** @typedef {import('stylelint').StylelintPostcssResult} StylelintPostcssResult */
/** @typedef {import('stylelint').GetLintSourceOptions} Options */

/**
 * Run stylelint on a PostCSS Result, either one that is provided
 * or one that we create
 * @param {StylelintInternalApi} stylelint
 * @param {Options} options
 * @returns {Promise<PostcssResult>}
 */
module.exports = function lintSource(stylelint, options = {}) {
	if (!options.filePath && options.code === undefined && !options.existingPostcssResult) {
		return Promise.reject(new Error('You must provide filePath, code, or existingPostcssResult'));
	}

	const isCodeNotFile = options.code !== undefined;

	const inputFilePath = isCodeNotFile ? options.codeFilename : options.filePath;

	if (inputFilePath !== undefined && !path.isAbsolute(inputFilePath)) {
		if (isCodeNotFile) {
			return Promise.reject(new Error('codeFilename must be an absolute path'));
		}

		return Promise.reject(new Error('filePath must be an absolute path'));
	}

	const getIsIgnored = stylelint.isPathIgnored(inputFilePath).catch((err) => {
		if (isCodeNotFile && err.code === 'ENOENT') return false;

		throw err;
	});

	return getIsIgnored.then((isIgnored) => {
		if (isIgnored) {
			const postcssResult =
				options.existingPostcssResult || createEmptyPostcssResult(inputFilePath);

			postcssResult.stylelint = postcssResult.stylelint || {};
			postcssResult.stylelint.ignored = true;
			postcssResult.standaloneIgnored = true; // TODO: remove need for this

			return postcssResult;
		}

		const configSearchPath = stylelint._options.configFile || inputFilePath;

		const getConfig = stylelint.getConfigForFile(configSearchPath).catch((err) => {
			if (isCodeNotFile && err.code === 'ENOENT') return stylelint.getConfigForFile(process.cwd());

			throw err;
		});

		return getConfig.then((result) => {
			// TODO TYPES possible null
			const config =
				/** @type {{ config: import('stylelint').StylelintConfig, filepath: string }} */ (result).config;
			const existingPostcssResult = options.existingPostcssResult;
			const stylelintResult = {
				ruleSeverities: {},
				customMessages: {},
				disabledRanges: {},
			};

			if (existingPostcssResult) {
				return lintPostcssResult(
					stylelint,
					Object.assign(existingPostcssResult, { stylelint: stylelintResult }),
					config,
				).then(() => existingPostcssResult);
			}

			return stylelint
				._getPostcssResult({
					code: options.code,
					codeFilename: options.codeFilename,
					filePath: inputFilePath,
					codeProcessors: config.codeProcessors,
				})
				.then((postcssResult) => {
					return lintPostcssResult(
						stylelint,
						Object.assign(postcssResult, { stylelint: stylelintResult }),
						config,
					).then(() => postcssResult);
				});
		});
	});
};

/**
 * @param {StylelintInternalApi} stylelint
 * @param {PostcssResult} postcssResult
 * @param {import('stylelint').StylelintConfig} config
 * @returns {Promise<any>}
 */
function lintPostcssResult(stylelint, postcssResult, config) {
	postcssResult.stylelint.ruleSeverities = {};
	postcssResult.stylelint.customMessages = {};
	postcssResult.stylelint.stylelintError = false;
	postcssResult.stylelint.quiet = config.quiet;

	/** @type {string} */
	let newline;
	const postcssDoc = postcssResult.root;

	if (postcssDoc) {
		// @ts-ignore TODO TYPES property css does not exists
		const newlineMatch = postcssDoc.source && postcssDoc.source.input.css.match(/\r?\n/);

		newline = newlineMatch ? newlineMatch[0] : getOsEol();

		assignDisabledRanges(postcssDoc, postcssResult);
	}

	if (stylelint._options.reportNeedlessDisables || stylelint._options.ignoreDisables) {
		postcssResult.stylelint.ignoreDisables = true;
	}

	const postcssRoots =
		/** @type {import('postcss').Root[]} */ (postcssDoc &&
		postcssDoc.constructor.name === 'Document'
			? postcssDoc.nodes
			: [postcssDoc]);

	// Promises for the rules. Although the rule code runs synchronously now,
	// the use of Promises makes it compatible with the possibility of async
	// rules down the line.
	/** @type {Array<Promise<any>>} */
	const performRules = [];

	const rules = config.rules
		? Object.keys(config.rules).sort((a, b) => rulesOrder.indexOf(a) - rulesOrder.indexOf(b))
		: [];

	rules.forEach((ruleName) => {
		const ruleFunction = requireRule(ruleName) || _.get(config, ['pluginFunctions', ruleName]);

		if (ruleFunction === undefined) {
			performRules.push(
				Promise.all(
					postcssRoots.map((postcssRoot) =>
						reportUnknownRuleNames(ruleName, postcssRoot, postcssResult),
					),
				),
			);

			return;
		}

		const ruleSettings = _.get(config, ['rules', ruleName]);

		if (ruleSettings === null || ruleSettings[0] === null) {
			return;
		}

		const primaryOption = ruleSettings[0];
		const secondaryOptions = ruleSettings[1];

		// Log the rule's severity in the PostCSS result
		const defaultSeverity = config.defaultSeverity || 'error';

		postcssResult.stylelint.ruleSeverities[ruleName] = _.get(
			secondaryOptions,
			'severity',
			defaultSeverity,
		);
		postcssResult.stylelint.customMessages[ruleName] = _.get(secondaryOptions, 'message');

		performRules.push(
			Promise.all(
				postcssRoots.map((postcssRoot) =>
					ruleFunction(primaryOption, secondaryOptions, {
						fix: stylelint._options.fix,
						newline,
					})(postcssRoot, postcssResult),
				),
			),
		);
	});

	return Promise.all(performRules);
}

/**
 * @param {string} [filePath]
 * @returns {Object}
 */
function createEmptyPostcssResult(filePath) {
	return {
		root: {
			source: {
				input: { file: filePath },
			},
		},
		messages: [],
		stylelint: { stylelintError: false },
	};
}
