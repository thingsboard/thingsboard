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

import { Component, ElementRef, EventEmitter, forwardRef, Input, OnInit, Output, ViewChild } from '@angular/core';
import { MatFormFieldAppearance, SubscriptSizing } from '@angular/material/form-field';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { firstValueFrom, merge, Observable, of, shareReplay, Subject } from 'rxjs';
import { catchError, debounceTime, first, map, switchMap, tap } from 'rxjs/operators';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { AliasEntityType, EntityType } from '@shared/models/entity-type.models';
import { BaseData, getEntityDisplayName } from '@shared/models/base-data';
import { EntityId } from '@shared/models/id/entity-id';
import { EntityService } from '@core/http/entity.service';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { Authority } from '@shared/models/authority.enum';
import { getEntityDetailsPageURL, isDefinedAndNotNull, isEqual, objectRequired } from '@core/utils';
import { coerceArray, coerceBoolean } from '@shared/decorators/coercion';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';

@Component({
  selector: 'tb-entity-autocomplete',
  templateUrl: './entity-autocomplete.component.html',
  styleUrls: ['./entity-autocomplete.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => EntityAutocompleteComponent),
    multi: true
  }]
})
export class EntityAutocompleteComponent implements ControlValueAccessor, OnInit {

  selectEntityFormGroup: UntypedFormGroup;

  private modelValue: string | EntityId | null;

  private entityTypeValue: EntityType | AliasEntityType;

  private entitySubtypeValue: string;

  private entityText: string;

  noEntitiesMatchingText: string;
  notFoundEntities = 'entity.no-entities-text';

  private entityRequiredText: string;

  filteredEntities: Observable<Array<BaseData<EntityId>>>;

  searchText = '';

  entityURL: string;

  private dirty = false;

  private refresh$ = new Subject<Array<BaseData<EntityId>>>();

  private propagateChange: (value: any) => void = () => { };

  private onTouched = () => { };

  @Input()
  set entityType(entityType: EntityType) {
    if (this.entityTypeValue !== entityType) {
      this.entityTypeValue = entityType;
      this.load();
      this.reset();
      this.refresh$.next([]);
      this.dirty = true;
    }
  }

  @Input()
  set entitySubtype(entitySubtype: string) {
    if (this.entitySubtypeValue !== entitySubtype) {
      this.entitySubtypeValue = entitySubtype;
      const currentEntity = this.getCurrentEntity();
      if (currentEntity) {
        if ((currentEntity as any).type !== this.entitySubtypeValue) {
          this.reset();
          this.refresh$.next([]);
          this.dirty = true;
        }
      }
      this.selectEntityFormGroup.get('entity').updateValueAndValidity();
    }
  }

  @Input()
  entityNotValidText = 'entity.entity-not-valid';

  @Input()
  excludeEntityIds: Array<string>;

  @Input()
  labelText: string;

  @Input()
  requiredText: string;

  @Input()
  placeholder: string;

  @Input()
  @coerceBoolean()
  useFullEntityId: boolean;

  @Input()
  appearance: MatFormFieldAppearance = 'fill';

  @Input()
  @coerceBoolean()
  inlineField: boolean;

  @Input()
  @coerceBoolean()
  required: boolean;

  @Input()
  @coerceBoolean()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  allowCreateNew: boolean;

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  @coerceArray()
  additionalClasses: Array<string>;

  @Input()
  @coerceBoolean()
  useEntityDisplayName = false;

  @Output()
  entityChanged = new EventEmitter<BaseData<EntityId>>();

  @Output()
  createNew = new EventEmitter<string>();

  @ViewChild('entityInput', {static: true}) entityInput: ElementRef;

  @ViewChild('autocompleteTrigger') autocompleteTrigger: MatAutocompleteTrigger;


  get requiredErrorText(): string {
    if (this.requiredText && this.requiredText.length) {
      return this.requiredText;
    }
    return this.entityRequiredText;
  }

  get label(): string {
    if (this.labelText && this.labelText.length) {
      return this.labelText;
    }
    return this.entityText;
  }


  constructor(private store: Store<AppState>,
              private entityService: EntityService,
              private fb: UntypedFormBuilder) {
    this.selectEntityFormGroup = this.fb.group({
      entity: [null, objectRequired()]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  ngOnInit() {
    this.filteredEntities = merge(
      this.refresh$.asObservable(),
      this.selectEntityFormGroup.get('entity').valueChanges
        .pipe(
          debounceTime(150),
          tap(value => {
            let modelValue: string | EntityId;
            if (typeof value === 'string' || !value) {
              modelValue = null;
            } else {
              modelValue = this.useFullEntityId ? value.id : value.id.id;
            }
            this.updateView(modelValue, value);
            if (value === null) {
              this.clear();
            }
          }),
          // startWith<string | BaseData<EntityId>>(''),
          map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
          switchMap(name => this.fetchEntities(name)),
          shareReplay(1)
        )
    );
  }

  private load(): void {
    if (this.entityTypeValue) {
      switch (this.entityTypeValue) {
        case EntityType.ASSET:
          this.entityText = 'asset.asset';
          this.noEntitiesMatchingText = 'asset.no-assets-matching';
          this.entityRequiredText = 'asset.asset-required';
          this.notFoundEntities = 'asset.no-assets-text';
          this.entityNotValidText = 'asset.asset-not-valid';
          break;
        case EntityType.DEVICE:
          this.entityText = 'device.device';
          this.noEntitiesMatchingText = 'device.no-devices-matching';
          this.entityRequiredText = 'device.device-required';
          this.notFoundEntities = 'device.no-devices-text';
          this.entityNotValidText = 'device.device-not-valid';
          break;
        case EntityType.EDGE:
          this.entityText = 'edge.edge';
          this.noEntitiesMatchingText = 'edge.no-edges-matching';
          this.entityRequiredText = 'edge.edge-required';
          this.notFoundEntities = 'edge.no-edges-text';
          this.entityNotValidText = 'edge.edge-not-valid';
          break;
        case EntityType.ENTITY_VIEW:
          this.entityText = 'entity-view.entity-view';
          this.noEntitiesMatchingText = 'entity-view.no-entity-views-matching';
          this.entityRequiredText = 'entity-view.entity-view-required';
          this.notFoundEntities = 'entity-view.no-entity-views-text';
          this.entityNotValidText = 'entity-view.entity-view-not-valid';
          break;
        case EntityType.RULE_CHAIN:
          this.entityText = 'rulechain.rulechain';
          this.noEntitiesMatchingText = 'rulechain.no-rulechains-matching';
          this.entityRequiredText = 'rulechain.rulechain-required';
          this.notFoundEntities = 'rulechain.no-rulechains-text';
          this.entityNotValidText = 'rulechain.rulechain-not-valid';
          break;
        case EntityType.TENANT:
        case AliasEntityType.CURRENT_TENANT:
          this.entityText = 'tenant.tenant';
          this.noEntitiesMatchingText = 'tenant.no-tenants-matching';
          this.entityRequiredText = 'tenant.tenant-required';
          this.notFoundEntities = 'tenant.no-tenants-text';
          this.entityNotValidText = 'tenant.tenant-not-valid';
          break;
        case EntityType.CUSTOMER:
          this.entityText = 'customer.customer';
          this.noEntitiesMatchingText = 'customer.no-customers-matching';
          this.entityRequiredText = 'customer.customer-required';
          this.notFoundEntities = 'customer.no-customers-text';
          this.entityNotValidText = 'customer.customer-not-valid';
          break;
        case EntityType.USER:
        case AliasEntityType.CURRENT_USER:
          this.entityText = 'user.user';
          this.noEntitiesMatchingText = 'user.no-users-matching';
          this.entityRequiredText = 'user.user-required';
          this.notFoundEntities = 'user.no-users-text';
          this.entityNotValidText = 'user.user-not-valid';
          break;
        case EntityType.DASHBOARD:
          this.entityText = 'dashboard.dashboard';
          this.noEntitiesMatchingText = 'dashboard.no-dashboards-matching';
          this.entityRequiredText = 'dashboard.dashboard-required';
          this.notFoundEntities = 'dashboard.no-dashboards-text';
          this.entityNotValidText = 'dashboard.dashboard-not-valid';
          break;
        case EntityType.ALARM:
          this.entityText = 'alarm.alarm';
          this.noEntitiesMatchingText = 'alarm.no-alarms-matching';
          this.entityRequiredText = 'alarm.alarm-required';
          this.notFoundEntities = 'alarm.no-alarms-prompt';
          this.entityNotValidText = 'alarm.alarm-not-valid';
          break;
        case EntityType.QUEUE_STATS:
          this.entityText = 'queue-statistics.queue-statistics';
          this.noEntitiesMatchingText = 'queue-statistics.no-queue-statistics-matching';
          this.entityRequiredText = 'queue-statistics.queue-statistics-required';
          this.notFoundEntities = 'queue-statistics.no-queue-statistics-text';
          this.entityNotValidText = 'queue-statistics.queue-statistics-not-valid';
          break;
        case EntityType.MOBILE_APP:
          this.entityText = 'mobile.application';
          this.noEntitiesMatchingText = 'mobile.no-application-matching';
          this.entityRequiredText = 'mobile.application-required';
          this.notFoundEntities = 'mobile.no-application-text';
          this.entityNotValidText = 'mobile.application-not-valid';
          break;
        case EntityType.MOBILE_APP_BUNDLE:
          this.entityText = 'mobile.bundle';
          this.noEntitiesMatchingText = 'mobile.no-bundle-matching';
          this.entityRequiredText = 'mobile.bundle-required';
          this.notFoundEntities = 'mobile.no-bundle-text';
          this.entityNotValidText = 'mobile.bundle-not-valid';
          break;
        case EntityType.NOTIFICATION_TARGET:
          this.entityText = 'notification.notification-recipient';
          this.noEntitiesMatchingText = 'notification.no-recipients-matching';
          this.entityRequiredText = 'notification.notification-recipient-required';
          this.notFoundEntities = 'notification.no-recipients-text';
          this.entityNotValidText = 'notification.notification-recipient-not-valid';
          break;
        case EntityType.AI_MODEL:
          this.entityText = 'ai-models.ai-model';
          this.noEntitiesMatchingText = 'ai-models.no-model-matching';
          this.entityRequiredText = 'ai-models.model-required';
          this.notFoundEntities = 'ai-models.no-model-text';
          this.entityNotValidText = 'ai-models.model-not-valid';
          break;
        case EntityType.DEVICE_PROFILE:
          this.entityText = 'device-profile.device-profile';
          this.noEntitiesMatchingText = 'device-profile.no-device-profiles-matching';
          this.entityRequiredText = 'device-profile.device-profile-required';
          this.notFoundEntities = 'device-profile.no-device-profiles-text';
          this.entityNotValidText = 'device-profile.device-profile-not-valid';
          break;
        case EntityType.ASSET_PROFILE:
          this.entityText = 'asset-profile.asset-profile';
          this.noEntitiesMatchingText = 'asset-profile.no-asset-profiles-matching';
          this.entityRequiredText = 'asset-profile.asset-profile-required';
          this.notFoundEntities = 'asset-profile.no-asset-profiles-text';
          this.entityNotValidText = 'asset-profile.asset-profile-not-valid';
          break;
        case AliasEntityType.CURRENT_CUSTOMER:
          this.entityText = 'customer.default-customer';
          this.noEntitiesMatchingText = 'customer.no-customers-matching';
          this.entityRequiredText = 'customer.default-customer-required';
          this.notFoundEntities = 'customer.no-customers-text';
          this.entityNotValidText = 'customer.customer-not-valid';
          break;
        case AliasEntityType.CURRENT_USER_OWNER:
          const authUser =  getCurrentAuthUser(this.store);
          if (authUser.authority === Authority.TENANT_ADMIN) {
            this.entityText = 'tenant.tenant';
            this.noEntitiesMatchingText = 'tenant.no-tenants-matching';
            this.entityRequiredText = 'tenant.tenant-required';
            this.entityNotValidText = 'tenant.tenant-not-valid';
          } else {
            this.entityText = 'customer.customer';
            this.noEntitiesMatchingText = 'customer.no-customers-matching';
            this.entityRequiredText = 'customer.customer-required';
            this.entityNotValidText = 'customer.customer-not-valid';
          }
          break;
      }
    }
    const currentEntity = this.getCurrentEntity();
    if (currentEntity) {
      const currentEntityType = currentEntity.id.entityType;
      if (this.entityTypeValue && currentEntityType !== this.entityTypeValue) {
        this.reset();
      }
    }
  }

  private getCurrentEntity(): BaseData<EntityId> | null {
    const currentEntity = this.selectEntityFormGroup.get('entity').value;
    if (currentEntity && typeof currentEntity !== 'string') {
      return currentEntity as BaseData<EntityId>;
    } else {
      return null;
    }
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectEntityFormGroup.disable({emitEvent: false});
    } else {
      this.selectEntityFormGroup.enable({emitEvent: false});
    }
  }

  async writeValue(value: string | EntityId | null): Promise<void> {
    this.searchText = '';
    if (isDefinedAndNotNull(value) && (typeof value === 'string' || (value.entityType && value.id))) {
      let targetEntityType: EntityType;
      let id: string;
      if (typeof value === 'string') {
        targetEntityType = this.checkEntityType(this.entityTypeValue);
        id = value;
      } else {
        targetEntityType = this.checkEntityType(value.entityType);
        id = value.id;
      }
      let entity: BaseData<EntityId> = null;
      try {
        entity = await firstValueFrom(this.entityService.getEntity(targetEntityType, id, {ignoreLoading: true, ignoreErrors: true}));
      } catch (e) {
        this.propagateChange(null);
      }
      this.modelValue = entity !== null ? (this.useFullEntityId ? entity.id : entity.id.id) : null;
      this.entityURL = !entity ? '' : getEntityDetailsPageURL(entity.id.id, targetEntityType);
      this.selectEntityFormGroup.get('entity').patchValue(entity !== null ? entity : '', {emitEvent: false});
      this.entityChanged.emit(entity);
    } else {
      this.modelValue = null;
      this.entityURL = '';
      this.selectEntityFormGroup.get('entity').patchValue('', {emitEvent: false});
    }
    this.dirty = true;
  }

  onFocus() {
    if (this.dirty) {
      this.selectEntityFormGroup.get('entity').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  private reset() {
    this.selectEntityFormGroup.get('entity').patchValue('', {emitEvent: false});
  }

  private updateView(value: string | EntityId | null, entity: BaseData<EntityId> | null) {
    if (!isEqual(this.modelValue, value)) {
      this.modelValue = value;
      this.entityURL = (typeof entity === 'string' || !entity) ? '' : getEntityDetailsPageURL(entity.id.id, entity.id.entityType as EntityType);
      this.propagateChange(this.modelValue);
      this.entityChanged.emit(entity);
    }
  }

  displayEntityFn(entity?: BaseData<EntityId>): string | undefined {
    return entity ? (this.useEntityDisplayName ? getEntityDisplayName(entity) : entity.name) : undefined;
  }

  private fetchEntities(searchText?: string): Observable<Array<BaseData<EntityId>>> {
    this.searchText = searchText;
    const targetEntityType = this.checkEntityType(this.entityTypeValue);
    return this.entityService.getEntitiesByNameFilter(targetEntityType, searchText,
      50, this.entitySubtypeValue, {ignoreLoading: true}).pipe(
      catchError(() => of(null)),
      map((data) => {
        if (data) {
          if (this.excludeEntityIds && this.excludeEntityIds.length) {
            const excludeEntityIdsSet = new Set(this.excludeEntityIds);
            const entities: Array<BaseData<EntityId>> = [];
            data.forEach(entity => !excludeEntityIdsSet.has(entity.id.id) && entities.push(entity));
            return entities;
          } else {
            return data;
          }
        } else {
          return [];
        }
      }
    ));
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  clear() {
    this.selectEntityFormGroup.get('entity').patchValue('', {emitEvent: true});
    setTimeout(() => {
      this.entityInput.nativeElement.blur();
      this.entityInput.nativeElement.focus();
    }, 0);
  }

  private checkEntityType(entityType: EntityType | AliasEntityType): EntityType {
    if (entityType === AliasEntityType.CURRENT_CUSTOMER) {
      return EntityType.CUSTOMER;
    } else if (entityType === AliasEntityType.CURRENT_TENANT) {
      return EntityType.TENANT;
    } else if (entityType === AliasEntityType.CURRENT_USER) {
      return EntityType.USER;
    } else if (entityType === AliasEntityType.CURRENT_USER_OWNER) {
      const authUser =  getCurrentAuthUser(this.store);
      if (authUser.authority === Authority.TENANT_ADMIN) {
        return EntityType.TENANT;
      } else {
        return EntityType.CUSTOMER;
      }
    }
    return entityType;
  }

  createNewEntity($event: Event, searchText?: string) {
    $event.stopPropagation();
    this.createNew.emit(searchText);
  }

  get showEntityLink(): boolean {
    return this.selectEntityFormGroup.get('entity').value && this.disabled && this.entityURL !== '';
  }

  markAsTouched(): void {
    this.selectEntityFormGroup.get('entity').markAsTouched();
  }

  onBlur(): void {
    this.onTouched();

    if (!this.modelValue) {
      this.filteredEntities.pipe(
        first()
      ).subscribe(entities => {
        if (entities && entities.length === 1) {
          const entity = entities[0];
          this.selectEntityFormGroup.get('entity').setValue(entity, { emitEvent: false });
          const newModelValue = this.useFullEntityId ? entity.id : entity.id.id;
          this.updateView(newModelValue, entity);
          this.autocompleteTrigger?.closePanel();
        }
      });
    }
  }
}
