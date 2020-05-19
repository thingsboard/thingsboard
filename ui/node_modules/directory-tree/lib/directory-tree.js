'use strict';

const FS = require('fs');
const PATH = require('path');
const constants = {
	DIRECTORY: 'directory',
	FILE: 'file'
}

function safeReadDirSync (path) {
	let dirData = {};
	try {
		dirData = FS.readdirSync(path);
	} catch(ex) {
		if (ex.code == "EACCES" || ex.code == "EPERM") {
			//User does not have permissions, ignore directory
			return null;
		}
		else throw ex;
	}
	return dirData;
}

/**
 * Normalizes windows style paths by replacing double backslahes with single forward slahes (unix style).
 * @param  {string} path
 * @return {string}
 */
function normalizePath(path) {
	return path.replace(/\\/g, '/');
}

/**
 * Tests if the supplied parameter is of type RegExp
 * @param  {any}  regExp
 * @return {Boolean}
 */
function isRegExp(regExp) {
	return typeof regExp === "object" && regExp.constructor == RegExp;
}

/**
 * Collects the files and folders for a directory path into an Object, subject
 * to the options supplied, and invoking optional
 * @param  {String} path
 * @param  {Object} options
 * @param  {function} onEachFile
 * @param  {function} onEachDirectory
 * @return {Object}
 */
function directoryTree (path, options, onEachFile, onEachDirectory) {
	const name = PATH.basename(path);
	path = options && options.normalizePath ? normalizePath(path) : path;
	const item = { path, name };
	let stats;

	try { stats = FS.statSync(path); }
	catch (e) { return null; }

	// Skip if it matches the exclude regex
	if (options && options.exclude) {
		const excludes =  isRegExp(options.exclude) ? [options.exclude] : options.exclude;
		if (excludes.some((exclusion) => exclusion.test(path))) {
			return null;
		}
	}

	if (stats.isFile()) {

		const ext = PATH.extname(path).toLowerCase();

		// Skip if it does not match the extension regex
		if (options && options.extensions && !options.extensions.test(ext))
			return null;

		item.size = stats.size;  // File size in bytes
		item.extension = ext;
		item.type = constants.FILE;

		if (options && options.attributes) {
			options.attributes.forEach((attribute) => {
				item[attribute] = stats[attribute];
			});
		}

		if (onEachFile) {
			onEachFile(item, PATH, stats);
		}
	}
	else if (stats.isDirectory()) {
		let dirData = safeReadDirSync(path);
		if (dirData === null) return null;

		if (options && options.attributes) {
			options.attributes.forEach((attribute) => {
				item[attribute] = stats[attribute];
			});
		}
		item.children = dirData
			.map(child => directoryTree(PATH.join(path, child), options, onEachFile, onEachDirectory))
			.filter(e => !!e);
		item.size = item.children.reduce((prev, cur) => prev + cur.size, 0);
		item.type = constants.DIRECTORY;
		if (onEachDirectory) {
			onEachDirectory(item, PATH, stats);
		}
	} else {
		return null; // Or set item.size = 0 for devices, FIFO and sockets ?
	}
	return item;
}

module.exports = directoryTree;
