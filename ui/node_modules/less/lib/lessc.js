import * as path from 'path';
import fs from './less-node/fs';
import * as os from 'os';
import * as utils from './less/utils';
import * as Constants from './less/constants';
let errno;
let mkdirp;

try {
    errno = require('errno');
} catch (err) {
    errno = null;
}

import less from './less-node';
const pluginManager = new less.PluginManager(less);
const fileManager = new less.FileManager();
const plugins = [];
const queuePlugins = [];
let args = process.argv.slice(1);
let silent = false;
let verbose = false;
const options = less.options;

options.plugins = plugins;
options.reUsePluginManager = true;

const sourceMapOptions = {};
let continueProcessing = true;

const checkArgFunc = (arg, option) => {
    if (!option) {
        console.error(`${arg} option requires a parameter`);
        continueProcessing = false;
        process.exitCode = 1;
        return false;
    }
    return true;
};

const checkBooleanArg = arg => {
    const onOff = /^((on|t|true|y|yes)|(off|f|false|n|no))$/i.exec(arg);
    if (!onOff) {
        console.error(` unable to parse ${arg} as a boolean. use one of on/t/true/y/yes/off/f/false/n/no`);
        continueProcessing = false;
        process.exitCode = 1;
        return false;
    }
    return Boolean(onOff[2]);
};

const parseVariableOption = (option, variables) => {
    const parts = option.split('=', 2);
    variables[parts[0]] = parts[1];
};

let sourceMapFileInline = false;

function printUsage() {
    less.lesscHelper.printUsage();
    pluginManager.Loader.printUsage(plugins);
    continueProcessing = false;
}
function render() {

    if (!continueProcessing) {
        return;
    }

    let input = args[1];
    if (input && input != '-') {
        input = path.resolve(process.cwd(), input);
    }
    let output = args[2];
    const outputbase = args[2];
    if (output) {
        output = path.resolve(process.cwd(), output);
    }

    if (options.sourceMap) {

        sourceMapOptions.sourceMapInputFilename = input;
        if (!sourceMapOptions.sourceMapFullFilename) {
            if (!output && !sourceMapFileInline) {
                console.error('the sourcemap option only has an optional filename if the css filename is given');
                console.error('consider adding --source-map-map-inline which embeds the sourcemap into the css');
                process.exitCode = 1;
                return;
            }
            // its in the same directory, so always just the basename
            if (output) {
                sourceMapOptions.sourceMapOutputFilename = path.basename(output);
                sourceMapOptions.sourceMapFullFilename = `${output}.map`;
            }
            // its in the same directory, so always just the basename
            if ('sourceMapFullFilename' in sourceMapOptions) {
                sourceMapOptions.sourceMapFilename = path.basename(sourceMapOptions.sourceMapFullFilename);
            }
        } else if (options.sourceMap && !sourceMapFileInline) {
            const mapFilename = path.resolve(process.cwd(), sourceMapOptions.sourceMapFullFilename);
            const mapDir = path.dirname(mapFilename);
            const outputDir = path.dirname(output);
            // find the path from the map to the output file
            sourceMapOptions.sourceMapOutputFilename = path.join(
                path.relative(mapDir, outputDir), path.basename(output));

            // make the sourcemap filename point to the sourcemap relative to the css file output directory
            sourceMapOptions.sourceMapFilename = path.join(
                path.relative(outputDir, mapDir), path.basename(sourceMapOptions.sourceMapFullFilename));
        }
    }

    if (sourceMapOptions.sourceMapBasepath === undefined) {
        sourceMapOptions.sourceMapBasepath = input ? path.dirname(input) : process.cwd();
    }

    if (sourceMapOptions.sourceMapRootpath === undefined) {
        const pathToMap = path.dirname((sourceMapFileInline ? output : sourceMapOptions.sourceMapFullFilename) || '.');
        const pathToInput = path.dirname(sourceMapOptions.sourceMapInputFilename || '.');
        sourceMapOptions.sourceMapRootpath = path.relative(pathToMap, pathToInput);
    }


    if (!input) {
        console.error('lessc: no input files');
        console.error('');
        printUsage();
        process.exitCode = 1;
        return;
    }

    const ensureDirectory = filepath => {
        const dir = path.dirname(filepath);
        let cmd;
        const existsSync = fs.existsSync || path.existsSync;
        if (!existsSync(dir)) {
            if (mkdirp === undefined) {
                try {mkdirp = require('mkdirp');}
                catch (e) { mkdirp = null; }
            }
            cmd = mkdirp && mkdirp.sync || fs.mkdirSync;
            cmd(dir);
        }
    };

    if (options.depends) {
        if (!outputbase) {
            console.error('option --depends requires an output path to be specified');
            process.exitCode = 1;
            return;
        }
        process.stdout.write(`${outputbase}: `);
    }

    if (!sourceMapFileInline) {
        var writeSourceMap = (output = '', onDone) => {
            const filename = sourceMapOptions.sourceMapFullFilename;
            ensureDirectory(filename);
            fs.writeFile(filename, output, 'utf8', err => {
                if (err) {
                    let description = 'Error: ';
                    if (errno && errno.errno[err.errno]) {
                        description += errno.errno[err.errno].description;
                    } else {
                        description += `${err.code} ${err.message}`;
                    }
                    console.error(`lessc: failed to create file ${filename}`);
                    console.error(description);
                    process.exitCode = 1;
                } else {
                    less.logger.info(`lessc: wrote ${filename}`);
                }
                onDone();
            });
        };
    }

    const writeSourceMapIfNeeded = (output, onDone) => {
        if (options.sourceMap && !sourceMapFileInline) {
            writeSourceMap(output, onDone);
        } else {
            onDone();
        }
    };

    const writeOutput = (output, result, onSuccess) => {
        if (options.depends) {
            onSuccess();
        } else if (output) {
            ensureDirectory(output);
            fs.writeFile(output, result.css, {encoding: 'utf8'}, err => {
                if (err) {
                    let description = 'Error: ';
                    if (errno && errno.errno[err.errno]) {
                        description += errno.errno[err.errno].description;
                    } else {
                        description += `${err.code} ${err.message}`;
                    }
                    console.error(`lessc: failed to create file ${output}`);
                    console.error(description);
                    process.exitCode = 1;
                } else {
                    less.logger.info(`lessc: wrote ${output}`);
                    onSuccess();
                }
            });
        } else if (!options.depends) {
            process.stdout.write(result.css);
            onSuccess();
        }
    };

    const logDependencies = (options, result) => {
        if (options.depends) {
            let depends = '';
            for (let i = 0; i < result.imports.length; i++) {
                depends += `${result.imports[i]} `;
            }
            console.log(depends);
        }
    };

    const parseLessFile = (e, data) => {
        if (e) {
            console.error(`lessc: ${e.message}`);
            process.exitCode = 1;
            return;
        }

        data = data.replace(/^\uFEFF/, '');

        options.paths = [path.dirname(input)].concat(options.paths);
        options.filename = input;

        if (options.lint) {
            options.sourceMap = false;
        }
        sourceMapOptions.sourceMapFileInline = sourceMapFileInline;

        if (options.sourceMap) {
            options.sourceMap = sourceMapOptions;
        }

        less.logger.addListener({
            info: function(msg) {
                if (verbose) {
                    console.log(msg);
                }
            },
            warn: function(msg) {
                // do not show warning if the silent option is used
                if (!silent) {
                    console.warn(msg);
                }
            },
            error: function(msg) {
                console.error(msg);
            }
        });

        less.render(data, options)
            .then(result => {
                if (!options.lint) {
                    writeOutput(output, result, () => {
                        writeSourceMapIfNeeded(result.map, () => {
                            logDependencies(options, result);
                        });
                    });
                }
            },
            err => {
                if (!options.silent) {
                    console.error(err.toString({
                        stylize: options.color && less.lesscHelper.stylize
                    }));
                }
                process.exitCode = 1;
            });
    };

    if (input != '-') {
        fs.readFile(input, 'utf8', parseLessFile);
    } else {
        process.stdin.resume();
        process.stdin.setEncoding('utf8');

        let buffer = '';
        process.stdin.on('data', data => {
            buffer += data;
        });

        process.stdin.on('end', () => {
            parseLessFile(false, buffer);
        });
    }
}

function processPluginQueue() {
    let x = 0;

    function pluginError(name) {
        console.error(`Unable to load plugin ${name} please make sure that it is installed under or at the same level as less`);
        process.exitCode = 1;
    }
    function pluginFinished(plugin) {
        x++;
        plugins.push(plugin);
        if (x === queuePlugins.length) {
            render();
        }
    }
    queuePlugins.forEach(queue => {
        const context = utils.clone(options);
        pluginManager.Loader.loadPlugin(queue.name, process.cwd(), context, less.environment, fileManager)
            .then(data => {
                pluginFinished({
                    fileContent: data.contents,
                    filename: data.filename,
                    options: queue.options
                });
            })
            .catch(() => {
                pluginError(queue.name);
            });
    });
}

// self executing function so we can return
(() => {
    args = args.filter(arg => {
        let match;

        match = arg.match(/^-I(.+)$/);
        if (match) {
            options.paths.push(match[1]);
            return false;
        }

        match = arg.match(/^--?([a-z][0-9a-z-]*)(?:=(.*))?$/i);
        if (match) {
            arg = match[1];
        } else {
            return arg;
        }

        switch (arg) {
            case 'v':
            case 'version':
                console.log(`lessc ${less.version.join('.')} (Less Compiler) [JavaScript]`);
                continueProcessing = false;
                break;
            case 'verbose':
                verbose = true;
                break;
            case 's':
            case 'silent':
                silent = true;
                break;
            case 'l':
            case 'lint':
                options.lint = true;
                break;
            case 'strict-imports':
                options.strictImports = true;
                break;
            case 'h':
            case 'help':
                printUsage();
                break;
            case 'x':
            case 'compress':
                options.compress = true;
                break;
            case 'insecure':
                options.insecure = true;
                break;
            case 'M':
            case 'depends':
                options.depends = true;
                break;
            case 'max-line-len':
                if (checkArgFunc(arg, match[2])) {
                    options.maxLineLen = parseInt(match[2], 10);
                    if (options.maxLineLen <= 0) {
                        options.maxLineLen = -1;
                    }
                }
                break;
            case 'no-color':
                options.color = false;
                break;
            case 'js':
                options.javascriptEnabled = true;
                break;
            case 'no-js':
                console.error('The "--no-js" argument is deprecated, as inline JavaScript ' +
                    'is disabled by default. Use "--js" to enable inline JavaScript (not recommended).');
                break;
            case 'include-path':
                if (checkArgFunc(arg, match[2])) {
                    // ; supported on windows.
                    // : supported on windows and linux, excluding a drive letter like C:\ so C:\file:D:\file parses to 2
                    options.paths = match[2]
                        .split(os.type().match(/Windows/) ? /:(?!\\)|;/ : ':')
                        .map(p => {
                            if (p) {
                                return path.resolve(process.cwd(), p);
                            }
                        });
                }
                break;
            case 'line-numbers':
                if (checkArgFunc(arg, match[2])) {
                    options.dumpLineNumbers = match[2];
                }
                break;
            case 'source-map':
                options.sourceMap = true;
                if (match[2]) {
                    sourceMapOptions.sourceMapFullFilename = match[2];
                }
                break;
            case 'source-map-rootpath':
                if (checkArgFunc(arg, match[2])) {
                    sourceMapOptions.sourceMapRootpath = match[2];
                }
                break;
            case 'source-map-basepath':
                if (checkArgFunc(arg, match[2])) {
                    sourceMapOptions.sourceMapBasepath = match[2];
                }
                break;
            case 'source-map-inline':
            case 'source-map-map-inline':
                sourceMapFileInline = true;
                options.sourceMap = true;
                break;
            case 'source-map-include-source':
            case 'source-map-less-inline':
                sourceMapOptions.outputSourceFiles = true;
                break;
            case 'source-map-url':
                if (checkArgFunc(arg, match[2])) {
                    sourceMapOptions.sourceMapURL = match[2];
                }
                break;
            case 'rp':
            case 'rootpath':
                if (checkArgFunc(arg, match[2])) {
                    options.rootpath = match[2].replace(/\\/g, '/');
                }
                break;
            case 'relative-urls':
                console.warn('The --relative-urls option has been deprecated. Use --rewrite-urls=all.');
                options.rewriteUrls = Constants.RewriteUrls.ALL;
                break;
            case 'ru':
            case 'rewrite-urls':
                const m = match[2];
                if (m) {
                    if (m === 'local') {
                        options.rewriteUrls = Constants.RewriteUrls.LOCAL;
                    } else if (m === 'off') {
                        options.rewriteUrls = Constants.RewriteUrls.OFF;
                    } else if (m === 'all') {
                        options.rewriteUrls = Constants.RewriteUrls.ALL;
                    } else {
                        console.error(`Unknown rewrite-urls argument ${m}`);
                        continueProcessing = false;
                        process.exitCode = 1;
                    }
                } else {
                    options.rewriteUrls = Constants.RewriteUrls.ALL;
                }
                break;
            case 'sm':
            case 'strict-math':
                console.warn('The --strict-math option has been deprecated. Use --math=strict.');
                if (checkArgFunc(arg, match[2])) {
                    if (checkBooleanArg(match[2])) {
                        options.math = Constants.Math.STRICT_LEGACY;
                    }
                }
                break;
            case 'm':
            case 'math':
                if (checkArgFunc(arg, match[2])) {
                    options.math = match[2];
                }
                break;
            case 'su':
            case 'strict-units':
                if (checkArgFunc(arg, match[2])) {
                    options.strictUnits = checkBooleanArg(match[2]);
                }
                break;
            case 'global-var':
                if (checkArgFunc(arg, match[2])) {
                    if (!options.globalVars) {
                        options.globalVars = {};
                    }
                    parseVariableOption(match[2], options.globalVars);
                }
                break;
            case 'modify-var':
                if (checkArgFunc(arg, match[2])) {
                    if (!options.modifyVars) {
                        options.modifyVars = {};
                    }

                    parseVariableOption(match[2], options.modifyVars);
                }
                break;
            case 'url-args':
                if (checkArgFunc(arg, match[2])) {
                    options.urlArgs = match[2];
                }
                break;
            case 'plugin':
                const splitupArg = match[2].match(/^([^=]+)(=(.*))?/);
                const name = splitupArg[1];
                const pluginOptions = splitupArg[3];
                queuePlugins.push({ name, options: pluginOptions });
                break;
            default:
                queuePlugins.push({ name: arg, options: match[2], default: true });
                break;
        }
    });

    if (queuePlugins.length > 0) {
        processPluginQueue();
    }
    else {
        render();
    }

})();
