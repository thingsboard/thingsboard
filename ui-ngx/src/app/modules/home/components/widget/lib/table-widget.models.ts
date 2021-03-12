///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { EntityId } from '@shared/models/id/entity-id';
import { DataKey, WidgetConfig } from '@shared/models/widget.models';
import { getDescendantProp, isDefined } from '@core/utils';
import { AlarmDataInfo, alarmFields } from '@shared/models/alarm.models';
import * as tinycolor_ from 'tinycolor2';
import { Direction, EntityDataSortOrder, EntityKey } from '@shared/models/query/query.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';

const tinycolor = tinycolor_;

type ColumnVisibilityOptions = 'visible' | 'hidden';

type ColumnSelectionOptions = 'enabled' | 'disabled';

export interface TableWidgetSettings {
  enableSearch: boolean;
  enableStickyAction: boolean;
  enableStickyHeader: boolean;
  displayPagination: boolean;
  defaultPageSize: number;
  useRowStyleFunction: boolean;
  rowStyleFunction?: string;
}

export interface TableWidgetDataKeySettings {
  columnWidth?: string;
  useCellStyleFunction: boolean;
  cellStyleFunction?: string;
  useCellContentFunction: boolean;
  cellContentFunction?: string;
  defaultColumnVisibility: ColumnVisibilityOptions;
  columnSelectionToDisplay: ColumnSelectionOptions;
}

export interface EntityData {
  id: EntityId;
  entityName: string;
  entityLabel?: string;
  entityType?: string;
  [key: string]: any;
}

export interface EntityColumn extends DataKey {
  def: string;
  title: string;
  entityKey?: EntityKey;
}

export interface DisplayColumn {
  title: string;
  def: string;
  display: boolean;
  selectable: boolean;
}

export type CellContentFunction = (...args: any[]) => string;

export interface CellContentInfo {
  useCellContentFunction: boolean;
  cellContentFunction?: CellContentFunction;
  units?: string;
  decimals?: number;
}

export type CellStyleFunction = (...args: any[]) => any;

export interface CellStyleInfo {
  useCellStyleFunction: boolean;
  cellStyleFunction?: CellStyleFunction;
}

export type RowStyleFunction = (...args: any[]) => any;

export interface RowStyleInfo {
  useRowStyleFunction: boolean;
  rowStyleFunction?: RowStyleFunction;
}


export function entityDataSortOrderFromString(strSortOrder: string, columns: EntityColumn[]): EntityDataSortOrder {
  if (!strSortOrder && !strSortOrder.length) {
    return null;
  }
  let property: string;
  let direction = Direction.ASC;
  if (strSortOrder.startsWith('-')) {
    direction = Direction.DESC;
    property = strSortOrder.substring(1);
  } else {
    if (strSortOrder.startsWith('+')) {
      property = strSortOrder.substring(1);
    } else {
      property = strSortOrder;
    }
  }
  if (!property && !property.length) {
    return null;
  }
  let column = findColumnByLabel(property, columns);
  if (!column) {
    column = findColumnByName(property, columns);
  }
  if (column && column.entityKey) {
    return {key: column.entityKey, direction};
  }
  return null;
}

export function findColumnByEntityKey(key: EntityKey, columns: EntityColumn[]): EntityColumn {
  if (key) {
    return columns.find(theColumn => theColumn.entityKey &&
      theColumn.entityKey.type === key.type && theColumn.entityKey.key === key.key);
  } else {
    return null;
  }
}

export function findEntityKeyByColumnDef(def: string, columns: EntityColumn[]): EntityKey {
  if (def) {
    const column = findColumnByDef(def, columns);
    return column ? column.entityKey : null;
  } else {
    return null;
  }
}

export function findColumn(searchProperty: string, searchValue: string, columns: EntityColumn[]): EntityColumn {
  return columns.find(theColumn => theColumn[searchProperty] === searchValue);
}

export function findColumnByName(name: string, columns: EntityColumn[]): EntityColumn {
  return findColumn('name', name, columns);
}

export function findColumnByLabel(label: string, columns: EntityColumn[]): EntityColumn {
  let column: EntityColumn;
  const alarmColumns = columns.filter(c => c.type === DataKeyType.alarm);
  if (alarmColumns.length) {
    column = findColumn('name', label, alarmColumns);
  }
  if (!column) {
    column = findColumn('label', label, columns);
  }
  return column;
}

export function findColumnByDef(def: string, columns: EntityColumn[]): EntityColumn {
  return findColumn('def', def, columns);
}

export function findColumnProperty(searchProperty: string, searchValue: string, columnProperty: string, columns: EntityColumn[]): string {
  let res = searchValue;
  const column = columns.find(theColumn => theColumn[searchProperty] === searchValue);
  if (column) {
    res = column[columnProperty];
  }
  return res;
}

export function toEntityKey(def: string, columns: EntityColumn[]): string {
  return findColumnProperty('def', def, 'label', columns);
}

export function toEntityColumnDef(label: string, columns: EntityColumn[]): string {
  return findColumnProperty('label', label, 'def', columns);
}

export function fromEntityColumnDef(def: string, columns: EntityColumn[]): string {
  return findColumnProperty('def', def, 'label', columns);
}

export function toAlarmColumnDef(name: string, columns: EntityColumn[]): string {
  return findColumnProperty('name', name, 'def', columns);
}

export function fromAlarmColumnDef(def: string, columns: EntityColumn[]): string {
  return findColumnProperty('def', def, 'name', columns);
}

export function getEntityValue(entity: any, key: DataKey): any {
  return getDescendantProp(entity, key.label);
}

export function getAlarmValue(alarm: AlarmDataInfo, key: EntityColumn) {
  let alarmField = null;
  if (key.type === DataKeyType.alarm) {
    alarmField = alarmFields[key.name]?.value;
    if (!alarmField && key.name.startsWith('details.')) {
      alarmField = key.name;
    }
  }
  if (alarmField) {
    return getDescendantProp(alarm, alarmField);
  } else {
    return getDescendantProp(alarm, key.label);
  }
}

export function getRowStyleInfo(settings: TableWidgetSettings, ...args: string[]): RowStyleInfo {
  let rowStyleFunction: RowStyleFunction = null;
  let useRowStyleFunction = false;

  if (settings.useRowStyleFunction === true) {
    if (isDefined(settings.rowStyleFunction) && settings.rowStyleFunction.length > 0) {
      try {
        rowStyleFunction = new Function(...args, settings.rowStyleFunction) as RowStyleFunction;
        useRowStyleFunction = true;
      } catch (e) {
        rowStyleFunction = null;
        useRowStyleFunction = false;
      }
    }
  }
  return {
    useRowStyleFunction,
    rowStyleFunction
  };
}

export function getCellStyleInfo(keySettings: TableWidgetDataKeySettings, ...args: string[]): CellStyleInfo {
  let cellStyleFunction: CellStyleFunction = null;
  let useCellStyleFunction = false;

  if (keySettings.useCellStyleFunction === true) {
    if (isDefined(keySettings.cellStyleFunction) && keySettings.cellStyleFunction.length > 0) {
      try {
        cellStyleFunction = new Function(...args, keySettings.cellStyleFunction) as CellStyleFunction;
        useCellStyleFunction = true;
      } catch (e) {
        cellStyleFunction = null;
        useCellStyleFunction = false;
      }
    }
  }
  return {
    useCellStyleFunction,
    cellStyleFunction
  };
}

export function getCellContentInfo(keySettings: TableWidgetDataKeySettings, ...args: string[]): CellContentInfo {
  let cellContentFunction: CellContentFunction = null;
  let useCellContentFunction = false;

  if (keySettings.useCellContentFunction === true) {
    if (isDefined(keySettings.cellContentFunction) && keySettings.cellContentFunction.length > 0) {
      try {
        cellContentFunction = new Function(...args, keySettings.cellContentFunction) as CellContentFunction;
        useCellContentFunction = true;
      } catch (e) {
        cellContentFunction = null;
        useCellContentFunction = false;
      }
    }
  }
  return {
    cellContentFunction,
    useCellContentFunction
  };
}

export function getColumnWidth(keySettings: TableWidgetDataKeySettings): string {
  return isDefined(keySettings.columnWidth) ? keySettings.columnWidth : '0px';
}

export function widthStyle(width: string): any {
  const widthStyleObj: any = {width};
  if (width !== '0px') {
    widthStyleObj.minWidth = width;
    widthStyleObj.maxWidth = width;
  }
  return widthStyleObj;
}

export function getColumnDefaultVisibility(keySettings: TableWidgetDataKeySettings): boolean {
  return !(isDefined(keySettings.defaultColumnVisibility) && keySettings.defaultColumnVisibility === 'hidden');
}

export function getColumnSelectionAvailability(keySettings: TableWidgetDataKeySettings): boolean {
  return !(isDefined(keySettings.columnSelectionToDisplay) && keySettings.columnSelectionToDisplay === 'disabled');
}


export function constructTableCssString(widgetConfig: WidgetConfig): string {
  const origColor = widgetConfig.color || 'rgba(0, 0, 0, 0.87)';
  const origBackgroundColor = widgetConfig.backgroundColor || 'rgb(255, 255, 255)';
  const currentEntityColor = 'rgba(221, 221, 221, 0.65)';
  const currentEntityStickyColor = tinycolor.mix(origBackgroundColor,
    tinycolor(currentEntityColor).setAlpha(1),  65).toRgbString();
  const selectedColor = 'rgba(221, 221, 221, 0.5)';
  const selectedStickyColor = tinycolor.mix(origBackgroundColor,
    tinycolor(selectedColor).setAlpha(1),  50).toRgbString();
  const hoverColor = 'rgba(221, 221, 221, 0.3)';
  const hoverStickyColor = tinycolor.mix(origBackgroundColor,
    tinycolor(hoverColor).setAlpha(1),  30).toRgbString();
  const defaultColor = tinycolor(origColor);
  const mdDark = defaultColor.setAlpha(0.87).toRgbString();
  const mdDarkSecondary = defaultColor.setAlpha(0.54).toRgbString();
  const mdDarkDisabled = defaultColor.setAlpha(0.26).toRgbString();
  const mdDarkDisabled2 = defaultColor.setAlpha(0.38).toRgbString();
  const mdDarkDivider = defaultColor.setAlpha(0.12).toRgbString();

  const cssString =
    '.mat-input-element::placeholder {\n' +
    '   color: ' + mdDarkSecondary + ';\n' +
    '}\n' +
    '.mat-input-element::-moz-placeholder {\n' +
    '   color: ' + mdDarkSecondary + ';\n' +
    '}\n' +
    '.mat-input-element::-webkit-input-placeholder {\n' +
    '   color: ' + mdDarkSecondary + ';\n' +
    '}\n' +
    '.mat-input-element:-ms-input-placeholder {\n' +
    '   color: ' + mdDarkSecondary + ';\n' +
    '}\n' +
    'mat-toolbar.mat-table-toolbar {\n' +
    'color: ' + mdDark + ';\n' +
    '}\n' +
    'mat-toolbar.mat-table-toolbar:not([color="primary"]) button.mat-icon-button mat-icon {\n' +
    'color: ' + mdDarkSecondary + ';\n' +
    '}\n' +
    '.mat-tab-label {\n' +
    'color: ' + mdDark + ';\n' +
    '}\n' +
    '.mat-tab-header-pagination-chevron {\n' +
    'border-color: ' + mdDark + ';\n' +
    '}\n' +
    '.mat-tab-header-pagination-disabled .mat-tab-header-pagination-chevron {\n' +
    'border-color: ' + mdDarkDisabled2 + ';\n' +
    '}\n' +
    '.mat-table .mat-header-row {\n' +
    'background-color: ' + origBackgroundColor + ';\n' +
    '}\n' +
    '.mat-table .mat-header-cell {\n' +
    'color: ' + mdDarkSecondary + ';\n' +
    '}\n' +
    '.mat-table .mat-header-cell .mat-sort-header-arrow {\n' +
    'color: ' + mdDarkDisabled + ';\n' +
    '}\n' +
    '.mat-table .mat-cell, .mat-table .mat-header-cell {\n' +
    'border-bottom-color: ' + mdDarkDivider + ';\n' +
    '}\n' +
    '.mat-table .mat-cell .mat-checkbox-frame, .mat-table .mat-header-cell .mat-checkbox-frame {\n' +
    'border-color: ' + mdDarkSecondary + ';\n' +
    '}\n' +
    '.mat-table .mat-row .mat-cell.mat-table-sticky {\n' +
    'transition: background-color .2s;\n' +
    '}\n' +
    '.mat-table .mat-row.tb-current-entity {\n' +
    'background-color: ' + currentEntityColor + ';\n' +
    '}\n' +
    '.mat-table .mat-row.tb-current-entity .mat-cell.mat-table-sticky {\n' +
    'background-color: ' + currentEntityStickyColor + ';\n' +
    '}\n' +
    '.mat-table .mat-row:hover:not(.tb-current-entity) {\n' +
    'background-color: ' + hoverColor + ';\n' +
    '}\n' +
    '.mat-table .mat-row:hover:not(.tb-current-entity) .mat-cell.mat-table-sticky {\n' +
    'background-color: ' + hoverStickyColor + ';\n' +
    '}\n' +
    '.mat-table .mat-row.mat-row-select.mat-selected:not(.tb-current-entity) {\n' +
    'background-color: ' + selectedColor + ';\n' +
    '}\n' +
    '.mat-table .mat-row.mat-row-select.mat-selected:not(.tb-current-entity) .mat-cell.mat-table-sticky {\n' +
    'background-color: ' + selectedStickyColor + ';\n' +
    '}\n' +
    '.mat-table .mat-row .mat-cell.mat-table-sticky, .mat-table .mat-header-cell.mat-table-sticky {\n' +
    'background-color: ' + origBackgroundColor + ';\n' +
    '}\n' +
    '.mat-table .mat-cell {\n' +
    'color: ' + mdDark + ';\n' +
    '}\n' +
    '.mat-table .mat-cell button.mat-icon-button mat-icon {\n' +
    'color: ' + mdDarkSecondary + ';\n' +
    '}\n' +
    '.mat-table .mat-cell button.mat-icon-button[disabled][disabled] mat-icon {\n' +
    'color: ' + mdDarkDisabled + ';\n' +
    '}\n' +
    '.mat-divider {\n' +
    'border-top-color: ' + mdDarkDivider + ';\n' +
    '}\n' +
    '.mat-paginator {\n' +
    'color: ' + mdDarkSecondary + ';\n' +
    '}\n' +
    '.mat-paginator button.mat-icon-button {\n' +
    'color: ' + mdDarkSecondary + ';\n' +
    '}\n' +
    '.mat-paginator button.mat-icon-button[disabled][disabled] {\n' +
    'color: ' + mdDarkDisabled + ';\n' +
    '}\n' +
    '.mat-paginator .mat-select-value {\n' +
    'color: ' + mdDarkSecondary + ';\n' +
    '}';
  return cssString;
}
