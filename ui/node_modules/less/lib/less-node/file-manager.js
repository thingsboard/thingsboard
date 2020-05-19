import path from 'path';
import fs from './fs';
import AbstractFileManager from '../less/environment/abstract-file-manager.js';

class FileManager extends AbstractFileManager {
    supports() {
        return true;
    }

    supportsSync() {
        return true;
    }

    loadFile(filename, currentDirectory, options, environment, callback) {
        let fullFilename;
        const isAbsoluteFilename = this.isPathAbsolute(filename);
        const filenamesTried = [];
        const self = this;
        const prefix = filename.slice(0, 1);
        const explicit = prefix === '.' || prefix === '/';
        let result = null;
        let isNodeModule = false;
        const npmPrefix = 'npm://';

        options = options || {};

        const paths = isAbsoluteFilename ? [''] : [currentDirectory];

        if (options.paths) { paths.push(...options.paths); }

        if (!isAbsoluteFilename && paths.indexOf('.') === -1) { paths.push('.'); }

        const prefixes = options.prefixes || [''];
        const fileParts = this.extractUrlParts(filename);

        if (options.syncImport) {
            getFileData(returnData, returnData);
            if (callback) {
                callback(result.error, result);
            }
            else {
                return result;
            }
        }
        else {
            // promise is guaranteed to be asyncronous
            // which helps as it allows the file handle
            // to be closed before it continues with the next file
            return new Promise(getFileData);
        }

        function returnData(data) {
            if (!data.filename) {
                result = { error: data };
            }
            else {
                result = data;
            }
        }

        function getFileData(fulfill, reject) {
            (function tryPathIndex(i) {
                if (i < paths.length) {
                    (function tryPrefix(j) {
                        if (j < prefixes.length) {
                            isNodeModule = false;
                            fullFilename = fileParts.rawPath + prefixes[j] + fileParts.filename;

                            if (paths[i]) {
                                fullFilename = path.join(paths[i], fullFilename);
                            }

                            if (!explicit && paths[i] === '.') {
                                try {
                                    fullFilename = require.resolve(fullFilename);
                                    isNodeModule = true;
                                }
                                catch (e) {
                                    filenamesTried.push(npmPrefix + fullFilename);
                                    tryWithExtension();
                                }
                            }
                            else {
                                tryWithExtension();
                            }

                            function tryWithExtension() {
                                const extFilename = options.ext ? self.tryAppendExtension(fullFilename, options.ext) : fullFilename;

                                if (extFilename !== fullFilename && !explicit && paths[i] === '.') {
                                    try {
                                        fullFilename = require.resolve(extFilename);
                                        isNodeModule = true;
                                    }
                                    catch (e) {
                                        filenamesTried.push(npmPrefix + extFilename);
                                        fullFilename = extFilename;
                                    }
                                }
                                else {
                                    fullFilename = extFilename;
                                }
                            }
                        
                            const readFileArgs = [fullFilename];
                            if (!options.rawBuffer) {
                                readFileArgs.push('utf-8');
                            }
                            if (options.syncImport) {
                                try {
                                    const data = fs.readFileSync.apply(this, readFileArgs);
                                    fulfill({ contents: data, filename: fullFilename});
                                }
                                catch (e) {
                                    filenamesTried.push(isNodeModule ? npmPrefix + fullFilename : fullFilename);
                                    return tryPrefix(j + 1);
                                }
                            }
                            else {
                                readFileArgs.push(function(e, data) {
                                    if (e) {
                                        filenamesTried.push(isNodeModule ? npmPrefix + fullFilename : fullFilename);
                                        return tryPrefix(j + 1);
                                    }
                                    fulfill({ contents: data, filename: fullFilename});
                                });
                                fs.readFile.apply(this, readFileArgs);
                            }
                        }
                        else {
                            tryPathIndex(i + 1);
                        }
                    })(0);
                } else {
                    reject({ type: 'File', message: `'${filename}' wasn't found. Tried - ${filenamesTried.join(',')}` });
                }
            }(0));
        }
    }

    loadFileSync(filename, currentDirectory, options, environment) {
        options.syncImport = true;
        return this.loadFile(filename, currentDirectory, options, environment);
    }
}

export default FileManager;