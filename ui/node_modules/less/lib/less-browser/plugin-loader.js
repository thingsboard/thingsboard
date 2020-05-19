// TODO: Add tests for browser @plugin
/* global window */

import AbstractPluginLoader from '../less/environment/abstract-plugin-loader.js';

/**
 * Browser Plugin Loader
 */
class PluginLoader extends AbstractPluginLoader {
    constructor(less) {
        super();

        this.less = less;
        // Should we shim this.require for browser? Probably not?
    }

    loadPlugin(filename, basePath, context, environment, fileManager) {
        return new Promise((fulfill, reject) => {
            fileManager.loadFile(filename, basePath, context, environment)
                .then(fulfill).catch(reject);
        });
    }
}

export default PluginLoader;

