'use strict';

var _lodash = require('lodash');

var _lodash2 = _interopRequireDefault(_lodash);

var _objectpath = require('objectpath');

var _objectpath2 = _interopRequireDefault(_objectpath);

var _tv = require('tv4');

var _tv2 = _interopRequireDefault(_tv);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function stripNullType(type) {
    if (Array.isArray(type) && type.length == 2) {
        if (type[0] === 'null') return type[1];
        if (type[1] === 'null') return type[0];
    }
    return type;
}

//Creates an default titleMap list from an enum, i.e. a list of strings.
var enumToTitleMap = function enumToTitleMap(enm) {
    var titleMap = []; //canonical titleMap format is a list.
    enm.forEach(function (name) {
        titleMap.push({ name: name, value: name });
    });
    return titleMap;
};

// Takes a titleMap in either object or list format and returns one in
// in the list format.
var canonicalTitleMap = function canonicalTitleMap(titleMap, originalEnum) {
    if (!_lodash2.default.isArray(titleMap)) {
        var canonical = [];
        if (originalEnum) {
            originalEnum.forEach(function (value) {
                canonical.push({ name: titleMap[value], value: value });
            });
        } else {
            for (var k in titleMap) {
                if (titleMap.hasOwnProperty(k)) {
                    canonical.push({ name: k, value: titleMap[k] });
                }
            }
        }
        return canonical;
    }
    return titleMap;
};

//Creates a form object with all common properties
var stdFormObj = function stdFormObj(name, schema, options) {
    options = options || {};
    var f = options.global && options.global.formDefaults ? _lodash2.default.cloneDeep(options.global.formDefaults) : {};
    if (options.global && options.global.supressPropertyTitles === true) {
        f.title = schema.title;
    } else {
        f.title = schema.title || name;
    }

    if (schema.description) {
        f.description = schema.description;
    }
    if (options.required === true || schema.required === true) {
        f.required = true;
    }
    if (schema.maxLength) {
        f.maxlength = schema.maxLength;
    }
    if (schema.minLength) {
        f.minlength = schema.minLength;
    }
    if (schema.readOnly || schema.readonly) {
        f.readonly = true;
    }
    if (schema.minimum) {
        f.minimum = schema.minimum + (schema.exclusiveMinimum ? 1 : 0);
    }
    if (schema.maximum) {
        f.maximum = schema.maximum - (schema.exclusiveMaximum ? 1 : 0);
    }

    // Non standard attributes (DONT USE DEPRECATED)
    // If you must set stuff like this in the schema use the x-schema-form attribute
    if (schema.validationMessage) {
        f.validationMessage = schema.validationMessage;
    }
    if (schema.enumNames) {
        f.titleMap = canonicalTitleMap(schema.enumNames, schema['enum']);
    }
    f.schema = schema;

    // Ng model options doesn't play nice with undefined, might be defined
    // globally though
    f.ngModelOptions = f.ngModelOptions || {};

    return f;
};

var text = function text(name, schema, options) {
    if (stripNullType(schema.type) === 'string' && !schema['enum']) {
        var f = stdFormObj(name, schema, options);
        f.key = options.path;
        f.type = 'text';
        options.lookup[_objectpath2.default.stringify(options.path)] = f;
        return f;
    }
};

//default in json form for number and integer is a text field
//input type="number" would be more suitable don't ya think?
var number = function number(name, schema, options) {
    if (stripNullType(schema.type) === 'number') {
        var f = stdFormObj(name, schema, options);
        f.key = options.path;
        f.type = 'number';
        options.lookup[_objectpath2.default.stringify(options.path)] = f;
        return f;
    }
};

var integer = function integer(name, schema, options) {
    if (stripNullType(schema.type) === 'integer') {
        var f = stdFormObj(name, schema, options);
        f.key = options.path;
        f.type = 'number';
        options.lookup[_objectpath2.default.stringify(options.path)] = f;
        return f;
    }
};

var date = function date(name, schema, options) {
    if (stripNullType(schema.type) === 'date') {
        var f = stdFormObj(name, schema, options);
        f.key = options.path;
        f.type = 'date';
        options.lookup[_objectpath2.default.stringify(options.path)] = f;
        return f;
    }
};

var checkbox = function checkbox(name, schema, options) {
    if (stripNullType(schema.type) === 'boolean') {
        var f = stdFormObj(name, schema, options);
        f.key = options.path;
        f.type = 'checkbox';
        options.lookup[_objectpath2.default.stringify(options.path)] = f;
        return f;
    }
};

var select = function select(name, schema, options) {
    if (stripNullType(schema.type) === 'string' && schema['enum']) {
        var f = stdFormObj(name, schema, options);
        f.key = options.path;
        f.type = 'select';
        if (!f.titleMap) {
            f.titleMap = enumToTitleMap(schema['enum']);
        }
        options.lookup[_objectpath2.default.stringify(options.path)] = f;
        return f;
    }
};

var checkboxes = function checkboxes(name, schema, options) {
    if (stripNullType(schema.type) === 'array' && schema.items && schema.items['enum']) {
        var f = stdFormObj(name, schema, options);
        f.key = options.path;
        f.type = 'checkboxes';
        if (!f.titleMap) {
            f.titleMap = enumToTitleMap(schema.items['enum']);
        }
        options.lookup[_objectpath2.default.stringify(options.path)] = f;
        return f;
    }
};

var fieldset = function fieldset(name, schema, options) {
    if (stripNullType(schema.type) === 'object') {
        var f = stdFormObj(name, schema, options);
        f.type = 'fieldset';
        f.items = [];
        options.lookup[_objectpath2.default.stringify(options.path)] = f;

        //recurse down into properties
        for (var k in schema.properties) {
            if (schema.properties.hasOwnProperty(k)) {
                var path = options.path.slice();
                path.push(k);
                if (options.ignore[_objectpath2.default.stringify(path)] !== true) {
                    var required = schema.required && schema.required.indexOf(k) !== -1;

                    var def = defaultFormDefinition(k, schema.properties[k], {
                        path: path,
                        required: required || false,
                        lookup: options.lookup,
                        ignore: options.ignore,
                        global: options.global
                    });
                    if (def) {
                        f.items.push(def);
                    }
                }
            }
        }
        return f;
    }
};

var array = function array(name, schema, options) {

    if (stripNullType(schema.type) === 'array') {
        var f = stdFormObj(name, schema, options);
        f.type = 'array';
        f.key = options.path;
        options.lookup[_objectpath2.default.stringify(options.path)] = f;

        // don't do anything if items is not defined.
        if (typeof schema.items !== 'undefined') {
            var required = schema.required && schema.required.indexOf(options.path[options.path.length - 1]) !== -1;

            // The default is to always just create one child. This works since if the
            // schemas items declaration is of type: "object" then we get a fieldset.
            // We also follow json form notatation, adding empty brackets "[]" to
            // signify arrays.

            var arrPath = options.path.slice();
            arrPath.push('');
            var def = defaultFormDefinition(name, schema.items, {
                path: arrPath,
                required: required || false,
                lookup: options.lookup,
                ignore: options.ignore,
                global: options.global
            });
            if (def) {
                f.items = [def];
            } else {
                // This is the case that item only contains key value pair for rc-select multipel
                f.items = schema.items;
            }
        }
        return f;
    }
};

var defaults = {
    string: [select, text],
    object: [fieldset],
    number: [number],
    integer: [integer],
    boolean: [checkbox],
    array: [checkboxes, array],
    date: [date]
};

function defaultFormDefinition(name, schema, options) {
    //console.log("defaultFormDefinition name, schema", name, schema);
    var rules = defaults[stripNullType(schema.type)];
    //console.log('defaultFormDefinition:defaults = ', defaults);
    //console.log('defaultFormDefinition:rules = ', rules);
    if (rules) {
        var def;
        for (var i = 0; i < rules.length; i++) {
            def = rules[i](name, schema, options);

            //first handler in list that actually returns something is our handler!
            if (def) {

                // Do we have form defaults in the schema under the x-schema-form-attribute?
                if (def.schema['x-schema-form'] && _lodash2.default.isObject(def.schema['x-schema-form'])) {
                    def = _lodash2.default.extend(def, def.schema['x-schema-form']);
                }
                return def;
            }
        }
    }
}

function getDefaults(schema, ignore, globalOptions) {
    var form = [];
    var lookup = {}; //Map path => form obj for fast lookup in merging
    ignore = ignore || {};
    globalOptions = globalOptions || {};
    //console.log('getDefaults:schema.type = ', schema.type);
    if (stripNullType(schema.type) === 'object') {
        //console.log('getDefaults:schema.properties = ', schema.properties);
        for (var k in schema.properties) {
            if (schema.properties.hasOwnProperty(k)) {
                if (ignore[k] !== true) {
                    var required = schema.required && schema.required.indexOf(k) !== -1;
                    //console.log('getDefaults:required = ', required);
                    //console.log('getDefaults: k = ', k);
                    //console.log('getDefaults: v = ', schema.properties[k]);
                    var def = defaultFormDefinition(k, schema.properties[k], {
                        path: [k], // Path to this property in bracket notation.
                        lookup: lookup, // Extra map to register with. Optimization for merger.
                        ignore: ignore, // The ignore list of paths (sans root level name)
                        required: required, // Is it required? (v4 json schema style)
                        global: globalOptions // Global options, including form defaults
                    });
                    //console.log('getDefaults:def = ', def);
                    if (def) {
                        form.push(def);
                    }
                }
            }
        }
    } else {
        throw new Error('Not implemented. Only type "object" allowed at root level of schema.');
    }
    return { form: form, lookup: lookup };
}

var postProcessFn = function postProcessFn(form) {
    return form;
};

/**
 * Append default form rule
 * @param {string}   type json schema type
 * @param {Function} rule a function(propertyName,propertySchema,options) that returns a form
 *                        definition or undefined
 */
function appendRule(type, rule) {
    if (!defaults[type]) {
        defaults[type] = [];
    }
    defaults[type].push(rule);
}
/**
 * Prepend default form rule
 * @param {string}   type json schema type
 * @param {Function} rule a function(propertyName,propertySchema,options) that returns a form
 *                        definition or undefined
 */
function prependRule(type, rule) {
    if (!defaults[type]) {
        defaults[type] = [];
    }
    defaults[type].unshift(rule);
}

//Utility functions
/**
 * Traverse a schema, applying a function(schema,path) on every sub schema
 * i.e. every property of an object.
 */
function traverseSchema(schema, fn, path, ignoreArrays) {
    ignoreArrays = typeof ignoreArrays !== 'undefined' ? ignoreArrays : true;

    path = path || [];

    var traverse = function traverse(schema, fn, path) {
        fn(schema, path);
        for (var k in schema.properties) {
            if (schema.properties.hasOwnProperty(k)) {
                var currentPath = path.slice();
                currentPath.push(k);
                traverse(schema.properties[k], fn, currentPath);
            }
        }
        //Only support type "array" which have a schema as "items".
        if (!ignoreArrays && schema.items) {
            var arrPath = path.slice();arrPath.push('');
            traverse(schema.items, fn, arrPath);
        }
    };

    traverse(schema, fn, path || []);
}

function traverseForm(form, fn) {
    fn(form);
    if (form.items) {
        form.items.forEach(function (f) {
            traverseForm(f, fn);
        });
    }

    if (form.tabs) {
        form.tabs.forEach(function (tab) {
            tab.items.forEach(function (f) {
                traverseForm(f, fn);
            });
        });
    }
}

function merge(schema, form, ignore, options, readonly) {
    //console.log('merge schema', schema);
    //console.log('merge form', form);
    form = form || ['*'];
    options = options || {};

    // Get readonly from root object
    readonly = readonly || schema.readonly || schema.readOnly;

    var stdForm = getDefaults(schema, ignore, options);
    //console.log('merge stdForm', stdForm);
    //simple case, we have a "*", just put the stdForm there
    var idx = form.indexOf('*');
    if (idx !== -1) {
        form = form.slice(0, idx).concat(stdForm.form).concat(form.slice(idx + 1));
    }

    //ok let's merge!
    //We look at the supplied form and extend it with schema standards
    var lookup = stdForm.lookup;
    //console.log('form', form);
    return postProcessFn(form.map(function (obj) {

        //handle the shortcut with just a name
        if (typeof obj === 'string') {
            obj = { key: obj };
        }
        //console.log('obj', obj);
        if (obj.key) {
            if (typeof obj.key === 'string') {
                obj.key = _objectpath2.default.parse(obj.key);
            }
        }

        //If it has a titleMap make sure it's a list
        if (obj.titleMap) {
            obj.titleMap = canonicalTitleMap(obj.titleMap);
        }

        //
        if (obj.itemForm) {
            obj.items = [];
            var str = _objectpath2.default.stringify(obj.key);
            var stdForm = lookup[str];
            stdForm.items.forEach(function (item) {
                var o = _lodash2.default.cloneDeep(obj.itemForm);
                o.key = item.key;
                obj.items.push(o);
            });
        }

        //extend with std form from schema.
        if (obj.key) {
            var strid = _objectpath2.default.stringify(obj.key);
            if (lookup[strid]) {
                var schemaDefaults = lookup[strid];
                for (var k in schemaDefaults) {
                    if (schemaDefaults.hasOwnProperty(k)) {
                        if (obj[k] === undefined) {
                            obj[k] = schemaDefaults[k];
                        }
                    }
                }
            }
        }

        // Are we inheriting readonly?
        if (readonly === true) {
            // Inheriting false is not cool.
            obj.readonly = true;
        }

        //if it's a type with items, merge 'em!
        if (obj.items && obj.items.length > 0) {
            //console.log('items is not empty schema', schema);
            //console.log('items is not empty obj.items', obj.items);

            obj.items = merge(schema, obj.items, ignore, options, obj.readonly);
        }

        //if its has tabs, merge them also!
        if (obj.tabs) {
            obj.tabs.forEach(function (tab) {
                tab.items = merge(schema, tab.items, ignore, options, obj.readonly);
            });
        }

        // Special case: checkbox
        // Since have to ternary state we need a default
        if (obj.type === 'checkbox' && _lodash2.default.isUndefined(obj.schema['default'])) {
            obj.schema['default'] = false;
        }

        return obj;
    }));
}

function selectOrSet(projection, obj, valueToSet) {
    //console.log('selectOrSet', projection, obj, valueToSet);
    var numRe = /^\d+$/;

    if (!obj) {
        obj = this;
    }
    //Support [] array syntax
    var parts = typeof projection === 'string' ? _objectpath2.default.parse(projection) : projection;

    if (typeof valueToSet !== 'undefined' && parts.length === 1) {
        //special case, just setting one variable
        obj[parts[0]] = valueToSet;
        return obj;
    }

    if (typeof valueToSet !== 'undefined' && typeof obj[parts[0]] === 'undefined') {
        // We need to look ahead to check if array is appropriate
        obj[parts[0]] = parts.length > 2 && numRe.test(parts[1]) ? [] : {};
    }

    var value = obj[parts[0]];
    for (var i = 1; i < parts.length; i++) {
        // Special case: We allow JSON Form syntax for arrays using empty brackets
        // These will of course not work here so we exit if they are found.
        if (parts[i] === '') {
            return undefined;
        }
        if (typeof valueToSet !== 'undefined') {
            if (i === parts.length - 1) {
                //last step. Let's set the value
                value[parts[i]] = valueToSet;
                return valueToSet;
            } else {
                // Make sure to create new objects on the way if they are not there.
                // We need to look ahead to check if array is appropriate
                var tmp = value[parts[i]];
                if (typeof tmp === 'undefined' || tmp === null) {
                    tmp = numRe.test(parts[i + 1]) ? [] : {};
                    value[parts[i]] = tmp;
                }
                value = tmp;
            }
        } else if (value) {
            //Just get nex value.
            value = value[parts[i]];
        }
    }
    return value;
}

function validateBySchema(schema, value) {
    return _tv2.default.validateResult(value, schema);
}

function validate(form, value) {
    //console.log('utils validate form ', form);
    if (!form) {
        return { valid: true };
    }
    var schema = form.schema;
    if (!schema) {
        return { valid: true };
    }
    //console.log('utils validate schema = ', schema);
    // Input of type text and textareas will give us a viewValue of ''
    // when empty, this is a valid value in a schema and does not count as something
    // that breaks validation of 'required'. But for our own sanity an empty field should
    // not validate if it's required.

    if (value === '') {
        value = undefined;
    }

    // Numbers fields will give a null value, which also means empty field
    if (form.type === 'number' && value === null) {
        //console.log('utils validate form.type is number');
        value = undefined;
    }

    if (form.type === 'number' && isNaN(parseFloat(value))) {
        value = undefined;
    }

    // Version 4 of JSON Schema has the required property not on the
    // property itself but on the wrapping object. Since we like to test
    // only this property we wrap it in a fake object.
    var wrap = { type: 'object', 'properties': {} };
    var propName = form.key[form.key.length - 1];
    wrap.properties[propName] = schema;

    if (form.required) {
        wrap.required = [propName];
    }
    var valueWrap = {};
    if (typeof value !== 'undefined') {
        valueWrap[propName] = value;
    }
    //console.log('utils validate value = ', typeof value);
    //console.log('utils validate valueWrap = ', valueWrap);
    //console.log('utils validate wrap = ', wrap);

    var tv4Result = _tv2.default.validateResult(valueWrap, wrap);
    if (tv4Result != null && !tv4Result.valid && form.validationMessage != null && typeof value !== 'undefined') {
        tv4Result.error.message = form.validationMessage;
    }
    return tv4Result;
}

module.exports = {
    traverseForm: traverseForm,
    traverseSchema: traverseSchema,
    prependRule: prependRule,
    appendRule: appendRule,
    postProcessFn: postProcessFn,
    getDefaults: getDefaults,
    defaultFormDefinition: defaultFormDefinition,
    defaults: defaults,
    array: array,
    fieldset: fieldset,
    checkboxes: checkboxes,
    select: select,
    checkbox: checkbox,
    integer: integer,
    number: number,
    text: text,
    stdFormObj: stdFormObj,
    canonicalTitleMap: canonicalTitleMap,
    enumToTitleMap: enumToTitleMap,
    stripNullType: stripNullType,
    merge: merge,
    validate: validate,
    validateBySchema: validateBySchema,
    selectOrSet: selectOrSet
};
;

var _temp = function () {
    if (typeof __REACT_HOT_LOADER__ === 'undefined') {
        return;
    }

    __REACT_HOT_LOADER__.register(stripNullType, 'stripNullType', 'src/utils.js');

    __REACT_HOT_LOADER__.register(enumToTitleMap, 'enumToTitleMap', 'src/utils.js');

    __REACT_HOT_LOADER__.register(canonicalTitleMap, 'canonicalTitleMap', 'src/utils.js');

    __REACT_HOT_LOADER__.register(stdFormObj, 'stdFormObj', 'src/utils.js');

    __REACT_HOT_LOADER__.register(text, 'text', 'src/utils.js');

    __REACT_HOT_LOADER__.register(number, 'number', 'src/utils.js');

    __REACT_HOT_LOADER__.register(integer, 'integer', 'src/utils.js');

    __REACT_HOT_LOADER__.register(date, 'date', 'src/utils.js');

    __REACT_HOT_LOADER__.register(checkbox, 'checkbox', 'src/utils.js');

    __REACT_HOT_LOADER__.register(select, 'select', 'src/utils.js');

    __REACT_HOT_LOADER__.register(checkboxes, 'checkboxes', 'src/utils.js');

    __REACT_HOT_LOADER__.register(fieldset, 'fieldset', 'src/utils.js');

    __REACT_HOT_LOADER__.register(array, 'array', 'src/utils.js');

    __REACT_HOT_LOADER__.register(defaults, 'defaults', 'src/utils.js');

    __REACT_HOT_LOADER__.register(defaultFormDefinition, 'defaultFormDefinition', 'src/utils.js');

    __REACT_HOT_LOADER__.register(getDefaults, 'getDefaults', 'src/utils.js');

    __REACT_HOT_LOADER__.register(postProcessFn, 'postProcessFn', 'src/utils.js');

    __REACT_HOT_LOADER__.register(appendRule, 'appendRule', 'src/utils.js');

    __REACT_HOT_LOADER__.register(prependRule, 'prependRule', 'src/utils.js');

    __REACT_HOT_LOADER__.register(traverseSchema, 'traverseSchema', 'src/utils.js');

    __REACT_HOT_LOADER__.register(traverseForm, 'traverseForm', 'src/utils.js');

    __REACT_HOT_LOADER__.register(merge, 'merge', 'src/utils.js');

    __REACT_HOT_LOADER__.register(selectOrSet, 'selectOrSet', 'src/utils.js');

    __REACT_HOT_LOADER__.register(validateBySchema, 'validateBySchema', 'src/utils.js');

    __REACT_HOT_LOADER__.register(validate, 'validate', 'src/utils.js');
}();

;