// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
export interface FormOption {
  formDefaults?: {
    startEmpty?: boolean;
    readonly?: boolean;
  };
  supressPropertyTitles?: boolean;
}

export interface DefaultsFormOptions {
  global?: FormOption;
  required?: boolean;
  path?: string[];
  lookup?: {[key: string]: any};
  ignore?: {[key: string]: boolean};
}

export interface KeyLabelItem {
  key: string;
  label: string;
  value?: string;
}

export interface JsonSchemaData {
  type: string;
  default: any;
  items?: JsonSchemaData;
  properties?: any;
}

export interface JsonFormData {
  type: string;
  key: (string | number)[];
  title: string;
  readonly: boolean;
  required: boolean;
  default?: any;
  condition?: string;
  conditionFunction?: Function;
  style?: any;
  rows?: number;
  rowsMax?: number;
  placeholder?: string;
  schema: JsonSchemaData;
  titleMap: {
    value: any;
    name: string;
  }[];
  items?: Array<KeyLabelItem> | Array<JsonFormData>;
  tabs?: Array<JsonFormData>;
  tags?: any;
  helpId?: string;
  startEmpty?: boolean;
  [key: string]: any;
}

export interface GroupInfo {
  formIndex: number;
  GroupTitle: string;
}

export interface JsonSchema {
  type: string;
  title?: string;
  properties: {[key: string]: any};
  required?: string[];
}

export interface JsonSettingsSchema {
  schema?: JsonSchema;
  form?: any[];
  groupInfoes?: GroupInfo[];
}
