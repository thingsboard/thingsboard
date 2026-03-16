///
/// Copyright © 2016-2026 The Thingsboard Authors
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
import { deepClone, isDefinedAndNotNull, isEmptyStr, isString, isUndefinedOrNull } from '@core/utils';
import { JsonFormData, JsonSchema, JsonSettingsSchema, KeyLabelItem } from '@shared/legacy/json-form.models';
import JsonFormUtils from '@shared/legacy/json-form-utils';
import { constantColor, Font } from '@shared/models/widget-settings.models';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

export enum FormPropertyType {
  text = 'text',
  number = 'number',
  password = 'password',
  textarea = 'textarea',
  switch = 'switch',
  select = 'select',
  radios = 'radios',
  datetime = 'datetime',
  image = 'image',
  javascript = 'javascript',
  json = 'json',
  html = 'html',
  css = 'css',
  markdown = 'markdown',
  color = 'color',
  color_settings = 'color_settings',
  font = 'font',
  units = 'units',
  icon = 'icon',
  fieldset = 'fieldset',
  array = 'array',
  htmlSection = 'htmlSection'
}

export const formPropertyTypes = Object.keys(FormPropertyType) as FormPropertyType[];

export const formPropertyTypeTranslations = new Map<FormPropertyType, string>(
  [
    [FormPropertyType.text, 'dynamic-form.property.type-text'],
    [FormPropertyType.number, 'dynamic-form.property.type-number'],
    [FormPropertyType.password, 'dynamic-form.property.type-password'],
    [FormPropertyType.textarea, 'dynamic-form.property.type-textarea'],
    [FormPropertyType.switch, 'dynamic-form.property.type-switch'],
    [FormPropertyType.select, 'dynamic-form.property.type-select'],
    [FormPropertyType.radios, 'dynamic-form.property.type-radios'],
    [FormPropertyType.datetime, 'dynamic-form.property.type-datetime'],
    [FormPropertyType.image, 'dynamic-form.property.type-image'],
    [FormPropertyType.javascript, 'dynamic-form.property.type-javascript'],
    [FormPropertyType.json, 'dynamic-form.property.type-json'],
    [FormPropertyType.html, 'dynamic-form.property.type-html'],
    [FormPropertyType.css, 'dynamic-form.property.type-css'],
    [FormPropertyType.markdown, 'dynamic-form.property.type-markdown'],
    [FormPropertyType.color, 'dynamic-form.property.type-color'],
    [FormPropertyType.color_settings, 'dynamic-form.property.type-color-settings'],
    [FormPropertyType.font, 'dynamic-form.property.type-font'],
    [FormPropertyType.units, 'dynamic-form.property.type-units'],
    [FormPropertyType.icon, 'dynamic-form.property.type-icon'],
    [FormPropertyType.fieldset, 'dynamic-form.property.type-fieldset'],
    [FormPropertyType.array, 'dynamic-form.property.type-array'],
    [FormPropertyType.htmlSection, 'dynamic-form.property.type-html-section']
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
  hint?: string;
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

export interface FormTextareaProperty extends FormPropertyBase {
  rows?: number;
}

export interface FormNumberProperty extends FormPropertyBase {
  min?: number;
  max?: number;
  step?: number;
}

export interface FormFieldSetProperty extends FormPropertyBase {
  properties?: FormProperty[];
}

export interface FormArrayProperty extends FormPropertyBase {
  arrayItemName?: string;
  arrayItemType?: FormPropertyType;
}

export interface FormSelectItem {
  value: any;
  label: string;
}

export interface FormSelectProperty extends FormPropertyBase {
  multiple?: boolean;
  allowEmptyOption?: boolean;
  items?: FormSelectItem[];
  minItems?: number;
  maxItems?: number;
}

export type FormPropertyDirection = 'row' | 'column';

export interface FormRadiosProperty extends FormPropertyBase {
  direction?: FormPropertyDirection;
  items?: FormSelectItem[];
}

export type FormPropertyDateTimeType = 'date' | 'time' | 'datetime';

export interface FormDateTimeProperty extends FormPropertyBase {
  allowClear?: boolean;
  dateTimeType?: FormPropertyDateTimeType;
}

export interface FormJavascriptProperty extends FormPropertyBase {
  helpId?: string;
}

export interface FormMarkdownProperty extends FormPropertyBase {
  helpId?: string;
}

export interface FormHtmlSection extends FormPropertyBase {
  htmlClassList?: string[];
  htmlContent?: string;
}

export interface FormUnitProperty extends FormPropertyBase {
  supportsUnitConversion?: boolean;
}

export type FormProperty = FormPropertyBase & FormTextareaProperty & FormNumberProperty & FormSelectProperty & FormRadiosProperty
  & FormDateTimeProperty & FormJavascriptProperty & FormMarkdownProperty & FormFieldSetProperty & FormArrayProperty & FormHtmlSection
  & FormUnitProperty;

export const cleanupFormProperties = (properties: FormProperty[]): FormProperty[] => {
  for (const property of properties) {
    cleanupFormProperty(property);
  }
  return properties;
}

export const cleanupFormProperty = (property: FormProperty): FormProperty => {
  if (property.type !== FormPropertyType.number) {
    delete property.min;
    delete property.max;
    delete property.step;
  }
  if (property.type !== FormPropertyType.textarea) {
    delete property.rows;
  }
  if (property.type !== FormPropertyType.array) {
    delete property.arrayItemName;
    delete property.arrayItemType;
  }
  if (property.type !== FormPropertyType.fieldset && property.arrayItemType !== FormPropertyType.fieldset) {
    delete property.properties;
  } else if (property.properties?.length) {
    property.properties = cleanupFormProperties(property.properties);
  }
  if (property.type !== FormPropertyType.select) {
    delete property.multiple;
    delete property.allowEmptyOption;
    delete property.minItems;
    delete property.maxItems;
  }
  if (property.type !== FormPropertyType.radios) {
    delete property.direction;
  }
  if (![FormPropertyType.select, FormPropertyType.radios].includes(property.type)) {
    delete property.items;
  }
  if (property.type !== FormPropertyType.datetime) {
    delete property.allowClear;
    delete property.dateTimeType;
  }
  if (![FormPropertyType.javascript, FormPropertyType.markdown].includes(property.type)) {
    delete property.helpId;
  }
  if (property.type !== FormPropertyType.htmlSection) {
    delete property.htmlClassList;
    delete property.htmlContent;
  }
  if (property.type !== FormPropertyType.units) {
    delete property.supportsUnitConversion;
  }
  for (const key of Object.keys(property)) {
    const val = property[key];
    if (isUndefinedOrNull(val) || isEmptyStr(val)) {
      delete property[key];
    }
  }
  return property;
}

export enum FormPropertyContainerType {
  field = 'field',
  row = 'row',
  fieldset = 'fieldset',
  array = 'array',
  htmlSection = 'htmlSection'
}

export interface FormPropertyContainerBase {
  type: FormPropertyContainerType;
  label: string;
  visible: boolean;
}

export interface FormPropertyRow extends FormPropertyContainerBase {
  hint?: string;
  properties?: FormProperty[];
  switch?: FormProperty;
  rowClass?: string;
  propertiesRowClass?: string;
}

export interface FormPropertyField extends FormPropertyContainerBase {
  property?: FormProperty;
}

export interface FormPropertyFieldset extends FormPropertyContainerBase {
  property?: FormProperty;
  properties?: FormProperty[];
}

export interface FormPropertyArray extends FormPropertyContainerBase {
  property?: FormProperty;
  arrayItemProperty?: FormProperty;
}

export interface FormPropertyHtml extends FormPropertyContainerBase {
  property?: FormProperty;
  safeHtml?: SafeHtml;
  htmlClass?: string;
}

export type FormPropertyContainer = FormPropertyField & FormPropertyRow & FormPropertyFieldset & FormPropertyHtml;

export interface FormPropertyGroup {
  title?: string;
  containers: FormPropertyContainer[];
  visible: boolean;
}

export const toPropertyGroups = (properties: FormProperty[],
                                 isArrayItem: boolean,
                                 customTranslate: CustomTranslatePipe,
                                 sanitizer: DomSanitizer): FormPropertyGroup[] => {
  const groups: {title: string, properties: FormProperty[]}[] = [];
  for (const property of properties) {
    if (!property.group) {
      const group = groups.length ? groups[groups.length - 1] : null;
      if (group && !group.title) {
        group.properties.push(property);
      } else {
        groups.push({
          title: null,
          properties: [property]
        });
      }
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
    containers: toPropertyContainers(g.properties, isArrayItem, customTranslate, sanitizer),
    visible: true
  }));
};

const toPropertyContainers = (properties: FormProperty[],
                              isArrayItem: boolean,
                              customTranslate: CustomTranslatePipe,
                              sanitizer: DomSanitizer): FormPropertyContainer[] => {
  const result: FormPropertyContainer[] = [];
  for (const property of properties) {
    if (property.type === FormPropertyType.array) {
      const propertyArray: FormPropertyArray = {
        property,
        label: property.name,
        type: FormPropertyContainerType.array,
        visible: true
      };
      const arrayItemProperty = deepClone(property);
      arrayItemProperty.name = property.arrayItemName;
      arrayItemProperty.type = property.arrayItemType;
      arrayItemProperty.required = true;
      delete arrayItemProperty.disableOnProperty;
      delete arrayItemProperty.condition;
      delete arrayItemProperty.conditionFunction;
      delete arrayItemProperty.group;
      propertyArray.arrayItemProperty = arrayItemProperty;
      result.push(propertyArray);
    } else if (property.type === FormPropertyType.fieldset) {
      const propertyFieldset: FormPropertyFieldset = {
        property,
        label: property.name,
        type: FormPropertyContainerType.fieldset,
        properties: property.properties,
        visible: true
      };
      result.push(propertyFieldset);
    } else if (property.type === FormPropertyType.htmlSection) {
      const propertyHtml: FormPropertyHtml = {
        property,
        label: property.name,
        type: FormPropertyContainerType.htmlSection,
        htmlClass: property.htmlClassList ? property.htmlClassList.join(' ') : '',
        safeHtml: sanitizer.bypassSecurityTrustHtml(property.htmlContent),
        visible: true
      };
      result.push(propertyHtml);
    } else if (isSingleFieldPropertyType(property.type) || isArrayItem) {
      const propertyField: FormPropertyField = {
        property,
        label: property.name,
        type: FormPropertyContainerType.field,
        visible: true
      };
      result.push(propertyField);
    } else {
      let propertyRow =
        result.find(r => r.type === FormPropertyContainerType.row && r.label === property.name);
      if (!propertyRow) {
        propertyRow = {
          label: property.name,
          hint: property.hint,
          type: FormPropertyContainerType.row,
          properties: [],
          rowClass: property.rowClass,
          propertiesRowClass: 'row flex-end align-center',
          visible: true
        };
        result.push(propertyRow);
        const rowClasses = (propertyRow.rowClass || '').split(' ').filter(cls => cls.trim().length > 0);
        if (!rowClasses.includes('flex-wrap')) {
          rowClasses.push('flex-wrap');
        }
        propertyRow.rowClass = rowClasses.join(' ');
      }
      if (property.type === FormPropertyType.switch) {
        propertyRow.switch = property;
      } else {
        propertyRow.properties.push(property);
      }
    }
  }
  for (const container of result.filter(c =>
    c.type === FormPropertyContainerType.row && !c.switch && c.properties?.length === 1)) {
    const property = container.properties[0];
    if (isInputFieldPropertyType(property.type)) {
      const labelText = customTranslate.transform(property.name);
      if (property.type !== FormPropertyType.number && labelText.length > 40) {
        container.type = FormPropertyContainerType.field;
        container.property = property;
        delete container.properties;
        delete container.rowClass;
      } else {
        container.propertiesRowClass = 'gt-xs:align-center xs:flex-col gt-xs:flex-row gt-xs:justify-end';
        const rowClasses = (container.rowClass || '').split(' ').filter(cls => cls.trim().length > 0);
        if (!rowClasses.includes('column-xs')) {
          rowClasses.push('column-xs');
        }
        if (property.fieldClass && property.fieldClass.split(' ').includes('flex')) {
          container.propertiesRowClass += ' overflow-hidden';
          if (rowClasses.includes('flex-wrap')) {
            rowClasses.splice(rowClasses.indexOf('flex-wrap'), 1);
          }
        }
        container.rowClass = rowClasses.join(' ');
      }
    }
  }
  return result;
}

export const isPropertyTypeAllowedForRow = (type: FormPropertyType): boolean => {
  return !isSingleFieldPropertyType(type) && ![FormPropertyType.fieldset, FormPropertyType.array, FormPropertyType.htmlSection].includes(type);
}

export const isSingleFieldPropertyType = (type: FormPropertyType): boolean => {
  return [FormPropertyType.radios, FormPropertyType.textarea, FormPropertyType.image, FormPropertyType.javascript, FormPropertyType.json, FormPropertyType.html,
    FormPropertyType.css, FormPropertyType.markdown].includes(type);
}

export const isInputFieldPropertyType = (type: FormPropertyType): boolean => {
  return [FormPropertyType.text, FormPropertyType.password, FormPropertyType.number, FormPropertyType.select, FormPropertyType.datetime,
    FormPropertyType.textarea].includes(type);
}

export const defaultFormProperties = (properties: FormProperty[]): {[id: string]: any} => {
  const formProperties: {[id: string]: any} = {};
  for (const property of properties) {
    if (property.type !== FormPropertyType.htmlSection) {
      formProperties[property.id] = defaultFormPropertyValue(property);
    }
  }
  return formProperties;
};

export const defaultFormPropertyValue = (property: FormProperty): any => {
  if (property.type === FormPropertyType.array) {
    return [];
  } else if (property.type === FormPropertyType.fieldset) {
    const propertyValue: {[id: string]: any} = {};
    for (const childProperty of property.properties) {
      if (childProperty.type !== FormPropertyType.htmlSection) {
        propertyValue[childProperty.id] = defaultFormPropertyValue(childProperty);
      }
    }
    return propertyValue;
  } else {
    return property.default;
  }
}

export const propertyValid = (property: FormProperty): boolean =>
  !(!property.id || !property.name || !property.type || (property.type === FormPropertyType.array && !property.arrayItemType));

export const defaultPropertyValue = (type: FormPropertyType): any => {
  switch (type) {
    case FormPropertyType.text:
    case FormPropertyType.textarea:
    case FormPropertyType.password:
    case FormPropertyType.javascript:
    case FormPropertyType.json:
    case FormPropertyType.html:
    case FormPropertyType.css:
    case FormPropertyType.markdown:
      return '';
    case FormPropertyType.number:
      return 0;
    case FormPropertyType.switch:
      return false;
    case FormPropertyType.color:
      return '#000';
    case FormPropertyType.color_settings:
      return constantColor('#000');
    case FormPropertyType.font:
      return {
        size: 12,
        sizeUnit: 'px',
        family: 'Roboto',
        weight: 'normal',
        style: 'normal',
        lineHeight: '1'
      } as Font;
    case FormPropertyType.units:
      return '';
    case FormPropertyType.icon:
      return 'star';
    case FormPropertyType.fieldset:
    case FormPropertyType.array:
    case FormPropertyType.select:
    case FormPropertyType.radios:
    case FormPropertyType.datetime:
    case FormPropertyType.htmlSection:
    case FormPropertyType.image:
      return null;
  }
};

export const formPropertyCompletions = (properties: FormProperty[], customTranslate: CustomTranslatePipe): TbEditorCompletions => {
  const propertiesCompletions: TbEditorCompletions = {};
  for (const property of properties) {
    if (property.type !== FormPropertyType.htmlSection) {
      propertiesCompletions[property.id] = formPropertyCompletion(property, customTranslate);
    }
  }
  return propertiesCompletions;
}

export const formPropertyCompletion = (property: FormProperty, customTranslate: CustomTranslatePipe): TbEditorCompletion => {
  let description = customTranslate.transform(property.name, property.name);
  if (property.subLabel) {
    description += ` <small>${customTranslate.transform(property.subLabel, property.subLabel)}</small>`;
  }
  const isArray = property.type === FormPropertyType.array;
  const type = isArray ? property.arrayItemType : property.type;
  if (type === FormPropertyType.select) {
    if (property.multiple || isArray) {
      description += '<br/><br/><code class="title"><b>Possible values of array element:</b></code>';
    } else {
      description += '<br/><br/><code class="title"><b>Possible values:</b></code>';
    }
    description += `<ul>${property.items.map(item => `<li>${item.value} <code style="font-size: 11px;">${typeof item.value}</code></li>`).join('\n')}</ul>`;
  }
  if (type === FormPropertyType.datetime) {
    if (isArray) {
      description += '<br/><br/><code class="title">Stores array of time values in milliseconds since midnight, January 1, 1970 UTC.</code>';
    } else {
      description += '<br/><br/><code class="title">Stores time value in milliseconds since midnight, January 1, 1970 UTC.</code>';
    }
  }
  const completion: TbEditorCompletion = {
    meta: 'property',
    description,
    type: formPropertyCompletionType(property)
  };
  if (type === FormPropertyType.fieldset && !isArray) {
    completion.children = {};
    for (const childProperty of property.properties) {
      if (childProperty.type !== FormPropertyType.htmlSection) {
        completion.children[childProperty.id] = formPropertyCompletion(childProperty, customTranslate);
      }
    }
  }
  return completion;
};

const formPropertyCompletionType = (property: FormProperty): string => {
  const isArray = property.type === FormPropertyType.array;
  const type = isArray ? property.arrayItemType : property.type;
  let typeStr: string;
  switch (type) {
    case FormPropertyType.text:
    case FormPropertyType.password:
    case FormPropertyType.textarea:
      typeStr = 'string';
      break;
    case FormPropertyType.number:
      typeStr = 'number';
      break;
    case FormPropertyType.switch:
      typeStr = 'boolean';
      break;
    case FormPropertyType.datetime:
      typeStr = 'number';
      break;
    case FormPropertyType.image:
      typeStr = 'image URL string';
      break;
    case FormPropertyType.select:
    case FormPropertyType.radios:
      const items = property.items || [];
      const types: string[] = [];
      items.forEach(item => {
        const type = typeof item.value;
        if (!types.includes(type)) {
          types.push(type);
        }
      });
      const typesString = types.length ? types.join(' | ') : 'string';
      if (property.type === FormPropertyType.select && property.multiple) {
        typeStr = `Array&lt;${typesString}&gt;`;
      } else {
        typeStr = typesString;
      }
      break;
    case FormPropertyType.color:
      typeStr = 'color string';
      break;
    case FormPropertyType.color_settings:
      typeStr = 'ColorProcessor';
      break;
    case FormPropertyType.font:
      typeStr = 'Font';
      break;
    case FormPropertyType.units:
      typeStr = 'units string';
      break;
    case FormPropertyType.icon:
      typeStr = 'icon string';
      break;
    case FormPropertyType.fieldset:
      typeStr = 'object';
      break;
    case FormPropertyType.javascript:
      typeStr = 'JavaScript function body string';
      break;
    case FormPropertyType.json:
      typeStr = 'JSON string';
      break;
    case FormPropertyType.html:
      typeStr = 'HTML string';
      break;
    case FormPropertyType.css:
      typeStr = 'CSS string';
      break;
    case FormPropertyType.markdown:
      typeStr = 'Markdown string';
      break;
    default:
      typeStr = 'unknown';
      break;
  }
  if (isArray) {
    typeStr = `Array&lt;${typeStr}&gt;`;
  }
  return typeStr;
};


export const jsonFormSchemaToFormProperties = (rawSchema: string | any) : FormProperty[] => {
  try {
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
  } catch (e) {
    console.warn('Failed to convert old JSON form schema to form properties:', e);
    return [];
  }
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
    const id = form.key[level] + '';
    let property: FormProperty = {
      id,
      name: form.title || id,
      group: groupTitle,
      type: null,
      default: (isDefinedAndNotNull(form.default) ? form.default : form.schema?.default) || null,
      required: isDefinedAndNotNull(form.required) ? form.required : false
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
      case 'password':
        property.type = FormPropertyType.password;
        property.fieldClass = 'flex';
        break;
      case 'textarea':
        property.type = FormPropertyType.textarea;
        property.rows = form.rows || form.rowsMax || 2;
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
        property.allowEmptyOption = isDefinedAndNotNull(form.allowClear) ? form.allowClear : false;
        if (property.multiple) {
          if (typeof (form.schema as any)?.minItems === 'number') {
            property.minItems = (form.schema as any).minItems;
          }
          if (typeof (form.schema as any)?.maxItems === 'number') {
            property.maxItems = (form.schema as any).maxItems;
          }
        }
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
        property.allowEmptyOption = false;
        break;
      case 'radios':
        property.type = FormPropertyType.radios;
        if (form.titleMap?.length) {
          property.items = form.titleMap.map(item => ({value: item.value, label: item.name}));
        } else {
          property.items = [];
        }
        property.direction = form.direction === 'row' ? 'row' : 'column';
        break;
      case 'date':
        property.type = FormPropertyType.datetime;
        property.dateTimeType = 'date';
        property.fieldClass = 'flex';
        property.allowClear = true;
        break;
      case 'image':
        property.type = FormPropertyType.image;
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
      case 'javascript':
        property.type = FormPropertyType.javascript;
        property.helpId = form.helpId;
        break;
      case 'json':
        property.type = FormPropertyType.json;
        break;
      case 'html':
        property.type = FormPropertyType.html;
        break;
      case 'css':
        property.type = FormPropertyType.css;
        break;
      case 'markdown':
        property.type = FormPropertyType.markdown;
        break;
      case 'help':
        property.type = FormPropertyType.htmlSection;
        property.htmlContent = form.description || '';
        property.htmlClassList = form.htmlClass ? form.htmlClass.split(' ') : [];
        break;
      case 'array':
        if (form.items?.length) {
          const arrayItemSchema = form.schema.items;
          if (arrayItemSchema && arrayItemSchema.type && arrayItemSchema.type !== 'array') {
            if (arrayItemSchema.type === 'object') {
              property.arrayItemType = FormPropertyType.fieldset;
              property.arrayItemName = '';
              property.properties = form.items ? (form.items as JsonFormData[]).map(item =>
                jsonFormDataToProperty(item, level+2)).filter(p => p !== null) : [];
            } else {
              const item: JsonFormData = form.items[0] as JsonFormData;
              const arrayProperty = jsonFormDataToProperty(item, 0);
              arrayProperty.arrayItemType = arrayProperty.type;
              arrayProperty.arrayItemName = arrayProperty.name;
              arrayProperty.id = property.id;
              arrayProperty.name = property.name;
              arrayProperty.group = property.group;
              arrayProperty.condition = property.condition;
              arrayProperty.required = property.required;
              property = arrayProperty;
            }
            property.type = FormPropertyType.array;
          }
        }
        break;
    }
    if (!property.type) {
      return null;
    }
    return property;
  }
  return null;
}
