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
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  forwardRef,
  Input,
  Renderer2,
  ViewChild,
  ViewContainerRef,
} from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
} from '@angular/forms';
import {
  ArgumentEntityType,
  CalculatedFieldGeofencing,
  CalculatedFieldGeofencingValue,
  GeofencingReportStrategyTranslations,
} from '@shared/models/calculated-field.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { getEntityDetailsPageURL, isEqual } from '@core/utils';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { TbTableDatasource } from '@shared/components/table/table-datasource.abstract';
import { EntityService } from '@core/http/entity.service';
import { MatSort } from '@angular/material/sort';
import { getCurrentAuthState } from '@core/auth/auth.selectors';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { forkJoin, Observable } from 'rxjs';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { BaseData } from '@shared/models/base-data';
import {
  CalculatedFieldGeofencingZoneGroupsPanelComponent
} from '@home/components/calculated-fields/components/geofencing-configuration/calculated-field-geofencing-zone-groups-panel.component';

@Component({
  selector: 'tb-calculated-field-geofencing-zone-groups-table',
  templateUrl: './calculated-field-geofencing-zone-groups-table.component.html',
  styleUrls: [`../calculated-field-arguments/calculated-field-arguments-table.component.scss`],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => CalculatedFieldGeofencingZoneGroupsTableComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => CalculatedFieldGeofencingZoneGroupsTableComponent),
      multi: true
    }
  ],
})
export class CalculatedFieldGeofencingZoneGroupsTableComponent implements ControlValueAccessor, Validator, AfterViewInit {

  @Input({required: true}) entityId: EntityId;
  @Input({required: true}) tenantId: string;
  @Input({required: true}) entityName: string;
  @Input({required: true}) ownerId: EntityId;

  @ViewChild(MatSort, { static: true }) sort: MatSort;

  errorText = '';
  zoneGroupsFormArray = this.fb.array<CalculatedFieldGeofencingValue>([]);
  entityNameMap = new Map<string, string>();
  sortOrder = { direction: 'asc', property: '' };
  dataSource = new CalculatedFieldZoneDatasource();

  readonly GeofencingReportStrategyTranslations = GeofencingReportStrategyTranslations;
  readonly entityTypeTranslations = entityTypeTranslations;
  readonly ArgumentEntityType = ArgumentEntityType;
  readonly maxArgumentsPerCF = getCurrentAuthState(this.store).maxArgumentsPerCF - 2;
  readonly NULL_UUID = NULL_UUID;

  private popoverComponent: TbPopoverComponent<CalculatedFieldGeofencingZoneGroupsPanelComponent>;
  private propagateChange: (zonesObj: Record<string, CalculatedFieldGeofencing>) => void = () => {};

  constructor(
    private fb: FormBuilder,
    private popoverService: TbPopoverService,
    private viewContainerRef: ViewContainerRef,
    private cd: ChangeDetectorRef,
    private renderer: Renderer2,
    private entityService: EntityService,
    private destroyRef: DestroyRef,
    private store: Store<AppState>
  ) {
    this.zoneGroupsFormArray.valueChanges.pipe(takeUntilDestroyed()).subscribe(value => {
      this.updateDataSource(value);
      this.propagateChange(this.getZonesObject(value));
    });
  }

  ngAfterViewInit(): void {
    this.sort.sortChange.asObservable().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.sortOrder.property = this.sort.active;
      this.sortOrder.direction = this.sort.direction;
      this.updateDataSource(this.zoneGroupsFormArray.value);
    });
  }

  registerOnChange(fn: (zonesObj: Record<string, CalculatedFieldGeofencing>) => void): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_): void {}

  validate(): ValidationErrors | null {
    this.updateErrorText();
    return this.errorText ? { zonesFormArray: false } : null;
  }

  onDelete($event: Event, zone: CalculatedFieldGeofencingValue): void {
    $event.stopPropagation();
    const index = this.zoneGroupsFormArray.controls.findIndex(control => isEqual(control.value, zone));
    this.zoneGroupsFormArray.removeAt(index);
    this.zoneGroupsFormArray.markAsDirty();
  }

  manageZone($event: Event, matButton: MatButton, zone = {} as CalculatedFieldGeofencingValue): void {
    $event?.stopPropagation();
    if (this.popoverComponent && !this.popoverComponent.tbHidden) {
      this.popoverComponent.hide();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const index = this.zoneGroupsFormArray.controls.findIndex(control => isEqual(control.value, zone));
      const isExists = index !== -1;
      const ctx = {
        index,
        zone,
        entityId: this.entityId,
        buttonTitle: isExists ? 'action.apply' : 'action.add',
        tenantId: this.tenantId,
        entityName: this.entityName,
        ownerId: this.ownerId,
        usedNames: this.zoneGroupsFormArray.value.map(({ name }) => name).filter(name => name !== zone.name),
      };
      this.popoverComponent = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: CalculatedFieldGeofencingZoneGroupsPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: isExists ? ['leftOnly', 'leftTopOnly', 'leftBottomOnly'] : ['rightOnly', 'rightTopOnly', 'rightBottomOnly'],
        context: ctx,
        isModal: true
      });
      this.popoverComponent.tbComponentRef.instance.geofencingDataApplied.subscribe(({ entityName, ...value }) => {
        this.popoverComponent.hide();
        if (entityName) {
          this.entityNameMap.set(value.refEntityId.id, entityName);
        }
        if (isExists) {
          this.zoneGroupsFormArray.at(index).setValue(value);
        } else {
          this.zoneGroupsFormArray.push(this.fb.control(value));
        }
        this.cd.markForCheck();
      });
    }
  }

  private updateDataSource(value: CalculatedFieldGeofencingValue[]): void {
    const sortedValue = this.sortData(value);
    this.dataSource.loadData(sortedValue);
  }

  private updateErrorText(): void {
    if (this.zoneGroupsFormArray.controls.some(control => control.value.refEntityId?.id === NULL_UUID)) {
      this.errorText = 'calculated-fields.hint.geofencing-entity-not-found';
    } else {
      this.errorText = '';
    }
  }

  private getZonesObject(value: CalculatedFieldGeofencingValue[]): Record<string, CalculatedFieldGeofencing> {
    return value.reduce((acc, zoneValue) => {
      const { name, ...zone } = zoneValue as CalculatedFieldGeofencingValue;
      acc[name] = zone;
      return acc;
    }, {} as Record<string, CalculatedFieldGeofencing>);
  }

  writeValue(zonesObj: Record<string, CalculatedFieldGeofencing>): void {
    this.zoneGroupsFormArray.clear();
    this.populateZonesFormArray(zonesObj);
    this.updateEntityNameMap(this.zoneGroupsFormArray.value);
  }

  getEntityDetailsPageURL(id: string, type: EntityType): string {
    return getEntityDetailsPageURL(id, type);
  }

  private populateZonesFormArray(zonesObj: Record<string, CalculatedFieldGeofencing>): void {
    Object.keys(zonesObj).forEach(key => {
      const value: CalculatedFieldGeofencingValue = {
        ...zonesObj[key],
        name: key
      };
      this.zoneGroupsFormArray.push(this.fb.control(value), { emitEvent: false });
    });
    this.zoneGroupsFormArray.updateValueAndValidity();
  }

  private updateEntityNameMap(values: CalculatedFieldGeofencingValue[]): void {
    const entitiesByType = values.reduce((acc, { refEntityId = {}}) => {
      if (refEntityId.id && refEntityId.entityType !== ArgumentEntityType.Tenant) {
        const { id, entityType } = refEntityId as EntityId;
        acc[entityType] = acc[entityType] ?? [];
        acc[entityType].push(id);
      }
      return acc;
    }, {} as Record<EntityType, string[]>);
    const tasks = Object.entries(entitiesByType).map(([entityType, ids]) =>
      this.entityService.getEntities(entityType as EntityType, ids)
    );
    if (!tasks.length) {
      return;
    }
    this.fetchEntityNames(tasks, values);
  }

  private fetchEntityNames(tasks: Observable<BaseData<EntityId>[]>[], values: CalculatedFieldGeofencingValue[]): void {
    forkJoin(tasks as Observable<BaseData<EntityId>[]>[])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result: Array<BaseData<EntityId>>[]) => {
        result.forEach((entities: BaseData<EntityId>[]) => entities.forEach((entity: BaseData<EntityId>) => this.entityNameMap.set(entity.id.id, entity.name)));
        let updateTable = false;
        values.forEach(({ refEntityId }) => {
          if (refEntityId?.id && !this.entityNameMap.has(refEntityId.id) && refEntityId.entityType !== ArgumentEntityType.Tenant) {
            updateTable = true;
            const control = this.zoneGroupsFormArray.controls.find(control => control.value.refEntityId?.id === refEntityId.id);
            const value = control.value;
            value.refEntityId.id = NULL_UUID;
            control.setValue(value, { emitEvent: false });
          }
        });
        if (updateTable) {
          this.zoneGroupsFormArray.updateValueAndValidity();
        }
      });
  }

  private getSortValue(zone: CalculatedFieldGeofencingValue, column: string): string {
    switch (column) {
      case 'entityType':
        if (zone.refEntityId?.entityType === ArgumentEntityType.Tenant) {
          return 'calculated-fields.argument-current-tenant';
        } else if (zone.refDynamicSourceConfiguration.type === ArgumentEntityType.RelationQuery) {
          return 'calculated-fields.argument-relation-query';
        } else if (zone.refEntityId?.id) {
          return entityTypeTranslations.get((zone.refEntityId)?.entityType as unknown as EntityType).type;
        } else {
          return 'calculated-fields.argument-current';
        }
      case 'key':
        return zone.perimeterKeyName;
      case 'reportStrategy':
        return GeofencingReportStrategyTranslations.get(zone.reportStrategy);
      default:
        return zone.name;
    }
  }

  private sortData(data: CalculatedFieldGeofencingValue[]): CalculatedFieldGeofencingValue[] {
    return data.sort((a, b) => {
      const valA = this.getSortValue(a, this.sortOrder.property) ?? '';
      const valB = this.getSortValue(b, this.sortOrder.property) ?? '';
      return (this.sortOrder.direction === 'asc' ? 1 : -1) * valA.localeCompare(valB);
    });
  }
}

class CalculatedFieldZoneDatasource extends TbTableDatasource<CalculatedFieldGeofencingValue> {
  constructor() {
    super();
  }
}
