///
/// Copyright © 2016-2025 The Thingsboard Authors
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
  DateEntityTableColumn,
  EntityLinkTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { Direction } from '@shared/models/page/sort-order';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { PageLink } from '@shared/models/page/page-link';
import { EMPTY, Observable, of } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { EntityId } from '@shared/models/id/entity-id';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { DestroyRef, Renderer2 } from '@angular/core';
import { EntityDebugSettings } from '@shared/models/entity.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { catchError, filter, first, switchMap, tap } from 'rxjs/operators';
import {
  ArgumentEntityType,
  ArgumentType,
  CalculatedField,
  CalculatedFieldEventArguments,
  CalculatedFieldInfo,
  CalculatedFieldScriptConfiguration,
  CalculatedFieldsQuery,
  CalculatedFieldType,
  CalculatedFieldTypeTranslations,
  debugCfActionEnabled,
  getCalculatedFieldArgumentsEditorCompleter,
  getCalculatedFieldArgumentsHighlights,
  PropagationWithExpression,
} from '@shared/models/calculated-field.models';
import {
  CalculatedFieldDialogComponent,
  CalculatedFieldDialogData,
  CalculatedFieldScriptTestDialogComponent,
  CalculatedFieldTestScriptDialogData
} from './components/public-api';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { deepClone, getEntityDetailsPageURL, isObject } from '@core/utils';
import { EntityDebugSettingsService } from '@home/components/entity/debug/entity-debug-settings.service';
import { DatePipe } from '@angular/common';
import { UtilsService } from "@core/services/utils.service";
import { ActionNotificationShow } from "@core/notification/notification.actions";
import { CalculatedFieldEventBody, DebugEventType, EventType } from '@shared/models/event.models';
import { EventsDialogComponent, EventsDialogData } from '@home/dialogs/events-dialog.component';
import {
  CalculatedFieldsHeaderComponent
} from '@home/components/calculated-fields/table-header/calculated-fields-header.component';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { CalculatedFieldComponent } from '@home/components/calculated-fields/calculated-field.component';
import { Router } from '@angular/router';
import { CalculatedFieldsTabsComponent } from '@home/pages/calculated-fields/calculated-fields-tabs.component';

export type CalculatedFieldsTableEntity = CalculatedField | CalculatedFieldInfo;

export class CalculatedFieldsTableConfig extends EntityTableConfig<CalculatedFieldsTableEntity> {

  readonly tenantId = getCurrentAuthUser(this.store).tenantId;
  additionalDebugActionConfig = {
    title: this.translate.instant('action.see-debug-events'),
    action: (calculatedField: CalculatedFieldsTableEntity,
             openCalculatedFieldEdit = true,
             afterCloseCallback?: (expression: string) => void) => this.openDebugEventsDialog.call(this, null, calculatedField, openCalculatedFieldEdit, afterCloseCallback),
  };

  calculatedFieldFilterConfig: CalculatedFieldsQuery;

  constructor(private calculatedFieldsService: CalculatedFieldsService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private datePipe: DatePipe,
              private entityId: EntityId = null,
              private store: Store<AppState>,
              private destroyRef: DestroyRef,
              private renderer: Renderer2,
              private entityName: string,
              private ownerId: EntityId = null,
              private importExportService: ImportExportService,
              private entityDebugSettingsService: EntityDebugSettingsService,
              private utilsService: UtilsService,
              private router: Router,
              public pageMode = false,
  ) {
    super();
    if (this.pageMode) {
      this.headerComponent = CalculatedFieldsHeaderComponent;

      this.entityComponent = CalculatedFieldComponent;
      this.entityTabsComponent = CalculatedFieldsTabsComponent;
      this.rowPointer = true;
    }
    this.tableTitle = this.pageMode ? '' : this.translate.instant('entity.type-calculated-fields');
    this.detailsPanelEnabled = this.pageMode;
    this.entityType = EntityType.CALCULATED_FIELD;
    this.entityTranslations = entityTypeTranslations.get(EntityType.CALCULATED_FIELD);
    this.entityResources = entityTypeResources.get(EntityType.CALCULATED_FIELD);

    this.entitiesFetchFunction = (pageLink: PageLink) => this.fetchCalculatedFields(pageLink);
    this.addEntity = this.getCalculatedFieldDialog.bind(this);
    this.saveEntity = (cf) => this.calculatedFieldsService.saveCalculatedField(cf);
    this.loadEntity = id => this.calculatedFieldsService.getCalculatedFieldById(id.id);
    this.deleteEntityTitle = (field) => this.translate.instant('calculated-fields.delete-title', {title: field.name});
    this.deleteEntityContent = () => this.translate.instant('calculated-fields.delete-text');
    this.deleteEntitiesTitle = count => this.translate.instant('calculated-fields.delete-multiple-title', {count});
    this.deleteEntitiesContent = () => this.translate.instant('calculated-fields.delete-multiple-text');
    this.deleteEntity = id => this.calculatedFieldsService.deleteCalculatedField(id.id);

    this.onEntityAction = action => this.onCFAction(action);

    this.addActionDescriptors = [
      {
        name: this.translate.instant('calculated-fields.create'),
        icon: 'insert_drive_file',
        isEnabled: () => true,
        onAction: ($event) => this.getTable().addEntity($event)
      },
      {
        name: this.translate.instant('calculated-fields.import'),
        icon: 'file_upload',
        isEnabled: () => true,
        onAction: () => this.importCalculatedField()
      }
    ];

    this.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.columns.push(new DateEntityTableColumn<CalculatedField>('createdTime', 'common.created-time', this.datePipe, '150px'));
    this.columns.push(new EntityTableColumn<CalculatedField>('name', 'common.name', this.pageMode ? '33%' : '60%',
      entity => this.utilsService.customTranslation(entity.name, entity.name)));
    if (this.pageMode) {
      this.columns.push(new EntityTableColumn<CalculatedFieldInfo>('entityType', 'entity.entity-type', '10%',
        entity => this.translate.instant(entityTypeTranslations.get(entity.entityId.entityType).type)));
      this.columns.push(new EntityLinkTableColumn<CalculatedFieldInfo>('entityName', 'entity.entity', '33%',
        entity => this.utilsService.customTranslation(entity.entityName, entity.entityName),
        entity => getEntityDetailsPageURL(entity.entityId?.id, entity.entityId?.entityType as EntityType), false));
    }
    this.columns.push(new EntityTableColumn<CalculatedField>('type', 'common.type', this.pageMode ? '23%' : '40%', entity => this.translate.instant(CalculatedFieldTypeTranslations.get(entity.type).name), () => ({whiteSpace: 'nowrap' })));

    this.cellActionDescriptors.push(
      {
        name: this.translate.instant('action.copy'),
        icon: 'content_copy',
        isEnabled: () => true,
        onAction: ($event, entity) => this.copyCalculatedField($event, entity),
      },
      {
        name: this.translate.instant('action.export'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => this.exportCalculatedField($event, entity),
      },
      {
        name: this.translate.instant('entity-view.events'),
        icon: 'mdi:clipboard-text-clock',
        isEnabled: () => true,
        onAction: ($event, entity) =>
          this.pageMode ? this.openDebugTab($event, entity) : this.openDebugEventsDialog($event, entity),
      },
      {
        name: '',
        nameFunction: entity => this.entityDebugSettingsService.getDebugConfigLabel(entity?.debugSettings),
        icon: 'mdi:bug',
        isEnabled: () => true,
        iconFunction: ({ debugSettings }) => this.entityDebugSettingsService.isDebugActive(debugSettings?.allEnabledUntil) || debugSettings?.failuresEnabled ? 'mdi:bug' : 'mdi:bug-outline',
        onAction: ($event, entity) => this.onOpenDebugConfig($event, entity),
      }
    );
    if (!this.pageMode) {
      this.cellActionDescriptors.push({
        name: this.translate.instant('action.edit'),
        icon: 'edit',
        isEnabled: () => true,
        onAction: ($event, entity) => this.editCalculatedField($event, entity),
      })
    }
  }

  fetchCalculatedFields(pageLink: PageLink): Observable<PageData<CalculatedFieldsTableEntity>> {
    return this.pageMode ?
      this.calculatedFieldsService.getCalculatedFields(pageLink, this.calculatedFieldFilterConfig):
      this.calculatedFieldsService.getCalculatedFieldsByEntityId(this.entityId, pageLink);
  }

  private onOpenDebugConfig($event: Event, calculatedField: CalculatedFieldsTableEntity): void {
    $event?.stopPropagation();
    const { debugSettings = {}, id } = calculatedField;
    const additionalActionConfig = {
      ...this.additionalDebugActionConfig,
      action: () => this.openDebugEventsDialog($event, calculatedField)
    };

    const { viewContainerRef, renderer } = this.entityDebugSettingsService;
    if (!viewContainerRef || !renderer) {
      this.entityDebugSettingsService.viewContainerRef = this.getTable().viewContainerRef;
      this.entityDebugSettingsService.renderer = this.renderer;
    }

    this.entityDebugSettingsService.openDebugStrategyPanel({
      debugSettings,
      debugConfig: {
        entityType: EntityType.CALCULATED_FIELD,
        additionalActionConfig,
      },
      onSettingsAppliedFn: settings => this.onDebugConfigChanged(id.id, settings)
    }, $event.target as Element);
  }

  private openDebugTab($event: Event, calculatedField: CalculatedFieldsTableEntity) {
    const table = this.getTable();
    if (!table.isDetailsOpen) {
      table.toggleEntityDetails($event, calculatedField);
      if (table.entityDetailsPanel.matTabGroup._tabs.length > 1) {
        table.entityDetailsPanel.matTabGroup.selectedIndex = 1;
      } else {
        table.entityDetailsPanel.matTabGroup._tabs.changes.pipe(
          first()
        ).subscribe(() => {
          table.entityDetailsPanel.matTabGroup.selectedIndex = 1;
        })
      }
    }
  }

  private editCalculatedField($event: Event, calculatedField: CalculatedFieldsTableEntity, isDirty = false): void {
    $event?.stopPropagation();
    this.getCalculatedFieldDialog(calculatedField, 'action.apply', isDirty)
      .subscribe((res) => {
        if (res) {
          this.updateData();
        }
      });
  }

  private getCalculatedFieldDialog(value?: CalculatedFieldsTableEntity, buttonTitle = 'action.add', isDirty = false, disabledSelectType = false): Observable<CalculatedField> {
    const entityId = this.entityId || value?.entityId;
    const entityName = this.entityName || (value as CalculatedFieldInfo)?.entityName;
    return this.dialog.open<CalculatedFieldDialogComponent, CalculatedFieldDialogData, CalculatedField>(CalculatedFieldDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        value,
        buttonTitle,
        entityId,
        entityName,
        tenantId: this.tenantId,
        ownerId: this.ownerId ?? {entityType: EntityType.TENANT, id: this.tenantId},
        additionalDebugActionConfig: this.additionalDebugActionConfig,
        getTestScriptDialogFn: this.getTestScriptDialog.bind(this),
        isDirty,
        disabledSelectType
      },
      enterAnimationDuration: isDirty ? 0 : null,
    })
      .afterClosed()
      .pipe(filter(Boolean));
  }

  private openDebugEventsDialog($event: Event, calculatedField: CalculatedFieldsTableEntity, openCalculatedFieldEdit = true, afterCloseCallback?: (expression: string) => void ): void {
    $event?.stopPropagation();

    const onDebugEventSelected = (event: CalculatedFieldEventBody, dialogRef: MatDialogRef<EventsDialogComponent, string>) => {
      this.getTestScriptDialog(calculatedField, JSON.parse(event.arguments), openCalculatedFieldEdit)
        .subscribe(expression => dialogRef.close(expression));
    };

    this.dialog.open<EventsDialogComponent, EventsDialogData, null>(EventsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        title: 'calculated-fields.debugging',
        tenantId: this.tenantId,
        entityId: calculatedField.id,
        debugEventTypes:[DebugEventType.DEBUG_CALCULATED_FIELD],
        disabledEventTypes:[EventType.LC_EVENT, EventType.ERROR, EventType.STATS],
        defaultEventType: DebugEventType.DEBUG_CALCULATED_FIELD,
        onDebugEventSelected,
        debugActionDisabled: !debugCfActionEnabled(calculatedField)
      }
    })
      .afterClosed()
      .subscribe(value => {
        if (afterCloseCallback) {
          afterCloseCallback(value)
        }
      });
  }

  private exportCalculatedField($event: Event, calculatedField: CalculatedFieldsTableEntity): void {
    $event?.stopPropagation();
    this.importExportService.exportCalculatedField(calculatedField.id.id);
  }

  private copyCalculatedField($event: Event, calculatedField: CalculatedField): void {
    $event?.stopPropagation();
    const copyCalculatedField = deepClone(calculatedField);
    if (this.pageMode) {
      copyCalculatedField.entityId = null;
      delete (copyCalculatedField as CalculatedFieldInfo).entityName;
    }
    delete copyCalculatedField.id;
    this.getCalculatedFieldDialog(copyCalculatedField, 'action.apply', false)
      .subscribe((res) => {
        if (res) {
          this.updateData();
        }
      });
  }

  private importCalculatedField(): void {
    this.importExportService.openCalculatedFieldImportDialog()
      .pipe(
        filter(Boolean),
        switchMap(calculatedField => {
          if (calculatedField.type === CalculatedFieldType.ALARM) {
            this.store.dispatch(new ActionNotificationShow({
              message: this.translate.instant('calculated-fields.hint.import-invalid-calculated-field-type'),
              type: 'error',
              verticalPosition: 'top',
              horizontalPosition: 'left',
              duration: 5000
            }));
            return EMPTY;
          }
          return of(calculatedField);
        }),
        switchMap(calculatedField => this.getCalculatedFieldDialog(this.updateImportedCalculatedField(calculatedField), 'action.add', true, true)),
        filter(Boolean),
        switchMap(calculatedField => this.calculatedFieldsService.saveCalculatedField(calculatedField)),
        filter(Boolean),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => this.updateData());
  }

  private updateImportedCalculatedField(calculatedField: CalculatedField): CalculatedField {
    if (calculatedField.type === CalculatedFieldType.GEOFENCING) {
      calculatedField.configuration.zoneGroups = Object.keys(calculatedField.configuration.zoneGroups).reduce((acc, key) => {
        const arg = calculatedField.configuration.zoneGroups[key];
        acc[key] = arg.refEntityId?.entityType === ArgumentEntityType.Tenant
          ? { ...arg, refEntityId: { id: this.tenantId, entityType: ArgumentEntityType.Tenant } }
          : arg;
        return acc;
      }, {});
    } else {
      calculatedField.configuration.arguments = Object.keys(calculatedField.configuration.arguments).reduce((acc, key) => {
        const arg = calculatedField.configuration.arguments[key];
        acc[key] = arg.refEntityId?.entityType === ArgumentEntityType.Tenant
          ? { ...arg, refEntityId: { id: this.tenantId, entityType: ArgumentEntityType.Tenant } }
          : arg;
        return acc;
      }, {});
    }

    return calculatedField;
  }

  private onDebugConfigChanged(id: string, debugSettings: EntityDebugSettings): void {
    this.calculatedFieldsService.getCalculatedFieldById(id).pipe(
      switchMap(field => this.calculatedFieldsService.saveCalculatedField({ ...field, debugSettings })),
      catchError(() => of(null)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => this.updateData());
  }

  getTestScriptDialog(calculatedField: CalculatedFieldsTableEntity, argumentsObj?: CalculatedFieldEventArguments, openCalculatedFieldEdit = true, expression?: string): Observable<string> {
    if (
      calculatedField.type === CalculatedFieldType.SCRIPT ||
      calculatedField.type === CalculatedFieldType.RELATED_ENTITIES_AGGREGATION ||
      (calculatedField.type === CalculatedFieldType.PROPAGATION && calculatedField.configuration.applyExpressionToResolvedArguments === true)
    ) {
      const resultArguments = Object.keys(calculatedField.configuration.arguments).reduce((acc, key) => {
        const type = calculatedField.configuration.arguments[key].refEntityKey.type;
        acc[key] = isObject(argumentsObj) && argumentsObj.hasOwnProperty(key)
          ? {...argumentsObj[key], type}
          : type === ArgumentType.Rolling ? {values: [], type} : {value: '', type, ts: new Date().getTime()};
        return acc;
      }, {});
      return this.dialog.open<CalculatedFieldScriptTestDialogComponent, CalculatedFieldTestScriptDialogData, string>(CalculatedFieldScriptTestDialogComponent,
        {
          disableClose: true,
          panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-fullscreen-dialog-gt-xs'],
          data: {
            arguments: resultArguments,
            expression: expression ?? (calculatedField.configuration as CalculatedFieldScriptConfiguration | PropagationWithExpression).expression,
            argumentsEditorCompleter: getCalculatedFieldArgumentsEditorCompleter(calculatedField.configuration.arguments),
            argumentsHighlightRules: getCalculatedFieldArgumentsHighlights(calculatedField.configuration.arguments),
            openCalculatedFieldEdit
          }
        }).afterClosed()
        .pipe(
          filter(Boolean),
          tap(expression => {
            if (openCalculatedFieldEdit) {
              this.editCalculatedField(null, {
                entityId: this.entityId, ...calculatedField,
                configuration: {...calculatedField.configuration, expression} as any
              }, true)
            }
          }),
        );
    } else {
      return of(null);
    }
  }

  private openCalculatedField($event: Event, entity: CalculatedFieldsTableEntity) {
    $event?.stopPropagation();
    const url = this.router.createUrlTree(['calculatedFields', entity.id.id]);
    this.router.navigateByUrl(url);
  }

  private onCFAction(action: EntityAction<CalculatedFieldsTableEntity>): boolean {
    switch (action.action) {
      case 'open':
        this.openCalculatedField(action.event, action.entity);
        return true;
      case 'export':
        this.exportCalculatedField(action.event, action.entity);
        return true;
    }
    return false;
  }
}
