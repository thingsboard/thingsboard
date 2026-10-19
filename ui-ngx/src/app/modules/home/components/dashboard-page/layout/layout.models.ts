// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { TbContextMenuEvent } from '@shared/models/jquery-event.models';

export interface ILayoutController {
  reload();
  resetHighlight();
  highlightWidget(widgetId: string, delay?: number);
  selectWidget(widgetId: string, delay?: number);
  pasteWidget($event: TbContextMenuEvent | KeyboardEvent);
  pasteWidgetReference($event: TbContextMenuEvent | KeyboardEvent);
}

export enum LayoutWidthType {
  PERCENTAGE = 'percentage',
  FIXED = 'fixed'
}

export enum LayoutPercentageSize {
  MIN = 10,
  MAX = 90
}

export enum LayoutFixedSize {
  MIN = 150,
  MAX = 4000
}

