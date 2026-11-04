// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
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
  LocationTargetEntityConfig,
  LocationTargetEntityMode,
  LocationTargetIndirection,
  LocationTargetSource,
  locationTargetSourceTranslationMap
} from '@shared/models/location.models';
import { WidgetActionCallbacks } from '@home/components/widget/action/manage-widget-actions.component.models';
import { defer, Observable, of, Subject } from 'rxjs';
import { catchError, map, startWith, switchMap } from 'rxjs/operators';
import { AttributeScope, DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { EntityFilter } from '@shared/models/query/query.models';
import { AliasFilterType, EntityAlias, EntityAliasFilter } from '@shared/models/alias.models';
import { EntityType } from '@shared/models/entity-type.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { EntityService } from '@core/http/entity.service';
import { isEqual } from '@core/utils';

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

  targetEntityMode = LocationTargetEntityMode;
  attributeSources = Object.values(LocationTargetSource);
  attributeSourceTranslations = locationTargetSourceTranslationMap;

  AttributeScope = AttributeScope;
  DataKeyType = DataKeyType;

  filteredEntityAliasNames: Observable<string[]>;

  attributeSourceEntityFilter: EntityFilter;

  private entityAliases: EntityAlias[] = [];

  private attributeSourceChange = new Subject<void>();

  private propagateChange = (_val: any) => {};

  constructor(private fb: FormBuilder,
              private store: Store<AppState>,
              private entityService: EntityService,
              private destroyRef: DestroyRef) {
  }

  get isFromAttribute(): boolean {
    return this.targetEntityFormGroup.get('mode').value === LocationTargetEntityMode.FROM_ATTRIBUTE;
  }

  get aliasNameRequired(): boolean {
    return this.targetEntityFormGroup.get('source').value === LocationTargetSource.ENTITY_ALIAS;
  }

  ngOnInit(): void {
    this.targetEntityFormGroup = this.fb.group({
      mode: [LocationTargetEntityMode.ENTITY],
      source: [LocationTargetSource.CURRENT_ENTITY],
      aliasName: [null],
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

    this.attributeSourceChange.pipe(
      switchMap(() => this.attributeSourceFilter()),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((entityFilter) => this.setAttributeSourceEntityFilter(entityFilter));

    this.targetEntityFormGroup.get('mode').valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => this.updateValidators());
    this.targetEntityFormGroup.get('source').valueChanges.pipe(
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
    ).subscribe(() => this.propagateChange(this.targetEntityConfig()));

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

  writeValue(value: LocationTargetEntityConfig | undefined): void {
    const type = value?.type || LocationTargetSource.CURRENT_ENTITY;
    const fromAttribute = type === LocationTargetIndirection.FROM_ATTRIBUTE;
    this.targetEntityFormGroup.patchValue({
      mode: fromAttribute ? LocationTargetEntityMode.FROM_ATTRIBUTE : LocationTargetEntityMode.ENTITY,
      source: fromAttribute ? (value?.attributeSource || LocationTargetSource.CURRENT_USER)
                            : type,
      aliasName: value?.aliasName ?? null,
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
    this.attributeSourceChange.next();
  }

  private attributeSourceFilter(): Observable<EntityFilter> {
    switch (this.targetEntityFormGroup.get('source').value) {
      case LocationTargetSource.CURRENT_USER:
        return of({
          type: AliasFilterType.singleEntity,
          singleEntity: {entityType: EntityType.USER, id: getCurrentAuthUser(this.store)?.userId}
        });
      case LocationTargetSource.ENTITY_ALIAS:
        return this.queryableAliasFilter(this.selectedAliasFilter());
      default:
        return of(null);
    }
  }

  private selectedAliasFilter(): EntityAliasFilter {
    return this.entityAliases
      .find((alias) => alias.alias === this.targetEntityFormGroup.get('aliasName').value)?.filter;
  }

  private queryableAliasFilter(filter: EntityAliasFilter): Observable<EntityFilter> {
    if (!filter?.type) {
      return of(null);
    }
    return defer(() => this.entityService.resolveAliasFilter(filter, null) ?? of(null)).pipe(
      map((result) => result?.stateEntity ? null : result?.entityFilter ?? null),
      catchError(() => of(null))
    );
  }

  private setAttributeSourceEntityFilter(entityFilter: EntityFilter): void {
    const previousFilter = this.attributeSourceEntityFilter;
    this.attributeSourceEntityFilter = entityFilter;
    if (this.isFromAttribute && previousFilter && !isEqual(entityFilter, previousFilter)) {
      this.targetEntityFormGroup.get('attributeKey').patchValue(null);
    }
  }

  private updateValidators(): void {
    const aliasName = this.targetEntityFormGroup.get('aliasName');
    const attributeKey = this.targetEntityFormGroup.get('attributeKey');
    aliasName.setValidators(this.aliasNameRequired ? [Validators.required] : []);
    attributeKey.setValidators(this.isFromAttribute ? [Validators.required] : []);
    aliasName.updateValueAndValidity({emitEvent: false});
    attributeKey.updateValueAndValidity({emitEvent: false});
  }

  private targetEntityConfig(): LocationTargetEntityConfig {
    const value = this.targetEntityFormGroup.getRawValue();
    if (value.mode === LocationTargetEntityMode.FROM_ATTRIBUTE) {
      const config: LocationTargetEntityConfig = {
        type: LocationTargetIndirection.FROM_ATTRIBUTE,
        attributeSource: value.source,
        attributeKey: value.attributeKey
      };
      if (value.source === LocationTargetSource.ENTITY_ALIAS) {
        config.aliasName = value.aliasName;
      }
      return config;
    }
    const config: LocationTargetEntityConfig = {
      type: value.source
    };
    if (value.source === LocationTargetSource.ENTITY_ALIAS) {
      config.aliasName = value.aliasName;
    }
    return config;
  }
}
