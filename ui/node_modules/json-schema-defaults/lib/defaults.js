(function(root, factory) {

 'use strict';

  if (typeof require === 'function' && typeof exports === 'object' && typeof module === 'object') {
    // CommonJS
    module.exports = factory();
  } else if (typeof define === 'function' && define.amd) {
    // AMD
    define('json-schema-defaults', [], function() {
      return factory();
    });
  } else {
    // global with noConflict
    var jsonSchemaDefaults = root.jsonSchemaDefaults;
    root.jsonSchemaDefaults = factory();
    root.jsonSchemaDefaults.noConflict = function() {
      var defaults = root.jsonSchemaDefaults;
      root.jsonSchemaDefaults = jsonSchemaDefaults;
      return defaults;
    };
  }

}(this, function() {

  'use strict';

  /**
   * check whether item is plain object
   * @param {*} item
   * @return {Boolean}
   */
  var isObject = function(item) {
    return typeof item === 'object' && item !== null && item.toString() === {}.toString();
  };

  /**
   * deep JSON object clone
   *
   * @param {Object} source
   * @return {Object}
   */
  var cloneJSON = function(source) {
    return JSON.parse(JSON.stringify(source));
  };

  /**
   * returns a result of deep merge of two objects
   *
   * @param {Object} target
   * @param {Object} source
   * @return {Object}
   */
  var merge = function(target, source) {
    target = cloneJSON(target);

    for (var key in source) {
      if (source.hasOwnProperty(key)) {
        if (isObject(target[key]) && isObject(source[key])) {
          target[key] = merge(target[key], source[key]);
        } else {
          target[key] = source[key];
        }
      }
    }
    return target;
  };


  /**
   * get object by reference. works only with local references that points on
   * definitions object
   *
   * @param {String} path
   * @param {Object} definitions
   * @return {Object}
   */
  var getLocalRef = function(path, definitions) {
    path = path.replace(/^#\/definitions\//, '').split('/');

    var find = function(path, root) {
      var key = path.shift();
      if (!root[key]) {
        return {};
      } else if (!path.length) {
        return root[key];
      } else {
        return find(path, root[key]);
      }
    };

    var result = find(path, definitions);

    if (!isObject(result)) {
      return result;
    }
    return cloneJSON(result);
  };


  /**
   * merge list of objects from allOf properties
   * if some of objects contains $ref field extracts this reference and merge it
   *
   * @param {Array} allOfList
   * @param {Object} definitions
   * @return {Object}
   */
  var mergeAllOf = function(allOfList, definitions) {
    var length = allOfList.length,
        index = -1,
        result = {};

    while (++index < length) {
      var item = allOfList[index];

      item = (typeof item.$ref !== 'undefined') ? getLocalRef(item.$ref, definitions) : item;

      result = merge(result, item);
    }

    return result;
  };


  /**
   * returns a object that built with default values from json schema
   *
   * @param {Object} schema
   * @param {Object} definitions
   * @return {Object}
   */
  var defaults = function(schema, definitions) {

    if (typeof schema['default'] !== 'undefined') {

      return schema['default'];

    } else if (typeof schema.allOf !== 'undefined') {

      var mergedItem = mergeAllOf(schema.allOf, definitions);
      return defaults(mergedItem, definitions);

    } else if (typeof schema.$ref !== 'undefined') {

      var reference = getLocalRef(schema.$ref, definitions);
      return defaults(reference, definitions);

    } else if (schema.type === 'object') {

      if (!schema.properties) { return {}; }

      for (var key in schema.properties) {
        if (schema.properties.hasOwnProperty(key)) {
          schema.properties[key] = defaults(schema.properties[key], definitions);

          if (typeof schema.properties[key] === 'undefined') {
            delete schema.properties[key];
          }
        }
      }

      return schema.properties;

    } else if (schema.type === 'array') {

      if (!schema.items) { return []; }

      // minimum item count
      var ct = schema.minItems || 0;
      // tuple-typed arrays
      if (schema.items.constructor === Array) {
        var values = schema.items.map(function (item) {
          return defaults(item, definitions);
        });
        // remove undefined items at the end (unless required by minItems)
        for (var i = values.length - 1; i >= 0; i--) {
          if (typeof values[i] !== 'undefined') {
            break;
          }
          if (i + 1 > ct) {
            values.pop();
          }
        }
        return values;
      }
      // object-typed arrays
      var value = defaults(schema.items, definitions);
      if (typeof value === 'undefined') {
        return [];
      } else {
        var values = [];
        for (var i = 0; i < Math.max(1, ct); i++) {
          values.push(value);
        }
        return values;
      }

    }

  };

  /**
   * main function
   *
   * @param {Object} schema
   * @param {Object|undefined} definitions
   * @return {Object}
   */
  return function (schema, definitions) {

    if (typeof definitions === 'undefined') {
      definitions = schema.definitions || {};
    } else if (isObject(schema.definitions)) {
      definitions = merge(definitions, schema.definitions);
    }

    return defaults(cloneJSON(schema), definitions);
  };

}));
