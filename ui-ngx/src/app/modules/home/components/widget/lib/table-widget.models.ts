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

import { EntityId } from '@shared/models/id/entity-id';
import { DataKey, FormattedData, WidgetActionDescriptor, WidgetConfig } from '@shared/models/widget.models';
import { getDescendantProp, isDefined, isNotEmptyStr } from '@core/utils';
import { AlarmDataInfo, alarmFields } from '@shared/models/alarm.models';
import tinycolor from 'tinycolor2';
import { Direction } from '@shared/models/page/sort-order';
import { EntityDataSortOrder, EntityKey } from '@shared/models/query/query.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { EntityType } from '@shared/models/entity-type.models';
import {
  CompiledTbFunction,
  compileTbFunction,
  isNotEmptyTbFunction,
  TbFunction
} from '@shared/models/js-function.models';
import { forkJoin, Observable, of, ReplaySubject } from 'rxjs';
import { catchError, map, share } from 'rxjs/operators';
import type { ValueFormatProcessor } from '@shared/models/widget-settings.models';

type ColumnVisibilityOptions = 'visible' | 'hidden' | 'hidden-mobile';

type ColumnSelectionOptions = 'enabled' | 'disabled';

export interface TableWidgetSettings {
  enableSearch: boolean;
  enableSelectColumnDisplay: boolean;
  enableStickyAction: boolean;
  showCellActionsMenu: boolean;
  enableStickyHeader: boolean;
  displayPagination: boolean;
  defaultPageSize: number;
  pageStepIncrement: number;
  pageStepCount: number;
  useRowStyleFunction: boolean;
  rowStyleFunction?: TbFunction;
  reserveSpaceForHiddenAction?: boolean;
}

export interface TableWidgetDataKeySettings {
  customTitle?: string;
  columnWidth?: string;
  useCellStyleFunction: boolean;
  cellStyleFunction?: TbFunction;
  useCellContentFunction: boolean;
  cellContentFunction?: TbFunction;
  defaultColumnVisibility?: ColumnVisibilityOptions;
  columnSelectionToDisplay?: ColumnSelectionOptions;
  disableSorting?: boolean;
}

export type ShowCellButtonActionFunction = (ctx: WidgetContext, data: EntityData | AlarmDataInfo | FormattedData) => boolean;

export interface TableCellButtonActionDescriptor extends  WidgetActionDescriptor {
  useShowActionCellButtonFunction: boolean;
  showActionCellButtonFunction: CompiledTbFunction<ShowCellButtonActionFunction>;
}

export interface EntityData {
  id: EntityId;
  entityName: string;
  entityLabel?: string;
  entityType?: EntityType;
  actionCellButtons?: TableCellButtonActionDescriptor[];
  hasActions?: boolean;
  [key: string]: any;
}

export interface EntityColumn extends DataKey {
  def: string;
  title: string;
  sortable: boolean;
  entityKey?: EntityKey;
}

export interface DisplayColumn {
  title: string;
  def: string;
  display: boolean;
  selectable: boolean;
}

export type CellContentFunction = (...args: any[]) => string;

export interface CellContentFunctionInfo {
  useCellContentFunction: boolean;
  cellContentFunction?: CompiledTbFunction<CellContentFunction>;
}

export interface CellContentInfo {
  contentFunction: Observable<CellContentFunctionInfo>;
  valueFormat: ValueFormatProcessor
}

export type CellStyleFunction = (...args: any[]) => any;

export interface CellStyleInfo {
  useCellStyleFunction: boolean;
  cellStyleFunction?: CompiledTbFunction<CellStyleFunction>;
}

export type RowStyleFunction = (...args: any[]) => any;

export interface RowStyleInfo {
  useRowStyleFunction: boolean;
  rowStyleFunction?: CompiledTbFunction<RowStyleFunction>;
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
  if (column && column.entityKey && column.sortable) {
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

export function getRowStyleInfo(widgetContext: WidgetContext, settings: TableWidgetSettings, ...args: string[]): Observable<RowStyleInfo> {
  let rowStyleInfo$: Observable<RowStyleInfo>;
  if (settings.useRowStyleFunction === true && isNotEmptyTbFunction(settings.rowStyleFunction)) {
    rowStyleInfo$ = compileTbFunction<RowStyleFunction>(widgetContext.http, settings.rowStyleFunction, ...args).pipe(
      catchError(() => { return of(null) }),
      map((rowStyleFunction) => {
        if (!rowStyleFunction) {
          return {
            useRowStyleFunction: false,
            rowStyleFunction: null
          }
        } else {
          return {
            useRowStyleFunction: true,
            rowStyleFunction
          }
        }
      })
    );
  } else {
    rowStyleInfo$ = of({
      useRowStyleFunction: false,
      rowStyleFunction: null
    });
  }
  return rowStyleInfo$.pipe(
    share({
      connector: () => new ReplaySubject(1),
      resetOnError: false,
      resetOnComplete: false,
      resetOnRefCountZero: false
    })
  );
}

export function getCellStyleInfo(widgetContext: WidgetContext, keySettings: TableWidgetDataKeySettings, ...args: string[]): Observable<CellStyleInfo> {
  let cellStyleInfo$: Observable<CellStyleInfo>;
  if (keySettings.useCellStyleFunction === true && isNotEmptyTbFunction(keySettings.cellStyleFunction)) {
    cellStyleInfo$ = compileTbFunction<CellStyleFunction>(widgetContext.http, keySettings.cellStyleFunction, ...args).pipe(
      catchError(() => { return of(null) }),
      map((cellStyleFunction) => {
        if (!cellStyleFunction) {
          return {
            useCellStyleFunction: false,
            cellStyleFunction: null
          }
        } else {
          return {
            useCellStyleFunction: true,
            cellStyleFunction
          }
        }
      })
    );
  } else {
    cellStyleInfo$ = of(
      {
        useCellStyleFunction: false,
        cellStyleFunction: null
      }
    )
  }
  return cellStyleInfo$.pipe(
    share({
      connector: () => new ReplaySubject(1),
      resetOnError: false,
      resetOnComplete: false,
      resetOnRefCountZero: false
    })
  );
}

export function getCellContentFunctionInfo(widgetContext: WidgetContext, keySettings: TableWidgetDataKeySettings, ...args: string[]): Observable<CellContentFunctionInfo> {
  let cellContentFunctionInfo$: Observable<CellContentFunctionInfo>;
  if (keySettings.useCellContentFunction === true && isNotEmptyTbFunction(keySettings.cellContentFunction)) {
    cellContentFunctionInfo$ = compileTbFunction<CellContentFunction>(widgetContext.http, keySettings.cellContentFunction, ...args).pipe(
      catchError(() => { return of(null) }),
      map((cellContentFunction) => {
        if (!cellContentFunction) {
          return {
            useCellContentFunction: false,
            cellContentFunction: null
          }
        } else {
          return {
            useCellContentFunction: true,
            cellContentFunction
          }
        }
      })
    );
  } else {
    cellContentFunctionInfo$ = of(
      {
        useCellContentFunction: false,
        cellContentFunction: null
      }
    )
  }
  return cellContentFunctionInfo$.pipe(
    share({
      connector: () => new ReplaySubject(1),
      resetOnError: false,
      resetOnComplete: false,
      resetOnRefCountZero: false
    })
  );
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

export function getColumnDefaultVisibility(keySettings: TableWidgetDataKeySettings, ctx?: WidgetContext): boolean {
  return !(isDefined(keySettings.defaultColumnVisibility) && (keySettings.defaultColumnVisibility === 'hidden' ||
      (ctx && ctx.isMobile && keySettings.defaultColumnVisibility === 'hidden-mobile')));
}

export function getColumnSelectionAvailability(keySettings: TableWidgetDataKeySettings): boolean {
  return !(isDefined(keySettings.columnSelectionToDisplay) && keySettings.columnSelectionToDisplay === 'disabled');
}

export function getTableCellButtonActions(widgetContext: WidgetContext): Observable<TableCellButtonActionDescriptor[]> {
  const actions$ = widgetContext.actionsApi.getActionDescriptors('actionCellButton').map(descriptor => {
    let useShowActionCellButtonFunction = descriptor.useShowWidgetActionFunction || false;
    let showActionCellButtonFunction$: Observable<CompiledTbFunction<ShowCellButtonActionFunction>>;
    if (useShowActionCellButtonFunction && isNotEmptyTbFunction(descriptor.showWidgetActionFunction)) {
      showActionCellButtonFunction$ = compileTbFunction(widgetContext.http, descriptor.showWidgetActionFunction, 'widgetContext', 'data');
    } else {
      showActionCellButtonFunction$ = of(null);
    }
    return showActionCellButtonFunction$.pipe(
      catchError(() => { return of(null) }),
      map(showActionCellButtonFunction => {
        if (!showActionCellButtonFunction) {
          useShowActionCellButtonFunction = false;
        }
        return {...descriptor, showActionCellButtonFunction, useShowActionCellButtonFunction};
      })
    );
  });
  return actions$.length ? forkJoin(actions$) : of([]);
}

export function checkHasActions(cellButtonActions: TableCellButtonActionDescriptor[]): boolean {
  return cellButtonActions.some(action => action.icon);
}

export function prepareTableCellButtonActions(widgetContext: WidgetContext, cellButtonActions: TableCellButtonActionDescriptor[],
                                              data: EntityData | AlarmDataInfo | FormattedData,
                                              reserveSpaceForHiddenAction = true): TableCellButtonActionDescriptor[] {
  if (reserveSpaceForHiddenAction) {
    return cellButtonActions.map(action =>
      filterTableCellButtonAction(widgetContext, action, data) ? action : { id: action.id } as TableCellButtonActionDescriptor);
  }
  return cellButtonActions.filter(action => filterTableCellButtonAction(widgetContext, action, data));
}

function filterTableCellButtonAction(widgetContext: WidgetContext,
                                     action: TableCellButtonActionDescriptor, data: EntityData | AlarmDataInfo | FormattedData): boolean {
  if (action.useShowActionCellButtonFunction) {
    try {
      return action.showActionCellButtonFunction.execute(widgetContext, data);
    } catch (e) {
      console.warn('Failed to execute showActionCellButtonFunction', e);
      return false;
    }
  } else {
    return true;
  }
}

export function noDataMessage(noDataDisplayMessage: string, defaultMessage: string,
                              utils: UtilsService, translate: TranslateService): string {
  if (isNotEmptyStr(noDataDisplayMessage)) {
    return utils.customTranslation(noDataDisplayMessage, noDataDisplayMessage);
  }
  return translate.instant(defaultMessage);
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
    '.mat-mdc-input-element::placeholder {\n' +
    '   color: ' + mdDarkSecondary + ';\n' +
    '}\n' +
    '.mat-mdc-input-element::-moz-placeholder {\n' +
    '   color: ' + mdDarkSecondary + ';\n' +
    '}\n' +
    '.mat-mdc-input-element::-webkit-input-placeholder {\n' +
    '   color: ' + mdDarkSecondary + ';\n' +
    '}\n' +
    '.mat-mdc-input-element:-ms-input-placeholder {\n' +
    '   color: ' + mdDarkSecondary + ';\n' +
    '}\n' +
    'mat-toolbar.mat-mdc-table-toolbar {\n' +
    'color: ' + mdDark + ';\n' +
    '}\n' +
    'mat-toolbar.mat-mdc-table-toolbar:not([color="primary"]) button.mat-mdc-icon-button mat-icon {\n' +
    'color: ' + mdDarkSecondary + ';\n' +
    '}\n' +
    '.mat-mdc-tab .mdc-tab__text-label {\n' +
    'color: ' + mdDark + ';\n' +
    '}\n' +
    '.mat-mdc-tab-header-pagination-chevron {\n' +
    'border-color: ' + mdDark + ';\n' +
    '}\n' +
    '.mat-mdc-tab-header-pagination-disabled .mat-mdc-tab-header-pagination-chevron {\n' +
    'border-color: ' + mdDarkDisabled2 + ';\n' +
    '}\n' +
    '.mat-mdc-table .mat-mdc-header-row {\n' +
    'background-color: ' + origBackgroundColor + ';\n' +
    '}\n' +
    '.mat-mdc-table .mat-mdc-header-cell {\n' +
    'color: ' + mdDarkSecondary + ';\n' +
    '}\n' +
    '.mat-mdc-table .mat-mdc-cell, .mat-mdc-table .mat-mdc-header-cell {\n' +
    'border-bottom-color: ' + mdDarkDivider + ';\n' +
    '}\n' +
    '.mat-mdc-table .mat-mdc-cell .mat-mdc-checkbox ' +
    '.mdc-checkbox__native-control:focus:enabled:not(:checked):not(:indeterminate):not([data-indeterminate=true])'+
    '~.mdc-checkbox__background, ' +
    '.mat-table .mat-header-cell .mat-mdc-checkbox ' +
    '.mdc-checkbox__native-control:focus:enabled:not(:checked):not(:indeterminate):not([data-indeterminate=true])'+
    '~.mdc-checkbox__background {\n' +
    'border-color: ' + mdDarkSecondary + ';\n' +
    '}\n' +
    '.mat-mdc-table .mat-mdc-row .mat-mdc-cell.mat-mdc-table-sticky {\n' +
    'transition: background-color .2s;\n' +
    '}\n' +
    '.mat-mdc-table .mat-mdc-row.tb-current-entity {\n' +
    'background-color: ' + currentEntityColor + ';\n' +
    '}\n' +
    '.mat-mdc-table .mat-mdc-row.tb-current-entity .mat-mdc-cell.mat-mdc-table-sticky {\n' +
    'background-color: ' + currentEntityStickyColor + ';\n' +
    '}\n' +
    '.mat-mdc-table .mat-mdc-row:hover:not(.tb-current-entity) {\n' +
    'background-color: ' + hoverColor + ';\n' +
    '}\n' +
    '.mat-mdc-table .mat-mdc-row:hover:not(.tb-current-entity) .mat-mdc-cell.mat-mdc-table-sticky {\n' +
    'background-color: ' + hoverStickyColor + ';\n' +
    '}\n' +
    '.mat-mdc-table .mat-mdc-row.mat-row-select.mat-selected:not(.tb-current-entity) {\n' +
    'background-color: ' + selectedColor + ';\n' +
    '}\n' +
    '.mat-mdc-table .mat-mdc-row.mat-row-select.mat-selected:not(.tb-current-entity) .mat-mdc-cell.mat-mdc-table-sticky {\n' +
    'background-color: ' + selectedStickyColor + ';\n' +
    '}\n' +
    '.mat-mdc-table .mat-mdc-row .mat-mdc-cell.mat-mdc-table-sticky, .mat-mdc-table .mat-mdc-header-cell.mat-mdc-table-sticky {\n' +
    'background-color: ' + origBackgroundColor + ';\n' +
    '}\n' +
    '.mat-mdc-table .mat-mdc-row {\n' +
    'color: ' + mdDark + ';\n' +
    'background-color: rgba(0, 0, 0, 0);\n' +
    '}\n' +
    '.mat-mdc-table .mat-mdc-cell button.mat-mdc-icon-button mat-icon {\n' +
    'color: ' + mdDarkSecondary + ';\n' +
    '}\n' +
    '.mat-mdc-table .mat-mdc-cell button.mat-mdc-icon-button[disabled][disabled] mat-icon {\n' +
    'color: ' + mdDarkDisabled + ';\n' +
    '}\n' +
    '.mat-mdc-table .mat-mdc-cell button.mat-mdc-icon-button tb-icon {\n' +
    'color: ' + mdDarkSecondary + ';\n' +
    '}\n' +
    '.mat-mdc-table .mat-mdc-cell button.mat-mdc-icon-button[disabled][disabled] tb-icon {\n' +
    'color: ' + mdDarkDisabled + ';\n' +
    '}\n' +
    '.mat-divider {\n' +
    'border-top-color: ' + mdDarkDivider + ';\n' +
    '}\n' +
    '.mat-mdc-paginator {\n' +
    'color: ' + mdDarkSecondary + ';\n' +
    '}\n' +
    '.mat-mdc-paginator button.mat-mdc-icon-button {\n' +
    'color: ' + mdDarkSecondary + ';\n' +
    '}\n' +
    '.mat-mdc-paginator button.mat-mdc-icon-button[disabled][disabled] {\n' +
    'color: ' + mdDarkDisabled + ';\n' +
    '}\n' +
    '.mat-mdc-paginator .mat-mdc-select-value {\n' +
    'color: ' + mdDarkSecondary + ';\n' +
    '}';
  return cssString;
}

export function getHeaderTitle(dataKey: DataKey, keySettings: TableWidgetDataKeySettings, utils: UtilsService) {
  if (isDefined(keySettings.customTitle) && isNotEmptyStr(keySettings.customTitle)) {
    return utils.customTranslation(keySettings.customTitle, keySettings.customTitle);
  }
  return dataKey.label;
}

export function buildPageStepSizeValues(pageStepCount: number, pageStepIncrement: number): Array<number> {
  const pageSteps: Array<number> = [];
  if (isValidPageStepCount(pageStepCount) && isValidPageStepIncrement(pageStepIncrement)) {
    for (let i = 1; i <= pageStepCount; i++) {
      pageSteps.push(pageStepIncrement * i);
    }
  }
  return pageSteps;
}

export function isValidPageStepIncrement(value: number): boolean {
  return Number.isInteger(value) && value > 0;
}

export function isValidPageStepCount(value: number): boolean {
  return Number.isInteger(value) && value > 0 && value <= 100;
}
