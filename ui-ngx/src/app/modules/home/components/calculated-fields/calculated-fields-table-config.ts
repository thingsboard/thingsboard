///
/// Copyright © 2016-2024 The Thingsboard Authors
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

import { EntityTableColumn, EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { TranslateService } from '@ngx-translate/core';
import { Direction } from '@shared/models/page/sort-order';
import { MatDialog } from '@angular/material/dialog';
import { PageLink } from '@shared/models/page/page-link';
import { Observable, of } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { EntityId } from '@shared/models/id/entity-id';
import { MINUTE } from '@shared/models/time/time.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState, getCurrentAuthUser } from '@core/auth/auth.selectors';
import { DestroyRef, Renderer2 } from '@angular/core';
import { EntityDebugSettings } from '@shared/models/entity.models';
import { DurationLeftPipe } from '@shared/pipe/duration-left.pipe';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TbPopoverService } from '@shared/components/popover.service';
import { EntityDebugSettingsPanelComponent } from '@home/components/entity/debug/entity-debug-settings-panel.component';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { catchError, filter, switchMap, tap } from 'rxjs/operators';
import {
  ArgumentType,
  CalculatedField,
  CalculatedFieldEventArguments,
  CalculatedFieldDebugDialogData,
  CalculatedFieldDialogData,
  CalculatedFieldTestScriptDialogData,
  getCalculatedFieldArgumentsEditorCompleter,
  getCalculatedFieldArgumentsHighlights,
  CalculatedFieldTypeTranslations,
} from '@shared/models/calculated-field.models';
import {
  CalculatedFieldDebugDialogComponent,
  CalculatedFieldDialogComponent,
  CalculatedFieldScriptTestDialogComponent
} from './components/public-api';
import { ImportExportService } from '@shared/import-export/import-export.service';
import { isObject } from '@core/utils';

export class CalculatedFieldsTableConfig extends EntityTableConfig<CalculatedField, PageLink> {

  // TODO: [Calculated Fields] remove hardcode when BE variable implemented
  readonly calculatedFieldsDebugPerTenantLimitsConfiguration =
    getCurrentAuthState(this.store)['calculatedFieldsDebugPerTenantLimitsConfiguration'] || '1:1';
  readonly maxDebugModeDuration = getCurrentAuthState(this.store).maxDebugModeDurationMinutes * MINUTE;
  readonly tenantId = getCurrentAuthUser(this.store).tenantId;
  additionalDebugActionConfig = {
    title: this.translate.instant('calculated-fields.see-debug-events'),
    action: (calculatedField: CalculatedField) => this.openDebugEventsDialog.call(this, calculatedField),
  };

  constructor(private calculatedFieldsService: CalculatedFieldsService,
              private translate: TranslateService,
              private dialog: MatDialog,
              public entityId: EntityId = null,
              private store: Store<AppState>,
              private durationLeft: DurationLeftPipe,
              private popoverService: TbPopoverService,
              private destroyRef: DestroyRef,
              private renderer: Renderer2,
              public entityName: string,
              private importExportService: ImportExportService
  ) {
    super();
    this.tableTitle = this.translate.instant('entity.type-calculated-fields');
    this.detailsPanelEnabled = false;
    this.pageMode = false;
    this.entityType = EntityType.CALCULATED_FIELD;
    this.entityTranslations = entityTypeTranslations.get(EntityType.CALCULATED_FIELD);

    this.entitiesFetchFunction = (pageLink: PageLink) => this.fetchCalculatedFields(pageLink);
    this.addEntity = this.getCalculatedFieldDialog.bind(this);
    this.deleteEntityTitle = (field: CalculatedField) => this.translate.instant('calculated-fields.delete-title', {title: field.name});
    this.deleteEntityContent = () => this.translate.instant('calculated-fields.delete-text');
    this.deleteEntitiesTitle = count => this.translate.instant('calculated-fields.delete-multiple-title', {count});
    this.deleteEntitiesContent = () => this.translate.instant('calculated-fields.delete-multiple-text');
    this.deleteEntity = id => this.calculatedFieldsService.deleteCalculatedField(id.id);
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

    this.defaultSortOrder = {property: 'name', direction: Direction.DESC};

    const expressionColumn = new EntityTableColumn<CalculatedField>('expression', 'calculated-fields.expression', '33%', entity => entity.configuration?.expression);
    expressionColumn.sortable = false;

    this.columns.push(new EntityTableColumn<CalculatedField>('name', 'common.name', '33%'));
    this.columns.push(new EntityTableColumn<CalculatedField>('type', 'common.type', '50px', entity => this.translate.instant(CalculatedFieldTypeTranslations.get(entity.type))));
    this.columns.push(expressionColumn);

    this.cellActionDescriptors.push(
      {
        name: this.translate.instant('action.export'),
        icon: 'file_download',
        isEnabled: () => true,
        onAction: (event$, entity) => this.exportCalculatedField(event$, entity),
      },
      {
        name: this.translate.instant('entity-view.events'),
        icon: 'mdi:clipboard-text-clock',
        isEnabled: () => true,
        onAction: (_, entity) => this.openDebugEventsDialog(entity),
      },
      {
        name: '',
        nameFunction: entity => this.getDebugConfigLabel(entity?.debugSettings),
        icon: 'mdi:bug',
        isEnabled: () => true,
        iconFunction: ({ debugSettings }) => this.isDebugActive(debugSettings?.allEnabledUntil) || debugSettings?.failuresEnabled ? 'mdi:bug' : 'mdi:bug-outline',
        onAction: ($event, entity) => this.onOpenDebugConfig($event, entity),
      },
      {
        name: this.translate.instant('action.edit'),
        icon: 'edit',
        isEnabled: () => true,
        onAction: (_, entity) => this.editCalculatedField(entity),
      }
    );
  }

  fetchCalculatedFields(pageLink: PageLink): Observable<PageData<CalculatedField>> {
    return this.calculatedFieldsService.getCalculatedFields(this.entityId, pageLink);
  }

  onOpenDebugConfig($event: Event, calculatedField: CalculatedField): void {
    const { debugSettings = {}, id } = calculatedField;
    const additionalActionConfig = {
      ...this.additionalDebugActionConfig,
      action: () => this.openDebugEventsDialog(calculatedField)
    };
    const { viewContainerRef } = this.getTable();
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = $event.target as Element;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const debugStrategyPopover = this.popoverService.displayPopover(trigger, this.renderer,
        viewContainerRef, EntityDebugSettingsPanelComponent, 'bottom', true, null,
        {
          debugLimitsConfiguration: this.calculatedFieldsDebugPerTenantLimitsConfiguration,
          maxDebugModeDuration: this.maxDebugModeDuration,
          entityLabel: this.translate.instant('debug-settings.calculated-field'),
          additionalActionConfig,
          ...debugSettings
        },
        {},
        {}, {}, true);
      debugStrategyPopover.tbComponentRef.instance.onSettingsApplied.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((settings: EntityDebugSettings) => {
        this.onDebugConfigChanged(id.id, settings);
        debugStrategyPopover.hide();
      });
    }
  }

  private editCalculatedField(calculatedField: CalculatedField, isDirty = false): void {
    this.getCalculatedFieldDialog(calculatedField, 'action.apply', isDirty)
      .subscribe((res) => {
        if (res) {
          this.updateData();
        }
      });
  }

  private getCalculatedFieldDialog(value?: CalculatedField, buttonTitle = 'action.add', isDirty = false): Observable<CalculatedField> {
    return this.dialog.open<CalculatedFieldDialogComponent, CalculatedFieldDialogData, CalculatedField>(CalculatedFieldDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        value,
        buttonTitle,
        entityId: this.entityId,
        debugLimitsConfiguration: this.calculatedFieldsDebugPerTenantLimitsConfiguration,
        tenantId: this.tenantId,
        entityName: this.entityName,
        additionalDebugActionConfig: this.additionalDebugActionConfig,
        getTestScriptDialogFn: this.getTestScriptDialog.bind(this),
        isDirty,
      },
      enterAnimationDuration: isDirty ? 0 : null,
    })
      .afterClosed()
      .pipe(filter(Boolean));
  }

  private openDebugEventsDialog(calculatedField: CalculatedField): void {
    this.dialog.open<CalculatedFieldDebugDialogComponent, CalculatedFieldDebugDialogData, null>(CalculatedFieldDebugDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        tenantId: this.tenantId,
        value: calculatedField,
        getTestScriptDialogFn: this.getTestScriptDialog.bind(this),
      }
    })
      .afterClosed()
      .subscribe();
  }

  private exportCalculatedField($event: Event, calculatedField: CalculatedField): void {
    if ($event) {
      $event.stopPropagation();
    }
    this.importExportService.exportCalculatedField(calculatedField.id.id);
  }

  private importCalculatedField(): void {
    this.importExportService.importCalculatedField(this.entityId)
      .pipe(filter(Boolean), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.updateData());
  }

  private getDebugConfigLabel(debugSettings: EntityDebugSettings): string {
    const isDebugActive = this.isDebugActive(debugSettings?.allEnabledUntil);

    if (!isDebugActive) {
      return debugSettings?.failuresEnabled ? this.translate.instant('debug-settings.failures') : this.translate.instant('common.disabled');
    } else {
      return this.durationLeft.transform(debugSettings?.allEnabledUntil);
    }
  }

  private isDebugActive(allEnabledUntil: number): boolean {
    return allEnabledUntil > new Date().getTime();
  }

  private onDebugConfigChanged(id: string, debugSettings: EntityDebugSettings): void {
    this.calculatedFieldsService.getCalculatedFieldById(id).pipe(
      switchMap(field => this.calculatedFieldsService.saveCalculatedField({ ...field, debugSettings })),
      catchError(() => of(null)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => this.updateData());
  }

  private getTestScriptDialog(calculatedField: CalculatedField, argumentsObj?: CalculatedFieldEventArguments, openCalculatedFieldEdit = true): Observable<string> {
    const resultArguments = Object.keys(calculatedField.configuration.arguments).reduce((acc, key) => {
      const type = calculatedField.configuration.arguments[key].refEntityKey.type;
      acc[key] = isObject(argumentsObj) && argumentsObj.hasOwnProperty(key)
        ? { ...argumentsObj[key], type }
        : type === ArgumentType.Rolling ? { values: [], type } : { value: '', type, ts: new Date().getTime() };
      return acc;
    }, {});
    return this.dialog.open<CalculatedFieldScriptTestDialogComponent, CalculatedFieldTestScriptDialogData, string>(CalculatedFieldScriptTestDialogComponent,
      {
        disableClose: true,
        panelClass: ['tb-dialog', 'tb-fullscreen-dialog', 'tb-fullscreen-dialog-gt-xs'],
        data: {
          arguments: resultArguments,
          expression: calculatedField.configuration.expression,
          argumentsEditorCompleter: getCalculatedFieldArgumentsEditorCompleter(calculatedField.configuration.arguments),
          argumentsHighlightRules: getCalculatedFieldArgumentsHighlights(calculatedField.configuration.arguments),
          openCalculatedFieldEdit
        }
      }).afterClosed()
      .pipe(
        filter(Boolean),
        tap(expression => {
          if (openCalculatedFieldEdit) {
            this.editCalculatedField({ entityId: this.entityId, ...calculatedField, configuration: {...calculatedField.configuration, expression } }, true)
          }
        }),
      );
  }
}
