"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.readFile = exports.stat = void 0;

const stat = (inputFileSystem, path) => new Promise((resolve, reject) => {
  inputFileSystem.stat(path, (err, stats) => {
    if (err) {
      reject(err);
    }

    resolve(stats);
  });
});

exports.stat = stat;

const readFile = (inputFileSystem, path) => new Promise((resolve, reject) => {
  inputFileSystem.readFile(path, (err, stats) => {
    if (err) {
      reject(err);
    }

    resolve(stats);
  });
});

exports.readFile = readFile;