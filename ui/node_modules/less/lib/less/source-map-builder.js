export default (SourceMapOutput, environment) => {
    class SourceMapBuilder {
        constructor(options) {
            this.options = options;
        }

        toCSS(rootNode, options, imports) {
            const sourceMapOutput = new SourceMapOutput(
                {
                    contentsIgnoredCharsMap: imports.contentsIgnoredChars,
                    rootNode,
                    contentsMap: imports.contents,
                    sourceMapFilename: this.options.sourceMapFilename,
                    sourceMapURL: this.options.sourceMapURL,
                    outputFilename: this.options.sourceMapOutputFilename,
                    sourceMapBasepath: this.options.sourceMapBasepath,
                    sourceMapRootpath: this.options.sourceMapRootpath,
                    outputSourceFiles: this.options.outputSourceFiles,
                    sourceMapGenerator: this.options.sourceMapGenerator,
                    sourceMapFileInline: this.options.sourceMapFileInline
                });

            const css = sourceMapOutput.toCSS(options);
            this.sourceMap = sourceMapOutput.sourceMap;
            this.sourceMapURL = sourceMapOutput.sourceMapURL;
            if (this.options.sourceMapInputFilename) {
                this.sourceMapInputFilename = sourceMapOutput.normalizeFilename(this.options.sourceMapInputFilename);
            }
            if (this.options.sourceMapBasepath !== undefined && this.sourceMapURL !== undefined) {
                this.sourceMapURL = sourceMapOutput.removeBasepath(this.sourceMapURL);
            }
            return css + this.getCSSAppendage();
        }

        getCSSAppendage() {

            let sourceMapURL = this.sourceMapURL;
            if (this.options.sourceMapFileInline) {
                if (this.sourceMap === undefined) {
                    return '';
                }
                sourceMapURL = `data:application/json;base64,${environment.encodeBase64(this.sourceMap)}`;
            }

            if (sourceMapURL) {
                return `/*# sourceMappingURL=${sourceMapURL} */`;
            }
            return '';
        }

        getExternalSourceMap() {
            return this.sourceMap;
        }

        setExternalSourceMap(sourceMap) {
            this.sourceMap = sourceMap;
        }

        isInline() {
            return this.options.sourceMapFileInline;
        }

        getSourceMapURL() {
            return this.sourceMapURL;
        }

        getOutputFilename() {
            return this.options.sourceMapOutputFilename;
        }

        getInputFilename() {
            return this.sourceMapInputFilename;
        }
    }

    return SourceMapBuilder;
};
