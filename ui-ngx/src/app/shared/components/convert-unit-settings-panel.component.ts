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

import { Component, EventEmitter, Input, OnInit, Output, ViewChild, ViewEncapsulation } from '@angular/core';
import { TbUnit, UnitDescription, UnitSystem } from '@shared/models/unit.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { FormBuilder, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UnitService } from '@core/services/unit/unit.service';
import { AllMeasures } from '@core/services/unit/definitions/all';
import { debounceTime, first } from 'rxjs/operators';
import { isEmptyStr } from '@core/utils';
import type { UnitInputComponent } from '@shared/components/unit-input.component';

@Component({
  selector: 'tb-covert-unit-settings-panel',
  templateUrl: './convert-unit-settings-panel.component.html',
  styleUrls: ['./convert-unit-settings-panel.component.scss'],
  providers: [],
  encapsulation: ViewEncapsulation.None
})
export class ConvertUnitSettingsPanelComponent implements OnInit {

  @ViewChild('unitFrom', {static: true}) unitFrom: UnitInputComponent;

  @Input()
  unit: TbUnit;

  @Input()
  required: boolean;

  @Input()
  disabled: boolean;

  @Output()
  unitSettingsApplied = new EventEmitter<TbUnit>();

  UnitSystem = UnitSystem;

  measure: AllMeasures;

  convertUnitForm =  this.fb.group({
    from: [''],
    convertUnit: [true],
    METRIC: [''],
    IMPERIAL: [''],
    HYBRID: ['']
  })

  constructor(
    private popover: TbPopoverComponent<ConvertUnitSettingsPanelComponent>,
    private fb: FormBuilder,
    private unitService: UnitService
  ) {
    this.convertUnitForm.get('from').valueChanges.pipe(
      debounceTime(200),
      takeUntilDestroyed()
    ).subscribe(unit => {
      const unitDescription = this.unitService.getUnitDescription(unit);
      if (unitDescription) {
        this.convertUnitForm.get('convertUnit').enable({emitEvent: true});
        this.measure = unitDescription.measure;
        if (unitDescription.system === UnitSystem.IMPERIAL) {
          this.convertUnitForm.get('METRIC').setValue(this.unitService.getDefaultUnit(this.measure, UnitSystem.METRIC), {emitEvent: false});
          this.convertUnitForm.get('IMPERIAL').setValue(unit, {emitEvent: false});
          this.convertUnitForm.get('HYBRID').setValue(unit, {emitEvent: false});
        } else {
          this.convertUnitForm.get('METRIC').setValue(unit, {emitEvent: false});
          this.convertUnitForm.get('IMPERIAL').setValue(this.unitService.getDefaultUnit(this.measure, UnitSystem.IMPERIAL), {emitEvent: false});
          this.convertUnitForm.get('HYBRID').setValue(unit, {emitEvent: false});
        }
      } else {
        this.convertUnitForm.get('convertUnit').setValue(false, {onlySelf: true});
        this.convertUnitForm.get('convertUnit').disable({emitEvent: false});
        this.convertUnitForm.patchValue({
          METRIC: '',
          IMPERIAL: '',
          HYBRID: ''
        }, {emitEvent: false});
      }
    })

    this.convertUnitForm.get('convertUnit').valueChanges.pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      if (value) {
        this.convertUnitForm.get('METRIC').enable({emitEvent: false});
        this.convertUnitForm.get('IMPERIAL').enable({emitEvent: false});
        this.convertUnitForm.get('HYBRID').enable({emitEvent: false});
      } else {
        this.convertUnitForm.get('METRIC').disable({emitEvent: false});
        this.convertUnitForm.get('IMPERIAL').disable({emitEvent: false});
        this.convertUnitForm.get('HYBRID').disable({emitEvent: false});
      }
    });
  }

  ngOnInit() {
    let unitDescription: UnitDescription;
    if (this.required) {
      this.convertUnitForm.get('from').setValidators(Validators.required);
      this.convertUnitForm.get('from').updateValueAndValidity({emitEvent: false});
    }
    if (typeof this.unit === 'string') {
      this.convertUnitForm.get('convertUnit').setValue(false, {onlySelf: true});
      this.convertUnitForm.get('from').setValue(this.unit, {emitEvent: true});
      unitDescription = this.unitService.getUnitDescription(this.unit);
    } else if (this.unit === null) {
      this.convertUnitForm.get('convertUnit').setValue(false, {onlySelf: true});
      this.convertUnitForm.get('from').setValue(null, {emitEvent: true});
    } else {
      this.convertUnitForm.patchValue(this.unit, {emitEvent: false});
      unitDescription = this.unitService.getUnitDescription(this.unit.from);
    }

    if (unitDescription?.measure) {
      this.measure = unitDescription.measure;
    } else {
      this.convertUnitForm.get('convertUnit').disable({emitEvent: false});
    }
    if (this.disabled) {
      this.convertUnitForm.disable({emitEvent: false});
    } else if (this.unit === null || isEmptyStr(this.unit)) {
      this.popover.tbAnimationDone.pipe(first()).subscribe(() => {
        this.unitFrom.unitInput.nativeElement.focus();
      });
    }
  }

  cancel() {
    this.popover.hide();
  }

  applyUnitSettings() {
    if (this.convertUnitForm.value.convertUnit) {
      const formValue = this.convertUnitForm.value;
      delete formValue.convertUnit;
      this.unitSettingsApplied.emit(formValue as TbUnit);
    } else {
      this.unitSettingsApplied.emit(this.convertUnitForm.value.from);
    }
  }
}
