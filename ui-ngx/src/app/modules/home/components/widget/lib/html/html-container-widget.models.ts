// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { WidgetContext } from '@home/models/widget-component.models';
import { TbEditorCompleter, TbEditorCompletions } from '@shared/models/ace/completion.models';
import { widgetContextCompletions } from '@shared/models/ace/widget-completion.models';

// Moved to @shared/models/html-container.models (the HTML page layout uses them too); re-exported for existing imports.
export { HtmlContainerWidgetType, htmlContainerDefaultSettings } from '@shared/models/html-container.models';
export type { HtmlContainerWidgetSettings } from '@shared/models/html-container.models';

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

