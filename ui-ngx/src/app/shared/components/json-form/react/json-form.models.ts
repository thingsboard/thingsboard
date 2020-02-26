///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import * as tinycolor_ from 'tinycolor2';
import { GroupInfo } from '@shared/models/widget.models';

const tinycolor = tinycolor_;

export interface SchemaValidationResult {
  valid: boolean;
  error?: {
    message?: string;
  };
}

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

export type onChangeFn = (key: (string | number)[], val: any, forceUpdate?: boolean) => void;
export type OnColorClickFn = (key: (string | number)[], val: tinycolor.ColorFormats.RGBA,
                              colorSelectedFn: (color: tinycolor.ColorFormats.RGBA) => void) => void;
export type OnIconClickFn = (key: (string | number)[], val: string,
                              iconSelectedFn: (icon: string) => void) => void;
export type onToggleFullscreenFn = (element: HTMLElement, fullscreenFinishFn?: () => void) => void;

export interface JsonFormProps {
  model?: any;
  schema?: any;
  form?: any;
  groupInfoes?: GroupInfo[];
  isFullscreen: boolean;
  ignore?: {[key: string]: boolean};
  option: FormOption;
  onModelChange?: onChangeFn;
  onColorClick?: OnColorClickFn;
  onIconClick?: OnIconClickFn;
  onToggleFullscreen?: onToggleFullscreenFn;
  mapper?: {[type: string]: any};
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
  startEmpty?: boolean;
  [key: string]: any;
}

export type ComponentBuilderFn = (form: JsonFormData,
                                  model: any,
                                  index: number,
                                  onChange: onChangeFn,
                                  onColorClick: OnColorClickFn,
                                  onIconClick: OnIconClickFn,
                                  onToggleFullscreen: onToggleFullscreenFn,
                                  mapper: {[type: string]: any}) => JSX.Element;

export interface JsonFormFieldProps {
  value: any;
  model: any;
  form: JsonFormData;
  builder: ComponentBuilderFn;
  mapper?: {[type: string]: any};
  onChange?: onChangeFn;
  onColorClick?: OnColorClickFn;
  onIconClick?: OnIconClickFn;
  onChangeValidate?: (e: any, forceUpdate?: boolean) => void;
  onToggleFullscreen?: onToggleFullscreenFn;
  valid?: boolean;
  error?: string;
  options?: {
    setSchemaDefaults?: boolean;
  };
}

export interface JsonFormFieldState {
  value?: any;
  valid?: boolean;
  error?: string;
}
