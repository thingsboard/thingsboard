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

import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  UntypedFormBuilder,
  UntypedFormGroup,
  Validators
} from '@angular/forms';
import {
  MobileActionAttributeSource,
  mobileActionAttributeSourceTranslationMap,
  MobileActionSaveAs,
  mobileActionSaveAsTranslationMap,
  MobileActionTargetEntityType,
  mobileActionTargetEntityTypeTranslationMap,
  SaveBrowserLocationDescriptor
} from '@shared/models/widget.models';
import { WidgetActionCallbacks } from '@home/components/widget/action/manage-widget-actions.component.models';
import { Observable, Subject } from 'rxjs';
import { map, startWith, takeUntil } from 'rxjs/operators';

@Component({
  selector: 'tb-save-browser-location-action-editor',
  templateUrl: './save-browser-location-action-editor.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SaveBrowserLocationActionEditorComponent),
      multi: true
    }
  ],
  standalone: false
})
export class SaveBrowserLocationActionEditorComponent implements ControlValueAccessor, OnInit, OnDestroy {

  @Input()
  disabled: boolean;

  @Input()
  callbacks: WidgetActionCallbacks;

  formGroup: UntypedFormGroup;

  targetEntityTypes = Object.values(MobileActionTargetEntityType);
  targetEntityType = MobileActionTargetEntityType;
  targetEntityTypeTranslations = mobileActionTargetEntityTypeTranslationMap;
  attributeSources = Object.values(MobileActionAttributeSource);
  attributeSourceTranslations = mobileActionAttributeSourceTranslationMap;
  saveAsOptions = Object.values(MobileActionSaveAs);
  saveAsTranslations = mobileActionSaveAsTranslationMap;

  entityAliasNames: string[] = [];
  filteredEntityAliasNames: Observable<string[]>;

  private destroy$ = new Subject<void>();
  private propagateChange = (_val: any) => {};

  constructor(private fb: UntypedFormBuilder) {}

  ngOnInit(): void {
    this.formGroup = this.fb.group({
      targetEntity: this.fb.group({
        type: [MobileActionTargetEntityType.currentEntity, []],
        aliasName: [null, []],
        attributeSource: [MobileActionAttributeSource.currentUser, []],
        attributeKey: [null, []],
        defaultEntityType: [null, []]
      }),
      saveAs: [MobileActionSaveAs.attributes, []],
      latitudeKey: ['latitude', [Validators.required]],
      longitudeKey: ['longitude', [Validators.required]],
      accuracyKey: ['', []],
      altitudeKey: ['', []],
      altitudeAccuracyKey: ['', []],
      headingKey: ['', []],
      speedKey: ['', []],
      timestampKey: ['', []]
    });

    this.entityAliasNames = (this.callbacks?.fetchEntityAliases?.() ?? []).map((alias) => alias.alias);
    const aliasNameControl = this.formGroup.get('targetEntity.aliasName');
    this.filteredEntityAliasNames = aliasNameControl.valueChanges.pipe(
      startWith(aliasNameControl.value ?? ''),
      map((value: string) => (value ?? '').toLowerCase()),
      map((search) => this.entityAliasNames.filter((name) => name.toLowerCase().includes(search)))
    );

    this.formGroup.get('targetEntity.type').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.updateTargetEntityValidators());

    this.formGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => this.propagateChange(this.formGroup.getRawValue()));

    this.updateTargetEntityValidators();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {}

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.formGroup.disable({emitEvent: false});
    } else {
      this.formGroup.enable({emitEvent: false});
      this.updateTargetEntityValidators();
    }
  }

  writeValue(value?: SaveBrowserLocationDescriptor): void {
    this.formGroup.patchValue({
      targetEntity: {
        type: value?.targetEntity?.type || MobileActionTargetEntityType.currentEntity,
        aliasName: value?.targetEntity?.aliasName ?? null,
        attributeSource: value?.targetEntity?.attributeSource || MobileActionAttributeSource.currentUser,
        attributeKey: value?.targetEntity?.attributeKey ?? null,
        defaultEntityType: value?.targetEntity?.defaultEntityType ?? null
      },
      saveAs: value?.saveAs || MobileActionSaveAs.attributes,
      latitudeKey: value?.latitudeKey || 'latitude',
      longitudeKey: value?.longitudeKey || 'longitude',
      accuracyKey: value?.accuracyKey ?? '',
      altitudeKey: value?.altitudeKey ?? '',
      altitudeAccuracyKey: value?.altitudeAccuracyKey ?? '',
      headingKey: value?.headingKey ?? '',
      speedKey: value?.speedKey ?? '',
      timestampKey: value?.timestampKey ?? ''
    }, {emitEvent: false});
    this.updateTargetEntityValidators();
  }

  private updateTargetEntityValidators(): void {
    const type: MobileActionTargetEntityType = this.formGroup.get('targetEntity.type').value;
    const aliasName = this.formGroup.get('targetEntity.aliasName');
    const attributeKey = this.formGroup.get('targetEntity.attributeKey');
    aliasName.setValidators(type === MobileActionTargetEntityType.entityAlias ? [Validators.required] : []);
    attributeKey.setValidators(type === MobileActionTargetEntityType.fromAttribute ? [Validators.required] : []);
    aliasName.updateValueAndValidity({emitEvent: false});
    attributeKey.updateValueAndValidity({emitEvent: false});
  }
}
