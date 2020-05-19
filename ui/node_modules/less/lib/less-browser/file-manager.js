/* global window, XMLHttpRequest */

import AbstractFileManager from '../less/environment/abstract-file-manager.js';

let options;
let logger;
let fileCache = {};

// TODOS - move log somewhere. pathDiff and doing something similar in node. use pathDiff in the other browser file for the initial load
class FileManager extends AbstractFileManager {
    alwaysMakePathsAbsolute() {
        return true;
    }

    join(basePath, laterPath) {
        if (!basePath) {
            return laterPath;
        }
        return this.extractUrlParts(laterPath, basePath).path;
    }

    doXHR(url, type, callback, errback) {
        const xhr = new XMLHttpRequest();
        const async = options.isFileProtocol ? options.fileAsync : true;

        if (typeof xhr.overrideMimeType === 'function') {
            xhr.overrideMimeType('text/css');
        }
        logger.debug(`XHR: Getting '${url}'`);
        xhr.open('GET', url, async);
        xhr.setRequestHeader('Accept', type || 'text/x-less, text/css; q=0.9, */*; q=0.5');
        xhr.send(null);

        function handleResponse(xhr, callback, errback) {
            if (xhr.status >= 200 && xhr.status < 300) {
                callback(xhr.responseText,
                    xhr.getResponseHeader('Last-Modified'));
            } else if (typeof errback === 'function') {
                errback(xhr.status, url);
            }
        }

        if (options.isFileProtocol && !options.fileAsync) {
            if (xhr.status === 0 || (xhr.status >= 200 && xhr.status < 300)) {
                callback(xhr.responseText);
            } else {
                errback(xhr.status, url);
            }
        } else if (async) {
            xhr.onreadystatechange = () => {
                if (xhr.readyState == 4) {
                    handleResponse(xhr, callback, errback);
                }
            };
        } else {
            handleResponse(xhr, callback, errback);
        }
    }

    supports() {
        return true;
    }

    clearFileCache() {
        fileCache = {};
    }

    loadFile(filename, currentDirectory, options, environment) {
        // TODO: Add prefix support like less-node?
        // What about multiple paths?

        if (currentDirectory && !this.isPathAbsolute(filename)) {
            filename = currentDirectory + filename;
        }

        filename = options.ext ? this.tryAppendExtension(filename, options.ext) : filename;

        options = options || {};

        // sheet may be set to the stylesheet for the initial load or a collection of properties including
        // some context variables for imports
        const hrefParts = this.extractUrlParts(filename, window.location.href);
        const href      = hrefParts.url;
        const self      = this;
        
        return new Promise((resolve, reject) => {
            if (options.useFileCache && fileCache[href]) {
                try {
                    const lessText = fileCache[href];
                    return resolve({ contents: lessText, filename: href, webInfo: { lastModified: new Date() }});
                } catch (e) {
                    return reject({ filename: href, message: `Error loading file ${href} error was ${e.message}` });
                }
            }

            self.doXHR(href, options.mime, function doXHRCallback(data, lastModified) {
                // per file cache
                fileCache[href] = data;

                // Use remote copy (re-parse)
                resolve({ contents: data, filename: href, webInfo: { lastModified }});
            }, function doXHRError(status, url) {
                reject({ type: 'File', message: `'${url}' wasn't found (${status})`, href });
            });
        });
    }
}

export default (opts, log) => {
    options = opts;
    logger = log;
    return FileManager;
}
