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

import { Component, DestroyRef, ElementRef, forwardRef, Input, OnInit, ViewChild } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  MobileActionAttributeSource,
  mobileActionAttributeSourceTranslationMap,
  MobileActionTargetEntityConfig,
  MobileActionTargetEntityType,
  mobileActionTargetEntityTypeTranslationMap
} from '@shared/models/widget.models';
import { WidgetActionCallbacks } from '@home/components/widget/action/manage-widget-actions.component.models';
import { Observable } from 'rxjs';
import { map, startWith } from 'rxjs/operators';
import { AttributeScope, DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { EntityFilter } from '@shared/models/query/query.models';
import { AliasFilterType, EntityAliasFilter, EntityAlias } from '@shared/models/alias.models';
import { EntityType } from '@shared/models/entity-type.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';

const queryableAliasFilterTypes: AliasFilterType[] = [
  AliasFilterType.singleEntity,
  AliasFilterType.entityList,
  AliasFilterType.entityName,
  AliasFilterType.entityType,
  AliasFilterType.assetType,
  AliasFilterType.deviceType,
  AliasFilterType.edgeType,
  AliasFilterType.entityViewType,
  AliasFilterType.apiUsageState,
  AliasFilterType.relationsQuery,
  AliasFilterType.assetSearchQuery,
  AliasFilterType.deviceSearchQuery,
  AliasFilterType.edgeSearchQuery,
  AliasFilterType.entityViewSearchQuery
];

const rootedAliasFilterTypes: AliasFilterType[] = [
  AliasFilterType.relationsQuery,
  AliasFilterType.assetSearchQuery,
  AliasFilterType.deviceSearchQuery,
  AliasFilterType.edgeSearchQuery,
  AliasFilterType.entityViewSearchQuery
];

@Component({
    selector: 'tb-location-target-entity',
    templateUrl: './location-target-entity.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => LocationTargetEntityComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => LocationTargetEntityComponent),
            multi: true
        }
    ],
    standalone: false
})
export class LocationTargetEntityComponent implements ControlValueAccessor, OnInit, Validator {

  @ViewChild('aliasNameInput') aliasNameInput: ElementRef;

  @Input()
  disabled: boolean;

  @Input()
  callbacks: WidgetActionCallbacks;

  targetEntityFormGroup: FormGroup;

  targetEntityTypes = Object.values(MobileActionTargetEntityType);
  targetEntityType = MobileActionTargetEntityType;
  targetEntityTypeTranslations = mobileActionTargetEntityTypeTranslationMap;

  attributeSources = Object.values(MobileActionAttributeSource);
  attributeSourceTranslations = mobileActionAttributeSourceTranslationMap;

  AttributeScope = AttributeScope;
  DataKeyType = DataKeyType;

  filteredEntityAliasNames: Observable<string[]>;

  attributeSourceEntityFilter: EntityFilter;

  private entityAliases: EntityAlias[] = [];

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder,
              private store: Store<AppState>,
              private destroyRef: DestroyRef) {
  }

  get aliasNameRequired(): boolean {
    const type: MobileActionTargetEntityType = this.targetEntityFormGroup.get('type').value;
    return type === MobileActionTargetEntityType.entityAlias ||
      (type === MobileActionTargetEntityType.fromAttribute &&
        this.targetEntityFormGroup.get('attributeSource').value === MobileActionAttributeSource.entityAlias);
  }

  ngOnInit(): void {
    this.targetEntityFormGroup = this.fb.group({
      type: [MobileActionTargetEntityType.currentEntity],
      aliasName: [null],
      attributeSource: [MobileActionAttributeSource.currentUser],
      attributeKey: [null]
    });

    this.entityAliases = this.callbacks?.fetchEntityAliases?.() ?? [];
    const aliasNameControl = this.targetEntityFormGroup.get('aliasName');
    this.filteredEntityAliasNames = aliasNameControl.valueChanges.pipe(
      startWith(aliasNameControl.value ?? ''),
      map((value: string) => (value ?? '').toLowerCase()),
      map((search) => this.entityAliases.map((alias) => alias.alias)
        .filter((name) => name.toLowerCase().includes(search)))
    );

    this.targetEntityFormGroup.get('type').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateValidators());
    this.targetEntityFormGroup.get('attributeSource').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.updateValidators();
      this.updateAttributeSourceEntityFilter();
    });
    aliasNameControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateAttributeSourceEntityFilter());

    this.targetEntityFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.propagateChange(this.targetEntityFormGroup.getRawValue()));

    this.updateValidators();
    this.updateAttributeSourceEntityFilter();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.targetEntityFormGroup.disable({emitEvent: false});
    } else {
      this.targetEntityFormGroup.enable({emitEvent: false});
      this.updateValidators();
    }
  }

  writeValue(value: MobileActionTargetEntityConfig | undefined): void {
    this.targetEntityFormGroup.patchValue({
      type: value?.type || MobileActionTargetEntityType.currentEntity,
      aliasName: value?.aliasName ?? null,
      attributeSource: value?.attributeSource || MobileActionAttributeSource.currentUser,
      attributeKey: value?.attributeKey ?? null
    }, {emitEvent: false});
    this.updateValidators();
    this.updateAttributeSourceEntityFilter();
  }

  validate(): ValidationErrors | null {
    return this.targetEntityFormGroup.valid ? null : {
      targetEntity: {
        valid: false
      }
    };
  }

  clearAliasName(): void {
    this.targetEntityFormGroup.get('aliasName').patchValue('');
    setTimeout(() => {
      this.aliasNameInput.nativeElement.blur();
      this.aliasNameInput.nativeElement.focus();
    }, 0);
  }

  private updateAttributeSourceEntityFilter(): void {
    switch (this.targetEntityFormGroup.get('attributeSource').value) {
      case MobileActionAttributeSource.currentUser:
        this.attributeSourceEntityFilter = {
          type: AliasFilterType.singleEntity,
          singleEntity: {entityType: EntityType.USER, id: getCurrentAuthUser(this.store)?.userId}
        };
        break;
      case MobileActionAttributeSource.entityAlias:
        this.attributeSourceEntityFilter = this.queryableAliasFilter(this.entityAliases
          .find((alias) => alias.alias === this.targetEntityFormGroup.get('aliasName').value)?.filter);
        break;
      default:
        this.attributeSourceEntityFilter = null;
    }
  }

  private queryableAliasFilter(filter: EntityAliasFilter): EntityFilter {
    if (!filter?.type || !queryableAliasFilterTypes.includes(filter.type)) {
      return null;
    }
    if (rootedAliasFilterTypes.includes(filter.type) && (filter.rootStateEntity || !filter.rootEntity?.id)) {
      return null;
    }
    return filter as EntityFilter;
  }

  private updateValidators(): void {
    const type: MobileActionTargetEntityType = this.targetEntityFormGroup.get('type').value;
    const aliasName = this.targetEntityFormGroup.get('aliasName');
    const attributeKey = this.targetEntityFormGroup.get('attributeKey');
    aliasName.setValidators(this.aliasNameRequired ? [Validators.required] : []);
    attributeKey.setValidators(type === MobileActionTargetEntityType.fromAttribute ? [Validators.required] : []);
    aliasName.updateValueAndValidity({emitEvent: false});
    attributeKey.updateValueAndValidity({emitEvent: false});
  }
}
