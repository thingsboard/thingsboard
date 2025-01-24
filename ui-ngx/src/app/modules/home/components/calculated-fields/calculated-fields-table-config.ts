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
import { TimePageLink } from '@shared/models/page/page-link';
import { Observable, of } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { EntityId } from '@shared/models/id/entity-id';
import { DialogService } from '@core/services/dialog.service';
import { MINUTE } from '@shared/models/time/time.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { ChangeDetectorRef, DestroyRef, ViewContainerRef } from '@angular/core';
import { Overlay } from '@angular/cdk/overlay';
import { UtilsService } from '@core/services/utils.service';
import { EntityService } from '@core/http/entity.service';
import { EntityDebugSettings } from '@shared/models/entity.models';
import { DurationLeftPipe } from '@shared/pipe/duration-left.pipe';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TbPopoverService } from '@shared/components/popover.service';
import { EntityDebugSettingsPanelComponent } from '@home/components/entity/debug/entity-debug-settings-panel.component';
import { CalculatedFieldsService } from '@core/http/calculated-fields.service';
import { catchError, switchMap } from 'rxjs/operators';
import { CalculatedField } from '@shared/models/calculated-field.models';

export class CalculatedFieldsTableConfig extends EntityTableConfig<CalculatedField, TimePageLink> {

  readonly calculatedFieldsDebugPerTenantLimitsConfiguration =
    getCurrentAuthState(this.store)['calculatedFieldsDebugPerTenantLimitsConfiguration'] || '1:1';
  readonly maxDebugModeDuration = getCurrentAuthState(this.store).maxDebugModeDurationMinutes * MINUTE;

  constructor(private calculatedFieldsService: CalculatedFieldsService,
              private entityService: EntityService,
              private dialogService: DialogService,
              private translate: TranslateService,
              private dialog: MatDialog,
              public entityId: EntityId = null,
              private store: Store<AppState>,
              private viewContainerRef: ViewContainerRef,
              private overlay: Overlay,
              private cd: ChangeDetectorRef,
              private utilsService: UtilsService,
              private durationLeft: DurationLeftPipe,
              private popoverService: TbPopoverService,
              private destroyRef: DestroyRef,
  ) {
    super();
    this.tableTitle = this.translate.instant('entity.type-calculated-fields');
    this.detailsPanelEnabled = false;
    this.selectionEnabled = true;
    this.searchEnabled = true;
    this.addEnabled = true;
    this.entitiesDeleteEnabled = true;
    this.actionsColumnTitle = '';
    this.entityType = EntityType.CALCULATED_FIELD;
    this.entityTranslations = entityTypeTranslations.get(EntityType.CALCULATED_FIELD);

    this.entitiesFetchFunction = pageLink => this.fetchCalculatedFields(pageLink);

    this.defaultSortOrder = {property: 'name', direction: Direction.DESC};

    this.columns.push(
      new EntityTableColumn<CalculatedField>('name', 'common.name', '33%'));
    this.columns.push(
      new EntityTableColumn<CalculatedField>('type', 'common.type', '50px'));
    this.columns.push(
      new EntityTableColumn<CalculatedField>('expression', 'calculated-fields.expression', '50%', entity => entity.configuration.expression));

    this.cellActionDescriptors.push(
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
        // // [TODO]: [Calculated fields] - implement edit
        onAction: (_, entity) => {}
      }
    );
  }

  fetchCalculatedFields(pageLink: TimePageLink): Observable<PageData<CalculatedField>> {
    return this.calculatedFieldsService.getCalculatedFields(pageLink);
  }

  onOpenDebugConfig($event: Event, { debugSettings = {}, id }: CalculatedField): void {
    const { renderer, viewContainerRef } = this.getTable();
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = $event.target as Element;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const debugStrategyPopover = this.popoverService.displayPopover(trigger, renderer,
        viewContainerRef, EntityDebugSettingsPanelComponent, 'bottom', true, null,
        {
          debugLimitsConfiguration: this.calculatedFieldsDebugPerTenantLimitsConfiguration,
          maxDebugModeDuration: this.maxDebugModeDuration,
          entityLabel: this.translate.instant('debug-settings.integration'),
          ...debugSettings
        },
        {},
        {}, {}, true);
      debugStrategyPopover.tbComponentRef.instance.popover = debugStrategyPopover;
      debugStrategyPopover.tbComponentRef.instance.onSettingsApplied.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((settings: EntityDebugSettings) => {
        this.onDebugConfigChanged(id.id, settings);
        debugStrategyPopover.hide();
      });
    }
  }

  private getDebugConfigLabel(debugSettings: EntityDebugSettings): string {
    const isDebugActive = this.isDebugActive(debugSettings?.allEnabledUntil);

    if (!isDebugActive) {
      return debugSettings?.failuresEnabled ? this.translate.instant('debug-settings.failures') : this.translate.instant('common.disabled');
    } else {
      return this.durationLeft.transform(debugSettings?.allEnabledUntil)
    }
  }

  private isDebugActive(allEnabledUntil: number): boolean {
    return allEnabledUntil > new Date().getTime();
  }

  private onDebugConfigChanged(id: string, debugSettings: EntityDebugSettings): void {
    this.calculatedFieldsService.getCalculatedField(id).pipe(
      switchMap(field => this.calculatedFieldsService.saveCalculatedField({ ...field, debugSettings })),
      catchError(() => of(null)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(() => this.updateData());
  }
}
