// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { TbFunction } from '@shared/models/js-function.models';
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
