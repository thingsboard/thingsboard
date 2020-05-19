'use strict';

const importLazy = require('import-lazy')(require);

module.exports = {
	compact: require('./compactFormatter'),
	json: require('./jsonFormatter'),
	string: importLazy('./stringFormatter'),
	unix: require('./unixFormatter'),
	verbose: importLazy('./verboseFormatter'),
};
