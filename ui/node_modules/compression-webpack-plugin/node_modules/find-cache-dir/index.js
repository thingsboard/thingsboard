'use strict';
const path = require('path');
const fs = require('fs');
const commonDir = require('commondir');
const pkgDir = require('pkg-dir');
const makeDir = require('make-dir');

const isWritable = path => {
	try {
		fs.accessSync(path, fs.constants.W_OK);
		return true;
	} catch (_) {
		return false;
	}
};

module.exports = (options = {}) => {
	const {name} = options;
	let directory = options.cwd;

	if (options.files) {
		directory = commonDir(directory, options.files);
	} else {
		directory = directory || process.cwd();
	}

	directory = pkgDir.sync(directory);

	if (directory) {
		const nodeModules = path.join(directory, 'node_modules');
		if (
			!isWritable(nodeModules) &&
			(fs.existsSync(nodeModules) || !isWritable(path.join(directory)))
		) {
			return undefined;
		}

		directory = path.join(directory, 'node_modules', '.cache', name);

		if (directory && options.create) {
			makeDir.sync(directory);
		}

		if (options.thunk) {
			return (...arguments_) => path.join(directory, ...arguments_);
		}
	}

	return directory;
};
