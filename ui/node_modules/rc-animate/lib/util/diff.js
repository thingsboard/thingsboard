'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.STATUS_REMOVED = exports.STATUS_REMOVE = exports.STATUS_KEEP = exports.STATUS_ADD = undefined;

var _extends2 = require('babel-runtime/helpers/extends');

var _extends3 = _interopRequireDefault(_extends2);

exports.wrapKeyToObject = wrapKeyToObject;
exports.parseKeys = parseKeys;
exports.diffKeys = diffKeys;

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { 'default': obj }; }

var STATUS_ADD = exports.STATUS_ADD = 'add';
var STATUS_KEEP = exports.STATUS_KEEP = 'keep';
var STATUS_REMOVE = exports.STATUS_REMOVE = 'remove';
var STATUS_REMOVED = exports.STATUS_REMOVED = 'removed';

function wrapKeyToObject(key) {
  var keyObj = void 0;
  if (key && typeof key === 'object' && 'key' in key) {
    keyObj = key;
  } else {
    keyObj = { key: key };
  }
  return (0, _extends3['default'])({}, keyObj, {
    key: String(keyObj.key)
  });
}

function parseKeys() {
  var keys = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : [];

  return keys.map(wrapKeyToObject);
}

function diffKeys() {
  var prevKeys = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : [];
  var currentKeys = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : [];

  var list = [];
  var currentIndex = 0;
  var currentLen = currentKeys.length;

  var prevKeyObjects = parseKeys(prevKeys);
  var currentKeyObjects = parseKeys(currentKeys);

  // Check prev keys to insert or keep
  prevKeyObjects.forEach(function (keyObj) {
    var hit = false;

    for (var i = currentIndex; i < currentLen; i += 1) {
      var currentKeyObj = currentKeyObjects[i];
      if (currentKeyObj.key === keyObj.key) {
        // New added keys should add before current key
        if (currentIndex < i) {
          list = list.concat(currentKeyObjects.slice(currentIndex, i).map(function (obj) {
            return (0, _extends3['default'])({}, obj, { status: STATUS_ADD });
          }));
          currentIndex = i;
        }
        list.push((0, _extends3['default'])({}, currentKeyObj, {
          status: STATUS_KEEP
        }));
        currentIndex += 1;

        hit = true;
        break;
      }
    }

    // If not hit, it means key is removed
    if (!hit) {
      list.push((0, _extends3['default'])({}, keyObj, {
        status: STATUS_REMOVE
      }));
    }
  });

  // Add rest to the list
  if (currentIndex < currentLen) {
    list = list.concat(currentKeyObjects.slice(currentIndex).map(function (obj) {
      return (0, _extends3['default'])({}, obj, { status: STATUS_ADD });
    }));
  }

  /**
   * Merge same key when it remove and add again:
   *    [1 - add, 2 - keep, 1 - remove] -> [1 - keep, 2 - keep]
   */
  var keys = {};
  list.forEach(function (_ref) {
    var key = _ref.key;

    keys[key] = (keys[key] || 0) + 1;
  });
  var duplicatedKeys = Object.keys(keys).filter(function (key) {
    return keys[key] > 1;
  });
  duplicatedKeys.forEach(function (matchKey) {
    // Remove `STATUS_REMOVE` node.
    list = list.filter(function (_ref2) {
      var key = _ref2.key,
          status = _ref2.status;
      return key !== matchKey || status !== STATUS_REMOVE;
    });

    // Update `STATUS_ADD` to `STATUS_KEEP`
    list.forEach(function (node) {
      if (node.key === matchKey) {
        node.status = STATUS_KEEP;
      }
    });
  });

  return list;
}