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
import {
  ControlValueAccessor,
  FormControl,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup
} from '@angular/forms';
import { firstValueFrom, merge, Observable, of, shareReplay, Subject } from 'rxjs';
import { catchError, debounceTime, finalize, map, switchMap, tap } from 'rxjs/operators';
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
import { TranslateService } from '@ngx-translate/core';
import { AutocompleteBaseDirective } from '@shared/components/directives/autocomplete-base.directive';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

@Component({
    selector: 'tb-entity-autocomplete',
    templateUrl: './entity-autocomplete.component.html',
    styleUrls: ['./entity-autocomplete.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => EntityAutocompleteComponent),
            multi: true
        }],
    standalone: false
})
export class EntityAutocompleteComponent extends AutocompleteBaseDirective<BaseData<EntityId>, string | EntityId> implements ControlValueAccessor, OnInit {

  selectEntityFormGroup: UntypedFormGroup;

  private modelValue: string | EntityId | null;

  private entityTypeValue: EntityType | AliasEntityType;

  private entitySubtypeValue: string;

  private entityText: string;

  noEntitiesMatchingText: string;
  notFoundEntities = 'entity.no-entities-text';

  private entityRequiredText: string;

  filteredEntities: Observable<Array<BaseData<EntityId>>>;

  entityURL: string;

  private refresh$ = new Subject<Array<BaseData<EntityId>>>();

  private propagateChange: (value: any) => void = () => { };

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

  private _entityNotValidTranslationKey: string | null = null;

  @Input()
  set entityNotValidTranslationKey(value: string) {
    this._entityNotValidTranslationKey = value;
  }

  get entityNotValidTranslationKey(): string {
    if (this._entityNotValidTranslationKey) {
      return this._entityNotValidTranslationKey;
    }
    return this.allowCreateNew ? 'entity.entity-not-valid-create-new' : 'entity.entity-not-valid';
  }

  @Input()
  excludeEntityIds: Array<string>;

  @Input()
  labelText: string;

  @Input()
  requiredText: string;

  @Input()
  placeholder: string;

  @Input()
  set useFullEntityId(value: boolean) {
    super.useFullEntityId = coerceBooleanProperty(value);
  }

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

  get entityNameForError(): string {
    return this.label ? this.translate.instant(this.label).toLowerCase() : '';
  }

  protected getControl() {
    return this.selectEntityFormGroup.get('entity') as FormControl;
  }

  protected getAutocompleteTrigger() {
    return this.autocompleteTrigger;
  }

  protected getInput() {
    return this.entityInput;
  }

  protected getFilteredEntities() {
    return this.filteredEntities;
  }
  protected getModelValue() {
    return this.modelValue;
  }

  protected getDisplayName(entity: BaseData<EntityId>): string {
    return this.useEntityDisplayName ? getEntityDisplayName(entity) : entity.name;
  }

  protected isCreateNew(): boolean {
    return this.allowCreateNew;
  }

  constructor(private store: Store<AppState>,
              private entityService: EntityService,
              private fb: UntypedFormBuilder,
              private translate: TranslateService) {
    super();
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
          map(value => value ? (typeof value === 'string' ? value : value.name) : ''),
          switchMap(name => {
            this.isFetching = true;
            return this.fetchEntities(name).pipe(
              finalize(() => this.isFetching = false)
            );
          }),
          tap(entities => {
            if (this.pendingBlur) {
              this.performValidation(entities);
            }
          }),
          shareReplay(1)
        )
    );
  }

  private static resolveEntityTexts(entityTypeValue: EntityType | AliasEntityType, store: Store<AppState>): {
    entityText: string;
    noEntitiesMatchingText: string;
    entityRequiredText: string;
    notFoundEntities: string;
  } | null {
    if (!entityTypeValue) {
      return null;
    }
    switch (entityTypeValue) {
      case EntityType.ASSET:
        return {
          entityText: 'asset.asset',
          noEntitiesMatchingText: 'asset.no-assets-matching',
          entityRequiredText: 'asset.asset-required',
          notFoundEntities: 'asset.no-assets-text'
        };
      case EntityType.DEVICE:
        return {
          entityText: 'device.device',
          noEntitiesMatchingText: 'device.no-devices-matching',
          entityRequiredText: 'device.device-required',
          notFoundEntities: 'device.no-devices-text'
        };
      case EntityType.EDGE:
        return {
          entityText: 'edge.edge',
          noEntitiesMatchingText: 'edge.no-edges-matching',
          entityRequiredText: 'edge.edge-required',
          notFoundEntities: 'edge.no-edges-text'
        };
      case EntityType.ENTITY_VIEW:
        return {
          entityText: 'entity-view.entity-view',
          noEntitiesMatchingText: 'entity-view.no-entity-views-matching',
          entityRequiredText: 'entity-view.entity-view-required',
          notFoundEntities: 'entity-view.no-entity-views-text'
        };
      case EntityType.RULE_CHAIN:
        return {
          entityText: 'rulechain.rulechain',
          noEntitiesMatchingText: 'rulechain.no-rulechains-matching',
          entityRequiredText: 'rulechain.rulechain-required',
          notFoundEntities: 'rulechain.no-rulechains-text'
        };
      case EntityType.TENANT:
      case AliasEntityType.CURRENT_TENANT:
        return {
          entityText: 'tenant.tenant',
          noEntitiesMatchingText: 'tenant.no-tenants-matching',
          entityRequiredText: 'tenant.tenant-required',
          notFoundEntities: 'tenant.no-tenants-text'
        };
      case EntityType.CUSTOMER:
        return {
          entityText: 'customer.customer',
          noEntitiesMatchingText: 'customer.no-customers-matching',
          entityRequiredText: 'customer.customer-required',
          notFoundEntities: 'customer.no-customers-text'
        };
      case EntityType.USER:
      case AliasEntityType.CURRENT_USER:
        return {
          entityText: 'user.user',
          noEntitiesMatchingText: 'user.no-users-matching',
          entityRequiredText: 'user.user-required',
          notFoundEntities: 'user.no-users-text'
        };
      case EntityType.DASHBOARD:
        return {
          entityText: 'dashboard.dashboard',
          noEntitiesMatchingText: 'dashboard.no-dashboards-matching',
          entityRequiredText: 'dashboard.dashboard-required',
          notFoundEntities: 'dashboard.no-dashboards-text'
        };
      case EntityType.ALARM:
        return {
          entityText: 'alarm.alarm',
          noEntitiesMatchingText: 'alarm.no-alarms-matching',
          entityRequiredText: 'alarm.alarm-required',
          notFoundEntities: 'alarm.no-alarms-prompt'
        };
      case EntityType.QUEUE_STATS:
        return {
          entityText: 'queue-statistics.queue-statistics',
          noEntitiesMatchingText: 'queue-statistics.no-queue-statistics-matching',
          entityRequiredText: 'queue-statistics.queue-statistics-required',
          notFoundEntities: 'queue-statistics.no-queue-statistics-text'
        };
      case EntityType.MOBILE_APP:
        return {
          entityText: 'mobile.application',
          noEntitiesMatchingText: 'mobile.no-application-matching',
          entityRequiredText: 'mobile.application-required',
          notFoundEntities: 'mobile.no-application-text'
        };
      case EntityType.MOBILE_APP_BUNDLE:
        return {
          entityText: 'mobile.bundle',
          noEntitiesMatchingText: 'mobile.no-bundle-matching',
          entityRequiredText: 'mobile.bundle-required',
          notFoundEntities: 'mobile.no-bundle-text'
        };
      case EntityType.NOTIFICATION_TARGET:
        return {
          entityText: 'notification.notification-recipient',
          noEntitiesMatchingText: 'notification.no-recipients-matching',
          entityRequiredText: 'notification.notification-recipient-required',
          notFoundEntities: 'notification.no-recipients-text'
        };
      case EntityType.AI_MODEL:
        return {
          entityText: 'ai-models.ai-model',
          noEntitiesMatchingText: 'ai-models.no-model-matching',
          entityRequiredText: 'ai-models.model-required',
          notFoundEntities: 'ai-models.no-model-text'
        };
      case EntityType.DEVICE_PROFILE:
        return {
          entityText: 'device-profile.device-profile',
          noEntitiesMatchingText: 'device-profile.no-device-profiles-matching',
          entityRequiredText: 'device-profile.device-profile-required',
          notFoundEntities: 'device-profile.no-device-profiles-text'
        };
      case EntityType.ASSET_PROFILE:
        return {
          entityText: 'asset-profile.asset-profile',
          noEntitiesMatchingText: 'asset-profile.no-asset-profiles-matching',
          entityRequiredText: 'asset-profile.asset-profile-required',
          notFoundEntities: 'asset-profile.no-asset-profiles-text'
        };
      case AliasEntityType.CURRENT_CUSTOMER:
        return {
          entityText: 'customer.default-customer',
          noEntitiesMatchingText: 'customer.no-customers-matching',
          entityRequiredText: 'customer.default-customer-required',
          notFoundEntities: 'customer.no-customers-text'
        };
      case AliasEntityType.CURRENT_USER_OWNER:
        const authUser = getCurrentAuthUser(store);
        if (authUser.authority === Authority.TENANT_ADMIN) {
          return {
            entityText: 'tenant.tenant',
            noEntitiesMatchingText: 'tenant.no-tenants-matching',
            entityRequiredText: 'tenant.tenant-required',
            notFoundEntities: null
          };
        } else {
          return {
            entityText: 'customer.customer',
            noEntitiesMatchingText: 'customer.no-customers-matching',
            entityRequiredText: 'customer.customer-required',
            notFoundEntities: null
          };
        }
      default:
        return null;
    }
  }

  private load(): void {
    const texts = EntityAutocompleteComponent.resolveEntityTexts(this.entityTypeValue, this.store);
    if (texts) {
      this.entityText = texts.entityText;
      this.noEntitiesMatchingText = texts.noEntitiesMatchingText;
      this.entityRequiredText = texts.entityRequiredText;
      if (texts.notFoundEntities !== null) {
        this.notFoundEntities = texts.notFoundEntities;
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

  protected updateView(value: string | EntityId | null, entity: BaseData<EntityId> | null) {
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
    this.searchText = searchText ?? '';
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
}
