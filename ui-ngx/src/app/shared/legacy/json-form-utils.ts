///
/// Copyright © 2016-2025 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import ObjectPath from 'objectpath';
import _ from 'lodash';
import {
  DefaultsFormOptions,
  FormOption
} from './json-form.models';

function stripNullType(type: any): string {
  if (Array.isArray(type) && type.length === 2) {
    if (type[0] === 'null') {
      return type[1];
    }
    if (type[1] === 'null') {
      return type[0];
    }
  }
  return type;
}

const enumToTitleMap = (enm: string[]): { name: string, value: string }[] => {
  const titleMap: { name: string, value: string }[] = [];
  enm.forEach((name) => {
    titleMap.push({name, value: name});
  });
  return titleMap;
};

const canonicalTitleMap = (titleMap: any, originalEnum?: string[]): { name: string, value: string }[] => {
  if (!_.isArray(titleMap)) {
    const canonical: { name: string, value: string }[] = [];
    if (originalEnum) {
      originalEnum.forEach((value) => {
        canonical.push({name: titleMap[value], value});
      });
    } else {
      for (const k of Object.keys(titleMap)) {
        if (titleMap.hasOwnProperty(k)) {
          canonical.push({name: k, value: titleMap[k]});
        }
      }
    }
    return canonical;
  }
  return titleMap;
};

const stdFormObj = (name: string, schema: any, options: DefaultsFormOptions): any => {
  options = options || {};
  const f: any = options.global && options.global.formDefaults ? _.cloneDeep(options.global.formDefaults) : {};
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
    f.titleMap = canonicalTitleMap(schema.enumNames, schema.enum);
  }
  f.schema = schema;

  // Ng model options doesn't play nice with undefined, might be defined
  // globally though
  f.ngModelOptions = f.ngModelOptions || {};

  return f;
};

const text = (name: string, schema: any, options: DefaultsFormOptions): any => {
  if (stripNullType(schema.type) === 'string' && !schema.enum) {
    const f = stdFormObj(name, schema, options);
    f.key = options.path;
    f.type = 'text';
    options.lookup[ObjectPath.stringify(options.path)] = f;
    return f;
  }
};

const numberType = (name: string, schema: any, options: DefaultsFormOptions): any => {
  if (stripNullType(schema.type) === 'number') {
    const f = stdFormObj(name, schema, options);
    f.key = options.path;
    f.type = 'number';
    options.lookup[ObjectPath.stringify(options.path)] = f;
    return f;
  }
};

const integer = (name: string, schema: any, options: DefaultsFormOptions): any => {
  if (stripNullType(schema.type) === 'integer') {
    const f = stdFormObj(name, schema, options);
    f.key = options.path;
    f.type = 'number';
    options.lookup[ObjectPath.stringify(options.path)] = f;
    return f;
  }
};

const date = (name: string, schema: any, options: DefaultsFormOptions): any => {
  if (stripNullType(schema.type) === 'date') {
    const f = stdFormObj(name, schema, options);
    f.key = options.path;
    f.type = 'date';
    options.lookup[ObjectPath.stringify(options.path)] = f;
    return f;
  }
};

const checkbox = (name: string, schema: any, options: DefaultsFormOptions): any => {
  if (stripNullType(schema.type) === 'boolean') {
    const f = stdFormObj(name, schema, options);
    f.key = options.path;
    f.type = 'checkbox';
    options.lookup[ObjectPath.stringify(options.path)] = f;
    return f;
  }
};

const select = (name: string, schema: any, options: DefaultsFormOptions): any => {
  if (stripNullType(schema.type) === 'string' && schema.enum) {
    const f = stdFormObj(name, schema, options);
    f.key = options.path;
    f.type = 'select';
    if (!f.titleMap) {
      f.titleMap = enumToTitleMap(schema.enum);
    }
    options.lookup[ObjectPath.stringify(options.path)] = f;
    return f;
  }
};

const checkboxes = (name: string, schema: any, options: DefaultsFormOptions): any => {
  if (stripNullType(schema.type) === 'array' && schema.items && schema.items.enum) {
    const f = stdFormObj(name, schema, options);
    f.key = options.path;
    f.type = 'checkboxes';
    if (!f.titleMap) {
      f.titleMap = enumToTitleMap(schema.items.enum);
    }
    options.lookup[ObjectPath.stringify(options.path)] = f;
    return f;
  }
};

const fieldset = (name: string, schema: any, options: DefaultsFormOptions): any => {
  if (stripNullType(schema.type) === 'object') {
    const f = stdFormObj(name, schema, options);
    f.type = 'fieldset';
    f.items = [];
    options.lookup[ObjectPath.stringify(options.path)] = f;

    // recurse down into properties
    for (const k of Object.keys(schema.properties)) {
      if (schema.properties.hasOwnProperty(k)) {
        const path = options.path.slice();
        path.push(k);
        if (options.ignore[ObjectPath.stringify(path)] !== true) {
          const required = schema.required && schema.required.indexOf(k) !== -1;

          const def = defaultFormDefinition(k, schema.properties[k], {
            path,
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

const array = (name: string, schema: any, options: DefaultsFormOptions): any => {

  if (stripNullType(schema.type) === 'array') {
    const f = stdFormObj(name, schema, options);
    f.type = 'array';
    f.key = options.path;
    options.lookup[ObjectPath.stringify(options.path)] = f;

    // don't do anything if items is not defined.
    if (typeof schema.items !== 'undefined') {
      const required = schema.required && schema.required.indexOf(options.path[options.path.length - 1]) !== -1;

      const arrPath = options.path.slice();
      arrPath.push('');
      const def = defaultFormDefinition(name, schema.items, {
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

const defaults: { [key: string]: ((name: string, schema: any, options: DefaultsFormOptions) => any)[] } = {
  string: [select, text],
  object: [fieldset],
  number: [numberType],
  integer: [integer],
  boolean: [checkbox],
  array: [checkboxes, array],
  date: [date]
};

function defaultFormDefinition(name: string, schema: any, options: DefaultsFormOptions): any {
  const rules = defaults[stripNullType(schema.type)];
  if (rules) {
    let def;
    for (const rule of rules) {
      def = rule(name, schema, options);
      if (def) {

        // Do we have form defaults in the schema under the x-schema-form-attribute?
        if (def.schema['x-schema-form'] && _.isObject(def.schema['x-schema-form'])) {
          def = _.extend(def, def.schema['x-schema-form']);
        }
        return def;
      }
    }
  }
}

interface DefaultsFormData {
  form: any[];
  lookup: { [key: string]: any };
}

function getDefaults(schema: any, ignore: { [key: string]: boolean }, globalOptions: FormOption): DefaultsFormData {
  const form = [];
  const lookup: { [key: string]: any } = {};
  ignore = ignore || {};
  globalOptions = globalOptions || {};
  if (stripNullType(schema.type) === 'object') {
    for (const k of Object.keys(schema.properties)) {
      if (schema.properties.hasOwnProperty(k)) {
        if (ignore[k] !== true) {
          const required = schema.required && schema.required.indexOf(k) !== -1;
          const def = defaultFormDefinition(k, schema.properties[k], {
            path: [k], // Path to this property in bracket notation.
            lookup, // Extra map to register with. Optimization for merger.
            ignore, // The ignore list of paths (sans root level name)
            required, // Is it required? (v4 json schema style)
            global: globalOptions // Global options, including form defaults
          });
          if (def) {
            form.push(def);
          }
        }
      }
    }
  } else {
    throw new Error('Not implemented. Only type "object" allowed at root level of schema.');
  }
  return {form, lookup};
}

const postProcessFn = (form: any[]): any[] => {
  return form;
};

function merge(schema: any, form: any[], ignore: { [key: string]: boolean }, options: FormOption, isReadonly?: boolean): any[] {
  form = form || ['*'];
  options = options || {};
  isReadonly = isReadonly || schema.readonly || schema.readOnly;
  const stdForm = getDefaults(schema, ignore, options);
  const idx = form.indexOf('*');
  if (idx !== -1) {
    form = form.slice(0, idx).concat(stdForm.form).concat(form.slice(idx + 1));
  }
  const lookup = stdForm.lookup;
  return postProcessFn(form.map((obj) => {

    if (typeof obj === 'string') {
      obj = {key: obj};
    }

    if (obj.key) {
      if (typeof obj.key === 'string') {
        obj.key = ObjectPath.parse(obj.key);
      }
    }

    if (obj.titleMap) {
      obj.titleMap = canonicalTitleMap(obj.titleMap);
    }

    if (obj.itemForm) {
      obj.items = [];
      const str: string = ObjectPath.stringify(obj.key);
      const lookupForm = lookup[str];
      lookupForm.items.forEach((item) => {
        const o = _.cloneDeep(obj.itemForm);
        o.key = item.key;
        obj.items.push(o);
      });
    }

    if (obj.key) {
      const strid: string = ObjectPath.stringify(obj.key);
      if (lookup[strid]) {
        const schemaDefaults = lookup[strid];
        for (const k of Object.keys(schemaDefaults)) {
          if (schemaDefaults.hasOwnProperty(k)) {
            if (obj[k] === undefined) {
              obj[k] = schemaDefaults[k];
            }
          }
        }
      }
    }

    if (isReadonly === true) {
      obj.readonly = true;
    }

    if (obj.items && obj.items.length > 0) {
      obj.items = merge(schema, obj.items, ignore, options, obj.readonly);
    }

    if (obj.tabs) {
      obj.tabs.forEach((tab) => {
        tab.items = merge(schema, tab.items, ignore, options, obj.readonly);
      });
    }

    if (obj.type === 'checkbox' && _.isUndefined(obj.schema.default)) {
      obj.schema.default = false;
    }

    return obj;
  }));
}

const utils = {
  merge
};
export default utils;
