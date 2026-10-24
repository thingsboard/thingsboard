// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { AfterViewInit, Component, DestroyRef, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
import { EntityType } from '@shared/models/entity-type.models';
import { PageComponent } from '@shared/components/page.component';
import {
  EntityGroupDetailsMode,
  entityGroupDetailsModeTranslationMap,
  EntityGroupSettings,
  groupSettingsDefaults
} from '@shared/models/entity-group.models';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { buildPageStepSizeValues } from '@home/components/widget/lib/table-widget.models';
import { pairwise } from 'rxjs';
import { startWith } from 'rxjs/operators';

@Component({
    selector: 'tb-entity-group-settings',
    templateUrl: './entity-group-settings.component.html',
    styleUrls: [],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => EntityGroupSettingsComponent),
            multi: true
        }],
    standalone: false
})
export class EntityGroupSettingsComponent extends PageComponent implements ControlValueAccessor, OnInit, AfterViewInit, OnDestroy {

  modelValue: EntityGroupSettings | null;

  settingsFormGroup: UntypedFormGroup;

  pageStepSizeValues = [];

  @Input()
  entityType: EntityType;

  @Input()
  disabled: boolean;

  entityGroupDetailsModes = Object.values(EntityGroupDetailsMode);
  entityGroupDetailsModeTranslations = entityGroupDetailsModeTranslationMap;

  entityTypes = EntityType;

  constructor(protected store: Store<AppState>,
              private fb: UntypedFormBuilder,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit() {
    this.settingsFormGroup = this.fb.group(groupSettingsDefaults(this.entityType, {} as EntityGroupSettings));
    this.settingsFormGroup.get('defaultPageSize').setValidators([Validators.min(1)]);
    this.settingsFormGroup.get('pageStepCount').setValidators([Validators.min(1), Validators.max(100),
      Validators.required, Validators.pattern(/^\d*$/)]);
    this.settingsFormGroup.get('pageStepIncrement').setValidators([Validators.min(1), Validators.required,
      Validators.pattern(/^\d*$/)]);
    this.pageStepSizeValues = buildPageStepSizeValues(this.settingsFormGroup.get('pageStepCount').value,
      this.settingsFormGroup.get('pageStepIncrement').value);
    this.settingsFormGroup.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef),
      startWith(this.settingsFormGroup.getRawValue()),
      pairwise(),
    ).subscribe(([prev, curr]) => {
      if (prev.pageStepCount !== curr.pageStepCount || prev.pageStepIncrement !== curr.pageStepIncrement) {
        this.settingsFormGroup.get('defaultPageSize').reset();
        this.pageStepSizeValues = buildPageStepSizeValues(this.settingsFormGroup.get('pageStepCount').value,
          this.settingsFormGroup.get('pageStepIncrement').value);
      }
      if (prev.displayPagination !== curr.displayPagination) {
        if (curr.displayPagination) {
          this.settingsFormGroup.get('pageStepCount').enable();
          this.settingsFormGroup.get('pageStepIncrement').enable();
        } else {
          this.settingsFormGroup.get('pageStepCount').disable();
          this.settingsFormGroup.get('pageStepIncrement').disable();
        }
      }
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngAfterViewInit(): void {
  }

  ngOnDestroy(): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.settingsFormGroup.disable({emitEvent: false})
    } else {
      this.settingsFormGroup.enable({emitEvent: false})
    }
  }

  writeValue(value: EntityGroupSettings | null): void {
    this.modelValue = groupSettingsDefaults(this.entityType, value || {} as EntityGroupSettings);
    this.settingsFormGroup.reset(this.modelValue,{emitEvent: false});
    this.pageStepSizeValues = buildPageStepSizeValues(this.settingsFormGroup.get('pageStepCount').value,
      this.settingsFormGroup.get('pageStepIncrement').value);
  }

  private propagateChange = (v: any) => { };

  private updateModel() {
    if (this.settingsFormGroup.valid) {
      const value = this.settingsFormGroup.value;
      this.modelValue = {...this.modelValue, ...value};
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }

}
