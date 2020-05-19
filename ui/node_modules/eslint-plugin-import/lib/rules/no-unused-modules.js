'use strict';

var _ExportMap = require('../ExportMap');

var _ExportMap2 = _interopRequireDefault(_ExportMap);

var _ignore = require('eslint-module-utils/ignore');

var _resolve = require('eslint-module-utils/resolve');

var _resolve2 = _interopRequireDefault(_resolve);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

var _path = require('path');

var _readPkgUp = require('read-pkg-up');

var _readPkgUp2 = _interopRequireDefault(_readPkgUp);

var _object = require('object.values');

var _object2 = _interopRequireDefault(_object);

var _arrayIncludes = require('array-includes');

var _arrayIncludes2 = _interopRequireDefault(_arrayIncludes);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _toConsumableArray(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) arr2[i] = arr[i]; return arr2; } else { return Array.from(arr); } } /**
                                                                                                                                                                                                 * @fileOverview Ensures that modules contain exports and/or all
                                                                                                                                                                                                 * modules are consumed within other modules.
                                                                                                                                                                                                 * @author René Fermann
                                                                                                                                                                                                 */

// eslint/lib/util/glob-util has been moved to eslint/lib/util/glob-utils with version 5.3
// and has been moved to eslint/lib/cli-engine/file-enumerator in version 6
let listFilesToProcess;
try {
  const FileEnumerator = require('eslint/lib/cli-engine/file-enumerator').FileEnumerator;
  listFilesToProcess = function (src, extensions) {
    const e = new FileEnumerator({
      extensions: extensions
    });
    return Array.from(e.iterateFiles(src), (_ref) => {
      let filePath = _ref.filePath,
          ignored = _ref.ignored;
      return {
        ignored,
        filename: filePath
      };
    });
  };
} catch (e1) {
  // Prevent passing invalid options (extensions array) to old versions of the function.
  // https://github.com/eslint/eslint/blob/v5.16.0/lib/util/glob-utils.js#L178-L280
  // https://github.com/eslint/eslint/blob/v5.2.0/lib/util/glob-util.js#L174-L269
  let originalListFilesToProcess;
  try {
    originalListFilesToProcess = require('eslint/lib/util/glob-utils').listFilesToProcess;
    listFilesToProcess = function (src, extensions) {
      return originalListFilesToProcess(src, {
        extensions: extensions
      });
    };
  } catch (e2) {
    originalListFilesToProcess = require('eslint/lib/util/glob-util').listFilesToProcess;

    listFilesToProcess = function (src, extensions) {
      const patterns = src.reduce((carry, pattern) => {
        return carry.concat(extensions.map(extension => {
          return (/\*\*|\*\./.test(pattern) ? pattern : `${pattern}/**/*${extension}`
          );
        }));
      }, src.slice());

      return originalListFilesToProcess(patterns);
    };
  }
}

const EXPORT_DEFAULT_DECLARATION = 'ExportDefaultDeclaration';
const EXPORT_NAMED_DECLARATION = 'ExportNamedDeclaration';
const EXPORT_ALL_DECLARATION = 'ExportAllDeclaration';
const IMPORT_DECLARATION = 'ImportDeclaration';
const IMPORT_NAMESPACE_SPECIFIER = 'ImportNamespaceSpecifier';
const IMPORT_DEFAULT_SPECIFIER = 'ImportDefaultSpecifier';
const VARIABLE_DECLARATION = 'VariableDeclaration';
const FUNCTION_DECLARATION = 'FunctionDeclaration';
const CLASS_DECLARATION = 'ClassDeclaration';
const DEFAULT = 'default';
const TYPE_ALIAS = 'TypeAlias';

const importList = new Map();
const exportList = new Map();
const ignoredFiles = new Set();
const filesOutsideSrc = new Set();

const isNodeModule = path => {
  return (/\/(node_modules)\//.test(path)
  );
};

/**
 * read all files matching the patterns in src and ignoreExports
 *
 * return all files matching src pattern, which are not matching the ignoreExports pattern
 */
const resolveFiles = (src, ignoreExports, context) => {
  const extensions = Array.from((0, _ignore.getFileExtensions)(context.settings));

  const srcFiles = new Set();
  const srcFileList = listFilesToProcess(src, extensions);

  // prepare list of ignored files
  const ignoredFilesList = listFilesToProcess(ignoreExports, extensions);
  ignoredFilesList.forEach((_ref2) => {
    let filename = _ref2.filename;
    return ignoredFiles.add(filename);
  });

  // prepare list of source files, don't consider files from node_modules
  srcFileList.filter((_ref3) => {
    let filename = _ref3.filename;
    return !isNodeModule(filename);
  }).forEach((_ref4) => {
    let filename = _ref4.filename;

    srcFiles.add(filename);
  });
  return srcFiles;
};

/**
 * parse all source files and build up 2 maps containing the existing imports and exports
 */
const prepareImportsAndExports = (srcFiles, context) => {
  const exportAll = new Map();
  srcFiles.forEach(file => {
    const exports = new Map();
    const imports = new Map();
    const currentExports = _ExportMap2.default.get(file, context);
    if (currentExports) {
      const dependencies = currentExports.dependencies,
            reexports = currentExports.reexports,
            localImportList = currentExports.imports,
            namespace = currentExports.namespace;

      // dependencies === export * from

      const currentExportAll = new Set();
      dependencies.forEach(getDependency => {
        const dependency = getDependency();
        if (dependency === null) {
          return;
        }

        currentExportAll.add(dependency.path);
      });
      exportAll.set(file, currentExportAll);

      reexports.forEach((value, key) => {
        if (key === DEFAULT) {
          exports.set(IMPORT_DEFAULT_SPECIFIER, { whereUsed: new Set() });
        } else {
          exports.set(key, { whereUsed: new Set() });
        }
        const reexport = value.getImport();
        if (!reexport) {
          return;
        }
        let localImport = imports.get(reexport.path);
        let currentValue;
        if (value.local === DEFAULT) {
          currentValue = IMPORT_DEFAULT_SPECIFIER;
        } else {
          currentValue = value.local;
        }
        if (typeof localImport !== 'undefined') {
          localImport = new Set([].concat(_toConsumableArray(localImport), [currentValue]));
        } else {
          localImport = new Set([currentValue]);
        }
        imports.set(reexport.path, localImport);
      });

      localImportList.forEach((value, key) => {
        if (isNodeModule(key)) {
          return;
        }
        imports.set(key, value.importedSpecifiers);
      });
      importList.set(file, imports);

      // build up export list only, if file is not ignored
      if (ignoredFiles.has(file)) {
        return;
      }
      namespace.forEach((value, key) => {
        if (key === DEFAULT) {
          exports.set(IMPORT_DEFAULT_SPECIFIER, { whereUsed: new Set() });
        } else {
          exports.set(key, { whereUsed: new Set() });
        }
      });
    }
    exports.set(EXPORT_ALL_DECLARATION, { whereUsed: new Set() });
    exports.set(IMPORT_NAMESPACE_SPECIFIER, { whereUsed: new Set() });
    exportList.set(file, exports);
  });
  exportAll.forEach((value, key) => {
    value.forEach(val => {
      const currentExports = exportList.get(val);
      const currentExport = currentExports.get(EXPORT_ALL_DECLARATION);
      currentExport.whereUsed.add(key);
    });
  });
};

/**
 * traverse through all imports and add the respective path to the whereUsed-list
 * of the corresponding export
 */
const determineUsage = () => {
  importList.forEach((listValue, listKey) => {
    listValue.forEach((value, key) => {
      const exports = exportList.get(key);
      if (typeof exports !== 'undefined') {
        value.forEach(currentImport => {
          let specifier;
          if (currentImport === IMPORT_NAMESPACE_SPECIFIER) {
            specifier = IMPORT_NAMESPACE_SPECIFIER;
          } else if (currentImport === IMPORT_DEFAULT_SPECIFIER) {
            specifier = IMPORT_DEFAULT_SPECIFIER;
          } else {
            specifier = currentImport;
          }
          if (typeof specifier !== 'undefined') {
            const exportStatement = exports.get(specifier);
            if (typeof exportStatement !== 'undefined') {
              const whereUsed = exportStatement.whereUsed;

              whereUsed.add(listKey);
              exports.set(specifier, { whereUsed });
            }
          }
        });
      }
    });
  });
};

const getSrc = src => {
  if (src) {
    return src;
  }
  return [process.cwd()];
};

/**
 * prepare the lists of existing imports and exports - should only be executed once at
 * the start of a new eslint run
 */
let srcFiles;
let lastPrepareKey;
const doPreparation = (src, ignoreExports, context) => {
  const prepareKey = JSON.stringify({
    src: (src || []).sort(),
    ignoreExports: (ignoreExports || []).sort(),
    extensions: Array.from((0, _ignore.getFileExtensions)(context.settings)).sort()
  });
  if (prepareKey === lastPrepareKey) {
    return;
  }

  importList.clear();
  exportList.clear();
  ignoredFiles.clear();
  filesOutsideSrc.clear();

  srcFiles = resolveFiles(getSrc(src), ignoreExports, context);
  prepareImportsAndExports(srcFiles, context);
  determineUsage();
  lastPrepareKey = prepareKey;
};

const newNamespaceImportExists = specifiers => specifiers.some((_ref5) => {
  let type = _ref5.type;
  return type === IMPORT_NAMESPACE_SPECIFIER;
});

const newDefaultImportExists = specifiers => specifiers.some((_ref6) => {
  let type = _ref6.type;
  return type === IMPORT_DEFAULT_SPECIFIER;
});

const fileIsInPkg = file => {
  var _readPkgUp$sync = _readPkgUp2.default.sync({ cwd: file, normalize: false });

  const path = _readPkgUp$sync.path,
        pkg = _readPkgUp$sync.pkg;

  const basePath = (0, _path.dirname)(path);

  const checkPkgFieldString = pkgField => {
    if ((0, _path.join)(basePath, pkgField) === file) {
      return true;
    }
  };

  const checkPkgFieldObject = pkgField => {
    const pkgFieldFiles = (0, _object2.default)(pkgField).map(value => (0, _path.join)(basePath, value));
    if ((0, _arrayIncludes2.default)(pkgFieldFiles, file)) {
      return true;
    }
  };

  const checkPkgField = pkgField => {
    if (typeof pkgField === 'string') {
      return checkPkgFieldString(pkgField);
    }

    if (typeof pkgField === 'object') {
      return checkPkgFieldObject(pkgField);
    }
  };

  if (pkg.private === true) {
    return false;
  }

  if (pkg.bin) {
    if (checkPkgField(pkg.bin)) {
      return true;
    }
  }

  if (pkg.browser) {
    if (checkPkgField(pkg.browser)) {
      return true;
    }
  }

  if (pkg.main) {
    if (checkPkgFieldString(pkg.main)) {
      return true;
    }
  }

  return false;
};

module.exports = {
  meta: {
    type: 'suggestion',
    docs: { url: (0, _docsUrl2.default)('no-unused-modules') },
    schema: [{
      properties: {
        src: {
          description: 'files/paths to be analyzed (only for unused exports)',
          type: 'array',
          minItems: 1,
          items: {
            type: 'string',
            minLength: 1
          }
        },
        ignoreExports: {
          description: 'files/paths for which unused exports will not be reported (e.g module entry points)',
          type: 'array',
          minItems: 1,
          items: {
            type: 'string',
            minLength: 1
          }
        },
        missingExports: {
          description: 'report modules without any exports',
          type: 'boolean'
        },
        unusedExports: {
          description: 'report exports without any usage',
          type: 'boolean'
        }
      },
      not: {
        properties: {
          unusedExports: { enum: [false] },
          missingExports: { enum: [false] }
        }
      },
      anyOf: [{
        not: {
          properties: {
            unusedExports: { enum: [true] }
          }
        },
        required: ['missingExports']
      }, {
        not: {
          properties: {
            missingExports: { enum: [true] }
          }
        },
        required: ['unusedExports']
      }, {
        properties: {
          unusedExports: { enum: [true] }
        },
        required: ['unusedExports']
      }, {
        properties: {
          missingExports: { enum: [true] }
        },
        required: ['missingExports']
      }]
    }]
  },

  create: context => {
    var _ref7 = context.options[0] || {};

    const src = _ref7.src;
    var _ref7$ignoreExports = _ref7.ignoreExports;
    const ignoreExports = _ref7$ignoreExports === undefined ? [] : _ref7$ignoreExports,
          missingExports = _ref7.missingExports,
          unusedExports = _ref7.unusedExports;


    if (unusedExports) {
      doPreparation(src, ignoreExports, context);
    }

    const file = context.getFilename();

    const checkExportPresence = node => {
      if (!missingExports) {
        return;
      }

      if (ignoredFiles.has(file)) {
        return;
      }

      const exportCount = exportList.get(file);
      const exportAll = exportCount.get(EXPORT_ALL_DECLARATION);
      const namespaceImports = exportCount.get(IMPORT_NAMESPACE_SPECIFIER);

      exportCount.delete(EXPORT_ALL_DECLARATION);
      exportCount.delete(IMPORT_NAMESPACE_SPECIFIER);
      if (exportCount.size < 1) {
        // node.body[0] === 'undefined' only happens, if everything is commented out in the file
        // being linted
        context.report(node.body[0] ? node.body[0] : node, 'No exports found');
      }
      exportCount.set(EXPORT_ALL_DECLARATION, exportAll);
      exportCount.set(IMPORT_NAMESPACE_SPECIFIER, namespaceImports);
    };

    const checkUsage = (node, exportedValue) => {
      if (!unusedExports) {
        return;
      }

      if (ignoredFiles.has(file)) {
        return;
      }

      if (fileIsInPkg(file)) {
        return;
      }

      if (filesOutsideSrc.has(file)) {
        return;
      }

      // make sure file to be linted is included in source files
      if (!srcFiles.has(file)) {
        srcFiles = resolveFiles(getSrc(src), ignoreExports, context);
        if (!srcFiles.has(file)) {
          filesOutsideSrc.add(file);
          return;
        }
      }

      exports = exportList.get(file);

      // special case: export * from
      const exportAll = exports.get(EXPORT_ALL_DECLARATION);
      if (typeof exportAll !== 'undefined' && exportedValue !== IMPORT_DEFAULT_SPECIFIER) {
        if (exportAll.whereUsed.size > 0) {
          return;
        }
      }

      // special case: namespace import
      const namespaceImports = exports.get(IMPORT_NAMESPACE_SPECIFIER);
      if (typeof namespaceImports !== 'undefined') {
        if (namespaceImports.whereUsed.size > 0) {
          return;
        }
      }

      const exportStatement = exports.get(exportedValue);

      const value = exportedValue === IMPORT_DEFAULT_SPECIFIER ? DEFAULT : exportedValue;

      if (typeof exportStatement !== 'undefined') {
        if (exportStatement.whereUsed.size < 1) {
          context.report(node, `exported declaration '${value}' not used within other modules`);
        }
      } else {
        context.report(node, `exported declaration '${value}' not used within other modules`);
      }
    };

    /**
     * only useful for tools like vscode-eslint
     *
     * update lists of existing exports during runtime
     */
    const updateExportUsage = node => {
      if (ignoredFiles.has(file)) {
        return;
      }

      let exports = exportList.get(file);

      // new module has been created during runtime
      // include it in further processing
      if (typeof exports === 'undefined') {
        exports = new Map();
      }

      const newExports = new Map();
      const newExportIdentifiers = new Set();

      node.body.forEach((_ref8) => {
        let type = _ref8.type,
            declaration = _ref8.declaration,
            specifiers = _ref8.specifiers;

        if (type === EXPORT_DEFAULT_DECLARATION) {
          newExportIdentifiers.add(IMPORT_DEFAULT_SPECIFIER);
        }
        if (type === EXPORT_NAMED_DECLARATION) {
          if (specifiers.length > 0) {
            specifiers.forEach(specifier => {
              if (specifier.exported) {
                newExportIdentifiers.add(specifier.exported.name);
              }
            });
          }
          if (declaration) {
            if (declaration.type === FUNCTION_DECLARATION || declaration.type === CLASS_DECLARATION || declaration.type === TYPE_ALIAS) {
              newExportIdentifiers.add(declaration.id.name);
            }
            if (declaration.type === VARIABLE_DECLARATION) {
              declaration.declarations.forEach((_ref9) => {
                let id = _ref9.id;

                newExportIdentifiers.add(id.name);
              });
            }
          }
        }
      });

      // old exports exist within list of new exports identifiers: add to map of new exports
      exports.forEach((value, key) => {
        if (newExportIdentifiers.has(key)) {
          newExports.set(key, value);
        }
      });

      // new export identifiers added: add to map of new exports
      newExportIdentifiers.forEach(key => {
        if (!exports.has(key)) {
          newExports.set(key, { whereUsed: new Set() });
        }
      });

      // preserve information about namespace imports
      let exportAll = exports.get(EXPORT_ALL_DECLARATION);
      let namespaceImports = exports.get(IMPORT_NAMESPACE_SPECIFIER);

      if (typeof namespaceImports === 'undefined') {
        namespaceImports = { whereUsed: new Set() };
      }

      newExports.set(EXPORT_ALL_DECLARATION, exportAll);
      newExports.set(IMPORT_NAMESPACE_SPECIFIER, namespaceImports);
      exportList.set(file, newExports);
    };

    /**
     * only useful for tools like vscode-eslint
     *
     * update lists of existing imports during runtime
     */
    const updateImportUsage = node => {
      if (!unusedExports) {
        return;
      }

      let oldImportPaths = importList.get(file);
      if (typeof oldImportPaths === 'undefined') {
        oldImportPaths = new Map();
      }

      const oldNamespaceImports = new Set();
      const newNamespaceImports = new Set();

      const oldExportAll = new Set();
      const newExportAll = new Set();

      const oldDefaultImports = new Set();
      const newDefaultImports = new Set();

      const oldImports = new Map();
      const newImports = new Map();
      oldImportPaths.forEach((value, key) => {
        if (value.has(EXPORT_ALL_DECLARATION)) {
          oldExportAll.add(key);
        }
        if (value.has(IMPORT_NAMESPACE_SPECIFIER)) {
          oldNamespaceImports.add(key);
        }
        if (value.has(IMPORT_DEFAULT_SPECIFIER)) {
          oldDefaultImports.add(key);
        }
        value.forEach(val => {
          if (val !== IMPORT_NAMESPACE_SPECIFIER && val !== IMPORT_DEFAULT_SPECIFIER) {
            oldImports.set(val, key);
          }
        });
      });

      node.body.forEach(astNode => {
        let resolvedPath;

        // support for export { value } from 'module'
        if (astNode.type === EXPORT_NAMED_DECLARATION) {
          if (astNode.source) {
            resolvedPath = (0, _resolve2.default)(astNode.source.raw.replace(/('|")/g, ''), context);
            astNode.specifiers.forEach(specifier => {
              let name;
              if (specifier.exported.name === DEFAULT) {
                name = IMPORT_DEFAULT_SPECIFIER;
              } else {
                name = specifier.local.name;
              }
              newImports.set(name, resolvedPath);
            });
          }
        }

        if (astNode.type === EXPORT_ALL_DECLARATION) {
          resolvedPath = (0, _resolve2.default)(astNode.source.raw.replace(/('|")/g, ''), context);
          newExportAll.add(resolvedPath);
        }

        if (astNode.type === IMPORT_DECLARATION) {
          resolvedPath = (0, _resolve2.default)(astNode.source.raw.replace(/('|")/g, ''), context);
          if (!resolvedPath) {
            return;
          }

          if (isNodeModule(resolvedPath)) {
            return;
          }

          if (newNamespaceImportExists(astNode.specifiers)) {
            newNamespaceImports.add(resolvedPath);
          }

          if (newDefaultImportExists(astNode.specifiers)) {
            newDefaultImports.add(resolvedPath);
          }

          astNode.specifiers.forEach(specifier => {
            if (specifier.type === IMPORT_DEFAULT_SPECIFIER || specifier.type === IMPORT_NAMESPACE_SPECIFIER) {
              return;
            }
            newImports.set(specifier.imported.name, resolvedPath);
          });
        }
      });

      newExportAll.forEach(value => {
        if (!oldExportAll.has(value)) {
          let imports = oldImportPaths.get(value);
          if (typeof imports === 'undefined') {
            imports = new Set();
          }
          imports.add(EXPORT_ALL_DECLARATION);
          oldImportPaths.set(value, imports);

          let exports = exportList.get(value);
          let currentExport;
          if (typeof exports !== 'undefined') {
            currentExport = exports.get(EXPORT_ALL_DECLARATION);
          } else {
            exports = new Map();
            exportList.set(value, exports);
          }

          if (typeof currentExport !== 'undefined') {
            currentExport.whereUsed.add(file);
          } else {
            const whereUsed = new Set();
            whereUsed.add(file);
            exports.set(EXPORT_ALL_DECLARATION, { whereUsed });
          }
        }
      });

      oldExportAll.forEach(value => {
        if (!newExportAll.has(value)) {
          const imports = oldImportPaths.get(value);
          imports.delete(EXPORT_ALL_DECLARATION);

          const exports = exportList.get(value);
          if (typeof exports !== 'undefined') {
            const currentExport = exports.get(EXPORT_ALL_DECLARATION);
            if (typeof currentExport !== 'undefined') {
              currentExport.whereUsed.delete(file);
            }
          }
        }
      });

      newDefaultImports.forEach(value => {
        if (!oldDefaultImports.has(value)) {
          let imports = oldImportPaths.get(value);
          if (typeof imports === 'undefined') {
            imports = new Set();
          }
          imports.add(IMPORT_DEFAULT_SPECIFIER);
          oldImportPaths.set(value, imports);

          let exports = exportList.get(value);
          let currentExport;
          if (typeof exports !== 'undefined') {
            currentExport = exports.get(IMPORT_DEFAULT_SPECIFIER);
          } else {
            exports = new Map();
            exportList.set(value, exports);
          }

          if (typeof currentExport !== 'undefined') {
            currentExport.whereUsed.add(file);
          } else {
            const whereUsed = new Set();
            whereUsed.add(file);
            exports.set(IMPORT_DEFAULT_SPECIFIER, { whereUsed });
          }
        }
      });

      oldDefaultImports.forEach(value => {
        if (!newDefaultImports.has(value)) {
          const imports = oldImportPaths.get(value);
          imports.delete(IMPORT_DEFAULT_SPECIFIER);

          const exports = exportList.get(value);
          if (typeof exports !== 'undefined') {
            const currentExport = exports.get(IMPORT_DEFAULT_SPECIFIER);
            if (typeof currentExport !== 'undefined') {
              currentExport.whereUsed.delete(file);
            }
          }
        }
      });

      newNamespaceImports.forEach(value => {
        if (!oldNamespaceImports.has(value)) {
          let imports = oldImportPaths.get(value);
          if (typeof imports === 'undefined') {
            imports = new Set();
          }
          imports.add(IMPORT_NAMESPACE_SPECIFIER);
          oldImportPaths.set(value, imports);

          let exports = exportList.get(value);
          let currentExport;
          if (typeof exports !== 'undefined') {
            currentExport = exports.get(IMPORT_NAMESPACE_SPECIFIER);
          } else {
            exports = new Map();
            exportList.set(value, exports);
          }

          if (typeof currentExport !== 'undefined') {
            currentExport.whereUsed.add(file);
          } else {
            const whereUsed = new Set();
            whereUsed.add(file);
            exports.set(IMPORT_NAMESPACE_SPECIFIER, { whereUsed });
          }
        }
      });

      oldNamespaceImports.forEach(value => {
        if (!newNamespaceImports.has(value)) {
          const imports = oldImportPaths.get(value);
          imports.delete(IMPORT_NAMESPACE_SPECIFIER);

          const exports = exportList.get(value);
          if (typeof exports !== 'undefined') {
            const currentExport = exports.get(IMPORT_NAMESPACE_SPECIFIER);
            if (typeof currentExport !== 'undefined') {
              currentExport.whereUsed.delete(file);
            }
          }
        }
      });

      newImports.forEach((value, key) => {
        if (!oldImports.has(key)) {
          let imports = oldImportPaths.get(value);
          if (typeof imports === 'undefined') {
            imports = new Set();
          }
          imports.add(key);
          oldImportPaths.set(value, imports);

          let exports = exportList.get(value);
          let currentExport;
          if (typeof exports !== 'undefined') {
            currentExport = exports.get(key);
          } else {
            exports = new Map();
            exportList.set(value, exports);
          }

          if (typeof currentExport !== 'undefined') {
            currentExport.whereUsed.add(file);
          } else {
            const whereUsed = new Set();
            whereUsed.add(file);
            exports.set(key, { whereUsed });
          }
        }
      });

      oldImports.forEach((value, key) => {
        if (!newImports.has(key)) {
          const imports = oldImportPaths.get(value);
          imports.delete(key);

          const exports = exportList.get(value);
          if (typeof exports !== 'undefined') {
            const currentExport = exports.get(key);
            if (typeof currentExport !== 'undefined') {
              currentExport.whereUsed.delete(file);
            }
          }
        }
      });
    };

    return {
      'Program:exit': node => {
        updateExportUsage(node);
        updateImportUsage(node);
        checkExportPresence(node);
      },
      'ExportDefaultDeclaration': node => {
        checkUsage(node, IMPORT_DEFAULT_SPECIFIER);
      },
      'ExportNamedDeclaration': node => {
        node.specifiers.forEach(specifier => {
          checkUsage(node, specifier.exported.name);
        });
        if (node.declaration) {
          if (node.declaration.type === FUNCTION_DECLARATION || node.declaration.type === CLASS_DECLARATION || node.declaration.type === TYPE_ALIAS) {
            checkUsage(node, node.declaration.id.name);
          }
          if (node.declaration.type === VARIABLE_DECLARATION) {
            node.declaration.declarations.forEach(declaration => {
              checkUsage(node, declaration.id.name);
            });
          }
        }
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby11bnVzZWQtbW9kdWxlcy5qcyJdLCJuYW1lcyI6WyJsaXN0RmlsZXNUb1Byb2Nlc3MiLCJGaWxlRW51bWVyYXRvciIsInJlcXVpcmUiLCJzcmMiLCJleHRlbnNpb25zIiwiZSIsIkFycmF5IiwiZnJvbSIsIml0ZXJhdGVGaWxlcyIsImZpbGVQYXRoIiwiaWdub3JlZCIsImZpbGVuYW1lIiwiZTEiLCJvcmlnaW5hbExpc3RGaWxlc1RvUHJvY2VzcyIsImUyIiwicGF0dGVybnMiLCJyZWR1Y2UiLCJjYXJyeSIsInBhdHRlcm4iLCJjb25jYXQiLCJtYXAiLCJleHRlbnNpb24iLCJ0ZXN0Iiwic2xpY2UiLCJFWFBPUlRfREVGQVVMVF9ERUNMQVJBVElPTiIsIkVYUE9SVF9OQU1FRF9ERUNMQVJBVElPTiIsIkVYUE9SVF9BTExfREVDTEFSQVRJT04iLCJJTVBPUlRfREVDTEFSQVRJT04iLCJJTVBPUlRfTkFNRVNQQUNFX1NQRUNJRklFUiIsIklNUE9SVF9ERUZBVUxUX1NQRUNJRklFUiIsIlZBUklBQkxFX0RFQ0xBUkFUSU9OIiwiRlVOQ1RJT05fREVDTEFSQVRJT04iLCJDTEFTU19ERUNMQVJBVElPTiIsIkRFRkFVTFQiLCJUWVBFX0FMSUFTIiwiaW1wb3J0TGlzdCIsIk1hcCIsImV4cG9ydExpc3QiLCJpZ25vcmVkRmlsZXMiLCJTZXQiLCJmaWxlc091dHNpZGVTcmMiLCJpc05vZGVNb2R1bGUiLCJwYXRoIiwicmVzb2x2ZUZpbGVzIiwiaWdub3JlRXhwb3J0cyIsImNvbnRleHQiLCJzZXR0aW5ncyIsInNyY0ZpbGVzIiwic3JjRmlsZUxpc3QiLCJpZ25vcmVkRmlsZXNMaXN0IiwiZm9yRWFjaCIsImFkZCIsImZpbHRlciIsInByZXBhcmVJbXBvcnRzQW5kRXhwb3J0cyIsImV4cG9ydEFsbCIsImZpbGUiLCJleHBvcnRzIiwiaW1wb3J0cyIsImN1cnJlbnRFeHBvcnRzIiwiRXhwb3J0cyIsImdldCIsImRlcGVuZGVuY2llcyIsInJlZXhwb3J0cyIsImxvY2FsSW1wb3J0TGlzdCIsIm5hbWVzcGFjZSIsImN1cnJlbnRFeHBvcnRBbGwiLCJnZXREZXBlbmRlbmN5IiwiZGVwZW5kZW5jeSIsInNldCIsInZhbHVlIiwia2V5Iiwid2hlcmVVc2VkIiwicmVleHBvcnQiLCJnZXRJbXBvcnQiLCJsb2NhbEltcG9ydCIsImN1cnJlbnRWYWx1ZSIsImxvY2FsIiwiaW1wb3J0ZWRTcGVjaWZpZXJzIiwiaGFzIiwidmFsIiwiY3VycmVudEV4cG9ydCIsImRldGVybWluZVVzYWdlIiwibGlzdFZhbHVlIiwibGlzdEtleSIsImN1cnJlbnRJbXBvcnQiLCJzcGVjaWZpZXIiLCJleHBvcnRTdGF0ZW1lbnQiLCJnZXRTcmMiLCJwcm9jZXNzIiwiY3dkIiwibGFzdFByZXBhcmVLZXkiLCJkb1ByZXBhcmF0aW9uIiwicHJlcGFyZUtleSIsIkpTT04iLCJzdHJpbmdpZnkiLCJzb3J0IiwiY2xlYXIiLCJuZXdOYW1lc3BhY2VJbXBvcnRFeGlzdHMiLCJzcGVjaWZpZXJzIiwic29tZSIsInR5cGUiLCJuZXdEZWZhdWx0SW1wb3J0RXhpc3RzIiwiZmlsZUlzSW5Qa2ciLCJyZWFkUGtnVXAiLCJzeW5jIiwibm9ybWFsaXplIiwicGtnIiwiYmFzZVBhdGgiLCJjaGVja1BrZ0ZpZWxkU3RyaW5nIiwicGtnRmllbGQiLCJjaGVja1BrZ0ZpZWxkT2JqZWN0IiwicGtnRmllbGRGaWxlcyIsImNoZWNrUGtnRmllbGQiLCJwcml2YXRlIiwiYmluIiwiYnJvd3NlciIsIm1haW4iLCJtb2R1bGUiLCJtZXRhIiwiZG9jcyIsInVybCIsInNjaGVtYSIsInByb3BlcnRpZXMiLCJkZXNjcmlwdGlvbiIsIm1pbkl0ZW1zIiwiaXRlbXMiLCJtaW5MZW5ndGgiLCJtaXNzaW5nRXhwb3J0cyIsInVudXNlZEV4cG9ydHMiLCJub3QiLCJlbnVtIiwiYW55T2YiLCJyZXF1aXJlZCIsImNyZWF0ZSIsIm9wdGlvbnMiLCJnZXRGaWxlbmFtZSIsImNoZWNrRXhwb3J0UHJlc2VuY2UiLCJub2RlIiwiZXhwb3J0Q291bnQiLCJuYW1lc3BhY2VJbXBvcnRzIiwiZGVsZXRlIiwic2l6ZSIsInJlcG9ydCIsImJvZHkiLCJjaGVja1VzYWdlIiwiZXhwb3J0ZWRWYWx1ZSIsInVwZGF0ZUV4cG9ydFVzYWdlIiwibmV3RXhwb3J0cyIsIm5ld0V4cG9ydElkZW50aWZpZXJzIiwiZGVjbGFyYXRpb24iLCJsZW5ndGgiLCJleHBvcnRlZCIsIm5hbWUiLCJpZCIsImRlY2xhcmF0aW9ucyIsInVwZGF0ZUltcG9ydFVzYWdlIiwib2xkSW1wb3J0UGF0aHMiLCJvbGROYW1lc3BhY2VJbXBvcnRzIiwibmV3TmFtZXNwYWNlSW1wb3J0cyIsIm9sZEV4cG9ydEFsbCIsIm5ld0V4cG9ydEFsbCIsIm9sZERlZmF1bHRJbXBvcnRzIiwibmV3RGVmYXVsdEltcG9ydHMiLCJvbGRJbXBvcnRzIiwibmV3SW1wb3J0cyIsImFzdE5vZGUiLCJyZXNvbHZlZFBhdGgiLCJzb3VyY2UiLCJyYXciLCJyZXBsYWNlIiwiaW1wb3J0ZWQiXSwibWFwcGluZ3MiOiI7O0FBTUE7Ozs7QUFDQTs7QUFDQTs7OztBQUNBOzs7O0FBQ0E7O0FBQ0E7Ozs7QUFDQTs7OztBQUNBOzs7Ozs7Z01BYkE7Ozs7OztBQWVBO0FBQ0E7QUFDQSxJQUFJQSxrQkFBSjtBQUNBLElBQUk7QUFDRixRQUFNQyxpQkFBaUJDLFFBQVEsdUNBQVIsRUFBaURELGNBQXhFO0FBQ0FELHVCQUFxQixVQUFVRyxHQUFWLEVBQWVDLFVBQWYsRUFBMkI7QUFDOUMsVUFBTUMsSUFBSSxJQUFJSixjQUFKLENBQW1CO0FBQzNCRyxrQkFBWUE7QUFEZSxLQUFuQixDQUFWO0FBR0EsV0FBT0UsTUFBTUMsSUFBTixDQUFXRixFQUFFRyxZQUFGLENBQWVMLEdBQWYsQ0FBWCxFQUFnQztBQUFBLFVBQUdNLFFBQUgsUUFBR0EsUUFBSDtBQUFBLFVBQWFDLE9BQWIsUUFBYUEsT0FBYjtBQUFBLGFBQTRCO0FBQ2pFQSxlQURpRTtBQUVqRUMsa0JBQVVGO0FBRnVELE9BQTVCO0FBQUEsS0FB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QW9EO0FBQ2xEMkcsaUJBQUtZLFdBQUwsQ0FBaUJLLFlBQWpCLENBQThCeEcsT0FBOUIsQ0FBc0NtRyxlQUFlO0FBQ25ETCx5QkFBV1AsSUFBWCxFQUFpQlksWUFBWUksRUFBWixDQUFlRCxJQUFoQztBQUNELGFBRkQ7QUFHRDtBQUNGO0FBQ0Y7QUEzQkksS0FBUDtBQTZCRDtBQWhpQmMsQ0FBakIiLCJmaWxlIjoibm8tdW51c2VkLW1vZHVsZXMuanMiLCJzb3VyY2VzQ29udGVudCI6WyIvKipcbiAqIEBmaWxlT3ZlcnZpZXcgRW5zdXJlcyB0aGF0IG1vZHVsZXMgY29udGFpbiBleHBvcnRzIGFuZC9vciBhbGxcbiAqIG1vZHVsZXMgYXJlIGNvbnN1bWVkIHdpdGhpbiBvdGhlciBtb2R1bGVzLlxuICogQGF1dGhvciBSZW7DqSBGZXJtYW5uXG4gKi9cblxuaW1wb3J0IEV4cG9ydHMgZnJvbSAnLi4vRXhwb3J0TWFwJ1xuaW1wb3J0IHsgZ2V0RmlsZUV4dGVuc2lvbnMgfSBmcm9tICdlc2xpbnQtbW9kdWxlLXV0aWxzL2lnbm9yZSdcbmltcG9ydCByZXNvbHZlIGZyb20gJ2VzbGludC1tb2R1bGUtdXRpbHMvcmVzb2x2ZSdcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnXG5pbXBvcnQgeyBkaXJuYW1lLCBqb2luIH0gZnJvbSAncGF0aCdcbmltcG9ydCByZWFkUGtnVXAgZnJvbSAncmVhZC1wa2ctdXAnXG5pbXBvcnQgdmFsdWVzIGZyb20gJ29iamVjdC52YWx1ZXMnXG5pbXBvcnQgaW5jbHVkZXMgZnJvbSAnYXJyYXktaW5jbHVkZXMnXG5cbi8vIGVzbGludC9saWIvdXRpbC9nbG9iLXV0aWwgaGFzIGJlZW4gbW92ZWQgdG8gZXNsaW50L2xpYi91dGlsL2dsb2ItdXRpbHMgd2l0aCB2ZXJzaW9uIDUuM1xuLy8gYW5kIGhhcyBiZWVuIG1vdmVkIHRvIGVzbGludC9saWIvY2xpLWVuZ2luZS9maWxlLWVudW1lcmF0b3IgaW4gdmVyc2lvbiA2XG5sZXQgbGlzdEZpbGVzVG9Qcm9jZXNzXG50cnkge1xuICBjb25zdCBGaWxlRW51bWVyYXRvciA9IHJlcXVpcmUoJ2VzbGludC9saWIvY2xpLWVuZ2luZS9maWxlLWVudW1lcmF0b3InKS5GaWxlRW51bWVyYXRvclxuICBsaXN0RmlsZXNUb1Byb2Nlc3MgPSBmdW5jdGlvbiAoc3JjLCBleHRlbnNpb25zKSB7XG4gICAgY29uc3QgZSA9IG5ldyBGaWxlRW51bWVyYXRvcih7XG4gICAgICBleHRlbnNpb25zOiBleHRlbnNpb25zLFxuICAgIH0pXG4gICAgcmV0dXJuIEFycmF5LmZyb20oZS5pdGVyYXRlRmlsZXMoc3JjKSwgKHsgZmlsZVBhdGgsIGlnbm9yZWQgfSkgPT4gKHtcbiAgICAgIGlnbm9yZWQsXG4gICAgICBmaWxlbmFtZTogZmlsZVBhdGgsXG4gICAgfSkpXG4gIH1cbn0gY2F0Y2ggKGUxKSB7XG4gIC8vIFByZXZlbnQgcGFzc2luZyBpbnZhbGlkIG9wdGlvbnMgKGV4dGVuc2lvbnMgYXJyYXkpIHRvIG9sZCB2ZXJzaW9ucyBvZiB0aGUgZnVuY3Rpb24uXG4gIC8vIGh0dHBzOi8vZ2l0aHViLmNvbS9lc2xpbnQvZXNsaW50L2Jsb2IvdjUuMTYuMC9saWIvdXRpbC9nbG9iLXV0aWxzLmpzI0wxNzgtTDI4MFxuICAvLyBodHRwczovL2dpdGh1Yi5jb20vZXNsaW50L2VzbGludC9ibG9iL3Y1LjIuMC9saWIvdXRpbC9nbG9iLXV0aWwuanMjTDE3NC1MMjY5XG4gIGxldCBvcmlnaW5hbExpc3RGaWxlc1RvUHJvY2Vzc1xuICB0cnkge1xuICAgIG9yaWdpbmFsTGlzdEZpbGVzVG9Qcm9jZXNzID0gcmVxdWlyZSgnZXNsaW50L2xpYi91dGlsL2dsb2ItdXRpbHMnKS5saXN0RmlsZXNUb1Byb2Nlc3NcbiAgICBsaXN0RmlsZXNUb1Byb2Nlc3MgPSBmdW5jdGlvbiAoc3JjLCBleHRlbnNpb25zKSB7XG4gICAgICByZXR1cm4gb3JpZ2luYWxMaXN0RmlsZXNUb1Byb2Nlc3Moc3JjLCB7XG4gICAgICAgIGV4dGVuc2lvbnM6IGV4dGVuc2lvbnMsXG4gICAgICB9KVxuICAgIH1cbiAgfSBjYXRjaCAoZTIpIHtcbiAgICBvcmlnaW5hbExpc3RGaWxlc1RvUHJvY2VzcyA9IHJlcXVpcmUoJ2VzbGludC9saWIvdXRpbC9nbG9iLXV0aWwnKS5saXN0RmlsZXNUb1Byb2Nlc3NcblxuICAgIGxpc3RGaWxlc1RvUHJvY2VzcyA9IGZ1bmN0aW9uIChzcmMsIGV4dGVuc2lvbnMpIHtcbiAgICAgIGNvbnN0IHBhdHRlcm5zID0gc3JjLnJlZHVjZSgoY2FycnksIHBhdHRlcm4pID0+IHtcbiAgICAgICAgcmV0dXJuIGNhcnJ5LmNvbmNhdChleHRlbnNpb25zLm1hcCgoZXh0ZW5zaW9uKSA9PiB7XG4gICAgICAgICAgcmV0dXJuIC9cXCpcXCp8XFwqXFwuLy50ZXN0KHBhdHRlcm4pID8gcGF0dGVybiA6IGAke3BhdHRlcm59LyoqLyoke2V4dGVuc2lvbn1gXG4gICAgICAgIH0pKVxuICAgICAgfSwgc3JjLnNsaWNlKCkpXG5cbiAgICAgIHJldHVybiBvcmlnaW5hbExpc3RGaWxlc1RvUHJvY2VzcyhwYXR0ZXJucylcbiAgICB9XG4gIH1cbn1cblxuY29uc3QgRVhQT1JUX0RFRkFVTFRfREVDTEFSQVRJT04gPSAnRXhwb3J0RGVmYXVsdERlY2xhcmF0aW9uJ1xuY29uc3QgRVhQT1JUX05BTUVEX0RFQ0xBUkFUSU9OID0gJ0V4cG9ydE5hbWVkRGVjbGFyYXRpb24nXG5jb25zdCBFWFBPUlRfQUxMX0RFQ0xBUkFUSU9OID0gJ0V4cG9ydEFsbERlY2xhcmF0aW9uJ1xuY29uc3QgSU1QT1JUX0RFQ0xBUkFUSU9OID0gJ0ltcG9ydERlY2xhcmF0aW9uJ1xuY29uc3QgSU1QT1JUX05BTUVTUEFDRV9TUEVDSUZJRVIgPSAnSW1wb3J0TmFtZXNwYWNlU3BlY2lmaWVyJ1xuY29uc3QgSU1QT1JUX0RFRkFVTFRfU1BFQ0lGSUVSID0gJ0ltcG9ydERlZmF1bHRTcGVjaWZpZXInXG5jb25zdCBWQVJJQUJMRV9ERUNMQVJBVElPTiA9ICdWYXJpYWJsZURlY2xhcmF0aW9uJ1xuY29uc3QgRlVOQ1RJT05fREVDTEFSQVRJT04gPSAnRnVuY3Rpb25EZWNsYXJhdGlvbidcbmNvbnN0IENMQVNTX0RFQ0xBUkFUSU9OID0gJ0NsYXNzRGVjbGFyYXRpb24nXG5jb25zdCBERUZBVUxUID0gJ2RlZmF1bHQnXG5jb25zdCBUWVBFX0FMSUFTID0gJ1R5cGVBbGlhcydcblxuY29uc3QgaW1wb3J0TGlzdCA9IG5ldyBNYXAoKVxuY29uc3QgZXhwb3J0TGlzdCA9IG5ldyBNYXAoKVxuY29uc3QgaWdub3JlZEZpbGVzID0gbmV3IFNldCgpXG5jb25zdCBmaWxlc091dHNpZGVTcmMgPSBuZXcgU2V0KClcblxuY29uc3QgaXNOb2RlTW9kdWxlID0gcGF0aCA9PiB7XG4gIHJldHVybiAvXFwvKG5vZGVfbW9kdWxlcylcXC8vLnRlc3QocGF0aClcbn1cblxuLyoqXG4gKiByZWFkIGFsbCBmaWxlcyBtYXRjaGluZyB0aGUgcGF0dGVybnMgaW4gc3JjIGFuZCBpZ25vcmVFeHBvcnRzXG4gKlxuICogcmV0dXJuIGFsbCBmaWxlcyBtYXRjaGluZyBzcmMgcGF0dGVybiwgd2hpY2ggYXJlIG5vdCBtYXRjaGluZyB0aGUgaWdub3JlRXhwb3J0cyBwYXR0ZXJuXG4gKi9cbmNvbnN0IHJlc29sdmVGaWxlcyA9IChzcmMsIGlnbm9yZUV4cG9ydHMsIGNvbnRleHQpID0+IHtcbiAgY29uc3QgZXh0ZW5zaW9ucyA9IEFycmF5LmZyb20oZ2V0RmlsZUV4dGVuc2lvbnMoY29udGV4dC5zZXR0aW5ncykpXG5cbiAgY29uc3Qgc3JjRmlsZXMgPSBuZXcgU2V0KClcbiAgY29uc3Qgc3JjRmlsZUxpc3QgPSBsaXN0RmlsZXNUb1Byb2Nlc3Moc3JjLCBleHRlbnNpb25zKVxuXG4gIC8vIHByZXBhcmUgbGlzdCBvZiBpZ25vcmVkIGZpbGVzXG4gIGNvbnN0IGlnbm9yZWRGaWxlc0xpc3QgPSAgbGlzdEZpbGVzVG9Qcm9jZXNzKGlnbm9yZUV4cG9ydHMsIGV4dGVuc2lvbnMpXG4gIGlnbm9yZWRGaWxlc0xpc3QuZm9yRWFjaCgoeyBmaWxlbmFtZSB9KSA9PiBpZ25vcmVkRmlsZXMuYWRkKGZpbGVuYW1lKSlcblxuICAvLyBwcmVwYXJlIGxpc3Qgb2Ygc291cmNlIGZpbGVzLCBkb24ndCBjb25zaWRlciBmaWxlcyBmcm9tIG5vZGVfbW9kdWxlc1xuICBzcmNGaWxlTGlzdC5maWx0ZXIoKHsgZmlsZW5hbWUgfSkgPT4gIWlzTm9kZU1vZHVsZShmaWxlbmFtZSkpLmZvckVhY2goKHsgZmlsZW5hbWUgfSkgPT4ge1xuICAgIHNyY0ZpbGVzLmFkZChmaWxlbmFtZSlcbiAgfSlcbiAgcmV0dXJuIHNyY0ZpbGVzXG59XG5cbi8qKlxuICogcGFyc2UgYWxsIHNvdXJjZSBmaWxlcyBhbmQgYnVpbGQgdXAgMiBtYXBzIGNvbnRhaW5pbmcgdGhlIGV4aXN0aW5nIGltcG9ydHMgYW5kIGV4cG9ydHNcbiAqL1xuY29uc3QgcHJlcGFyZUltcG9ydHNBbmRFeHBvcnRzID0gKHNyY0ZpbGVzLCBjb250ZXh0KSA9PiB7XG4gIGNvbnN0IGV4cG9ydEFsbCA9IG5ldyBNYXAoKVxuICBzcmNGaWxlcy5mb3JFYWNoKGZpbGUgPT4ge1xuICAgIGNvbnN0IGV4cG9ydHMgPSBuZXcgTWFwKClcbiAgICBjb25zdCBpbXBvcnRzID0gbmV3IE1hcCgpXG4gICAgY29uc3QgY3VycmVudEV4cG9ydHMgPSBFeHBvcnRzLmdldChmaWxlLCBjb250ZXh0KVxuICAgIGlmIChjdXJyZW50RXhwb3J0cykge1xuICAgICAgY29uc3QgeyBkZXBlbmRlbmNpZXMsIHJlZXhwb3J0cywgaW1wb3J0czogbG9jYWxJbXBvcnRMaXN0LCBuYW1lc3BhY2UgIH0gPSBjdXJyZW50RXhwb3J0c1xuXG4gICAgICAvLyBkZXBlbmRlbmNpZXMgPT09IGV4cG9ydCAqIGZyb21cbiAgICAgIGNvbnN0IGN1cnJlbnRFeHBvcnRBbGwgPSBuZXcgU2V0KClcbiAgICAgIGRlcGVuZGVuY2llcy5mb3JFYWNoKGdldERlcGVuZGVuY3kgPT4ge1xuICAgICAgICBjb25zdCBkZXBlbmRlbmN5ID0gZ2V0RGVwZW5kZW5jeSgpXG4gICAgICAgIGlmIChkZXBlbmRlbmN5ID09PSBudWxsKSB7XG4gICAgICAgICAgcmV0dXJuXG4gICAgICAgIH1cblxuICAgICAgICBjdXJyZW50RXhwb3J0QWxsLmFkZChkZXBlbmRlbmN5LnBhdGgpXG4gICAgICB9KVxuICAgICAgZXhwb3J0QWxsLnNldChmaWxlLCBjdXJyZW50RXhwb3J0QWxsKVxuXG4gICAgICByZWV4cG9ydHMuZm9yRWFjaCgodmFsdWUsIGtleSkgPT4ge1xuICAgICAgICBpZiAoa2V5ID09PSBERUZBVUxUKSB7XG4gICAgICAgICAgZXhwb3J0cy5zZXQoSU1QT1JUX0RFRkFVTFRfU1BFQ0lGSUVSLCB7IHdoZXJlVXNlZDogbmV3IFNldCgpIH0pXG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgZXhwb3J0cy5zZXQoa2V5LCB7IHdoZXJlVXNlZDogbmV3IFNldCgpIH0pXG4gICAgICAgIH1cbiAgICAgICAgY29uc3QgcmVleHBvcnQgPSAgdmFsdWUuZ2V0SW1wb3J0KClcbiAgICAgICAgaWYgKCFyZWV4cG9ydCkge1xuICAgICAgICAgIHJldHVyblxuICAgICAgICB9XG4gICAgICAgIGxldCBsb2NhbEltcG9ydCA9IGltcG9ydHMuZ2V0KHJlZXhwb3J0LnBhdGgpXG4gICAgICAgIGxldCBjdXJyZW50VmFsdWVcbiAgICAgICAgaWYgKHZhbHVlLmxvY2FsID09PSBERUZBVUxUKSB7XG4gICAgICAgICAgY3VycmVudFZhbHVlID0gSU1QT1JUX0RFRkFVTFRfU1BFQ0lGSUVSXG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgY3VycmVudFZhbHVlID0gdmFsdWUubG9jYWxcbiAgICAgICAgfVxuICAgICAgICBpZiAodHlwZW9mIGxvY2FsSW1wb3J0ICE9PSAndW5kZWZpbmVkJykge1xuICAgICAgICAgIGxvY2FsSW1wb3J0ID0gbmV3IFNldChbLi4ubG9jYWxJbXBvcnQsIGN1cnJlbnRWYWx1ZV0pXG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgbG9jYWxJbXBvcnQgPSBuZXcgU2V0KFtjdXJyZW50VmFsdWVdKVxuICAgICAgICB9XG4gICAgICAgIGltcG9ydHMuc2V0KHJlZXhwb3J0LnBhdGgsIGxvY2FsSW1wb3J0KVxuICAgICAgfSlcblxuICAgICAgbG9jYWxJbXBvcnRMaXN0LmZvckVhY2goKHZhbHVlLCBrZXkpID0+IHtcbiAgICAgICAgaWYgKGlzTm9kZU1vZHVsZShrZXkpKSB7XG4gICAgICAgICAgcmV0dXJuXG4gICAgICAgIH1cbiAgICAgICAgaW1wb3J0cy5zZXQoa2V5LCB2YWx1ZS5pbXBvcnRlZFNwZWNpZmllcnMpXG4gICAgICB9KVxuICAgICAgaW1wb3J0TGlzdC5zZXQoZmlsZSwgaW1wb3J0cylcblxuICAgICAgLy8gYnVpbGQgdXAgZXhwb3J0IGxpc3Qgb25seSwgaWYgZmlsZSBpcyBub3QgaWdub3JlZFxuICAgICAgaWYgKGlnbm9yZWRGaWxlcy5oYXMoZmlsZSkpIHtcbiAgICAgICAgcmV0dXJuXG4gICAgICB9XG4gICAgICBuYW1lc3BhY2UuZm9yRWFjaCgodmFsdWUsIGtleSkgPT4ge1xuICAgICAgICBpZiAoa2V5ID09PSBERUZBVUxUKSB7XG4gICAgICAgICAgZXhwb3J0cy5zZXQoSU1QT1JUX0RFRkFVTFRfU1BFQ0lGSUVSLCB7IHdoZXJlVXNlZDogbmV3IFNldCgpIH0pXG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgZXhwb3J0cy5zZXQoa2V5LCB7IHdoZXJlVXNlZDogbmV3IFNldCgpIH0pXG4gICAgICAgIH1cbiAgICAgIH0pXG4gICAgfVxuICAgIGV4cG9ydHMuc2V0KEVYUE9SVF9BTExfREVDTEFSQVRJT04sIHsgd2hlcmVVc2VkOiBuZXcgU2V0KCkgfSlcbiAgICBleHBvcnRzLnNldChJTVBPUlRfTkFNRVNQQUNFX1NQRUNJRklFUiwgeyB3aGVyZVVzZWQ6IG5ldyBTZXQoKSB9KVxuICAgIGV4cG9ydExpc3Quc2V0KGZpbGUsIGV4cG9ydHMpXG4gIH0pXG4gIGV4cG9ydEFsbC5mb3JFYWNoKCh2YWx1ZSwga2V5KSA9PiB7XG4gICAgdmFsdWUuZm9yRWFjaCh2YWwgPT4ge1xuICAgICAgY29uc3QgY3VycmVudEV4cG9ydHMgPSBleHBvcnRMaXN0LmdldCh2YWwpXG4gICAgICBjb25zdCBjdXJyZW50RXhwb3J0ID0gY3VycmVudEV4cG9ydHMuZ2V0KEVYUE9SVF9BTExfREVDTEFSQVRJT04pXG4gICAgICBjdXJyZW50RXhwb3J0LndoZXJlVXNlZC5hZGQoa2V5KVxuICAgIH0pXG4gIH0pXG59XG5cbi8qKlxuICogdHJhdmVyc2UgdGhyb3VnaCBhbGwgaW1wb3J0cyBhbmQgYWRkIHRoZSByZXNwZWN0aXZlIHBhdGggdG8gdGhlIHdoZXJlVXNlZC1saXN0XG4gKiBvZiB0aGUgY29ycmVzcG9uZGluZyBleHBvcnRcbiAqL1xuY29uc3QgZGV0ZXJtaW5lVXNhZ2UgPSAoKSA9PiB7XG4gIGltcG9ydExpc3QuZm9yRWFjaCgobGlzdFZhbHVlLCBsaXN0S2V5KSA9PiB7XG4gICAgbGlzdFZhbHVlLmZvckVhY2goKHZhbHVlLCBrZXkpID0+IHtcbiAgICAgIGNvbnN0IGV4cG9ydHMgPSBleHBvcnRMaXN0LmdldChrZXkpXG4gICAgICBpZiAodHlwZW9mIGV4cG9ydHMgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgICAgIHZhbHVlLmZvckVhY2goY3VycmVudEltcG9ydCA9PiB7XG4gICAgICAgICAgbGV0IHNwZWNpZmllclxuICAgICAgICAgIGlmIChjdXJyZW50SW1wb3J0ID09PSBJTVBPUlRfTkFNRVNQQUNFX1NQRUNJRklFUikge1xuICAgICAgICAgICAgc3BlY2lmaWVyID0gSU1QT1JUX05BTUVTUEFDRV9TUEVDSUZJRVJcbiAgICAgICAgICB9IGVsc2UgaWYgKGN1cnJlbnRJbXBvcnQgPT09IElNUE9SVF9ERUZBVUxUX1NQRUNJRklFUikge1xuICAgICAgICAgICAgc3BlY2lmaWVyID0gSU1QT1JUX0RFRkFVTFRfU1BFQ0lGSUVSXG4gICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIHNwZWNpZmllciA9IGN1cnJlbnRJbXBvcnRcbiAgICAgICAgICB9XG4gICAgICAgICAgaWYgKHR5cGVvZiBzcGVjaWZpZXIgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgICAgICAgICBjb25zdCBleHBvcnRTdGF0ZW1lbnQgPSBleHBvcnRzLmdldChzcGVjaWZpZXIpXG4gICAgICAgICAgICBpZiAodHlwZW9mIGV4cG9ydFN0YXRlbWVudCAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICAgICAgICAgICAgY29uc3QgeyB3aGVyZVVzZWQgfSA9IGV4cG9ydFN0YXRlbWVudFxuICAgICAgICAgICAgICB3aGVyZVVzZWQuYWRkKGxpc3RLZXkpXG4gICAgICAgICAgICAgIGV4cG9ydHMuc2V0KHNwZWNpZmllciwgeyB3aGVyZVVzZWQgfSlcbiAgICAgICAgICAgIH1cbiAgICAgICAgICB9XG4gICAgICAgIH0pXG4gICAgICB9XG4gICAgfSlcbiAgfSlcbn1cblxuY29uc3QgZ2V0U3JjID0gc3JjID0+IHtcbiAgaWYgKHNyYykge1xuICAgIHJldHVybiBzcmNcbiAgfVxuICByZXR1cm4gW3Byb2Nlc3MuY3dkKCldXG59XG5cbi8qKlxuICogcHJlcGFyZSB0aGUgbGlzdHMgb2YgZXhpc3RpbmcgaW1wb3J0cyBhbmQgZXhwb3J0cyAtIHNob3VsZCBvbmx5IGJlIGV4ZWN1dGVkIG9uY2UgYXRcbiAqIHRoZSBzdGFydCBvZiBhIG5ldyBlc2xpbnQgcnVuXG4gKi9cbmxldCBzcmNGaWxlc1xubGV0IGxhc3RQcmVwYXJlS2V5XG5jb25zdCBkb1ByZXBhcmF0aW9uID0gKHNyYywgaWdub3JlRXhwb3J0cywgY29udGV4dCkgPT4ge1xuICBjb25zdCBwcmVwYXJlS2V5ID0gSlNPTi5zdHJpbmdpZnkoe1xuICAgIHNyYzogKHNyYyB8fCBbXSkuc29ydCgpLFxuICAgIGlnbm9yZUV4cG9ydHM6IChpZ25vcmVFeHBvcnRzIHx8IFtdKS5zb3J0KCksXG4gICAgZXh0ZW5zaW9uczogQXJyYXkuZnJvbShnZXRGaWxlRXh0ZW5zaW9ucyhjb250ZXh0LnNldHRpbmdzKSkuc29ydCgpLFxuICB9KVxuICBpZiAocHJlcGFyZUtleSA9PT0gbGFzdFByZXBhcmVLZXkpIHtcbiAgICByZXR1cm5cbiAgfVxuXG4gIGltcG9ydExpc3QuY2xlYXIoKVxuICBleHBvcnRMaXN0LmNsZWFyKClcbiAgaWdub3JlZEZpbGVzLmNsZWFyKClcbiAgZmlsZXNPdXRzaWRlU3JjLmNsZWFyKClcblxuICBzcmNGaWxlcyA9IHJlc29sdmVGaWxlcyhnZXRTcmMoc3JjKSwgaWdub3JlRXhwb3J0cywgY29udGV4dClcbiAgcHJlcGFyZUltcG9ydHNBbmRFeHBvcnRzKHNyY0ZpbGVzLCBjb250ZXh0KVxuICBkZXRlcm1pbmVVc2FnZSgpXG4gIGxhc3RQcmVwYXJlS2V5ID0gcHJlcGFyZUtleVxufVxuXG5jb25zdCBuZXdOYW1lc3BhY2VJbXBvcnRFeGlzdHMgPSBzcGVjaWZpZXJzID0+XG4gIHNwZWNpZmllcnMuc29tZSgoeyB0eXBlIH0pID0+IHR5cGUgPT09IElNUE9SVF9OQU1FU1BBQ0VfU1BFQ0lGSUVSKVxuXG5jb25zdCBuZXdEZWZhdWx0SW1wb3J0RXhpc3RzID0gc3BlY2lmaWVycyA9PlxuICBzcGVjaWZpZXJzLnNvbWUoKHsgdHlwZSB9KSA9PiB0eXBlID09PSBJTVBPUlRfREVGQVVMVF9TUEVDSUZJRVIpXG5cbmNvbnN0IGZpbGVJc0luUGtnID0gZmlsZSA9PiB7XG4gIGNvbnN0IHsgcGF0aCwgcGtnIH0gPSByZWFkUGtnVXAuc3luYyh7Y3dkOiBmaWxlLCBub3JtYWxpemU6IGZhbHNlfSlcbiAgY29uc3QgYmFzZVBhdGggPSBkaXJuYW1lKHBhdGgpXG5cbiAgY29uc3QgY2hlY2tQa2dGaWVsZFN0cmluZyA9IHBrZ0ZpZWxkID0+IHtcbiAgICBpZiAoam9pbihiYXNlUGF0aCwgcGtnRmllbGQpID09PSBmaWxlKSB7XG4gICAgICAgIHJldHVybiB0cnVlXG4gICAgICB9XG4gIH1cblxuICBjb25zdCBjaGVja1BrZ0ZpZWxkT2JqZWN0ID0gcGtnRmllbGQgPT4ge1xuICAgICAgY29uc3QgcGtnRmllbGRGaWxlcyA9IHZhbHVlcyhwa2dGaWVsZCkubWFwKHZhbHVlID0+IGpvaW4oYmFzZVBhdGgsIHZhbHVlKSlcbiAgICAgIGlmIChpbmNsdWRlcyhwa2dGaWVsZEZpbGVzLCBmaWxlKSkge1xuICAgICAgICByZXR1cm4gdHJ1ZVxuICAgICAgfVxuICB9XG5cbiAgY29uc3QgY2hlY2tQa2dGaWVsZCA9IHBrZ0ZpZWxkID0+IHtcbiAgICBpZiAodHlwZW9mIHBrZ0ZpZWxkID09PSAnc3RyaW5nJykge1xuICAgICAgcmV0dXJuIGNoZWNrUGtnRmllbGRTdHJpbmcocGtnRmllbGQpXG4gICAgfVxuXG4gICAgaWYgKHR5cGVvZiBwa2dGaWVsZCA9PT0gJ29iamVjdCcpIHtcbiAgICAgIHJldHVybiBjaGVja1BrZ0ZpZWxkT2JqZWN0KHBrZ0ZpZWxkKVxuICAgIH1cbiAgfVxuXG4gIGlmIChwa2cucHJpdmF0ZSA9PT0gdHJ1ZSkge1xuICAgIHJldHVybiBmYWxzZVxuICB9XG5cbiAgaWYgKHBrZy5iaW4pIHtcbiAgICBpZiAoY2hlY2tQa2dGaWVsZChwa2cuYmluKSkge1xuICAgICAgcmV0dXJuIHRydWVcbiAgICB9XG4gIH1cblxuICBpZiAocGtnLmJyb3dzZXIpIHtcbiAgICBpZiAoY2hlY2tQa2dGaWVsZChwa2cuYnJvd3NlcikpIHtcbiAgICAgIHJldHVybiB0cnVlXG4gICAgfVxuICB9XG5cbiAgaWYgKHBrZy5tYWluKSB7XG4gICAgaWYgKGNoZWNrUGtnRmllbGRTdHJpbmcocGtnLm1haW4pKSB7XG4gICAgICByZXR1cm4gdHJ1ZVxuICAgIH1cbiAgfVxuXG4gIHJldHVybiBmYWxzZVxufVxuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdzdWdnZXN0aW9uJyxcbiAgICBkb2NzOiB7IHVybDogZG9jc1VybCgnbm8tdW51c2VkLW1vZHVsZXMnKSB9LFxuICAgIHNjaGVtYTogW3tcbiAgICAgIHByb3BlcnRpZXM6IHtcbiAgICAgICAgc3JjOiB7XG4gICAgICAgICAgZGVzY3JpcHRpb246ICdmaWxlcy9wYXRocyB0byBiZSBhbmFseXplZCAob25seSBmb3IgdW51c2VkIGV4cG9ydHMpJyxcbiAgICAgICAgICB0eXBlOiAnYXJyYXknLFxuICAgICAgICAgIG1pbkl0ZW1zOiAxLFxuICAgICAgICAgIGl0ZW1zOiB7XG4gICAgICAgICAgICB0eXBlOiAnc3RyaW5nJyxcbiAgICAgICAgICAgIG1pbkxlbmd0aDogMSxcbiAgICAgICAgICB9LFxuICAgICAgICB9LFxuICAgICAgICBpZ25vcmVFeHBvcnRzOiB7XG4gICAgICAgICAgZGVzY3JpcHRpb246XG4gICAgICAgICAgICAnZmlsZXMvcGF0aHMgZm9yIHdoaWNoIHVudXNlZCBleHBvcnRzIHdpbGwgbm90IGJlIHJlcG9ydGVkIChlLmcgbW9kdWxlIGVudHJ5IHBvaW50cyknLFxuICAgICAgICAgIHR5cGU6ICdhcnJheScsXG4gICAgICAgICAgbWluSXRlbXM6IDEsXG4gICAgICAgICAgaXRlbXM6IHtcbiAgICAgICAgICAgIHR5cGU6ICdzdHJpbmcnLFxuICAgICAgICAgICAgbWluTGVuZ3RoOiAxLFxuICAgICAgICAgIH0sXG4gICAgICAgIH0sXG4gICAgICAgIG1pc3NpbmdFeHBvcnRzOiB7XG4gICAgICAgICAgZGVzY3JpcHRpb246ICdyZXBvcnQgbW9kdWxlcyB3aXRob3V0IGFueSBleHBvcnRzJyxcbiAgICAgICAgICB0eXBlOiAnYm9vbGVhbicsXG4gICAgICAgIH0sXG4gICAgICAgIHVudXNlZEV4cG9ydHM6IHtcbiAgICAgICAgICBkZXNjcmlwdGlvbjogJ3JlcG9ydCBleHBvcnRzIHdpdGhvdXQgYW55IHVzYWdlJyxcbiAgICAgICAgICB0eXBlOiAnYm9vbGVhbicsXG4gICAgICAgIH0sXG4gICAgICB9LFxuICAgICAgbm90OiB7XG4gICAgICAgIHByb3BlcnRpZXM6IHtcbiAgICAgICAgICB1bnVzZWRFeHBvcnRzOiB7IGVudW06IFtmYWxzZV0gfSxcbiAgICAgICAgICBtaXNzaW5nRXhwb3J0czogeyBlbnVtOiBbZmFsc2VdIH0sXG4gICAgICAgIH0sXG4gICAgICB9LFxuICAgICAgYW55T2Y6W3tcbiAgICAgICAgbm90OiB7XG4gICAgICAgICAgcHJvcGVydGllczoge1xuICAgICAgICAgICAgdW51c2VkRXhwb3J0czogeyBlbnVtOiBbdHJ1ZV0gfSxcbiAgICAgICAgICB9LFxuICAgICAgICB9LFxuICAgICAgICByZXF1aXJlZDogWydtaXNzaW5nRXhwb3J0cyddLFxuICAgICAgfSwge1xuICAgICAgICBub3Q6IHtcbiAgICAgICAgICBwcm9wZXJ0aWVzOiB7XG4gICAgICAgICAgICBtaXNzaW5nRXhwb3J0czogeyBlbnVtOiBbdHJ1ZV0gfSxcbiAgICAgICAgICB9LFxuICAgICAgICB9LFxuICAgICAgICByZXF1aXJlZDogWyd1bnVzZWRFeHBvcnRzJ10sXG4gICAgICB9LCB7XG4gICAgICAgIHByb3BlcnRpZXM6IHtcbiAgICAgICAgICB1bnVzZWRFeHBvcnRzOiB7IGVudW06IFt0cnVlXSB9LFxuICAgICAgICB9LFxuICAgICAgICByZXF1aXJlZDogWyd1bnVzZWRFeHBvcnRzJ10sXG4gICAgICB9LCB7XG4gICAgICAgIHByb3BlcnRpZXM6IHtcbiAgICAgICAgICBtaXNzaW5nRXhwb3J0czogeyBlbnVtOiBbdHJ1ZV0gfSxcbiAgICAgICAgfSxcbiAgICAgICAgcmVxdWlyZWQ6IFsnbWlzc2luZ0V4cG9ydHMnXSxcbiAgICAgIH1dLFxuICAgIH1dLFxuICB9LFxuXG4gIGNyZWF0ZTogY29udGV4dCA9PiB7XG4gICAgY29uc3Qge1xuICAgICAgc3JjLFxuICAgICAgaWdub3JlRXhwb3J0cyA9IFtdLFxuICAgICAgbWlzc2luZ0V4cG9ydHMsXG4gICAgICB1bnVzZWRFeHBvcnRzLFxuICAgIH0gPSBjb250ZXh0Lm9wdGlvbnNbMF0gfHwge31cblxuICAgIGlmICh1bnVzZWRFeHBvcnRzKSB7XG4gICAgICBkb1ByZXBhcmF0aW9uKHNyYywgaWdub3JlRXhwb3J0cywgY29udGV4dClcbiAgICB9XG5cbiAgICBjb25zdCBmaWxlID0gY29udGV4dC5nZXRGaWxlbmFtZSgpXG5cbiAgICBjb25zdCBjaGVja0V4cG9ydFByZXNlbmNlID0gbm9kZSA9PiB7XG4gICAgICBpZiAoIW1pc3NpbmdFeHBvcnRzKSB7XG4gICAgICAgIHJldHVyblxuICAgICAgfVxuXG4gICAgICBpZiAoaWdub3JlZEZpbGVzLmhhcyhmaWxlKSkge1xuICAgICAgICByZXR1cm5cbiAgICAgIH1cblxuICAgICAgY29uc3QgZXhwb3J0Q291bnQgPSBleHBvcnRMaXN0LmdldChmaWxlKVxuICAgICAgY29uc3QgZXhwb3J0QWxsID0gZXhwb3J0Q291bnQuZ2V0KEVYUE9SVF9BTExfREVDTEFSQVRJT04pXG4gICAgICBjb25zdCBuYW1lc3BhY2VJbXBvcnRzID0gZXhwb3J0Q291bnQuZ2V0KElNUE9SVF9OQU1FU1BBQ0VfU1BFQ0lGSUVSKVxuXG4gICAgICBleHBvcnRDb3VudC5kZWxldGUoRVhQT1JUX0FMTF9ERUNMQVJBVElPTilcbiAgICAgIGV4cG9ydENvdW50LmRlbGV0ZShJTVBPUlRfTkFNRVNQQUNFX1NQRUNJRklFUilcbiAgICAgIGlmIChleHBvcnRDb3VudC5zaXplIDwgMSkge1xuICAgICAgICAvLyBub2RlLmJvZHlbMF0gPT09ICd1bmRlZmluZWQnIG9ubHkgaGFwcGVucywgaWYgZXZlcnl0aGluZyBpcyBjb21tZW50ZWQgb3V0IGluIHRoZSBmaWxlXG4gICAgICAgIC8vIGJlaW5nIGxpbnRlZFxuICAgICAgICBjb250ZXh0LnJlcG9ydChub2RlLmJvZHlbMF0gPyBub2RlLmJvZHlbMF0gOiBub2RlLCAnTm8gZXhwb3J0cyBmb3VuZCcpXG4gICAgICB9XG4gICAgICBleHBvcnRDb3VudC5zZXQoRVhQT1JUX0FMTF9ERUNMQVJBVElPTiwgZXhwb3J0QWxsKVxuICAgICAgZXhwb3J0Q291bnQuc2V0KElNUE9SVF9OQU1FU1BBQ0VfU1BFQ0lGSUVSLCBuYW1lc3BhY2VJbXBvcnRzKVxuICAgIH1cblxuICAgIGNvbnN0IGNoZWNrVXNhZ2UgPSAobm9kZSwgZXhwb3J0ZWRWYWx1ZSkgPT4ge1xuICAgICAgaWYgKCF1bnVzZWRFeHBvcnRzKSB7XG4gICAgICAgIHJldHVyblxuICAgICAgfVxuXG4gICAgICBpZiAoaWdub3JlZEZpbGVzLmhhcyhmaWxlKSkge1xuICAgICAgICByZXR1cm5cbiAgICAgIH1cblxuICAgICAgaWYgKGZpbGVJc0luUGtnKGZpbGUpKSB7XG4gICAgICAgIHJldHVyblxuICAgICAgfVxuXG4gICAgICBpZiAoZmlsZXNPdXRzaWRlU3JjLmhhcyhmaWxlKSkge1xuICAgICAgICByZXR1cm5cbiAgICAgIH1cblxuICAgICAgLy8gbWFrZSBzdXJlIGZpbGUgdG8gYmUgbGludGVkIGlzIGluY2x1ZGVkIGluIHNvdXJjZSBmaWxlc1xuICAgICAgaWYgKCFzcmNGaWxlcy5oYXMoZmlsZSkpIHtcbiAgICAgICAgc3JjRmlsZXMgPSByZXNvbHZlRmlsZXMoZ2V0U3JjKHNyYyksIGlnbm9yZUV4cG9ydHMsIGNvbnRleHQpXG4gICAgICAgIGlmICghc3JjRmlsZXMuaGFzKGZpbGUpKSB7XG4gICAgICAgICAgZmlsZXNPdXRzaWRlU3JjLmFkZChmaWxlKVxuICAgICAgICAgIHJldHVyblxuICAgICAgICB9XG4gICAgICB9XG5cbiAgICAgIGV4cG9ydHMgPSBleHBvcnRMaXN0LmdldChmaWxlKVxuXG4gICAgICAvLyBzcGVjaWFsIGNhc2U6IGV4cG9ydCAqIGZyb21cbiAgICAgIGNvbnN0IGV4cG9ydEFsbCA9IGV4cG9ydHMuZ2V0KEVYUE9SVF9BTExfREVDTEFSQVRJT04pXG4gICAgICBpZiAodHlwZW9mIGV4cG9ydEFsbCAhPT0gJ3VuZGVmaW5lZCcgJiYgZXhwb3J0ZWRWYWx1ZSAhPT0gSU1QT1JUX0RFRkFVTFRfU1BFQ0lGSUVSKSB7XG4gICAgICAgIGlmIChleHBvcnRBbGwud2hlcmVVc2VkLnNpemUgPiAwKSB7XG4gICAgICAgICAgcmV0dXJuXG4gICAgICAgIH1cbiAgICAgIH1cblxuICAgICAgLy8gc3BlY2lhbCBjYXNlOiBuYW1lc3BhY2UgaW1wb3J0XG4gICAgICBjb25zdCBuYW1lc3BhY2VJbXBvcnRzID0gZXhwb3J0cy5nZXQoSU1QT1JUX05BTUVTUEFDRV9TUEVDSUZJRVIpXG4gICAgICBpZiAodHlwZW9mIG5hbWVzcGFjZUltcG9ydHMgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgICAgIGlmIChuYW1lc3BhY2VJbXBvcnRzLndoZXJlVXNlZC5zaXplID4gMCkge1xuICAgICAgICAgIHJldHVyblxuICAgICAgICB9XG4gICAgICB9XG5cbiAgICAgIGNvbnN0IGV4cG9ydFN0YXRlbWVudCA9IGV4cG9ydHMuZ2V0KGV4cG9ydGVkVmFsdWUpXG5cbiAgICAgIGNvbnN0IHZhbHVlID0gZXhwb3J0ZWRWYWx1ZSA9PT0gSU1QT1JUX0RFRkFVTFRfU1BFQ0lGSUVSID8gREVGQVVMVCA6IGV4cG9ydGVkVmFsdWVcblxuICAgICAgaWYgKHR5cGVvZiBleHBvcnRTdGF0ZW1lbnQgIT09ICd1bmRlZmluZWQnKXtcbiAgICAgICAgaWYgKGV4cG9ydFN0YXRlbWVudC53aGVyZVVzZWQuc2l6ZSA8IDEpIHtcbiAgICAgICAgICBjb250ZXh0LnJlcG9ydChcbiAgICAgICAgICAgIG5vZGUsXG4gICAgICAgICAgICBgZXhwb3J0ZWQgZGVjbGFyYXRpb24gJyR7dmFsdWV9JyBub3QgdXNlZCB3aXRoaW4gb3RoZXIgbW9kdWxlc2BcbiAgICAgICAgICApXG4gICAgICAgIH1cbiAgICAgIH0gZWxzZSB7XG4gICAgICAgIGNvbnRleHQucmVwb3J0KFxuICAgICAgICAgIG5vZGUsXG4gICAgICAgICAgYGV4cG9ydGVkIGRlY2xhcmF0aW9uICcke3ZhbHVlfScgbm90IHVzZWQgd2l0aGluIG90aGVyIG1vZHVsZXNgXG4gICAgICAgIClcbiAgICAgIH1cbiAgICB9XG5cbiAgICAvKipcbiAgICAgKiBvbmx5IHVzZWZ1bCBmb3IgdG9vbHMgbGlrZSB2c2NvZGUtZXNsaW50XG4gICAgICpcbiAgICAgKiB1cGRhdGUgbGlzdHMgb2YgZXhpc3RpbmcgZXhwb3J0cyBkdXJpbmcgcnVudGltZVxuICAgICAqL1xuICAgIGNvbnN0IHVwZGF0ZUV4cG9ydFVzYWdlID0gbm9kZSA9PiB7XG4gICAgICBpZiAoaWdub3JlZEZpbGVzLmhhcyhmaWxlKSkge1xuICAgICAgICByZXR1cm5cbiAgICAgIH1cblxuICAgICAgbGV0IGV4cG9ydHMgPSBleHBvcnRMaXN0LmdldChmaWxlKVxuXG4gICAgICAvLyBuZXcgbW9kdWxlIGhhcyBiZWVuIGNyZWF0ZWQgZHVyaW5nIHJ1bnRpbWVcbiAgICAgIC8vIGluY2x1ZGUgaXQgaW4gZnVydGhlciBwcm9jZXNzaW5nXG4gICAgICBpZiAodHlwZW9mIGV4cG9ydHMgPT09ICd1bmRlZmluZWQnKSB7XG4gICAgICAgIGV4cG9ydHMgPSBuZXcgTWFwKClcbiAgICAgIH1cblxuICAgICAgY29uc3QgbmV3RXhwb3J0cyA9IG5ldyBNYXAoKVxuICAgICAgY29uc3QgbmV3RXhwb3J0SWRlbnRpZmllcnMgPSBuZXcgU2V0KClcblxuICAgICAgbm9kZS5ib2R5LmZvckVhY2goKHsgdHlwZSwgZGVjbGFyYXRpb24sIHNwZWNpZmllcnMgfSkgPT4ge1xuICAgICAgICBpZiAodHlwZSA9PT0gRVhQT1JUX0RFRkFVTFRfREVDTEFSQVRJT04pIHtcbiAgICAgICAgICBuZXdFeHBvcnRJZGVudGlmaWVycy5hZGQoSU1QT1JUX0RFRkFVTFRfU1BFQ0lGSUVSKVxuICAgICAgICB9XG4gICAgICAgIGlmICh0eXBlID09PSBFWFBPUlRfTkFNRURfREVDTEFSQVRJT04pIHtcbiAgICAgICAgICBpZiAoc3BlY2lmaWVycy5sZW5ndGggPiAwKSB7XG4gICAgICAgICAgICBzcGVjaWZpZXJzLmZvckVhY2goc3BlY2lmaWVyID0+IHtcbiAgICAgICAgICAgICAgaWYgKHNwZWNpZmllci5leHBvcnRlZCkge1xuICAgICAgICAgICAgICAgIG5ld0V4cG9ydElkZW50aWZpZXJzLmFkZChzcGVjaWZpZXIuZXhwb3J0ZWQubmFtZSlcbiAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfSlcbiAgICAgICAgICB9XG4gICAgICAgICAgaWYgKGRlY2xhcmF0aW9uKSB7XG4gICAgICAgICAgICBpZiAoXG4gICAgICAgICAgICAgIGRlY2xhcmF0aW9uLnR5cGUgPT09IEZVTkNUSU9OX0RFQ0xBUkFUSU9OIHx8XG4gICAgICAgICAgICAgIGRlY2xhcmF0aW9uLnR5cGUgPT09IENMQVNTX0RFQ0xBUkFUSU9OIHx8XG4gICAgICAgICAgICAgIGRlY2xhcmF0aW9uLnR5cGUgPT09IFRZUEVfQUxJQVNcbiAgICAgICAgICAgICkge1xuICAgICAgICAgICAgICBuZXdFeHBvcnRJZGVudGlmaWVycy5hZGQoZGVjbGFyYXRpb24uaWQubmFtZSlcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmIChkZWNsYXJhdGlvbi50eXBlID09PSBWQVJJQUJMRV9ERUNMQVJBVElPTikge1xuICAgICAgICAgICAgICBkZWNsYXJhdGlvbi5kZWNsYXJhdGlvbnMuZm9yRWFjaCgoeyBpZCB9KSA9PiB7XG4gICAgICAgICAgICAgICAgbmV3RXhwb3J0SWRlbnRpZmllcnMuYWRkKGlkLm5hbWUpXG4gICAgICAgICAgICAgIH0pXG4gICAgICAgICAgICB9XG4gICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICB9KVxuXG4gICAgICAvLyBvbGQgZXhwb3J0cyBleGlzdCB3aXRoaW4gbGlzdCBvZiBuZXcgZXhwb3J0cyBpZGVudGlmaWVyczogYWRkIHRvIG1hcCBvZiBuZXcgZXhwb3J0c1xuICAgICAgZXhwb3J0cy5mb3JFYWNoKCh2YWx1ZSwga2V5KSA9PiB7XG4gICAgICAgIGlmIChuZXdFeHBvcnRJZGVudGlmaWVycy5oYXMoa2V5KSkge1xuICAgICAgICAgIG5ld0V4cG9ydHMuc2V0KGtleSwgdmFsdWUpXG4gICAgICAgIH1cbiAgICAgIH0pXG5cbiAgICAgIC8vIG5ldyBleHBvcnQgaWRlbnRpZmllcnMgYWRkZWQ6IGFkZCB0byBtYXAgb2YgbmV3IGV4cG9ydHNcbiAgICAgIG5ld0V4cG9ydElkZW50aWZpZXJzLmZvckVhY2goa2V5ID0+IHtcbiAgICAgICAgaWYgKCFleHBvcnRzLmhhcyhrZXkpKSB7XG4gICAgICAgICAgbmV3RXhwb3J0cy5zZXQoa2V5LCB7IHdoZXJlVXNlZDogbmV3IFNldCgpIH0pXG4gICAgICAgIH1cbiAgICAgIH0pXG5cbiAgICAgIC8vIHByZXNlcnZlIGluZm9ybWF0aW9uIGFib3V0IG5hbWVzcGFjZSBpbXBvcnRzXG4gICAgICBsZXQgZXhwb3J0QWxsID0gZXhwb3J0cy5nZXQoRVhQT1JUX0FMTF9ERUNMQVJBVElPTilcbiAgICAgIGxldCBuYW1lc3BhY2VJbXBvcnRzID0gZXhwb3J0cy5nZXQoSU1QT1JUX05BTUVTUEFDRV9TUEVDSUZJRVIpXG5cbiAgICAgIGlmICh0eXBlb2YgbmFtZXNwYWNlSW1wb3J0cyA9PT0gJ3VuZGVmaW5lZCcpIHtcbiAgICAgICAgbmFtZXNwYWNlSW1wb3J0cyA9IHsgd2hlcmVVc2VkOiBuZXcgU2V0KCkgfVxuICAgICAgfVxuXG4gICAgICBuZXdFeHBvcnRzLnNldChFWFBPUlRfQUxMX0RFQ0xBUkFUSU9OLCBleHBvcnRBbGwpXG4gICAgICBuZXdFeHBvcnRzLnNldChJTVBPUlRfTkFNRVNQQUNFX1NQRUNJRklFUiwgbmFtZXNwYWNlSW1wb3J0cylcbiAgICAgIGV4cG9ydExpc3Quc2V0KGZpbGUsIG5ld0V4cG9ydHMpXG4gICAgfVxuXG4gICAgLyoqXG4gICAgICogb25seSB1c2VmdWwgZm9yIHRvb2xzIGxpa2UgdnNjb2RlLWVzbGludFxuICAgICAqXG4gICAgICogdXBkYXRlIGxpc3RzIG9mIGV4aXN0aW5nIGltcG9ydHMgZHVyaW5nIHJ1bnRpbWVcbiAgICAgKi9cbiAgICBjb25zdCB1cGRhdGVJbXBvcnRVc2FnZSA9IG5vZGUgPT4ge1xuICAgICAgaWYgKCF1bnVzZWRFeHBvcnRzKSB7XG4gICAgICAgIHJldHVyblxuICAgICAgfVxuXG4gICAgICBsZXQgb2xkSW1wb3J0UGF0aHMgPSBpbXBvcnRMaXN0LmdldChmaWxlKVxuICAgICAgaWYgKHR5cGVvZiBvbGRJbXBvcnRQYXRocyA9PT0gJ3VuZGVmaW5lZCcpIHtcbiAgICAgICAgb2xkSW1wb3J0UGF0aHMgPSBuZXcgTWFwKClcbiAgICAgIH1cblxuICAgICAgY29uc3Qgb2xkTmFtZXNwYWNlSW1wb3J0cyA9IG5ldyBTZXQoKVxuICAgICAgY29uc3QgbmV3TmFtZXNwYWNlSW1wb3J0cyA9IG5ldyBTZXQoKVxuXG4gICAgICBjb25zdCBvbGRFeHBvcnRBbGwgPSBuZXcgU2V0KClcbiAgICAgIGNvbnN0IG5ld0V4cG9ydEFsbCA9IG5ldyBTZXQoKVxuXG4gICAgICBjb25zdCBvbGREZWZhdWx0SW1wb3J0cyA9IG5ldyBTZXQoKVxuICAgICAgY29uc3QgbmV3RGVmYXVsdEltcG9ydHMgPSBuZXcgU2V0KClcblxuICAgICAgY29uc3Qgb2xkSW1wb3J0cyA9IG5ldyBNYXAoKVxuICAgICAgY29uc3QgbmV3SW1wb3J0cyA9IG5ldyBNYXAoKVxuICAgICAgb2xkSW1wb3J0UGF0aHMuZm9yRWFjaCgodmFsdWUsIGtleSkgPT4ge1xuICAgICAgICBpZiAodmFsdWUuaGFzKEVYUE9SVF9BTExfREVDTEFSQVRJT04pKSB7XG4gICAgICAgICAgb2xkRXhwb3J0QWxsLmFkZChrZXkpXG4gICAgICAgIH1cbiAgICAgICAgaWYgKHZhbHVlLmhhcyhJTVBPUlRfTkFNRVNQQUNFX1NQRUNJRklFUikpIHtcbiAgICAgICAgICBvbGROYW1lc3BhY2VJbXBvcnRzLmFkZChrZXkpXG4gICAgICAgIH1cbiAgICAgICAgaWYgKHZhbHVlLmhhcyhJTVBPUlRfREVGQVVMVF9TUEVDSUZJRVIpKSB7XG4gICAgICAgICAgb2xkRGVmYXVsdEltcG9ydHMuYWRkKGtleSlcbiAgICAgICAgfVxuICAgICAgICB2YWx1ZS5mb3JFYWNoKHZhbCA9PiB7XG4gICAgICAgICAgaWYgKHZhbCAhPT0gSU1QT1JUX05BTUVTUEFDRV9TUEVDSUZJRVIgJiZcbiAgICAgICAgICAgICAgdmFsICE9PSBJTVBPUlRfREVGQVVMVF9TUEVDSUZJRVIpIHtcbiAgICAgICAgICAgICAgIG9sZEltcG9ydHMuc2V0KHZhbCwga2V5KVxuICAgICAgICAgICAgIH1cbiAgICAgICAgfSlcbiAgICAgIH0pXG5cbiAgICAgIG5vZGUuYm9keS5mb3JFYWNoKGFzdE5vZGUgPT4ge1xuICAgICAgICBsZXQgcmVzb2x2ZWRQYXRoXG5cbiAgICAgICAgLy8gc3VwcG9ydCBmb3IgZXhwb3J0IHsgdmFsdWUgfSBmcm9tICdtb2R1bGUnXG4gICAgICAgIGlmIChhc3ROb2RlLnR5cGUgPT09IEVYUE9SVF9OQU1FRF9ERUNMQVJBVElPTikge1xuICAgICAgICAgIGlmIChhc3ROb2RlLnNvdXJjZSkge1xuICAgICAgICAgICAgcmVzb2x2ZWRQYXRoID0gcmVzb2x2ZShhc3ROb2RlLnNvdXJjZS5yYXcucmVwbGFjZSgvKCd8XCIpL2csICcnKSwgY29udGV4dClcbiAgICAgICAgICAgIGFzdE5vZGUuc3BlY2lmaWVycy5mb3JFYWNoKHNwZWNpZmllciA9PiB7XG4gICAgICAgICAgICAgIGxldCBuYW1lXG4gICAgICAgICAgICAgIGlmIChzcGVjaWZpZXIuZXhwb3J0ZWQubmFtZSA9PT0gREVGQVVMVCkge1xuICAgICAgICAgICAgICAgIG5hbWUgPSBJTVBPUlRfREVGQVVMVF9TUEVDSUZJRVJcbiAgICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICBuYW1lID0gc3BlY2lmaWVyLmxvY2FsLm5hbWVcbiAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICBuZXdJbXBvcnRzLnNldChuYW1lLCByZXNvbHZlZFBhdGgpXG4gICAgICAgICAgICB9KVxuICAgICAgICAgIH1cbiAgICAgICAgfVxuXG4gICAgICAgIGlmIChhc3ROb2RlLnR5cGUgPT09IEVYUE9SVF9BTExfREVDTEFSQVRJT04pIHtcbiAgICAgICAgICByZXNvbHZlZFBhdGggPSByZXNvbHZlKGFzdE5vZGUuc291cmNlLnJhdy5yZXBsYWNlKC8oJ3xcIikvZywgJycpLCBjb250ZXh0KVxuICAgICAgICAgIG5ld0V4cG9ydEFsbC5hZGQocmVzb2x2ZWRQYXRoKVxuICAgICAgICB9XG5cbiAgICAgICAgaWYgKGFzdE5vZGUudHlwZSA9PT0gSU1QT1JUX0RFQ0xBUkFUSU9OKSB7XG4gICAgICAgICAgcmVzb2x2ZWRQYXRoID0gcmVzb2x2ZShhc3ROb2RlLnNvdXJjZS5yYXcucmVwbGFjZSgvKCd8XCIpL2csICcnKSwgY29udGV4dClcbiAgICAgICAgICBpZiAoIXJlc29sdmVkUGF0aCkge1xuICAgICAgICAgICAgcmV0dXJuXG4gICAgICAgICAgfVxuXG4gICAgICAgICAgaWYgKGlzTm9kZU1vZHVsZShyZXNvbHZlZFBhdGgpKSB7XG4gICAgICAgICAgICByZXR1cm5cbiAgICAgICAgICB9XG5cbiAgICAgICAgICBpZiAobmV3TmFtZXNwYWNlSW1wb3J0RXhpc3RzKGFzdE5vZGUuc3BlY2lmaWVycykpIHtcbiAgICAgICAgICAgIG5ld05hbWVzcGFjZUltcG9ydHMuYWRkKHJlc29sdmVkUGF0aClcbiAgICAgICAgICB9XG5cbiAgICAgICAgICBpZiAobmV3RGVmYXVsdEltcG9ydEV4aXN0cyhhc3ROb2RlLnNwZWNpZmllcnMpKSB7XG4gICAgICAgICAgICBuZXdEZWZhdWx0SW1wb3J0cy5hZGQocmVzb2x2ZWRQYXRoKVxuICAgICAgICAgIH1cblxuICAgICAgICAgIGFzdE5vZGUuc3BlY2lmaWVycy5mb3JFYWNoKHNwZWNpZmllciA9PiB7XG4gICAgICAgICAgICBpZiAoc3BlY2lmaWVyLnR5cGUgPT09IElNUE9SVF9ERUZBVUxUX1NQRUNJRklFUiB8fFxuICAgICAgICAgICAgICAgIHNwZWNpZmllci50eXBlID09PSBJTVBPUlRfTkFNRVNQQUNFX1NQRUNJRklFUikge1xuICAgICAgICAgICAgICByZXR1cm5cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIG5ld0ltcG9ydHMuc2V0KHNwZWNpZmllci5pbXBvcnRlZC5uYW1lLCByZXNvbHZlZFBhdGgpXG4gICAgICAgICAgfSlcbiAgICAgICAgfVxuICAgICAgfSlcblxuICAgICAgbmV3RXhwb3J0QWxsLmZvckVhY2godmFsdWUgPT4ge1xuICAgICAgICBpZiAoIW9sZEV4cG9ydEFsbC5oYXModmFsdWUpKSB7XG4gICAgICAgICAgbGV0IGltcG9ydHMgPSBvbGRJbXBvcnRQYXRocy5nZXQodmFsdWUpXG4gICAgICAgICAgaWYgKHR5cGVvZiBpbXBvcnRzID09PSAndW5kZWZpbmVkJykge1xuICAgICAgICAgICAgaW1wb3J0cyA9IG5ldyBTZXQoKVxuICAgICAgICAgIH1cbiAgICAgICAgICBpbXBvcnRzLmFkZChFWFBPUlRfQUxMX0RFQ0xBUkFUSU9OKVxuICAgICAgICAgIG9sZEltcG9ydFBhdGhzLnNldCh2YWx1ZSwgaW1wb3J0cylcblxuICAgICAgICAgIGxldCBleHBvcnRzID0gZXhwb3J0TGlzdC5nZXQodmFsdWUpXG4gICAgICAgICAgbGV0IGN1cnJlbnRFeHBvcnRcbiAgICAgICAgICBpZiAodHlwZW9mIGV4cG9ydHMgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgICAgICAgICBjdXJyZW50RXhwb3J0ID0gZXhwb3J0cy5nZXQoRVhQT1JUX0FMTF9ERUNMQVJBVElPTilcbiAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgZXhwb3J0cyA9IG5ldyBNYXAoKVxuICAgICAgICAgICAgZXhwb3J0TGlzdC5zZXQodmFsdWUsIGV4cG9ydHMpXG4gICAgICAgICAgfVxuXG4gICAgICAgICAgaWYgKHR5cGVvZiBjdXJyZW50RXhwb3J0ICE9PSAndW5kZWZpbmVkJykge1xuICAgICAgICAgICAgY3VycmVudEV4cG9ydC53aGVyZVVzZWQuYWRkKGZpbGUpXG4gICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIGNvbnN0IHdoZXJlVXNlZCA9IG5ldyBTZXQoKVxuICAgICAgICAgICAgd2hlcmVVc2VkLmFkZChmaWxlKVxuICAgICAgICAgICAgZXhwb3J0cy5zZXQoRVhQT1JUX0FMTF9ERUNMQVJBVElPTiwgeyB3aGVyZVVzZWQgfSlcbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgIH0pXG5cbiAgICAgIG9sZEV4cG9ydEFsbC5mb3JFYWNoKHZhbHVlID0+IHtcbiAgICAgICAgaWYgKCFuZXdFeHBvcnRBbGwuaGFzKHZhbHVlKSkge1xuICAgICAgICAgIGNvbnN0IGltcG9ydHMgPSBvbGRJbXBvcnRQYXRocy5nZXQodmFsdWUpXG4gICAgICAgICAgaW1wb3J0cy5kZWxldGUoRVhQT1JUX0FMTF9ERUNMQVJBVElPTilcblxuICAgICAgICAgIGNvbnN0IGV4cG9ydHMgPSBleHBvcnRMaXN0LmdldCh2YWx1ZSlcbiAgICAgICAgICBpZiAodHlwZW9mIGV4cG9ydHMgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgICAgICAgICBjb25zdCBjdXJyZW50RXhwb3J0ID0gZXhwb3J0cy5nZXQoRVhQT1JUX0FMTF9ERUNMQVJBVElPTilcbiAgICAgICAgICAgIGlmICh0eXBlb2YgY3VycmVudEV4cG9ydCAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICAgICAgICAgICAgY3VycmVudEV4cG9ydC53aGVyZVVzZWQuZGVsZXRlKGZpbGUpXG4gICAgICAgICAgICB9XG4gICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICB9KVxuXG4gICAgICBuZXdEZWZhdWx0SW1wb3J0cy5mb3JFYWNoKHZhbHVlID0+IHtcbiAgICAgICAgaWYgKCFvbGREZWZhdWx0SW1wb3J0cy5oYXModmFsdWUpKSB7XG4gICAgICAgICAgbGV0IGltcG9ydHMgPSBvbGRJbXBvcnRQYXRocy5nZXQodmFsdWUpXG4gICAgICAgICAgaWYgKHR5cGVvZiBpbXBvcnRzID09PSAndW5kZWZpbmVkJykge1xuICAgICAgICAgICAgaW1wb3J0cyA9IG5ldyBTZXQoKVxuICAgICAgICAgIH1cbiAgICAgICAgICBpbXBvcnRzLmFkZChJTVBPUlRfREVGQVVMVF9TUEVDSUZJRVIpXG4gICAgICAgICAgb2xkSW1wb3J0UGF0aHMuc2V0KHZhbHVlLCBpbXBvcnRzKVxuXG4gICAgICAgICAgbGV0IGV4cG9ydHMgPSBleHBvcnRMaXN0LmdldCh2YWx1ZSlcbiAgICAgICAgICBsZXQgY3VycmVudEV4cG9ydFxuICAgICAgICAgIGlmICh0eXBlb2YgZXhwb3J0cyAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICAgICAgICAgIGN1cnJlbnRFeHBvcnQgPSBleHBvcnRzLmdldChJTVBPUlRfREVGQVVMVF9TUEVDSUZJRVIpXG4gICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIGV4cG9ydHMgPSBuZXcgTWFwKClcbiAgICAgICAgICAgIGV4cG9ydExpc3Quc2V0KHZhbHVlLCBleHBvcnRzKVxuICAgICAgICAgIH1cblxuICAgICAgICAgIGlmICh0eXBlb2YgY3VycmVudEV4cG9ydCAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICAgICAgICAgIGN1cnJlbnRFeHBvcnQud2hlcmVVc2VkLmFkZChmaWxlKVxuICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICBjb25zdCB3aGVyZVVzZWQgPSBuZXcgU2V0KClcbiAgICAgICAgICAgIHdoZXJlVXNlZC5hZGQoZmlsZSlcbiAgICAgICAgICAgIGV4cG9ydHMuc2V0KElNUE9SVF9ERUZBVUxUX1NQRUNJRklFUiwgeyB3aGVyZVVzZWQgfSlcbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgIH0pXG5cbiAgICAgIG9sZERlZmF1bHRJbXBvcnRzLmZvckVhY2godmFsdWUgPT4ge1xuICAgICAgICBpZiAoIW5ld0RlZmF1bHRJbXBvcnRzLmhhcyh2YWx1ZSkpIHtcbiAgICAgICAgICBjb25zdCBpbXBvcnRzID0gb2xkSW1wb3J0UGF0aHMuZ2V0KHZhbHVlKVxuICAgICAgICAgIGltcG9ydHMuZGVsZXRlKElNUE9SVF9ERUZBVUxUX1NQRUNJRklFUilcblxuICAgICAgICAgIGNvbnN0IGV4cG9ydHMgPSBleHBvcnRMaXN0LmdldCh2YWx1ZSlcbiAgICAgICAgICBpZiAodHlwZW9mIGV4cG9ydHMgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgICAgICAgICBjb25zdCBjdXJyZW50RXhwb3J0ID0gZXhwb3J0cy5nZXQoSU1QT1JUX0RFRkFVTFRfU1BFQ0lGSUVSKVxuICAgICAgICAgICAgaWYgKHR5cGVvZiBjdXJyZW50RXhwb3J0ICE9PSAndW5kZWZpbmVkJykge1xuICAgICAgICAgICAgICBjdXJyZW50RXhwb3J0LndoZXJlVXNlZC5kZWxldGUoZmlsZSlcbiAgICAgICAgICAgIH1cbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgIH0pXG5cbiAgICAgIG5ld05hbWVzcGFjZUltcG9ydHMuZm9yRWFjaCh2YWx1ZSA9PiB7XG4gICAgICAgIGlmICghb2xkTmFtZXNwYWNlSW1wb3J0cy5oYXModmFsdWUpKSB7XG4gICAgICAgICAgbGV0IGltcG9ydHMgPSBvbGRJbXBvcnRQYXRocy5nZXQodmFsdWUpXG4gICAgICAgICAgaWYgKHR5cGVvZiBpbXBvcnRzID09PSAndW5kZWZpbmVkJykge1xuICAgICAgICAgICAgaW1wb3J0cyA9IG5ldyBTZXQoKVxuICAgICAgICAgIH1cbiAgICAgICAgICBpbXBvcnRzLmFkZChJTVBPUlRfTkFNRVNQQUNFX1NQRUNJRklFUilcbiAgICAgICAgICBvbGRJbXBvcnRQYXRocy5zZXQodmFsdWUsIGltcG9ydHMpXG5cbiAgICAgICAgICBsZXQgZXhwb3J0cyA9IGV4cG9ydExpc3QuZ2V0KHZhbHVlKVxuICAgICAgICAgIGxldCBjdXJyZW50RXhwb3J0XG4gICAgICAgICAgaWYgKHR5cGVvZiBleHBvcnRzICE9PSAndW5kZWZpbmVkJykge1xuICAgICAgICAgICAgY3VycmVudEV4cG9ydCA9IGV4cG9ydHMuZ2V0KElNUE9SVF9OQU1FU1BBQ0VfU1BFQ0lGSUVSKVxuICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICBleHBvcnRzID0gbmV3IE1hcCgpXG4gICAgICAgICAgICBleHBvcnRMaXN0LnNldCh2YWx1ZSwgZXhwb3J0cylcbiAgICAgICAgICB9XG5cbiAgICAgICAgICBpZiAodHlwZW9mIGN1cnJlbnRFeHBvcnQgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgICAgICAgICBjdXJyZW50RXhwb3J0LndoZXJlVXNlZC5hZGQoZmlsZSlcbiAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgY29uc3Qgd2hlcmVVc2VkID0gbmV3IFNldCgpXG4gICAgICAgICAgICB3aGVyZVVzZWQuYWRkKGZpbGUpXG4gICAgICAgICAgICBleHBvcnRzLnNldChJTVBPUlRfTkFNRVNQQUNFX1NQRUNJRklFUiwgeyB3aGVyZVVzZWQgfSlcbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgIH0pXG5cbiAgICAgIG9sZE5hbWVzcGFjZUltcG9ydHMuZm9yRWFjaCh2YWx1ZSA9PiB7XG4gICAgICAgIGlmICghbmV3TmFtZXNwYWNlSW1wb3J0cy5oYXModmFsdWUpKSB7XG4gICAgICAgICAgY29uc3QgaW1wb3J0cyA9IG9sZEltcG9ydFBhdGhzLmdldCh2YWx1ZSlcbiAgICAgICAgICBpbXBvcnRzLmRlbGV0ZShJTVBPUlRfTkFNRVNQQUNFX1NQRUNJRklFUilcblxuICAgICAgICAgIGNvbnN0IGV4cG9ydHMgPSBleHBvcnRMaXN0LmdldCh2YWx1ZSlcbiAgICAgICAgICBpZiAodHlwZW9mIGV4cG9ydHMgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgICAgICAgICBjb25zdCBjdXJyZW50RXhwb3J0ID0gZXhwb3J0cy5nZXQoSU1QT1JUX05BTUVTUEFDRV9TUEVDSUZJRVIpXG4gICAgICAgICAgICBpZiAodHlwZW9mIGN1cnJlbnRFeHBvcnQgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgICAgICAgICAgIGN1cnJlbnRFeHBvcnQud2hlcmVVc2VkLmRlbGV0ZShmaWxlKVxuICAgICAgICAgICAgfVxuICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgfSlcblxuICAgICAgbmV3SW1wb3J0cy5mb3JFYWNoKCh2YWx1ZSwga2V5KSA9PiB7XG4gICAgICAgIGlmICghb2xkSW1wb3J0cy5oYXMoa2V5KSkge1xuICAgICAgICAgIGxldCBpbXBvcnRzID0gb2xkSW1wb3J0UGF0aHMuZ2V0KHZhbHVlKVxuICAgICAgICAgIGlmICh0eXBlb2YgaW1wb3J0cyA9PT0gJ3VuZGVmaW5lZCcpIHtcbiAgICAgICAgICAgIGltcG9ydHMgPSBuZXcgU2V0KClcbiAgICAgICAgICB9XG4gICAgICAgICAgaW1wb3J0cy5hZGQoa2V5KVxuICAgICAgICAgIG9sZEltcG9ydFBhdGhzLnNldCh2YWx1ZSwgaW1wb3J0cylcblxuICAgICAgICAgIGxldCBleHBvcnRzID0gZXhwb3J0TGlzdC5nZXQodmFsdWUpXG4gICAgICAgICAgbGV0IGN1cnJlbnRFeHBvcnRcbiAgICAgICAgICBpZiAodHlwZW9mIGV4cG9ydHMgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgICAgICAgICBjdXJyZW50RXhwb3J0ID0gZXhwb3J0cy5nZXQoa2V5KVxuICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICBleHBvcnRzID0gbmV3IE1hcCgpXG4gICAgICAgICAgICBleHBvcnRMaXN0LnNldCh2YWx1ZSwgZXhwb3J0cylcbiAgICAgICAgICB9XG5cbiAgICAgICAgICBpZiAodHlwZW9mIGN1cnJlbnRFeHBvcnQgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgICAgICAgICBjdXJyZW50RXhwb3J0LndoZXJlVXNlZC5hZGQoZmlsZSlcbiAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgY29uc3Qgd2hlcmVVc2VkID0gbmV3IFNldCgpXG4gICAgICAgICAgICB3aGVyZVVzZWQuYWRkKGZpbGUpXG4gICAgICAgICAgICBleHBvcnRzLnNldChrZXksIHsgd2hlcmVVc2VkIH0pXG4gICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICB9KVxuXG4gICAgICBvbGRJbXBvcnRzLmZvckVhY2goKHZhbHVlLCBrZXkpID0+IHtcbiAgICAgICAgaWYgKCFuZXdJbXBvcnRzLmhhcyhrZXkpKSB7XG4gICAgICAgICAgY29uc3QgaW1wb3J0cyA9IG9sZEltcG9ydFBhdGhzLmdldCh2YWx1ZSlcbiAgICAgICAgICBpbXBvcnRzLmRlbGV0ZShrZXkpXG5cbiAgICAgICAgICBjb25zdCBleHBvcnRzID0gZXhwb3J0TGlzdC5nZXQodmFsdWUpXG4gICAgICAgICAgaWYgKHR5cGVvZiBleHBvcnRzICE9PSAndW5kZWZpbmVkJykge1xuICAgICAgICAgICAgY29uc3QgY3VycmVudEV4cG9ydCA9IGV4cG9ydHMuZ2V0KGtleSlcbiAgICAgICAgICAgIGlmICh0eXBlb2YgY3VycmVudEV4cG9ydCAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICAgICAgICAgICAgY3VycmVudEV4cG9ydC53aGVyZVVzZWQuZGVsZXRlKGZpbGUpXG4gICAgICAgICAgICB9XG4gICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICB9KVxuICAgIH1cblxuICAgIHJldHVybiB7XG4gICAgICAnUHJvZ3JhbTpleGl0Jzogbm9kZSA9PiB7XG4gICAgICAgIHVwZGF0ZUV4cG9ydFVzYWdlKG5vZGUpXG4gICAgICAgIHVwZGF0ZUltcG9ydFVzYWdlKG5vZGUpXG4gICAgICAgIGNoZWNrRXhwb3J0UHJlc2VuY2Uobm9kZSlcbiAgICAgIH0sXG4gICAgICAnRXhwb3J0RGVmYXVsdERlY2xhcmF0aW9uJzogbm9kZSA9PiB7XG4gICAgICAgIGNoZWNrVXNhZ2Uobm9kZSwgSU1QT1JUX0RFRkFVTFRfU1BFQ0lGSUVSKVxuICAgICAgfSxcbiAgICAgICdFeHBvcnROYW1lZERlY2xhcmF0aW9uJzogbm9kZSA9PiB7XG4gICAgICAgIG5vZGUuc3BlY2lmaWVycy5mb3JFYWNoKHNwZWNpZmllciA9PiB7XG4gICAgICAgICAgICBjaGVja1VzYWdlKG5vZGUsIHNwZWNpZmllci5leHBvcnRlZC5uYW1lKVxuICAgICAgICB9KVxuICAgICAgICBpZiAobm9kZS5kZWNsYXJhdGlvbikge1xuICAgICAgICAgIGlmIChcbiAgICAgICAgICAgIG5vZGUuZGVjbGFyYXRpb24udHlwZSA9PT0gRlVOQ1RJT05fREVDTEFSQVRJT04gfHxcbiAgICAgICAgICAgIG5vZGUuZGVjbGFyYXRpb24udHlwZSA9PT0gQ0xBU1NfREVDTEFSQVRJT04gfHxcbiAgICAgICAgICAgIG5vZGUuZGVjbGFyYXRpb24udHlwZSA9PT0gVFlQRV9BTElBU1xuICAgICAgICAgICkge1xuICAgICAgICAgICAgY2hlY2tVc2FnZShub2RlLCBub2RlLmRlY2xhcmF0aW9uLmlkLm5hbWUpXG4gICAgICAgICAgfVxuICAgICAgICAgIGlmIChub2RlLmRlY2xhcmF0aW9uLnR5cGUgPT09IFZBUklBQkxFX0RFQ0xBUkFUSU9OKSB7XG4gICAgICAgICAgICBub2RlLmRlY2xhcmF0aW9uLmRlY2xhcmF0aW9ucy5mb3JFYWNoKGRlY2xhcmF0aW9uID0+IHtcbiAgICAgICAgICAgICAgY2hlY2tVc2FnZShub2RlLCBkZWNsYXJhdGlvbi5pZC5uYW1lKVxuICAgICAgICAgICAgfSlcbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgIH0sXG4gICAgfVxuICB9LFxufVxuIl19