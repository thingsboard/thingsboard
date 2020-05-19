'use strict';

var _ignore = require('eslint-module-utils/ignore');

var _moduleVisitor = require('eslint-module-utils/moduleVisitor');

var _moduleVisitor2 = _interopRequireDefault(_moduleVisitor);

var _resolve = require('eslint-module-utils/resolve');

var _resolve2 = _interopRequireDefault(_resolve);

var _path = require('path');

var _path2 = _interopRequireDefault(_path);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/**
 * convert a potentially relative path from node utils into a true
 * relative path.
 *
 * ../ -> ..
 * ./ -> .
 * .foo/bar -> ./.foo/bar
 * ..foo/bar -> ./..foo/bar
 * foo/bar -> ./foo/bar
 *
 * @param relativePath {string} relative posix path potentially missing leading './'
 * @returns {string} relative posix path that always starts with a ./
 **/
function toRelativePath(relativePath) {
  const stripped = relativePath.replace(/\/$/g, ''); // Remove trailing /

  return (/^((\.\.)|(\.))($|\/)/.test(stripped) ? stripped : `./${stripped}`
  );
} /**
   * @fileOverview Ensures that there are no useless path segments
   * @author Thomas Grainger
   */

function normalize(fn) {
  return toRelativePath(_path2.default.posix.normalize(fn));
}

function countRelativeParents(pathSegments) {
  return pathSegments.reduce((sum, pathSegment) => pathSegment === '..' ? sum + 1 : sum, 0);
}

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('no-useless-path-segments')
    },

    fixable: 'code',

    schema: [{
      type: 'object',
      properties: {
        commonjs: { type: 'boolean' },
        noUselessIndex: { type: 'boolean' }
      },
      additionalProperties: false
    }]
  },

  create(context) {
    const currentDir = _path2.default.dirname(context.getFilename());
    const options = context.options[0];

    function checkSourceValue(source) {
      const importPath = source.value;


      function reportWithProposedPath(proposedPath) {
        context.report({
          node: source,
          // Note: Using messageIds is not possible due to the support for ESLint 2 and 3
          message: `Useless path segments for "${importPath}", should be "${proposedPath}"`,
          fix: fixer => proposedPath && fixer.replaceText(source, JSON.stringify(proposedPath))
        });
      }

      // Only relative imports are relevant for this rule --> Skip checking
      if (!importPath.startsWith('.')) {
        return;
      }

      // Report rule violation if path is not the shortest possible
      const resolvedPath = (0, _resolve2.default)(importPath, context);
      const normedPath = normalize(importPath);
      const resolvedNormedPath = (0, _resolve2.default)(normedPath, context);
      if (normedPath !== importPath && resolvedPath === resolvedNormedPath) {
        return reportWithProposedPath(normedPath);
      }

      const fileExtensions = (0, _ignore.getFileExtensions)(context.settings);
      const regexUnnecessaryIndex = new RegExp(`.*\\/index(\\${Array.from(fileExtensions).join('|\\')})?$`);

      // Check if path contains unnecessary index (including a configured extension)
      if (options && options.noUselessIndex && regexUnnecessaryIndex.test(importPath)) {
        const parentDirectory = _path2.default.dirname(importPath);

        // Try to find ambiguous imports
        if (parentDirectory !== '.' && parentDirectory !== '..') {
          for (let fileExtension of fileExtensions) {
            if ((0, _resolve2.default)(`${parentDirectory}${fileExtension}`, context)) {
              return reportWithProposedPath(`${parentDirectory}/`);
            }
          }
        }

        return reportWithProposedPath(parentDirectory);
      }

      // Path is shortest possible + starts from the current directory --> Return directly
      if (importPath.startsWith('./')) {
        return;
      }

      // Path is not existing --> Return directly (following code requires path to be defined)
      if (resolvedPath === undefined) {
        return;
      }

      const expected = _path2.default.relative(currentDir, resolvedPath); // Expected import path
      const expectedSplit = expected.split(_path2.default.sep); // Split by / or \ (depending on OS)
      const importPathSplit = importPath.replace(/^\.\//, '').split('/');
      const countImportPathRelativeParents = countRelativeParents(importPathSplit);
      const countExpectedRelativeParents = countRelativeParents(expectedSplit);
      const diff = countImportPathRelativeParents - countExpectedRelativeParents;

      // Same number of relative parents --> Paths are the same --> Return directly
      if (diff <= 0) {
        return;
      }

      // Report and propose minimal number of required relative parents
      return reportWithProposedPath(toRelativePath(importPathSplit.slice(0, countExpectedRelativeParents).concat(importPathSplit.slice(countImportPathRelativeParents + diff)).join('/')));
    }

    return (0, _moduleVisitor2.default)(checkSourceValue, options);
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby11c2VsZXNzLXBhdGgtc2VnbWVudHMuanMiXSwibmFtZXMiOlsidG9SZWxhdGl2ZVBhdGgiLCJyZWxhdGl2ZVBhdGgiLCJzdHJpcHBlZCIsInJlcGxhY2UiLCJ0ZXN0Iiwibm9ybWFsaXplIiwiZm4iLCJwYXRoIiwicG9zaXgiLCJjb3VudFJlbGF0aXZlUGFyZW50cyIsInBhdGhTZWdtZW50cyIsInJlZHVjZSIsInN1bSIsInBhdGhTZWdtZW50IiwibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsImZpeGFibGUiLCJzY2hlbWEiLCJwcm9wZXJ0aWVzIiwiY29tbW9uanMiLCJub1VzZWxlc3NJbmRleCIsImFkZGl0aW9uYWxQcm9wZXJ0aWVzIiwiY3JlYXRlIiwiY29udGV4dCIsImN1cnJlbnREaXIiLCJkaXJuYW1lIiwiZ2V0RmlsZW5hbWUiLCJvcHRpb25zIiwiY2hlY2tTb3VyY2VWYWx1ZSIsInNvdXJjZSIsImltcG9ydFBhdGgiLCJ2YWx1ZSIsInJlcG9ydFdpdGhQcm9wb3NlZFBhdGgiLCJwcm9wb3NlZFBhdGgiLCJyZXBvcnQiLCJub2RlIiwibWVzc2FnZSIsImZpeCIsImZpeGVyIiwicmVwbGFjZVRleHQiLCJKU09OIiwic3RyaW5naWZ5Iiwic3RhcnRzV2l0aCIsInJlc29sdmVkUGF0aCIsIm5vcm1lZFBhdGgiLCJyZXNvbHZlZE5vcm1lZFBhdGgiLCJmaWxlRXh0ZW5zaW9ucyIsInNldHRpbmdzIiwicmVnZXhVbm5lY2Vzc2FyeUluZGV4IiwiUmVnRXhwIiwiQXJyYXkiLCJmcm9tIiwiam9pbiIsInBhcmVudERpcmVjdG9yeSIsImZpbGVFeHRlbnNpb24iLCJ1bmRlZmluZWQiLCJleHBlY3RlZCIsInJlbGF0aXZlIiwiZXhwZWN0ZWRTcGxpdCIsInNwbGl0Iiwic2VwIiwiaW1wb3J0UGF0aFNwbGl0IiwiY291bnRJbXBvcnRQYXRoUmVsYXRpdmVQYXJlbnRzIiwiY291bnRFeHBl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WixFQUVHRyxNQUZILENBRVVMLGdCQUFnQkksS0FBaEIsQ0FBc0JILGlDQUFpQ0UsSUFBdkQsQ0FGVixFQUdHWixJQUhILENBR1EsR0FIUixDQURGLENBREssQ0FBUDtBQVFEOztBQUVELFdBQU8sNkJBQWN4QixnQkFBZCxFQUFnQ0QsT0FBaEMsQ0FBUDtBQUNEO0FBekdjLENBQWpCIiwiZmlsZSI6Im5vLXVzZWxlc3MtcGF0aC1zZWdtZW50cy5qcyIsInNvdXJjZXNDb250ZW50IjpbIi8qKlxuICogQGZpbGVPdmVydmlldyBFbnN1cmVzIHRoYXQgdGhlcmUgYXJlIG5vIHVzZWxlc3MgcGF0aCBzZWdtZW50c1xuICogQGF1dGhvciBUaG9tYXMgR3JhaW5nZXJcbiAqL1xuXG5pbXBvcnQgeyBnZXRGaWxlRXh0ZW5zaW9ucyB9IGZyb20gJ2VzbGludC1tb2R1bGUtdXRpbHMvaWdub3JlJ1xuaW1wb3J0IG1vZHVsZVZpc2l0b3IgZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9tb2R1bGVWaXNpdG9yJ1xuaW1wb3J0IHJlc29sdmUgZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9yZXNvbHZlJ1xuaW1wb3J0IHBhdGggZnJvbSAncGF0aCdcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnXG5cbi8qKlxuICogY29udmVydCBhIHBvdGVudGlhbGx5IHJlbGF0aXZlIHBhdGggZnJvbSBub2RlIHV0aWxzIGludG8gYSB0cnVlXG4gKiByZWxhdGl2ZSBwYXRoLlxuICpcbiAqIC4uLyAtPiAuLlxuICogLi8gLT4gLlxuICogLmZvby9iYXIgLT4gLi8uZm9vL2JhclxuICogLi5mb28vYmFyIC0+IC4vLi5mb28vYmFyXG4gKiBmb28vYmFyIC0+IC4vZm9vL2JhclxuICpcbiAqIEBwYXJhbSByZWxhdGl2ZVBhdGgge3N0cmluZ30gcmVsYXRpdmUgcG9zaXggcGF0aCBwb3RlbnRpYWxseSBtaXNzaW5nIGxlYWRpbmcgJy4vJ1xuICogQHJldHVybnMge3N0cmluZ30gcmVsYXRpdmUgcG9zaXggcGF0aCB0aGF0IGFsd2F5cyBzdGFydHMgd2l0aCBhIC4vXG4gKiovXG5mdW5jdGlvbiB0b1JlbGF0aXZlUGF0aChyZWxhdGl2ZVBhdGgpIHtcbiAgY29uc3Qgc3RyaXBwZWQgPSByZWxhdGl2ZVBhdGgucmVwbGFjZSgvXFwvJC9nLCAnJykgLy8gUmVtb3ZlIHRyYWlsaW5nIC9cblxuICByZXR1cm4gL14oKFxcLlxcLil8KFxcLikpKCR8XFwvKS8udGVzdChzdHJpcHBlZCkgPyBzdHJpcHBlZCA6IGAuLyR7c3RyaXBwZWR9YFxufVxuXG5mdW5jdGlvbiBub3JtYWxpemUoZm4pIHtcbiAgcmV0dXJuIHRvUmVsYXRpdmVQYXRoKHBhdGgucG9zaXgubm9ybWFsaXplKGZuKSlcbn1cblxuZnVuY3Rpb24gY291bnRSZWxhdGl2ZVBhcmVudHMocGF0aFNlZ21lbnRzKSB7XG4gIHJldHVybiBwYXRoU2VnbWVudHMucmVkdWNlKChzdW0sIHBhdGhTZWdtZW50KSA9PiBwYXRoU2VnbWVudCA9PT0gJy4uJyA/IHN1bSArIDEgOiBzdW0sIDApXG59XG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3N1Z2dlc3Rpb24nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnbm8tdXNlbGVzcy1wYXRoLXNlZ21lbnRzJyksXG4gICAgfSxcblxuICAgIGZpeGFibGU6ICdjb2RlJyxcblxuICAgIHNjaGVtYTogW1xuICAgICAge1xuICAgICAgICB0eXBlOiAnb2JqZWN0JyxcbiAgICAgICAgcHJvcGVydGllczoge1xuICAgICAgICAgIGNvbW1vbmpzOiB7IHR5cGU6ICdib29sZWFuJyB9LFxuICAgICAgICAgIG5vVXNlbGVzc0luZGV4OiB7IHR5cGU6ICdib29sZWFuJyB9LFxuICAgICAgICB9LFxuICAgICAgICBhZGRpdGlvbmFsUHJvcGVydGllczogZmFsc2UsXG4gICAgICB9LFxuICAgIF0sXG4gIH0sXG5cbiAgY3JlYXRlKGNvbnRleHQpIHtcbiAgICBjb25zdCBjdXJyZW50RGlyID0gcGF0aC5kaXJuYW1lKGNvbnRleHQuZ2V0RmlsZW5hbWUoKSlcbiAgICBjb25zdCBvcHRpb25zID0gY29udGV4dC5vcHRpb25zWzBdXG5cbiAgICBmdW5jdGlvbiBjaGVja1NvdXJjZVZhbHVlKHNvdXJjZSkge1xuICAgICAgY29uc3QgeyB2YWx1ZTogaW1wb3J0UGF0aCB9ID0gc291cmNlXG5cbiAgICAgIGZ1bmN0aW9uIHJlcG9ydFdpdGhQcm9wb3NlZFBhdGgocHJvcG9zZWRQYXRoKSB7XG4gICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICBub2RlOiBzb3VyY2UsXG4gICAgICAgICAgLy8gTm90ZTogVXNpbmcgbWVzc2FnZUlkcyBpcyBub3QgcG9zc2libGUgZHVlIHRvIHRoZSBzdXBwb3J0IGZvciBFU0xpbnQgMiBhbmQgM1xuICAgICAgICAgIG1lc3NhZ2U6IGBVc2VsZXNzIHBhdGggc2VnbWVudHMgZm9yIFwiJHtpbXBvcnRQYXRofVwiLCBzaG91bGQgYmUgXCIke3Byb3Bvc2VkUGF0aH1cImAsXG4gICAgICAgICAgZml4OiBmaXhlciA9PiBwcm9wb3NlZFBhdGggJiYgZml4ZXIucmVwbGFjZVRleHQoc291cmNlLCBKU09OLnN0cmluZ2lmeShwcm9wb3NlZFBhdGgpKSxcbiAgICAgICAgfSlcbiAgICAgIH1cblxuICAgICAgLy8gT25seSByZWxhdGl2ZSBpbXBvcnRzIGFyZSByZWxldmFudCBmb3IgdGhpcyBydWxlIC0tPiBTa2lwIGNoZWNraW5nXG4gICAgICBpZiAoIWltcG9ydFBhdGguc3RhcnRzV2l0aCgnLicpKSB7XG4gICAgICAgIHJldHVyblxuICAgICAgfVxuXG4gICAgICAvLyBSZXBvcnQgcnVsZSB2aW9sYXRpb24gaWYgcGF0aCBpcyBub3QgdGhlIHNob3J0ZXN0IHBvc3NpYmxlXG4gICAgICBjb25zdCByZXNvbHZlZFBhdGggPSByZXNvbHZlKGltcG9ydFBhdGgsIGNvbnRleHQpXG4gICAgICBjb25zdCBub3JtZWRQYXRoID0gbm9ybWFsaXplKGltcG9ydFBhdGgpXG4gICAgICBjb25zdCByZXNvbHZlZE5vcm1lZFBhdGggPSByZXNvbHZlKG5vcm1lZFBhdGgsIGNvbnRleHQpXG4gICAgICBpZiAobm9ybWVkUGF0aCAhPT0gaW1wb3J0UGF0aCAmJiByZXNvbHZlZFBhdGggPT09IHJlc29sdmVkTm9ybWVkUGF0aCkge1xuICAgICAgICByZXR1cm4gcmVwb3J0V2l0aFByb3Bvc2VkUGF0aChub3JtZWRQYXRoKVxuICAgICAgfVxuXG4gICAgICBjb25zdCBmaWxlRXh0ZW5zaW9ucyA9IGdldEZpbGVFeHRlbnNpb25zKGNvbnRleHQuc2V0dGluZ3MpXG4gICAgICBjb25zdCByZWdleFVubmVjZXNzYXJ5SW5kZXggPSBuZXcgUmVnRXhwKFxuICAgICAgICBgLipcXFxcL2luZGV4KFxcXFwke0FycmF5LmZyb20oZmlsZUV4dGVuc2lvbnMpLmpvaW4oJ3xcXFxcJyl9KT8kYFxuICAgICAgKVxuXG4gICAgICAvLyBDaGVjayBpZiBwYXRoIGNvbnRhaW5zIHVubmVjZXNzYXJ5IGluZGV4IChpbmNsdWRpbmcgYSBjb25maWd1cmVkIGV4dGVuc2lvbilcbiAgICAgIGlmIChvcHRpb25zICYmIG9wdGlvbnMubm9Vc2VsZXNzSW5kZXggJiYgcmVnZXhVbm5lY2Vzc2FyeUluZGV4LnRlc3QoaW1wb3J0UGF0aCkpIHtcbiAgICAgICAgY29uc3QgcGFyZW50RGlyZWN0b3J5ID0gcGF0aC5kaXJuYW1lKGltcG9ydFBhdGgpXG5cbiAgICAgICAgLy8gVHJ5IHRvIGZpbmQgYW1iaWd1b3VzIGltcG9ydHNcbiAgICAgICAgaWYgKHBhcmVudERpcmVjdG9yeSAhPT0gJy4nICYmIHBhcmVudERpcmVjdG9yeSAhPT0gJy4uJykge1xuICAgICAgICAgIGZvciAobGV0IGZpbGVFeHRlbnNpb24gb2YgZmlsZUV4dGVuc2lvbnMpIHtcbiAgICAgICAgICAgIGlmIChyZXNvbHZlKGAke3BhcmVudERpcmVjdG9yeX0ke2ZpbGVFeHRlbnNpb259YCwgY29udGV4dCkpIHtcbiAgICAgICAgICAgICAgcmV0dXJuIHJlcG9ydFdpdGhQcm9wb3NlZFBhdGgoYCR7cGFyZW50RGlyZWN0b3J5fS9gKVxuICAgICAgICAgICAgfVxuICAgICAgICAgIH1cbiAgICAgICAgfVxuXG4gICAgICAgIHJldHVybiByZXBvcnRXaXRoUHJvcG9zZWRQYXRoKHBhcmVudERpcmVjdG9yeSlcbiAgICAgIH1cblxuICAgICAgLy8gUGF0aCBpcyBzaG9ydGVzdCBwb3NzaWJsZSArIHN0YXJ0cyBmcm9tIHRoZSBjdXJyZW50IGRpcmVjdG9yeSAtLT4gUmV0dXJuIGRpcmVjdGx5XG4gICAgICBpZiAoaW1wb3J0UGF0aC5zdGFydHNXaXRoKCcuLycpKSB7XG4gICAgICAgIHJldHVyblxuICAgICAgfVxuXG4gICAgICAvLyBQYXRoIGlzIG5vdCBleGlzdGluZyAtLT4gUmV0dXJuIGRpcmVjdGx5IChmb2xsb3dpbmcgY29kZSByZXF1aXJlcyBwYXRoIHRvIGJlIGRlZmluZWQpXG4gICAgICBpZiAocmVzb2x2ZWRQYXRoID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgcmV0dXJuXG4gICAgICB9XG5cbiAgICAgIGNvbnN0IGV4cGVjdGVkID0gcGF0aC5yZWxhdGl2ZShjdXJyZW50RGlyLCByZXNvbHZlZFBhdGgpIC8vIEV4cGVjdGVkIGltcG9ydCBwYXRoXG4gICAgICBjb25zdCBleHBlY3RlZFNwbGl0ID0gZXhwZWN0ZWQuc3BsaXQocGF0aC5zZXApIC8vIFNwbGl0IGJ5IC8gb3IgXFwgKGRlcGVuZGluZyBvbiBPUylcbiAgICAgIGNvbnN0IGltcG9ydFBhdGhTcGxpdCA9IGltcG9ydFBhdGgucmVwbGFjZSgvXlxcLlxcLy8sICcnKS5zcGxpdCgnLycpXG4gICAgICBjb25zdCBjb3VudEltcG9ydFBhdGhSZWxhdGl2ZVBhcmVudHMgPSBjb3VudFJlbGF0aXZlUGFyZW50cyhpbXBvcnRQYXRoU3BsaXQpXG4gICAgICBjb25zdCBjb3VudEV4cGVjdGVkUmVsYXRpdmVQYXJlbnRzID0gY291bnRSZWxhdGl2ZVBhcmVudHMoZXhwZWN0ZWRTcGxpdClcbiAgICAgIGNvbnN0IGRpZmYgPSBjb3VudEltcG9ydFBhdGhSZWxhdGl2ZVBhcmVudHMgLSBjb3VudEV4cGVjdGVkUmVsYXRpdmVQYXJlbnRzXG5cbiAgICAgIC8vIFNhbWUgbnVtYmVyIG9mIHJlbGF0aXZlIHBhcmVudHMgLS0+IFBhdGhzIGFyZSB0aGUgc2FtZSAtLT4gUmV0dXJuIGRpcmVjdGx5XG4gICAgICBpZiAoZGlmZiA8PSAwKSB7XG4gICAgICAgIHJldHVyblxuICAgICAgfVxuXG4gICAgICAvLyBSZXBvcnQgYW5kIHByb3Bvc2UgbWluaW1hbCBudW1iZXIgb2YgcmVxdWlyZWQgcmVsYXRpdmUgcGFyZW50c1xuICAgICAgcmV0dXJuIHJlcG9ydFdpdGhQcm9wb3NlZFBhdGgoXG4gICAgICAgIHRvUmVsYXRpdmVQYXRoKFxuICAgICAgICAgIGltcG9ydFBhdGhTcGxpdFxuICAgICAgICAgICAgLnNsaWNlKDAsIGNvdW50RXhwZWN0ZWRSZWxhdGl2ZVBhcmVudHMpXG4gICAgICAgICAgICAuY29uY2F0KGltcG9ydFBhdGhTcGxpdC5zbGljZShjb3VudEltcG9ydFBhdGhSZWxhdGl2ZVBhcmVudHMgKyBkaWZmKSlcbiAgICAgICAgICAgIC5qb2luKCcvJylcbiAgICAgICAgKVxuICAgICAgKVxuICAgIH1cblxuICAgIHJldHVybiBtb2R1bGVWaXNpdG9yKGNoZWNrU291cmNlVmFsdWUsIG9wdGlvbnMpXG4gIH0sXG59XG4iXX0=