import LessError from './less-error';
import transformTree from './transform-tree';
import logger from './logger';

export default SourceMapBuilder => {
    class ParseTree {
        constructor(root, imports) {
            this.root = root;
            this.imports = imports;
        }

        toCSS(options) {
            let evaldRoot;
            const result = {};
            let sourceMapBuilder;
            try {
                evaldRoot = transformTree(this.root, options);
            } catch (e) {
                throw new LessError(e, this.imports);
            }

            try {
                const compress = Boolean(options.compress);
                if (compress) {
                    logger.warn('The compress option has been deprecated. ' + 
                        'We recommend you use a dedicated css minifier, for instance see less-plugin-clean-css.');
                }

                const toCSSOptions = {
                    compress,
                    dumpLineNumbers: options.dumpLineNumbers,
                    strictUnits: Boolean(options.strictUnits),
                    numPrecision: 8};

                if (options.sourceMap) {
                    sourceMapBuilder = new SourceMapBuilder(options.sourceMap);
                    result.css = sourceMapBuilder.toCSS(evaldRoot, toCSSOptions, this.imports);
                } else {
                    result.css = evaldRoot.toCSS(toCSSOptions);
                }
            } catch (e) {
                throw new LessError(e, this.imports);
            }

            if (options.pluginManager) {
                const postProcessors = options.pluginManager.getPostProcessors();
                for (let i = 0; i < postProcessors.length; i++) {
                    result.css = postProcessors[i].process(result.css, { sourceMap: sourceMapBuilder, options, imports: this.imports });
                }
            }
            if (options.sourceMap) {
                result.map = sourceMapBuilder.getExternalSourceMap();
            }

            result.imports = [];
            for (const file in this.imports.files) {
                if (this.imports.files.hasOwnProperty(file) && file !== this.imports.rootFilename) {
                    result.imports.push(file);
                }
            }
            return result;
        }
    }

    return ParseTree;
};
