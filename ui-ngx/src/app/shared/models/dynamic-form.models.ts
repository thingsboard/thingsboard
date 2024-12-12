///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { CustomTranslatePipe } from '@shared/pipe/custom-translate.pipe';
import { TbEditorCompletion, TbEditorCompletions } from '@shared/models/ace/completion.models';
import { isString } from '@core/utils';
import { JsonSchema, JsonSettingsSchema } from '@shared/models/widget.models';
import JsonFormUtils from '@shared/components/json-form/react/json-form-utils';
import { JsonFormData, KeyLabelItem } from '@shared/components/json-form/react/json-form.models';

export enum FormPropertyType {
  text = 'text',
  number = 'number',
  switch = 'switch',
  select = 'select',
  color = 'color',
  color_settings = 'color_settings',
  font = 'font',
  units = 'units',
  icon = 'icon',
  fieldset = 'fieldset'
}

export const formPropertyTypes = Object.keys(FormPropertyType) as FormPropertyType[];

export const formPropertyTypeTranslations = new Map<FormPropertyType, string>(
  [
    [FormPropertyType.text, 'dynamic-form.property.type-text'],
    [FormPropertyType.number, 'dynamic-form.property.type-number'],
    [FormPropertyType.switch, 'dynamic-form.property.type-switch'],
    [FormPropertyType.select, 'dynamic-form.property.type-select'],
    [FormPropertyType.color, 'dynamic-form.property.type-color'],
    [FormPropertyType.color_settings, 'dynamic-form.property.type-color-settings'],
    [FormPropertyType.font, 'dynamic-form.property.type-font'],
    [FormPropertyType.units, 'dynamic-form.property.type-units'],
    [FormPropertyType.icon, 'dynamic-form.property.type-icon'],
    [FormPropertyType.fieldset, 'dynamic-form.property.type-fieldset']
  ]
);

export const formPropertyRowClasses =
  ['column', 'column-xs', 'column-lt-md', 'align-start', 'no-border', 'no-gap', 'no-padding', 'same-padding'];

export const formPropertyFieldClasses =
  ['medium-width', 'flex', 'flex-xs', 'flex-lt-md'];

export type PropertyConditionFunction = (property: FormProperty, model: any) => boolean;

export interface FormPropertyBase {
  id: string;
  name: string;
  group?: string;
  type: FormPropertyType;
  default: any;
  required?: boolean;
  subLabel?: string;
  divider?: boolean;
  fieldSuffix?: string;
  disableOnProperty?: string;
  condition?: string;
  conditionFunction?: PropertyConditionFunction;
  disabled?: boolean;
  visible?: boolean;
  rowClass?: string;
  fieldClass?: string;
}

export interface FormNumberProperty extends FormPropertyBase {
  min?: number;
  max?: number;
  step?: number;
}

export interface FormFieldSetProperty extends FormPropertyBase {
  properties?: FormProperty[];
}

export interface FormSelectItem {
  value: any;
  label: string;
}

export interface FormSelectProperty extends FormPropertyBase {
  multiple?: boolean;
  items?: FormSelectItem[];
}

export type FormProperty = FormPropertyBase & FormNumberProperty & FormSelectProperty & FormFieldSetProperty;

export enum FormPropertyContainerType {
  row = 'row',
  fieldset = 'fieldset'
}

export interface FormPropertyContainerBase {
  type: FormPropertyContainerType;
  label: string;
  properties: FormProperty[];
  visible: boolean;
}

export interface FormPropertyRow extends FormPropertyContainerBase {
  switch?: FormProperty;
  rowClass?: string;
}

export interface FormPropertyFieldset extends FormPropertyContainerBase {
  property?: FormProperty;
}

export type FormPropertyContainer = FormPropertyRow & FormPropertyFieldset;

export interface FormPropertyGroup {
  title?: string;
  containers: FormPropertyContainer[];
  visible: boolean;
}

export const toPropertyGroups = (properties: FormProperty[]): FormPropertyGroup[] => {
  const groups: {title: string, properties: FormProperty[]}[] = [];
  for (let property of properties) {
    if (!property.group) {
      groups.push({
        title: null,
        properties: [property]
      });
    } else {
      let propertyGroup = groups.find(g => g.title === property.group);
      if (!propertyGroup) {
        propertyGroup = {
          title: property.group,
          properties: []
        };
        groups.push(propertyGroup);
      }
      propertyGroup.properties.push(property);
    }
  }
  return groups.map(g => ({
    title: g.title,
    containers: toPropertyContainers(g.properties),
    visible: true
  }));
};

const toPropertyContainers = (properties: FormProperty[]): FormPropertyContainer[] => {
  const result: FormPropertyContainer[] = [];
  for (let property of properties) {
    if (property.type === FormPropertyType.fieldset) {
      const propertyFieldset: FormPropertyFieldset = {
        property,
        label: property.name,
        type: FormPropertyContainerType.fieldset,
        properties: property.properties,
        visible: true
      };
      result.push(propertyFieldset);
    } else {
      let propertyRow =
        result.find(r => r.type === FormPropertyContainerType.row && r.label === property.name);
      if (!propertyRow) {
        propertyRow = {
          label: property.name,
          type: FormPropertyContainerType.row,
          properties: [],
          rowClass: property.rowClass,
          visible: true
        };
        result.push(propertyRow);
      }
      if (property.type === FormPropertyType.switch) {
        propertyRow.switch = property;
      } else {
        propertyRow.properties.push(property);
      }
    }
  }
  return result;
}

export const defaultFormProperties = (properties: FormProperty[]): {[id: string]: any} => {
  const formProperties: {[id: string]: any} = {};
  for (const property of properties) {
    formProperties[property.id] = defaultFormPropertyValue(property);
  }
  return formProperties;
};

export const defaultFormPropertyValue = (property: FormProperty): any => {
  if (property.type === FormPropertyType.fieldset) {
    const propertyValue: {[id: string]: any} = {};
    for (const childProperty of property.properties) {
      propertyValue[childProperty.id] = defaultFormPropertyValue(childProperty);
    }
    return propertyValue;
  } else {
    return property.default;
  }
}

export const formPropertyCompletions = (properties: FormProperty[], customTranslate: CustomTranslatePipe): TbEditorCompletions => {
  const propertiesCompletions: TbEditorCompletions = {};
  for (const property of properties) {
    propertiesCompletions[property.id] = formPropertyCompletion(property, customTranslate);
  }
  return propertiesCompletions;
}

export const formPropertyCompletion = (property: FormProperty, customTranslate: CustomTranslatePipe): TbEditorCompletion => {
  let description = customTranslate.transform(property.name, property.name);
  if (property.subLabel) {
    description += ` <small>${customTranslate.transform(property.subLabel, property.subLabel)}</small>`;
  }
  if (property.type === FormPropertyType.select) {
    if (property.multiple) {
      description += '<br/><br/><code class="title"><b>Possible values of array element:</b></code>';
    } else {
      description += '<br/><br/><code class="title"><b>Possible values:</b></code>';
    }
    description += `<ul>${property.items.map(item => `<li>${item.value} <code style="font-size: 11px;">${typeof item.value}</code></li>`).join('\n')}</ul>`;
  }
  const completion: TbEditorCompletion = {
    meta: 'property',
    description,
    type: formPropertyCompletionType(property)
  };
  if (property.type === FormPropertyType.fieldset) {
    completion.children = {};
    for (const childProperty of property.properties) {
      completion.children[childProperty.id] = formPropertyCompletion(childProperty, customTranslate);
    }
  }
  return completion;
};

const formPropertyCompletionType = (property: FormProperty): string => {
  switch (property.type) {
    case FormPropertyType.text:
      return 'string';
    case FormPropertyType.number:
      return 'number';
    case FormPropertyType.switch:
      return 'boolean';
    case FormPropertyType.select:
      const items = property.items || [];
      const types: string[] = [];
      items.forEach(item => {
        const type = typeof item.value;
        if (!types.includes(type)) {
          types.push(type);
        }
      });
      const typesString = types.length ? types.join(' | ') : 'string';
      if (property.multiple) {
        return `Array&lt;${typesString}&gt;`;
      } else {
        return typesString;
      }
    case FormPropertyType.color:
      return 'color string';
    case FormPropertyType.color_settings:
      return 'ColorProcessor';
    case FormPropertyType.font:
      return 'Font';
    case FormPropertyType.units:
      return 'units string';
    case FormPropertyType.icon:
      return 'icon string';
    case FormPropertyType.fieldset:
      return 'object';
  }
};


export const jsonFormSchemaToFormProperties = (rawSchema: string | any) : FormProperty[] => {
  const properties: FormProperty[] = [];
  let settingsSchema: JsonSettingsSchema;
  if (!rawSchema || rawSchema === '') {
    settingsSchema = {};
  } else {
    settingsSchema = isString(rawSchema) ? JSON.parse(rawSchema) : rawSchema;
  }
  if (settingsSchema.schema) {
    const schema = settingsSchema.schema;
    const form = settingsSchema.form || ['*'];
    const groupInfoes = settingsSchema.groupInfoes || [];
    if (form.length > 0) {
      if (groupInfoes.length) {
        for (const info of groupInfoes) {
          const theForm: any[] = form[info.formIndex];
          properties.push(...schemaFormToProperties(schema, theForm, info.GroupTitle));
        }
      } else {
        properties.push(...schemaFormToProperties(schema, form));
      }
    }
  }
  return properties;
}

const schemaFormToProperties = (schema: JsonSchema, theForm: any[], groupTitle?: string): FormProperty[] => {
  const merged = JsonFormUtils.merge(schema, theForm, {}, {
    formDefaults: {
      startEmpty: true
    }
  });
  return merged.map((form: JsonFormData) => jsonFormDataToProperty(form, 0, groupTitle)).filter(p => p != null);
}

const jsonFormDataToProperty = (form: JsonFormData, level: number, groupTitle?: string): FormProperty => {
  if (form.key && form.key.length > level) {
    const property: FormProperty = {
      id: form.key[level] + '',
      name: form.title,
      group: groupTitle,
      type: null,
      default: form.schema.default,
      required: form.required
    };
    if (form.condition?.length) {
      property.condition = `return ${form.condition};`;
    }
    switch (form.type) {
      case 'number':
        property.type = FormPropertyType.number;
        break;
      case 'text':
        property.type = FormPropertyType.text;
        property.fieldClass = 'flex';
        break;
      case 'checkbox':
        property.type = FormPropertyType.switch;
        break;
      case 'rc-select':
        property.type = FormPropertyType.select;
        if (form.items?.length) {
          property.items = (form.items as KeyLabelItem[]).map(item => ({value: item.value, label: item.label}));
        } else {
          property.items = [];
        }
        property.multiple = form.multiple;
        property.fieldClass = 'flex';
        break;
      case 'select':
        property.type = FormPropertyType.select;
        if (form.titleMap?.length) {
          property.items = form.titleMap.map(item => ({value: item.value, label: item.name}));
        } else {
          property.items = [];
        }
        property.multiple = false;
        property.fieldClass = 'flex';
        break;
      case 'color':
        property.type = FormPropertyType.color;
        break;
      case 'icon':
        property.type = FormPropertyType.icon;
        break;
      case 'fieldset':
        property.type = FormPropertyType.fieldset;
        property.properties = form.items ? (form.items as JsonFormData[]).map(item =>
          jsonFormDataToProperty(item, level+1)).filter(p => p !== null) : [];
        break;
    }
    if (!property.type) {
      return null;
    }
    return property;
  }
  return null;
}
