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

import { Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { AllMeasures, TbUnit, UnitInfo, UnitSystem } from '@shared/models/unit.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { FormBuilder, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UnitService } from '@core/services/unit.service';
import { debounceTime } from 'rxjs/operators';
import { isUndefinedOrNull } from '@core/utils';

@Component({
    selector: 'tb-covert-unit-settings-panel',
    templateUrl: './unit-settings-panel.component.html',
    styleUrls: ['./unit-settings-panel.component.scss'],
    providers: [],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class UnitSettingsPanelComponent implements OnInit {

  @Input()
  unit: TbUnit;

  @Input()
  required: boolean;

  @Input()
  disabled: boolean;

  @Output()
  unitSettingsApplied = new EventEmitter<TbUnit>();

  @Input()
  tagFilter: string;

  @Input()
  measure: AllMeasures;

  UnitSystem = UnitSystem;

  targetMeasure: AllMeasures;

  unitSettingForm =  this.fb.group({
    from: [''],
    convertUnit: [true],
    METRIC: [''],
    IMPERIAL: [''],
    HYBRID: ['']
  })

  constructor(
    private popover: TbPopoverComponent<UnitSettingsPanelComponent>,
    private fb: FormBuilder,
    private unitService: UnitService
  ) {
    this.unitSettingForm.get('from').valueChanges.pipe(
      debounceTime(200),
      takeUntilDestroyed()
    ).subscribe(unit => {
      const unitDescription = this.unitService.getUnitInfo(unit);
      const units = unitDescription ? this.unitService.getUnits(unitDescription.measure) : [];
      if (unitDescription && units.length > 1) {
        this.unitSettingForm.get('convertUnit').enable({emitEvent: true});
        this.targetMeasure = unitDescription.measure;
        if (unitDescription.system === UnitSystem.IMPERIAL) {
          this.unitSettingForm.get('METRIC').setValue(this.unitService.getDefaultUnit(this.targetMeasure, UnitSystem.METRIC), {emitEvent: false});
          this.unitSettingForm.get('IMPERIAL').setValue(unit, {emitEvent: false});
          this.unitSettingForm.get('HYBRID').setValue(unit, {emitEvent: false});
        } else {
          this.unitSettingForm.get('METRIC').setValue(unit, {emitEvent: false});
          this.unitSettingForm.get('IMPERIAL').setValue(this.unitService.getDefaultUnit(this.targetMeasure, UnitSystem.IMPERIAL), {emitEvent: false});
          this.unitSettingForm.get('HYBRID').setValue(unit, {emitEvent: false});
        }
      } else {
        this.unitSettingForm.get('convertUnit').setValue(false, {onlySelf: true});
        this.unitSettingForm.get('convertUnit').disable({emitEvent: false});
        this.unitSettingForm.patchValue({
          METRIC: '',
          IMPERIAL: '',
          HYBRID: ''
        }, {emitEvent: false});
      }
    })

    this.unitSettingForm.get('convertUnit').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      if (value) {
        this.unitSettingForm.get('METRIC').enable({emitEvent: false});
        this.unitSettingForm.get('IMPERIAL').enable({emitEvent: false});
        this.unitSettingForm.get('HYBRID').enable({emitEvent: false});
      } else {
        this.unitSettingForm.get('METRIC').disable({emitEvent: false});
        this.unitSettingForm.get('IMPERIAL').disable({emitEvent: false});
        this.unitSettingForm.get('HYBRID').disable({emitEvent: false});
      }
    });
  }

  ngOnInit() {
    let unitDescription: UnitInfo;
    if (this.required) {
      this.unitSettingForm.get('from').setValidators(Validators.required);
    }
    if (typeof this.unit === 'string') {
      this.unitSettingForm.get('convertUnit').setValue(false, {onlySelf: true});
      this.unitSettingForm.get('from').setValue(this.unit);
      unitDescription = this.unitService.getUnitInfo(this.unit);
    } else if (isUndefinedOrNull(this.unit)) {
      this.unitSettingForm.get('convertUnit').setValue(false, {onlySelf: true});
      this.unitSettingForm.get('from').setValue(null);
    } else {
      this.unitSettingForm.patchValue(this.unit, {emitEvent: false});
      unitDescription = this.unitService.getUnitInfo(this.unit.from);
    }

    if (unitDescription?.measure) {
      this.targetMeasure = unitDescription.measure;
    } else {
      this.unitSettingForm.get('convertUnit').disable({emitEvent: false});
    }
    if (this.disabled) {
      this.unitSettingForm.disable({emitEvent: false});
    }
  }

  clearUnit() {
    this.unitSettingForm.reset({
      convertUnit: false
    });
    if (this.required) {
      this.unitSettingForm.markAllAsTouched();
    }
    this.unitSettingForm.markAsDirty();
  }

  cancel() {
    this.popover.hide();
  }

  applyUnitSettings() {
    if (this.unitSettingForm.value.convertUnit) {
      const formValue = this.unitSettingForm.value;
      delete formValue.convertUnit;
      this.unitSettingsApplied.emit(formValue as TbUnit);
    } else {
      this.unitSettingsApplied.emit(this.unitSettingForm.value.from);
    }
  }
}
