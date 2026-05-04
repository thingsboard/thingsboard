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

import { TbFunction } from '@shared/models/js-function.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { TbEditorCompleter, TbEditorCompletions } from '@shared/models/ace/completion.models';
import { widgetContextCompletions } from '@shared/models/ace/widget-completion.models';
import { WidgetResource } from '@shared/models/widget.models';

export enum HtmlContainerWidgetType {
  PLAIN = 'PLAIN',
  ANGULAR = 'ANGULAR'
}

export interface HtmlContainerWidgetSettings {
  type: HtmlContainerWidgetType;
  html: string;
  css: string;
  js: TbFunction;
  resources: WidgetResource[];
}

export const htmlContainerDefaultSettings: HtmlContainerWidgetSettings = {
  type: HtmlContainerWidgetType.PLAIN,
  html: '',
  css: '',
  js: '',
  resources: [],
};

export type WidgetContainerPlainFunction = (ctx: WidgetContext, container: HTMLElement) => void;
export type WidgetContainerAngularFunction = (ctx: WidgetContext) => void;

const containerFunctionCompletions: TbEditorCompletions = {
  ...{
    ctx: {
      meta: 'argument',
      type: widgetContextCompletions.ctx.type,
      description: widgetContextCompletions.ctx.description,
      children: widgetContextCompletions.ctx.children
    },
    container: {
      meta: 'argument',
      type: 'HTMLElement',
      description: 'Container element of the widget'
    },
  }
};

export const ContainerFunctionEditorCompleter = new TbEditorCompleter(containerFunctionCompletions);
