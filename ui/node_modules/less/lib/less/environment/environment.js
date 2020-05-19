/**
 * @todo Document why this abstraction exists, and the relationship between
 *       environment, file managers, and plugin manager
 */

import logger from '../logger';

class environment {
    constructor(externalEnvironment, fileManagers) {
        this.fileManagers = fileManagers || [];
        externalEnvironment = externalEnvironment || {};

        const optionalFunctions = ['encodeBase64', 'mimeLookup', 'charsetLookup', 'getSourceMapGenerator'];
        const requiredFunctions = [];
        const functions = requiredFunctions.concat(optionalFunctions);

        for (let i = 0; i < functions.length; i++) {
            const propName = functions[i];
            const environmentFunc = externalEnvironment[propName];
            if (environmentFunc) {
                this[propName] = environmentFunc.bind(externalEnvironment);
            } else if (i < requiredFunctions.length) {
                this.warn(`missing required function in environment - ${propName}`);
            }
        }
    }

    getFileManager(filename, currentDirectory, options, environment, isSync) {

        if (!filename) {
            logger.warn('getFileManager called with no filename.. Please report this issue. continuing.');
        }
        if (currentDirectory == null) {
            logger.warn('getFileManager called with null directory.. Please report this issue. continuing.');
        }

        let fileManagers = this.fileManagers;
        if (options.pluginManager) {
            fileManagers = [].concat(fileManagers).concat(options.pluginManager.getFileManagers());
        }
        for (let i = fileManagers.length - 1; i >= 0 ; i--) {
            const fileManager = fileManagers[i];
            if (fileManager[isSync ? 'supportsSync' : 'supports'](filename, currentDirectory, options, environment)) {
                return fileManager;
            }
        }
        return null;
    }

    addFileManager(fileManager) {
        this.fileManagers.push(fileManager);
    }

    clearFileManagers() {
        this.fileManagers = [];
    }
}

export default environment;
