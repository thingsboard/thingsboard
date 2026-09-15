// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    }
  }
};

export const AngularContainerFunctionEditorCompleter = new TbEditorCompleter(containerFunctionCompletions);

export const HTMLContainerFunctionEditorCompleter = new TbEditorCompleter(
  {...containerFunctionCompletions,
    container: {
      meta: 'argument',
      type: 'HTMLElement',
      description: 'Container element of the widget'
    }}
);

