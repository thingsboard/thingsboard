'use strict';

const dynamicRequire = require('./dynamicRequire');
const fs = require('fs');
const LazyResult = require('postcss/lib/lazy-result');
const postcss = require('postcss');
/** @type {import('postcss-syntax')} */
let autoSyntax = null;

/** @typedef {import('stylelint').StylelintInternalApi} StylelintInternalApi */
/** @typedef {{parse: any, stringify: any}} Syntax */

const postcssProcessor = postcss();

/**
 * @param {StylelintInternalApi} stylelint
 * @param {import('stylelint').GetPostcssOptions} options
 *
 * @returns {Promise<import('postcss').Result>}
 */
module.exports = function(stylelint, options = {}) {
	const cached = options.filePath ? stylelint._postcssResultCache.get(options.filePath) : undefined;

	if (cached) return Promise.resolve(cached);

	/** @type {Promise<string> | undefined} */
	let getCode;

	if (options.code !== undefined) {
		getCode = Promise.resolve(options.code);
	} else if (options.filePath) {
		getCode = readFile(options.filePath);
	}

	if (!getCode) {
		throw new Error('code or filePath required');
	}

	return getCode
		.then((code) => {
			const customSyntax = stylelint._options.customSyntax;
			/** @type {Syntax | null} */
			let syntax = null;

			if (customSyntax) {
				try {
					// TODO TYPES determine which type has customSyntax
					const useCustomSyntax = /** @type {any} */ dynamicRequire(customSyntax);

					/*
					 * PostCSS allows for syntaxes that only contain a parser, however,
					 * it then expects the syntax to be set as the `parser` option rather than `syntax`.
					 */
					if (!useCustomSyntax.parse) {
						syntax = {
							parse: useCustomSyntax,
							stringify: postcss.stringify,
						};
					} else {
						syntax = useCustomSyntax;
					}
				} catch (e) {
					throw new Error(`Cannot resolve custom syntax module ${customSyntax}`);
				}
			} else if (stylelint._options.syntax === 'css') {
				syntax = cssSyntax(stylelint);
			} else if (stylelint._options.syntax) {
				/** @type {{[k: string]: string}} */
				const syntaxes = {
					'css-in-js': 'postcss-jsx',
					html: 'postcss-html',
					less: 'postcss-less',
					markdown: 'postcss-markdown',
					sass: 'postcss-sass',
					scss: 'postcss-scss',
					sugarss: 'sugarss',
				};

				const syntaxFromOptions = syntaxes[stylelint._options.syntax];

				if (!syntaxFromOptions) {
					throw new Error(
						'You must use a valid syntax option, either: css, css-in-js, html, less, markdown, sass, scss, or sugarss',
					);
				}

				syntax = dynamicRequire(syntaxFromOptions);
			} else if (
				!(options.codeProcessors && options.codeProcessors.length) ||
				(options.filePath && /\.(scss|sass|less)$/.test(options.filePath))
			) {
				if (!autoSyntax) {
					autoSyntax = require('postcss-syntax');
				}

				syntax = autoSyntax({
					css: cssSyntax(stylelint),
				});
			}

			const postcssOptions = {
				from: options.filePath,
				syntax,
			};

			const source = options.code ? options.codeFilename : options.filePath;
			let preProcessedCode = code;

			if (options.codeProcessors && options.codeProcessors.length) {
				if (stylelint._options.fix) {
					// eslint-disable-next-line no-console
					console.warn(
						'Autofix is incompatible with processors and will be disabled. Are you sure you need a processor?',
					);
					stylelint._options.fix = false;
				}

				options.codeProcessors.forEach((codeProcessor) => {
					preProcessedCode = codeProcessor(preProcessedCode, source);
				});
			}

			const result = new LazyResult(postcssProcessor, preProcessedCode, postcssOptions);

			return result;
		})
		.then((postcssResult) => {
			if (options.filePath) {
				stylelint._postcssResultCache.set(options.filePath, postcssResult);
			}

			return postcssResult;
		});
};

/**
 * @param {string} filePath
 * @returns {Promise<string>}
 */
function readFile(filePath) {
	return new Promise((resolve, reject) => {
		fs.readFile(filePath, 'utf8', (err, content) => {
			if (err) {
				return reject(err);
			}

			resolve(content);
		});
	});
}

/**
 * @param {StylelintInternalApi} stylelint
 * @returns {Syntax}
 */
function cssSyntax(stylelint) {
	return {
		parse: stylelint._options.fix ? dynamicRequire('postcss-safe-parser') : postcss.parse,
		stringify: postcss.stringify,
	};
}
