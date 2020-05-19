//
// index.js
// Should expose the additional browser functions on to the less object
//
import {addDataAttr} from './utils';
import lessRoot from '../less';
import browser from './browser';
import FM from './file-manager';
import PluginLoader from './plugin-loader';
import LogListener from './log-listener';
import ErrorReporting from './error-reporting';
import Cache from './cache';
import ImageSize from './image-size';

export default (window, options) => {
    const document = window.document;
    const less = lessRoot();

    less.options = options;
    const environment = less.environment;
    const FileManager = FM(options, less.logger);
    const fileManager = new FileManager();
    environment.addFileManager(fileManager);
    less.FileManager = FileManager;
    less.PluginLoader = PluginLoader;

    LogListener(less, options);
    const errors = ErrorReporting(window, less, options);
    const cache = less.cache = options.cache || Cache(window, options, less.logger);
    ImageSize(less.environment);

    // Setup user functions - Deprecate?
    if (options.functions) {
        less.functions.functionRegistry.addMultiple(options.functions);
    }

    const typePattern = /^text\/(x-)?less$/;

    function clone(obj) {
        const cloned = {};
        for (const prop in obj) {
            if (obj.hasOwnProperty(prop)) {
                cloned[prop] = obj[prop];
            }
        }
        return cloned;
    }

    // only really needed for phantom
    function bind(func, thisArg) {
        const curryArgs = Array.prototype.slice.call(arguments, 2);
        return function() {
            const args = curryArgs.concat(Array.prototype.slice.call(arguments, 0));
            return func.apply(thisArg, args);
        };
    }

    function loadStyles(modifyVars) {
        const styles = document.getElementsByTagName('style');
        let style;

        for (let i = 0; i < styles.length; i++) {
            style = styles[i];
            if (style.type.match(typePattern)) {
                const instanceOptions = clone(options);
                instanceOptions.modifyVars = modifyVars;
                const lessText = style.innerHTML || '';
                instanceOptions.filename = document.location.href.replace(/#.*$/, '');

                /* jshint loopfunc:true */
                // use closure to store current style
                less.render(lessText, instanceOptions,
                    bind((style, e, result) => {
                        if (e) {
                            errors.add(e, 'inline');
                        } else {
                            style.type = 'text/css';
                            if (style.styleSheet) {
                                style.styleSheet.cssText = result.css;
                            } else {
                                style.innerHTML = result.css;
                            }
                        }
                    }, null, style));
            }
        }
    }

    function loadStyleSheet(sheet, callback, reload, remaining, modifyVars) {

        const instanceOptions = clone(options);
        addDataAttr(instanceOptions, sheet);
        instanceOptions.mime = sheet.type;

        if (modifyVars) {
            instanceOptions.modifyVars = modifyVars;
        }

        function loadInitialFileCallback(loadedFile) {
            const data = loadedFile.contents;
            const path = loadedFile.filename;
            const webInfo = loadedFile.webInfo;

            const newFileInfo = {
                currentDirectory: fileManager.getPath(path),
                filename: path,
                rootFilename: path,
                rewriteUrls: instanceOptions.rewriteUrls
            };

            newFileInfo.entryPath = newFileInfo.currentDirectory;
            newFileInfo.rootpath = instanceOptions.rootpath || newFileInfo.currentDirectory;

            if (webInfo) {
                webInfo.remaining = remaining;

                const css = cache.getCSS(path, webInfo, instanceOptions.modifyVars);
                if (!reload && css) {
                    webInfo.local = true;
                    callback(null, css, data, sheet, webInfo, path);
                    return;
                }

            }

            // TODO add tests around how this behaves when reloading
            errors.remove(path);

            instanceOptions.rootFileInfo = newFileInfo;
            less.render(data, instanceOptions, (e, result) => {
                if (e) {
                    e.href = path;
                    callback(e);
                } else {
                    cache.setCSS(sheet.href, webInfo.lastModified, instanceOptions.modifyVars, result.css);
                    callback(null, result.css, data, sheet, webInfo, path);
                }
            });
        }

        fileManager.loadFile(sheet.href, null, instanceOptions, environment)
            .then(loadedFile => {
                loadInitialFileCallback(loadedFile);
            }).catch(err => {
                console.log(err);
                callback(err);
            });

    }

    function loadStyleSheets(callback, reload, modifyVars) {
        for (let i = 0; i < less.sheets.length; i++) {
            loadStyleSheet(less.sheets[i], callback, reload, less.sheets.length - (i + 1), modifyVars);
        }
    }

    function initRunningMode() {
        if (less.env === 'development') {
            less.watchTimer = setInterval(() => {
                if (less.watchMode) {
                    fileManager.clearFileCache();
                    loadStyleSheets((e, css, _, sheet, webInfo) => {
                        if (e) {
                            errors.add(e, e.href || sheet.href);
                        } else if (css) {
                            browser.createCSS(window.document, css, sheet);
                        }
                    });
                }
            }, options.poll);
        }
    }

    //
    // Watch mode
    //
    less.watch   = function () {
        if (!less.watchMode ) {
            less.env = 'development';
            initRunningMode();
        }
        this.watchMode = true;
        return true;
    };

    less.unwatch = function () {clearInterval(less.watchTimer); this.watchMode = false; return false; };

    //
    // Synchronously get all <link> tags with the 'rel' attribute set to
    // "stylesheet/less".
    //
    less.registerStylesheetsImmediately = () => {
        const links = document.getElementsByTagName('link');
        less.sheets = [];

        for (let i = 0; i < links.length; i++) {
            if (links[i].rel === 'stylesheet/less' || (links[i].rel.match(/stylesheet/) &&
                (links[i].type.match(typePattern)))) {
                less.sheets.push(links[i]);
            }
        }
    };

    //
    // Asynchronously get all <link> tags with the 'rel' attribute set to
    // "stylesheet/less", returning a Promise.
    //
    less.registerStylesheets = () => new Promise((resolve, reject) => {
        less.registerStylesheetsImmediately();
        resolve();
    });

    //
    // With this function, it's possible to alter variables and re-render
    // CSS without reloading less-files
    //
    less.modifyVars = record => less.refresh(true, record, false);

    less.refresh = (reload, modifyVars, clearFileCache) => {
        if ((reload || clearFileCache) && clearFileCache !== false) {
            fileManager.clearFileCache();
        }
        return new Promise((resolve, reject) => {
            let startTime;
            let endTime;
            let totalMilliseconds;
            let remainingSheets;
            startTime = endTime = new Date();

            // Set counter for remaining unprocessed sheets
            remainingSheets = less.sheets.length;

            if (remainingSheets === 0) {

                endTime = new Date();
                totalMilliseconds = endTime - startTime;
                less.logger.info('Less has finished and no sheets were loaded.');
                resolve({
                    startTime,
                    endTime,
                    totalMilliseconds,
                    sheets: less.sheets.length
                });

            } else {
                // Relies on less.sheets array, callback seems to be guaranteed to be called for every element of the array
                loadStyleSheets((e, css, _, sheet, webInfo) => {
                    if (e) {
                        errors.add(e, e.href || sheet.href);
                        reject(e);
                        return;
                    }
                    if (webInfo.local) {
                        less.logger.info(`Loading ${sheet.href} from cache.`);
                    } else {
                        less.logger.info(`Rendered ${sheet.href} successfully.`);
                    }
                    browser.createCSS(window.document, css, sheet);
                    less.logger.info(`CSS for ${sheet.href} generated in ${new Date() - endTime}ms`);

                    // Count completed sheet
                    remainingSheets--;

                    // Check if the last remaining sheet was processed and then call the promise
                    if (remainingSheets === 0) {
                        totalMilliseconds = new Date() - startTime;
                        less.logger.info(`Less has finished. CSS generated in ${totalMilliseconds}ms`);
                        resolve({
                            startTime,
                            endTime,
                            totalMilliseconds,
                            sheets: less.sheets.length
                        });
                    }
                    endTime = new Date();
                }, reload, modifyVars);
            }

            loadStyles(modifyVars);
        });
    };

    less.refreshStyles = loadStyles;
    return less;
};
