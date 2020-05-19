const isUrlRe = /^(?:https?:)?\/\//i;
import url from 'url';
let request;
import AbstractFileManager from '../less/environment/abstract-file-manager.js';
import logger from '../less/logger';

class UrlFileManager extends AbstractFileManager {
    supports(filename, currentDirectory, options, environment) {
        return isUrlRe.test( filename ) || isUrlRe.test(currentDirectory);
    }

    loadFile(filename, currentDirectory, options, environment) {
        return new Promise((fulfill, reject) => {
            if (request === undefined) {
                try { request = require('request'); }
                catch (e) { request = null; }
            }
            if (!request) {
                reject({ type: 'File', message: 'optional dependency \'request\' required to import over http(s)\n' });
                return;
            }

            let urlStr = isUrlRe.test( filename ) ? filename : url.resolve(currentDirectory, filename);
            const urlObj = url.parse(urlStr);

            if (!urlObj.protocol) {
                urlObj.protocol = 'http';
                urlStr = urlObj.format();
            }

            request.get({uri: urlStr, strictSSL: !options.insecure }, (error, res, body) => {
                if (error) {
                    reject({ type: 'File', message: `resource '${urlStr}' gave this Error:\n  ${error}\n` });
                    return;
                }
                if (res && res.statusCode === 404) {
                    reject({ type: 'File', message: `resource '${urlStr}' was not found\n` });
                    return;
                }
                if (!body) {
                    logger.warn(`Warning: Empty body (HTTP ${res.statusCode}) returned by "${urlStr}"`);
                }
                fulfill({ contents: body, filename: urlStr });
            });
        });
    }
}

export default UrlFileManager;
