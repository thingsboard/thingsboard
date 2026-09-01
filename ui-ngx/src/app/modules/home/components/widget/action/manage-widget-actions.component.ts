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

import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  forwardRef,
  Input,
  NgZone,
  OnDestroy,
  OnInit,
  SecurityContext,
  ViewChild
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { PageComponent } from '@shared/components/page.component';
import { MatDialog } from '@angular/material/dialog';
import { DialogService } from '@core/services/dialog.service';
import { PageLink } from '@shared/models/page/page-link';
import { Direction, SortOrder } from '@shared/models/page/sort-order';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { fromEvent, merge } from 'rxjs';
import { debounceTime, distinctUntilChanged, first, tap } from 'rxjs/operators';
import {
  mergeWidgetActionsMap,
  toWidgetActionDescriptor,
  WidgetActionCallbacks,
  WidgetActionDescriptorInfo,
  WidgetActionsData,
  WidgetActionsDatasource
} from '@home/components/widget/action/manage-widget-actions.component.models';
import { UtilsService } from '@core/services/utils.service';
import {
  WidgetActionDescriptor,
  WidgetActionSource,
  WidgetActionsMap,
  WidgetActionType,
  widgetType
} from '@shared/models/widget.models';
import {
  WidgetActionDialogComponent,
  WidgetActionDialogData
} from '@home/components/widget/action/widget-action-dialog.component';
import { deepClone } from '@core/utils';
import { hidePageSizePixelValue } from '@shared/models/constants';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { DomSanitizer } from '@angular/platform-browser';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { ItemBufferService } from '@core/services/item-buffer.service';

@Component({
    selector: 'tb-manage-widget-actions',
    templateUrl: './manage-widget-actions.component.html',
    styleUrls: ['./manage-widget-actions.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => ManageWidgetActionsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class ManageWidgetActionsComponent extends PageComponent implements OnInit, AfterViewInit, OnDestroy, ControlValueAccessor {

  @Input() disabled: boolean;

  @Input() widgetType: widgetType;

  @Input() widgetTitle: string;

  @Input() defaultIconColor: string;

  @Input() callbacks: WidgetActionCallbacks;

  @Input() actionSources: {[actionSourceId: string]: WidgetActionSource};

  @Input() additionalWidgetActionTypes: WidgetActionType[];

  displayedColumns: string[];
  pageLink: PageLink;
  textSearchMode = false;
  hidePageSize = false;
  dataSource: WidgetActionsDatasource;
  dragDisabled = true;

  private actionsMap: WidgetActionsMap;
  private viewsInited = false;
  private dirtyValue = false;
  private widgetResize$: ResizeObserver;
  private destroyed = false;

  @ViewChild('searchInput') searchInputField: ElementRef;

  @ViewChild(MatPaginator) paginator: MatPaginator;
  @ViewChild(MatSort) sort: MatSort;

  private propagateChange = (_: any) => {};

  constructor(private translate: TranslateService,
              private utils: UtilsService,
              private dialog: MatDialog,
              private dialogs: DialogService,
              private cd: ChangeDetectorRef,
              private elementRef: ElementRef,
              private zone: NgZone,
              private sanitizer: DomSanitizer,
              private importExportService: ImportExportService,
              private itembuffer: ItemBufferService) {
    super();
    const sortOrder: SortOrder = { property: 'actionSourceName', direction: Direction.ASC };
    this.pageLink = new PageLink(10, 0, null, sortOrder);
    this.dataSource = new WidgetActionsDatasource(this.translate, this.utils);
    this.displayedColumns = ['actionSourceId', 'actionSourceName', 'name', 'icon', 'typeName', 'actions'];
  }

  ngOnInit(): void {
    this.widgetResize$ = new ResizeObserver(() => {
      this.zone.run(() => {
        const showHidePageSize = this.elementRef.nativeElement.offsetWidth < hidePageSizePixelValue;
        if (showHidePageSize !== this.hidePageSize) {
          this.hidePageSize = showHidePageSize;
          this.cd.markForCheck();
        }
      });
    });
    this.widgetResize$.observe(this.elementRef.nativeElement);
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    if (this.widgetResize$) {
      this.widgetResize$.disconnect();
    }
  }

  ngAfterViewInit() {
    fromEvent(this.searchInputField.nativeElement, 'keyup')
      .pipe(
        debounceTime(150),
        distinctUntilChanged(),
        tap(() => {
          this.paginator.pageIndex = 0;
          this.updateData();
        })
      )
      .subscribe();

    this.sort.sortChange.subscribe(() => this.paginator.pageIndex = 0);

    merge(this.sort.sortChange, this.paginator.page)
      .pipe(
        tap(() => this.updateData())
      )
      .subscribe();

    this.viewsInited = true;
    if (this.dirtyValue) {
      this.dirtyValue = false;
      this.updateData(true);
    }
  }

  private updateData(reload: boolean = false) {
    this.pageLink.page = this.paginator.pageIndex;
    this.pageLink.pageSize = this.paginator.pageSize;
    this.pageLink.sortOrder.property = this.sort.active;
    this.pageLink.sortOrder.direction = Direction[this.sort.direction.toUpperCase()];
    this.dataSource.loadActions(this.pageLink, reload);
  }

  dropAction(event: CdkDragDrop<WidgetActionsDatasource>) {
    this.dragDisabled = true;
    const droppedAction: WidgetActionDescriptorInfo = event.item.data;
    this.dataSource.pageData$.pipe(
      first()
    ).subscribe((actions) => {
      const action = actions.data;
      let startActionSourceIndex = action.findIndex(element => element.actionSourceId === droppedAction.actionSourceId);
      const targetActions = this.getOrCreateTargetActions(droppedAction.actionSourceId);
      if (startActionSourceIndex === 0) {
        startActionSourceIndex -= targetActions.findIndex(element => element.id === action[0].id);
      }
      moveItemInArray(targetActions, event.previousIndex - startActionSourceIndex, event.currentIndex - startActionSourceIndex);
      this.onActionsUpdated();
    });
  }

  addAction($event: Event) {
    this.openWidgetActionDialog($event);
  }

  get hasActions(): boolean {
    return this.actionsMap && Object.keys(this.actionsMap).some(id => this.actionsMap[id]?.length > 0);
  }

  importActions($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExportService.importWidgetActions().subscribe((importedActionsMap) => {
      if (importedActionsMap) {
        this.applyImportedActions(importedActionsMap, 'widget-config.import-actions-imported');
      }
    });
  }

  copyAction($event: Event, action: WidgetActionDescriptorInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    this.itembuffer.copyWidgetActions({[action.actionSourceId]: [toWidgetActionDescriptor(action)]});
    this.store.dispatch(new ActionNotificationShow(
      {message: this.translate.instant('widget-config.action-copied'),
        type: 'success', duration: 1500, target: 'dashboardRoot'}));
  }

  pasteActions($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const actionsMap = this.itembuffer.pasteWidgetActions();
    if (actionsMap) {
      this.applyImportedActions(actionsMap, 'widget-config.paste-actions-pasted');
    }
  }

  get hasActionsInBuffer(): boolean {
    return this.itembuffer.hasWidgetActions();
  }

  private applyImportedActions(importedActionsMap: WidgetActionsMap, appliedMessage: string) {
    const result = mergeWidgetActionsMap(this.actionsMap, importedActionsMap, this.actionSources,
      this.additionalWidgetActionTypes, () => this.callbacks.fetchCellClickColumns()?.length ?? 0);
    if (result.imported) {
      this.onActionsUpdated();
    }
    const messageParts = [this.translate.instant(appliedMessage, {count: result.imported})];
    if (result.skipped) {
      messageParts.push(this.translate.instant('widget-config.import-actions-skipped', {count: result.skipped}));
    }
    if (result.columnIndexesReset) {
      messageParts.push(this.translate.instant('widget-config.import-actions-columns-reset', {count: result.columnIndexesReset}));
    }
    const hasWarnings = !!(result.skipped || result.columnIndexesReset);
    this.store.dispatch(new ActionNotificationShow(
      {message: messageParts.join('<br/>'),
        type: hasWarnings ? 'warn' : 'success',
        duration: hasWarnings ? 6000 : 2000,
        target: 'dashboardRoot'}));
  }

  duplicateAction($event: Event, action: WidgetActionDescriptorInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    const result = mergeWidgetActionsMap(this.actionsMap,
      {[action.actionSourceId]: [toWidgetActionDescriptor(action)]}, this.actionSources,
      this.additionalWidgetActionTypes, () => this.callbacks.fetchCellClickColumns()?.length ?? 0);
    if (result.imported) {
      this.onActionsUpdated();
    }
  }

  clearActions($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const title = this.translate.instant('widget-config.clear-actions-title');
    const content = this.translate.instant('widget-config.clear-actions-text');
    this.dialogs.confirm(title, content,
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'), true).subscribe(
      (res) => {
        if (res) {
          for (const actionSourceId of Object.keys(this.actionsMap)) {
            delete this.actionsMap[actionSourceId];
          }
          this.onActionsUpdated();
        }
      });
  }

  exportActions($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    const fileName = this.widgetTitle ? `${this.widgetTitle}_actions` : 'widget_actions';
    this.importExportService.exportWidgetActions(this.prepareActionsMap(), fileName);
  }

  copyAllActions($event: Event) {
    if ($event) {
      $event.stopPropagation();
    }
    this.itembuffer.copyWidgetActions(this.prepareActionsMap());
    this.store.dispatch(new ActionNotificationShow(
      {message: this.translate.instant('widget-config.actions-copied'),
        type: 'success', duration: 1500, target: 'dashboardRoot'}));
  }

  private prepareActionsMap(): WidgetActionsMap {
    const preparedActionsMap: WidgetActionsMap = {};
    for (const actionSourceId of Object.keys(this.actionsMap)) {
      const actions = this.actionsMap[actionSourceId];
      if (actions?.length) {
        preparedActionsMap[actionSourceId] = actions;
      }
    }
    return preparedActionsMap;
  }

  exportAction($event: Event, action: WidgetActionDescriptorInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    const exportedActionsMap: WidgetActionsMap = {
      [action.actionSourceId]: [toWidgetActionDescriptor(action)]
    };
    this.importExportService.exportWidgetActions(exportedActionsMap, `${action.name}_action`);
  }

  editAction($event: Event, action: WidgetActionDescriptorInfo) {
    this.openWidgetActionDialog($event, action);
  }

  private openWidgetActionDialog($event: Event, action: WidgetActionDescriptorInfo = null) {
    if ($event) {
      $event.stopPropagation();
    }
    const isAdd = action === null;
    let prevActionSourceId = null;
    if (!isAdd) {
      prevActionSourceId = action.actionSourceId;
    }
    const availableActionSources: {[actionSourceId: string]: WidgetActionSource} = {};
    for (const id of Object.keys(this.actionSources)) {
      const actionSource = this.actionSources[id];
      if (actionSource.multiple) {
        availableActionSources[id] = actionSource;
      } else {
        if (!isAdd && action.actionSourceId === id) {
          availableActionSources[id] = actionSource;
        } else {
          const existing = this.actionsMap[id];
          if (!existing || !existing.length) {
            availableActionSources[id] = actionSource;
          }
        }
      }
    }

    const actionsData: WidgetActionsData = {
      actionsMap: this.actionsMap,
      actionSources: availableActionSources
    };

    this.dialog.open<WidgetActionDialogComponent, WidgetActionDialogData,
      WidgetActionDescriptorInfo>(WidgetActionDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        isAdd,
        callbacks: this.callbacks,
        actionsData,
        action: deepClone(action),
        widgetType: this.widgetType,
        defaultIconColor: this.defaultIconColor,
        additionalWidgetActionTypes: this.additionalWidgetActionTypes
      }
    }).afterClosed().subscribe(
      (res) => {
        if (res) {
          this.saveAction(res, isAdd, prevActionSourceId);
        }
      }
    );
  }

  private saveAction(actionInfo: WidgetActionDescriptorInfo, isAdd: boolean, prevActionSourceId: string) {
    const actionSourceId = actionInfo.actionSourceId;
    const action = toWidgetActionDescriptor(actionInfo);
    if (isAdd) {
      const targetActions = this.getOrCreateTargetActions(actionSourceId);
      targetActions.push(action);
    } else {
      if (actionSourceId !== prevActionSourceId) {
        let targetActions = this.getOrCreateTargetActions(prevActionSourceId);
        const targetIndex = targetActions.findIndex((targetAction) => targetAction.id === action.id);
        if (targetIndex > -1) {
          targetActions.splice(targetIndex, 1);
        }
        targetActions = this.getOrCreateTargetActions(actionSourceId);
        targetActions.push(action);
      } else {
        const targetActions = this.getOrCreateTargetActions(actionSourceId);
        const targetIndex = targetActions.findIndex((targetAction) => targetAction.id === action.id);
        if (targetIndex > -1) {
          targetActions[targetIndex] = action;
        }
      }
    }
    this.onActionsUpdated();
  }

  private getOrCreateTargetActions(actionSourceId: string): Array<WidgetActionDescriptor> {
    const actionsMap = this.actionsMap;
    let targetActions = actionsMap[actionSourceId];
    if (!targetActions) {
      targetActions = [];
      actionsMap[actionSourceId] = targetActions;
    }
    return targetActions;
  }

  deleteAction($event: Event, action: WidgetActionDescriptorInfo) {
    if ($event) {
      $event.stopPropagation();
    }
    const title = this.translate.instant('widget-config.delete-action-title');
    const content = this.translate.instant('widget-config.delete-action-text', {actionName: action.name});
    const safeContent = this.sanitizer.sanitize(SecurityContext.HTML, content);
    this.dialogs.confirm(title, safeContent,
      this.translate.instant('action.no'),
      this.translate.instant('action.yes'), true).subscribe(
      (res) => {
        if (res) {
          const targetActions = this.getOrCreateTargetActions(action.actionSourceId);
          const targetIndex = targetActions.findIndex((targetAction) => targetAction.id === action.id);
          if (targetIndex > -1) {
            targetActions.splice(targetIndex, 1);
            this.onActionsUpdated();
          }
        }
      });
  }

  enterFilterMode() {
    this.textSearchMode = true;
    this.pageLink.textSearch = '';
    setTimeout(() => {
      this.searchInputField.nativeElement.focus();
      this.searchInputField.nativeElement.setSelectionRange(0, 0);
    }, 10);
  }

  exitFilterMode() {
    this.textSearchMode = false;
    this.pageLink.textSearch = null;
    this.paginator.pageIndex = 0;
    this.updateData();
  }

  private resetSortAndFilter(update: boolean = true) {
    this.pageLink.textSearch = null;
    this.paginator.pageIndex = 0;
    const sortable = this.sort.sortables.get('actionSourceName');
    this.sort.active = sortable.id;
    this.sort.direction = 'asc';
    if (update) {
      this.updateData(true);
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(actions?: WidgetActionsMap): void {
    this.actionsMap = actions ?? {};
    setTimeout(() => {
      if (!this.destroyed) {
        const actionData: WidgetActionsData = {
          actionsMap: this.actionsMap,
          actionSources: this.actionSources
        };
        this.dataSource.setActions(actionData);
        if (this.viewsInited) {
          this.resetSortAndFilter(true);
        } else {
          this.dirtyValue = true;
        }
      }
    }, 0);
  }

  private onActionsUpdated() {
    this.updateData(true);
    this.propagateChange(this.actionsMap);
  }
}
