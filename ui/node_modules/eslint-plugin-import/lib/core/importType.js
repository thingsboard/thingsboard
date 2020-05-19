'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _slicedToArray = function () { function sliceIterator(arr, i) { var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i["return"]) _i["return"](); } finally { if (_d) throw _e; } } return _arr; } return function (arr, i) { if (Array.isArray(arr)) { return arr; } else if (Symbol.iterator in Object(arr)) { return sliceIterator(arr, i); } else { throw new TypeError("Invalid attempt to destructure non-iterable instance"); } }; }();

exports.isAbsolute = isAbsolute;
exports.isBuiltIn = isBuiltIn;
exports.isExternalModule = isExternalModule;
exports.isExternalModuleMain = isExternalModuleMain;
exports.isScoped = isScoped;
exports.isScopedMain = isScopedMain;
exports.isScopedModule = isScopedModule;
exports.default = resolveImportType;

var _core = require('resolve/lib/core');

var _core2 = _interopRequireDefault(_core);

var _resolve = require('eslint-module-utils/resolve');

var _resolve2 = _interopRequireDefault(_resolve);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function baseModule(name) {
  if (isScoped(name)) {
    var _name$split = name.split('/'),
        _name$split2 = _slicedToArray(_name$split, 2);

    const scope = _name$split2[0],
          pkg = _name$split2[1];

    return `${scope}/${pkg}`;
  }

  var _name$split3 = name.split('/'),
      _name$split4 = _slicedToArray(_name$split3, 1);

  const pkg = _name$split4[0];

  return pkg;
}

function isAbsolute(name) {
  return name.indexOf('/') === 0;
}

// path is defined only when a resolver resolves to a non-standard path
function isBuiltIn(name, settings, path) {
  if (path || !name) return false;
  const base = baseModule(name);
  const extras = settings && settings['import/core-modules'] || [];
  return _core2.default[base] || extras.indexOf(base) > -1;
}

function isExternalPath(path, name, settings) {
  const folders = settings && settings['import/external-module-folders'] || ['node_modules'];
  return !path || folders.some(folder => isSubpath(folder, path));
}

function isSubpath(subpath, path) {
  const normSubpath = subpath.replace(/[/]$/, '');
  if (normSubpath.length === 0) {
    return false;
  }
  const left = path.indexOf(normSubpath);
  const right = left + normSubpath.length;
  return left !== -1 && (left === 0 || normSubpath[0] !== '/' && path[left - 1] === '/') && (right >= path.length || path[right] === '/');
}

const externalModuleRegExp = /^\w/;
function isExternalModule(name, settings, path) {
  return externalModuleRegExp.test(name) && isExternalPath(path, name, settings);
}

const externalModuleMainRegExp = /^[\w]((?!\/).)*$/;
function isExternalModuleMain(name, settings, path) {
  return externalModuleMainRegExp.test(name) && isExternalPath(path, name, settings);
}

const scopedRegExp = /^@[^/]*\/?[^/]+/;
function isScoped(name) {
  return name && scopedRegExp.test(name);
}

const scopedMainRegExp = /^@[^/]+\/?[^/]+$/;
function isScopedMain(name) {
  return name && scopedMainRegExp.test(name);
}

function isInternalModule(name, settings, path) {
  const internalScope = settings && settings['import/internal-regex'];
  const matchesScopedOrExternalRegExp = scopedRegExp.test(name) || externalModuleRegExp.test(name);
  return matchesScopedOrExternalRegExp && (internalScope && new RegExp(internalScope).test(name) || !isExternalPath(path, name, settings));
}

function isRelativeToParent(name) {
  return (/^\.\.[\\/]/.test(name)
  );
}

const indexFiles = ['.', './', './index', './index.js'];
function isIndex(name) {
  return indexFiles.indexOf(name) !== -1;
}

function isRelativeToSibling(name) {
  return (/^\.[\\/]/.test(name)
  );
}

function typeTest(name, settings, path) {
  if (isAbsolute(name, settings, path)) {
    return 'absolute';
  }
  if (isBuiltIn(name, settings, path)) {
    return 'builtin';
  }
  if (isInternalModule(name, settings, path)) {
    return 'internal';
  }
  if (isExternalModule(name, settings, path)) {
    return 'external';
  }
  if (isScoped(name, settings, path)) {
    return 'external';
  }
  if (isRelativeToParent(name, settings, path)) {
    return 'parent';
  }
  if (isIndex(name, settings, path)) {
    return 'index';
  }
  if (isRelativeToSibling(name, settings, path)) {
    return 'sibling';
  }
  return 'unknown';
}

function isScopedModule(name) {
  return name.indexOf('@') === 0;
}

function resolveImportType(name, context) {
  return typeTest(name, context.settings, (0, _resolve2.default)(name, context));
}
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9jb3JlL2ltcG9ydFR5cGUuanMiXSwibmFtZXMiOlsiaXNBYnNvbHV0ZSIsImlzQnVpbHRJbiIsImlzRXh0ZXJuYWxNb2R1bGUiLCJpc0V4dGVybmFsTW9kdWxlTWFpbiIsImlzU2NvcGVkIiwiaXNTY29wZWRNYWluIiwiaXNTY29wZWRNb2R1bGUiLCJyZXNvbHZlSW1wb3J0VHlwZSIsImJhc2VNb2R1bGUiLCJuYW1lIiwic3BsaXQiLCJzY29wZSIsInBrZyIsImluZGV4T2YiLCJzZXR0aW5ncyIsInBhdGgiLCJiYXNlIiwiZXh0cmFzIiwiY29yZU1vZHVsZXMiLCJpc0V4dGVybmFsUGF0aCIsImZvbGRlcnMiLCJzb21lIiwiZm9sZGVyIiwiaXNTdWJwYXRoIiwic3VicGF0aCIsIm5vcm1TdWJwYXRoIiwicmVwbGFjZSIsImxlbmd0aCIsImxlZnQiLCJyaWdodCIsImV4dGVybmFsTW9kdWxlUmVnRXhwIiwidGVzdCIsImV4dGVybmFsTW9kdWxlTWFpblJlZ0V4cCIsInNjb3BlZFJlZ0V4cCIsInNjb3BlZE1haW5SZWdFeHAiLCJpc0ludGVybmFsTW9kdWxlIiwiaW50ZXJuYWxTY29wZSIsIm1hdGNoZXNTY29wZWRPckV4dGVybmFsUmVnRXhwIiwiUmVnRXhwIiwiaXNSZWxhdGl2ZVRvUGFyZW50IiwiaW5kZXhGaWxlcyIsImlzSW5kZXgiLCJpc1JlbGF0aXZlVG9TaWJsaW5nIiwidHlwZVRlc3QiLCJjb250ZXh0Il0sIm1hcHBpbmdzIjoiOzs7Ozs7OztRQWFnQkEsVSxHQUFBQSxVO1FBS0FDLFMsR0FBQUEsUztRQXlCQUMsZ0IsR0FBQUEsZ0I7UUFLQUMsb0IsR0FBQUEsb0I7UUFLQUMsUSxHQUFBQSxRO1FBS0FDLFksR0FBQUEsWTtRQW1DQUMsYyxHQUFBQSxjO2tCQUlRQyxpQjs7QUFqR3hCOzs7O0FBRUE7Ozs7OztBQUVBLFNBQVNDLFVBQVQsQ0FBb0JDLElBQXBCLEVBQTBCO0FBQ3hCLE1BQUlMLFNBQVNLLElBQVQsQ0FBSixFQUFv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QWtCO0FBQ3pELE1BQUlvQixpQkFBaUIxQixJQUFqQixFQUF1QkssUUFBdkIsRUFBaUNDLElBQWpDLENBQUosRUFBNEM7QUFBRSxXQUFPLFVBQVA7QUFBbUI7QUFDakUsTUFBSWIsaUJBQWlCTyxJQUFqQixFQUF1QkssUUFBdkIsRUFBaUNDLElBQWpDLENBQUosRUFBNEM7QUFBRSxXQUFPLFVBQVA7QUFBbUI7QUFDakUsTUFBSVgsU0FBU0ssSUFBVCxFQUFlSyxRQUFmLEVBQXlCQyxJQUF6QixDQUFKLEVBQW9DO0FBQUUsV0FBTyxVQUFQO0FBQW1CO0FBQ3pELE1BQUl3QixtQkFBbUI5QixJQUFuQixFQUF5QkssUUFBekIsRUFBbUNDLElBQW5DLENBQUosRUFBOEM7QUFBRSxXQUFPLFFBQVA7QUFBaUI7QUFDakUsTUFBSTBCLFFBQVFoQyxJQUFSLEVBQWNLLFFBQWQsRUFBd0JDLElBQXhCLENBQUosRUFBbUM7QUFBRSxXQUFPLE9BQVA7QUFBZ0I7QUFDckQsTUFBSTJCLG9CQUFvQmpDLElBQXBCLEVBQTBCSyxRQUExQixFQUFvQ0MsSUFBcEMsQ0FBSixFQUErQztBQUFFLFdBQU8sU0FBUDtBQUFrQjtBQUNuRSxTQUFPLFNBQVA7QUFDRDs7QUFFTSxTQUFTVCxjQUFULENBQXdCRyxJQUF4QixFQUE4QjtBQUNuQyxTQUFPQSxLQUFLSSxPQUFMLENBQWEsR0FBYixNQUFzQixDQUE3QjtBQUNEOztBQUVjLFNBQVNOLGlCQUFULENBQTJCRSxJQUEzQixFQUFpQ21DLE9BQWpDLEVBQTBDO0FBQ3ZELFNBQU9ELFNBQVNsQyxJQUFULEVBQWVtQyxRQUFROUIsUUFBdkIsRUFBaUMsdUJBQVFMLElBQVIsRUFBY21DLE9BQWQsQ0FBakMsQ0FBUDtBQUNEIiwiZmlsZSI6ImltcG9ydFR5cGUuanMiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgY29yZU1vZHVsZXMgZnJvbSAncmVzb2x2ZS9saWIvY29yZSdcblxuaW1wb3J0IHJlc29sdmUgZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9yZXNvbHZlJ1xuXG5mdW5jdGlvbiBiYXNlTW9kdWxlKG5hbWUpIHtcbiAgaWYgKGlzU2NvcGVkKG5hbWUpKSB7XG4gICAgY29uc3QgW3Njb3BlLCBwa2ddID0gbmFtZS5zcGxpdCgnLycpXG4gICAgcmV0dXJuIGAke3Njb3BlfS8ke3BrZ31gXG4gIH1cbiAgY29uc3QgW3BrZ10gPSBuYW1lLnNwbGl0KCcvJylcbiAgcmV0dXJuIHBrZ1xufVxuXG5leHBvcnQgZnVuY3Rpb24gaXNBYnNvbHV0ZShuYW1lKSB7XG4gIHJldHVybiBuYW1lLmluZGV4T2YoJy8nKSA9PT0gMFxufVxuXG4vLyBwYXRoIGlzIGRlZmluZWQgb25seSB3aGVuIGEgcmVzb2x2ZXIgcmVzb2x2ZXMgdG8gYSBub24tc3RhbmRhcmQgcGF0aFxuZXhwb3J0IGZ1bmN0aW9uIGlzQnVpbHRJbihuYW1lLCBzZXR0aW5ncywgcGF0aCkge1xuICBpZiAocGF0aCB8fCAhbmFtZSkgcmV0dXJuIGZhbHNlXG4gIGNvbnN0IGJhc2UgPSBiYXNlTW9kdWxlKG5hbWUpXG4gIGNvbnN0IGV4dHJhcyA9IChzZXR0aW5ncyAmJiBzZXR0aW5nc1snaW1wb3J0L2NvcmUtbW9kdWxlcyddKSB8fCBbXVxuICByZXR1cm4gY29yZU1vZHVsZXNbYmFzZV0gfHwgZXh0cmFzLmluZGV4T2YoYmFzZSkgPiAtMVxufVxuXG5mdW5jdGlvbiBpc0V4dGVybmFsUGF0aChwYXRoLCBuYW1lLCBzZXR0aW5ncykge1xuICBjb25zdCBmb2xkZXJzID0gKHNldHRpbmdzICYmIHNldHRpbmdzWydpbXBvcnQvZXh0ZXJuYWwtbW9kdWxlLWZvbGRlcnMnXSkgfHwgWydub2RlX21vZHVsZXMnXVxuICByZXR1cm4gIXBhdGggfHwgZm9sZGVycy5zb21lKGZvbGRlciA9PiBpc1N1YnBhdGgoZm9sZGVyLCBwYXRoKSlcbn1cblxuZnVuY3Rpb24gaXNTdWJwYXRoKHN1YnBhdGgsIHBhdGgpIHtcbiAgY29uc3Qgbm9ybVN1YnBhdGggPSBzdWJwYXRoLnJlcGxhY2UoL1svXSQvLCAnJylcbiAgaWYgKG5vcm1TdWJwYXRoLmxlbmd0aCA9PT0gMCkge1xuICAgIHJldHVybiBmYWxzZVxuICB9XG4gIGNvbnN0IGxlZnQgPSBwYXRoLmluZGV4T2Yobm9ybVN1YnBhdGgpXG4gIGNvbnN0IHJpZ2h0ID0gbGVmdCArIG5vcm1TdWJwYXRoLmxlbmd0aFxuICByZXR1cm4gbGVmdCAhPT0gLTEgJiZcbiAgICAgICAgKGxlZnQgPT09IDAgfHwgbm9ybVN1YnBhdGhbMF0gIT09ICcvJyAmJiBwYXRoW2xlZnQgLSAxXSA9PT0gJy8nKSAmJlxuICAgICAgICAocmlnaHQgPj0gcGF0aC5sZW5ndGggfHwgcGF0aFtyaWdodF0gPT09ICcvJylcbn1cblxuY29uc3QgZXh0ZXJuYWxNb2R1bGVSZWdFeHAgPSAvXlxcdy9cbmV4cG9ydCBmdW5jdGlvbiBpc0V4dGVybmFsTW9kdWxlKG5hbWUsIHNldHRpbmdzLCBwYXRoKSB7XG4gIHJldHVybiBleHRlcm5hbE1vZHVsZVJlZ0V4cC50ZXN0KG5hbWUpICYmIGlzRXh0ZXJuYWxQYXRoKHBhdGgsIG5hbWUsIHNldHRpbmdzKVxufVxuXG5jb25zdCBleHRlcm5hbE1vZHVsZU1haW5SZWdFeHAgPSAvXltcXHddKCg/IVxcLykuKSokL1xuZXhwb3J0IGZ1bmN0aW9uIGlzRXh0ZXJuYWxNb2R1bGVNYWluKG5hbWUsIHNldHRpbmdzLCBwYXRoKSB7XG4gIHJldHVybiBleHRlcm5hbE1vZHVsZU1haW5SZWdFeHAudGVzdChuYW1lKSAmJiBpc0V4dGVybmFsUGF0aChwYXRoLCBuYW1lLCBzZXR0aW5ncylcbn1cblxuY29uc3Qgc2NvcGVkUmVnRXhwID0gL15AW14vXSpcXC8/W14vXSsvXG5leHBvcnQgZnVuY3Rpb24gaXNTY29wZWQobmFtZSkge1xuICByZXR1cm4gbmFtZSAmJiBzY29wZWRSZWdFeHAudGVzdChuYW1lKVxufVxuXG5jb25zdCBzY29wZWRNYWluUmVnRXhwID0gL15AW14vXStcXC8/W14vXSskL1xuZXhwb3J0IGZ1bmN0aW9uIGlzU2NvcGVkTWFpbihuYW1lKSB7XG4gIHJldHVybiBuYW1lICYmIHNjb3BlZE1haW5SZWdFeHAudGVzdChuYW1lKVxufVxuXG5mdW5jdGlvbiBpc0ludGVybmFsTW9kdWxlKG5hbWUsIHNldHRpbmdzLCBwYXRoKSB7XG4gIGNvbnN0IGludGVybmFsU2NvcGUgPSAoc2V0dGluZ3MgJiYgc2V0dGluZ3NbJ2ltcG9ydC9pbnRlcm5hbC1yZWdleCddKVxuICBjb25zdCBtYXRjaGVzU2NvcGVkT3JFeHRlcm5hbFJlZ0V4cCA9IHNjb3BlZFJlZ0V4cC50ZXN0KG5hbWUpIHx8IGV4dGVybmFsTW9kdWxlUmVnRXhwLnRlc3QobmFtZSlcbiAgcmV0dXJuIChtYXRjaGVzU2NvcGVkT3JFeHRlcm5hbFJlZ0V4cCAmJiAoaW50ZXJuYWxTY29wZSAmJiBuZXcgUmVnRXhwKGludGVybmFsU2NvcGUpLnRlc3QobmFtZSkgfHwgIWlzRXh0ZXJuYWxQYXRoKHBhdGgsIG5hbWUsIHNldHRpbmdzKSkpXG59XG5cbmZ1bmN0aW9uIGlzUmVsYXRpdmVUb1BhcmVudChuYW1lKSB7XG4gIHJldHVybiAvXlxcLlxcLltcXFxcL10vLnRlc3QobmFtZSlcbn1cblxuY29uc3QgaW5kZXhGaWxlcyA9IFsnLicsICcuLycsICcuL2luZGV4JywgJy4vaW5kZXguanMnXVxuZnVuY3Rpb24gaXNJbmRleChuYW1lKSB7XG4gIHJldHVybiBpbmRleEZpbGVzLmluZGV4T2YobmFtZSkgIT09IC0xXG59XG5cbmZ1bmN0aW9uIGlzUmVsYXRpdmVUb1NpYmxpbmcobmFtZSkge1xuICByZXR1cm4gL15cXC5bXFxcXC9dLy50ZXN0KG5hbWUpXG59XG5cbmZ1bmN0aW9uIHR5cGVUZXN0KG5hbWUsIHNldHRpbmdzLCBwYXRoKSB7XG4gIGlmIChpc0Fic29sdXRlKG5hbWUsIHNldHRpbmdzLCBwYXRoKSkgeyByZXR1cm4gJ2Fic29sdXRlJyB9XG4gIGlmIChpc0J1aWx0SW4obmFtZSwgc2V0dGluZ3MsIHBhdGgpKSB7IHJldHVybiAnYnVpbHRpbicgfVxuICBpZiAoaXNJbnRlcm5hbE1vZHVsZShuYW1lLCBzZXR0aW5ncywgcGF0aCkpIHsgcmV0dXJuICdpbnRlcm5hbCcgfVxuICBpZiAoaXNFeHRlcm5hbE1vZHVsZShuYW1lLCBzZXR0aW5ncywgcGF0aCkpIHsgcmV0dXJuICdleHRlcm5hbCcgfVxuICBpZiAoaXNTY29wZWQobmFtZSwgc2V0dGluZ3MsIHBhdGgpKSB7IHJldHVybiAnZXh0ZXJuYWwnIH1cbiAgaWYgKGlzUmVsYXRpdmVUb1BhcmVudChuYW1lLCBzZXR0aW5ncywgcGF0aCkpIHsgcmV0dXJuICdwYXJlbnQnIH1cbiAgaWYgKGlzSW5kZXgobmFtZSwgc2V0dGluZ3MsIHBhdGgpKSB7IHJldHVybiAnaW5kZXgnIH1cbiAgaWYgKGlzUmVsYXRpdmVUb1NpYmxpbmcobmFtZSwgc2V0dGluZ3MsIHBhdGgpKSB7IHJldHVybiAnc2libGluZycgfVxuICByZXR1cm4gJ3Vua25vd24nXG59XG5cbmV4cG9ydCBmdW5jdGlvbiBpc1Njb3BlZE1vZHVsZShuYW1lKSB7XG4gIHJldHVybiBuYW1lLmluZGV4T2YoJ0AnKSA9PT0gMFxufVxuXG5leHBvcnQgZGVmYXVsdCBmdW5jdGlvbiByZXNvbHZlSW1wb3J0VHlwZShuYW1lLCBjb250ZXh0KSB7XG4gIHJldHVybiB0eXBlVGVzdChuYW1lLCBjb250ZXh0LnNldHRpbmdzLCByZXNvbHZlKG5hbWUsIGNvbnRleHQpKVxufVxuIl19