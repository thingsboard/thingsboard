// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Component, forwardRef, Input, OnDestroy } from '@angular/core';
import {
  ControlValueAccessor,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { isDefinedAndNotNull } from '@core/utils';
import { OpcMappingType, OpcMappingTypeTranslation, OpcUaMapping } from '@shared/models/integration.models';

@Component({
    selector: 'tb-opc-ua-mapping',
    templateUrl: './opc-ua-mapping.component.html',
    styleUrls: ['opc-ua-mapping.component.scss'],
    providers: [{
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => OpcUaMappingComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => OpcUaMappingComponent),
            multi: true,
        }],
    standalone: false
})
export class OpcUaMappingComponent implements ControlValueAccessor, Validator, OnDestroy {

  opcMappingForm: UntypedFormGroup;

  OpcMappingTypes = Object.values(OpcMappingType) as Array<OpcMappingType>;
  OpcMappingType = OpcMappingType;
  OpcMappingTypeTranslation = OpcMappingTypeTranslation;

  @Input()
  disabled: boolean;

  private destroy$ = new Subject<void>();
  private propagateChange = (v: any) => { };

  constructor(private fb: UntypedFormBuilder) {
    this.opcMappingForm = this.fb.group({
      map: this.fb.array([], Validators.required)
    });
    this.opcMappingForm.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(value => {
      this.updateModels(value.map);
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: any) {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any) {
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.opcMappingForm.disable({emitEvent: false});
    } else {
      this.opcMappingForm.enable({emitEvent: false});
    }
  }

  writeValue(mappings: OpcUaMapping[]): void {
    if (isDefinedAndNotNull(mappings)) {
      if (this.mapFormArray.length === mappings.length) {
        this.opcMappingForm.get('map').patchValue(mappings, {emitEvent: false});
      } else {
        const mapControls: Array<UntypedFormGroup> = [];
        mappings.forEach((map) => {
          mapControls.push(this.createdFormGroup(map));
        });
        this.opcMappingForm.setControl('map', this.fb.array(mapControls), {emitEvent: false});
        if (this.disabled) {
          this.opcMappingForm.disable({emitEvent: false});
        }
      }
    } else {
      this.addMap();
    }
  }

  validate(): ValidationErrors | null {
    return this.opcMappingForm.valid && this.mapFormArray.length ? null : {
      opcMappingForm: {valid: false}
    };
  }

  addMap() {
    this.mapFormArray.push(this.createdFormGroup());
  }

  removeMap(index: number) {
    this.mapFormArray.removeAt(index);
  }

  get mapFormArray(): UntypedFormArray {
    return this.opcMappingForm.get('map') as UntypedFormArray;
  }

  get mapFormArrayControls(): UntypedFormGroup[] {
    return this.mapFormArray.controls as UntypedFormGroup[];
  }

  private createdFormGroup(value?): UntypedFormGroup {
    return this.fb.group({
      deviceNodePattern: [value?.deviceNodePattern || 'Channel1\\.Device\\d+$', Validators.required],
      mappingType: [value?.mappingType || OpcMappingType.FQN, Validators.required],
      subscriptionTags: [value?.subscriptionTags || null, Validators.required],
      namespace: [value?.namespace || null, Validators.min(0)]
    });
  }

  private updateModels(value) {
    this.propagateChange(value);
  }

}
