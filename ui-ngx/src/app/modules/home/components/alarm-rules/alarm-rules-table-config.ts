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
  checkBoxCell,
  DateEntityTableColumn,
  EntityLinkTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { Direction } from '@shared/models/page/sort-order';
import { MatDialog } from '@angular/material/dialog';
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
  CalculatedFieldAlarmRule,
  CalculatedFieldEventArguments,
  CalculatedFieldInfo,
  CalculatedFieldsQuery,
  CalculatedFieldType,
  getCalculatedFieldArgumentsEditorCompleter,
  getCalculatedFieldArgumentsHighlights,
} from '@shared/models/calculated-field.models';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { EntityDebugSettingsService } from '@home/components/entity/debug/entity-debug-settings.service';
import { DatePipe } from '@angular/common';
import {
  AlarmRuleDialogComponent,
  AlarmRuleDialogData
} from "@home/components/alarm-rules/alarm-rule-dialog.component";
import { AlarmSeverity, alarmSeverityTranslations } from "@shared/models/alarm.models";
import { UtilsService } from "@core/services/utils.service";
import { deepClone, getEntityDetailsPageURL, isObject } from "@core/utils";
import { AlarmRuleTableHeaderComponent } from "@home/components/alarm-rules/alarm-rule-table-header.component";
import { EventsDialogComponent, EventsDialogData } from '@home/dialogs/events-dialog.component';
import { DebugEventType, EventType } from '@shared/models/event.models';
import { ActionNotificationShow } from "@core/notification/notification.actions";
import {
  CalculatedFieldScriptTestDialogComponent,
  CalculatedFieldTestScriptDialogData
} from "@home/components/calculated-fields/components/test-dialog/calculated-field-script-test-dialog.component";
import { AlarmRulesTabsComponent } from '@home/pages/alarm/alarm-rules-tabs.component';
import { Router } from '@angular/router';
import { EntityAction } from '@home/models/entity/entity-component.models';
import { AlarmRulesComponent } from '@home/components/alarm-rules/alarm-rules.component';

type AlarmRuleTableEntity = CalculatedField | CalculatedFieldInfo;

export class AlarmRulesTableConfig extends EntityTableConfig<AlarmRuleTableEntity> {

  readonly tenantId = getCurrentAuthUser(this.store).tenantId;
  additionalDebugActionConfig = {
    title: this.translate.instant('action.see-debug-events'),
    action: (calculatedField: AlarmRuleTableEntity) => this.openDebugEventsDialog.call(this, null, calculatedField),
  };

  alarmRuleFilterConfig: CalculatedFieldsQuery;

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
              public pageMode: boolean = false,
  ) {
    super();
    if (this.pageMode) {
      this.headerComponent = AlarmRuleTableHeaderComponent;
      this.entityComponent = AlarmRulesComponent;
      this.entityTabsComponent = AlarmRulesTabsComponent;
      this.rowPointer = true;
    }
    this.tableTitle = this.pageMode ? '' : this.translate.instant('alarm-rule.alarm-rules');
    this.detailsPanelEnabled = this.pageMode;
    this.entityResources = {
      helpLinkId: 'alarmRules'
    };
    this.entityType = EntityType.CALCULATED_FIELD;
    this.entityTranslations = {
      type: 'alarm-rule.alarm-rule',
      typePlural: 'alarm-rule.alarm-rules',
      list: 'alarm-rule.list',
      add: 'action.add',
      details: 'alarm-rule.details',
      noEntities: 'alarm-rule.no-found',
      search: 'action.search',
      selectedEntities: 'alarm-rule.selected-fields'
    };

    this.entitiesFetchFunction = (pageLink: PageLink) => this.fetchCalculatedFields(pageLink);
    this.addEntity = this.getCalculatedAlarmDialog.bind(this);
    this.loadEntity = id => this.calculatedFieldsService.getCalculatedFieldById(id.id);
    this.saveEntity = (alarmRule) => this.calculatedFieldsService.saveCalculatedField(alarmRule);

    this.deleteEntityTitle = (field) => this.translate.instant('alarm-rule.delete-title', {title: field.name});
    this.deleteEntityContent = () => this.translate.instant('alarm-rule.delete-text');
    this.deleteEntitiesTitle = count => this.translate.instant('alarm-rule.delete-multiple-title', {count});
    this.deleteEntitiesContent = () => this.translate.instant('alarm-rule.delete-multiple-text');
    this.deleteEntity = id => this.calculatedFieldsService.deleteCalculatedField(id.id);

    this.onEntityAction = action => this.onCFAction(action);

    this.addActionDescriptors = [
      {
        name: this.translate.instant('alarm-rule.create'),
        icon: 'insert_drive_file',
        isEnabled: () => true,
        onAction: ($event) => this.getTable().addEntity($event)
      },
      {
        name: this.translate.instant('alarm-rule.import'),
        icon: 'file_upload',
        isEnabled: () => true,
        onAction: () => this.importCalculatedField()
      }
    ];

    this.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};
    this.columns.push(new DateEntityTableColumn<CalculatedFieldAlarmRule>('createdTime', 'common.created-time', this.datePipe, '150px'));
    this.columns.push(new EntityTableColumn<CalculatedFieldAlarmRule>('name', 'alarm-rule.alarm-type', '33%',
      entity => this.utilsService.customTranslation(entity.name, entity.name)));
    if (this.pageMode) {
      this.columns.push(new EntityTableColumn<CalculatedFieldInfo>('entityType', 'entity.entity-type', '10%',
        entity => this.translate.instant(entityTypeTranslations.get(entity.entityId.entityType).type)));
      this.columns.push(new EntityLinkTableColumn<CalculatedFieldInfo>('entityName', 'entity.entity', '33%',
        entity => this.utilsService.customTranslation(entity.entityName, entity.entityName),
        entity => getEntityDetailsPageURL(entity.entityId?.id, entity.entityId?.entityType as EntityType), false));
    }
    this.columns.push(new EntityTableColumn<CalculatedFieldAlarmRule>('createRule', 'alarm-rule.severities', this.pageMode ? '23%' : '67%',
      entity => Object.keys(entity.configuration.createRules).map((severity) => this.translate.instant(alarmSeverityTranslations.get(severity as AlarmSeverity))).join(', '),
      () => ({}), false));
    this.columns.push(new EntityTableColumn<CalculatedFieldAlarmRule>('clearRule', 'alarm-rule.cleared', '90px',
      entity => checkBoxCell(!!entity.configuration.clearRule), ()=> { return {padding: 0, textAlign: 'center'}}, false));

    this.cellActionDescriptors.push(
      {
        name: this.translate.instant('alarm-rule.copy'),
        icon: 'content_copy',
        isEnabled: () => true,
        onAction: ($event, entity) => this.copyCalculatedField($event, entity)
      }
    );
    this.cellActionDescriptors.push(
      {
        name: this.translate.instant('action.export'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: ($event, entity) => this.exportAlarmRule($event, entity),
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
      this.cellActionDescriptors.push(
        {
          name: this.translate.instant('action.edit'),
          icon: 'edit',
          isEnabled: () => true,
          onAction: ($event, entity) => this.editCalculatedField($event, entity),
        }
      )
    }
  }

  fetchCalculatedFields(pageLink: PageLink): Observable<PageData<AlarmRuleTableEntity>> {
    return this.pageMode ?
      this.calculatedFieldsService.getCalculatedFields(pageLink, {types: [CalculatedFieldType.ALARM], ...this.alarmRuleFilterConfig}) :
      this.calculatedFieldsService.getCalculatedFieldsByEntityId(this.entityId, pageLink, CalculatedFieldType.ALARM);
  }

  onOpenDebugConfig($event: Event, calculatedField: AlarmRuleTableEntity): void {
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
        entityLabel: 'alarm-rule.alarm-rule',
        additionalActionConfig,
      },
      onSettingsAppliedFn: settings => this.onDebugConfigChanged(id.id, settings)
    }, $event.target as Element);
  }

  private editCalculatedField($event: Event, calculatedField: AlarmRuleTableEntity, isDirty = false): void {
    $event?.stopPropagation();
    this.getCalculatedAlarmDialog(calculatedField, 'action.apply', isDirty)
      .subscribe((res) => {
        if (res) {
          this.updateData();
        }
      });
  }

  private copyCalculatedField($event: Event, calculatedField: AlarmRuleTableEntity, isDirty = false): void {
    $event?.stopPropagation();
    const copyCalculatedAlarmRule = deepClone(calculatedField);
    if (this.pageMode) {
      copyCalculatedAlarmRule.entityId = null;
      delete (copyCalculatedAlarmRule as CalculatedFieldInfo).entityName;
    }
    delete copyCalculatedAlarmRule.id;
    this.getCalculatedAlarmDialog(copyCalculatedAlarmRule, 'action.apply', isDirty)
      .subscribe((res) => {
        if (res) {
          this.updateData();
        }
      });
  }

  private getCalculatedAlarmDialog(value?: AlarmRuleTableEntity, buttonTitle = 'action.add', isDirty = false): Observable<CalculatedField> {
    const entityId = this.entityId || value?.entityId;
    const entityName = this.entityName || (value as CalculatedFieldInfo)?.entityName;
    return this.dialog.open<AlarmRuleDialogComponent, AlarmRuleDialogData, CalculatedField>(AlarmRuleDialogComponent, {
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
        isDirty,
        getTestScriptDialogFn: this.getTestScriptDialog.bind(this),
      },
      enterAnimationDuration: isDirty ? 0 : null,
    })
      .afterClosed()
      .pipe(filter(Boolean));
  }

  private openDebugEventsDialog($event: Event, calculatedField: AlarmRuleTableEntity): void {
    $event?.stopPropagation();
    this.dialog.open<EventsDialogComponent, EventsDialogData, null>(EventsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        title: 'alarm-rule.debugging',
        tenantId: this.tenantId,
        entityId: calculatedField.id,
        debugEventTypes:[DebugEventType.DEBUG_CALCULATED_FIELD],
        disabledEventTypes:[EventType.LC_EVENT, EventType.ERROR, EventType.STATS],
        defaultEventType: DebugEventType.DEBUG_CALCULATED_FIELD,
        debugActionDisabled: true
      }
    })
      .afterClosed()
      .subscribe();
  }

  private openDebugTab($event: Event, calculatedField: AlarmRuleTableEntity) {
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

  private exportAlarmRule($event: Event, calculatedField: AlarmRuleTableEntity): void {
    $event?.stopPropagation();
    this.importExportService.exportCalculatedField(calculatedField.id.id);
  }

  private importCalculatedField(): void {
    this.importExportService.openCalculatedFieldImportDialog('alarm-rule.import', 'alarm-rule.file')
      .pipe(
        filter(Boolean),
        switchMap(calculatedField => {
          if (calculatedField.type !== CalculatedFieldType.ALARM) {
            this.store.dispatch(new ActionNotificationShow({
              message: this.translate.instant('alarm-rule.import-invalid-alarm-rule-type'),
              type: 'error',
              verticalPosition: 'top',
              horizontalPosition: 'left',
              duration: 5000
            }));
            return EMPTY;
          }
          return of(calculatedField);
        }),
        switchMap(calculatedField => this.getCalculatedAlarmDialog(this.updateImportedCalculatedField(calculatedField), 'action.add', true)),
        filter(Boolean),
        switchMap(calculatedField => this.calculatedFieldsService.saveCalculatedField(calculatedField)),
        filter(Boolean),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => this.updateData());
  }

  private updateImportedCalculatedField(calculatedField: CalculatedField): CalculatedField {
    if (calculatedField.type === CalculatedFieldType.ALARM) {
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

  private getTestScriptDialog(calculatedField: AlarmRuleTableEntity, argumentsObj?: CalculatedFieldEventArguments, openCalculatedFieldEdit = true, expression?: string): Observable<string> {
    if (calculatedField.type === CalculatedFieldType.ALARM) {
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
            expression,
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

  private openCalculatedField($event: Event, entity: AlarmRuleTableEntity) {
    $event?.stopPropagation();
    const url = this.router.createUrlTree(['alarms', 'alarm-rules', entity.id.id]);
    this.router.navigateByUrl(url);
  }

  private onCFAction(action: EntityAction<AlarmRuleTableEntity>): boolean {
    switch (action.action) {
      case 'open':
        this.openCalculatedField(action.event, action.entity);
        return true;
      case 'export':
        this.exportAlarmRule(action.event, action.entity);
        return true;
    }
    return false;
  }
}
