'use strict';

const _ = require('lodash');

/** @typedef {import('stylelint').PostcssResult} PostcssResult */
/** @typedef {import('postcss').NodeSource} NodeSource */
/** @typedef {import('stylelint').StylelintResult} StylelintResult */

/**
 * @param {import('stylelint').StylelintInternalApi} stylelint
 * @param {PostcssResult} [postcssResult]
 * @param {string} [filePath]
 * @param {import('stylelint').StylelintCssSyntaxError} [cssSyntaxError]
 * @return {Promise<StylelintResult>}
 */
module.exports = function(stylelint, postcssResult, filePath, cssSyntaxError) {
	/** @type {StylelintResult} */
	let stylelintResult;
	/** @type {string | undefined} */
	let source;

	if (postcssResult && postcssResult.root) {
		source = !postcssResult.root.source
			? undefined
			: postcssResult.root.source.input.file || postcssResult.root.source.input.id;

		// Strip out deprecation warnings from the messages
		const deprecationMessages = _.remove(postcssResult.messages, {
			stylelintType: 'deprecation',
		});
		const deprecations = deprecationMessages.map((deprecationMessage) => {
			return {
				text: deprecationMessage.text,
				reference: deprecationMessage.stylelintReference,
			};
		});

		// Also strip out invalid options
		const invalidOptionMessages = _.remove(postcssResult.messages, {
			stylelintType: 'invalidOption',
		});
		const invalidOptionWarnings = invalidOptionMessages.map((invalidOptionMessage) => {
			return {
				text: invalidOptionMessage.text,
			};
		});

		const parseErrors = _.remove(postcssResult.messages, {
			stylelintType: 'parseError',
		});

		// This defines the stylelint result object that formatters receive
		stylelintResult = {
			source,
			deprecations,
			invalidOptionWarnings,
			// TODO TYPES check which types are valid? postcss? stylelint?
			/* eslint-disable-next-line */
			parseErrors: /** @type {any} */ (parseErrors),
			errored: postcssResult.stylelint.stylelintError,
			warnings: postcssResult.messages.map((message) => {
				return {
					line: message.line,
					column: message.column,
					rule: message.rule,
					severity: message.severity,
					text: message.text,
				};
			}),
			ignored: postcssResult.stylelint.ignored,
			_postcssResult: postcssResult,
		};
	} else if (cssSyntaxError) {
		if (cssSyntaxError.name !== 'CssSyntaxError') {
			throw cssSyntaxError;
		}

		stylelintResult = {
			source: cssSyntaxError.file || '<input css 1>',
			deprecations: [],
			invalidOptionWarnings: [],
			parseErrors: [],
			errored: true,
			warnings: [
				{
					line: cssSyntaxError.line,
					column: cssSyntaxError.column,
					rule: cssSyntaxError.name,
					severity: 'error',
					text: `${cssSyntaxError.reason} (${cssSyntaxError.name})`,
				},
			],
		};
	} else {
		throw new Error(
			'createStylelintResult must be called with either postcssResult or CssSyntaxError',
		);
	}

	return stylelint.getConfigForFile(filePath).then((configForFile) => {
		// TODO TYPES handle possible null here
		const config =
			/** @type {{ config: import('stylelint').StylelintConfig, filepath: string }} */ (configForFile).config;
		const file = source || (cssSyntaxError && cssSyntaxError.file);

		if (config.resultProcessors) {
			config.resultProcessors.forEach((resultProcessor) => {
				// Result processors might just mutate the result object,
				// or might return a new one
				const returned = resultProcessor(stylelintResult, file);

				if (returned) {
					stylelintResult = returned;
				}
			});
		}

		return stylelintResult;
	});
};
