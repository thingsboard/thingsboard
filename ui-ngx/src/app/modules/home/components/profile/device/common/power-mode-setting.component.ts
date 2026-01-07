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

import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormGroup } from '@angular/forms';
import {
  DEFAULT_EDRX_CYCLE,
  DEFAULT_PAGING_TRANSMISSION_WINDOW, DEFAULT_PSM_ACTIVITY_TIMER,
  PowerMode,
  PowerModeTranslationMap
} from '@home/components/profile/device/lwm2m/lwm2m-profile-config.models';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
  selector: 'tb-power-mode-settings',
  templateUrl: './power-mode-setting.component.html',
  styleUrls: []
})
export class PowerModeSettingComponent implements OnInit, OnDestroy {

  powerMods = Object.values(PowerMode);
  powerModeTranslationMap = PowerModeTranslationMap;

  private destroy$ = new Subject<void>();

  @Input()
  parentForm: UntypedFormGroup;

  @Input()
  isDeviceSetting = false;

  ngOnInit() {
    this.parentForm.get('powerMode').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe((powerMode: PowerMode) => {
      if (powerMode === PowerMode.E_DRX) {
        this.parentForm.get('edrxCycle').enable({emitEvent: false});
        this.parentForm.get('pagingTransmissionWindow').enable({emitEvent: false});
        this.disablePSKMode();
      } else if (powerMode === PowerMode.PSM) {
        this.parentForm.get('psmActivityTimer').enable({emitEvent: false});
        this.disableEdrxMode();
      } else {
        this.disableEdrxMode();
        this.disablePSKMode();
      }
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private disablePSKMode() {
    this.parentForm.get('psmActivityTimer').disable({emitEvent: false});
    this.parentForm.get('psmActivityTimer').reset(DEFAULT_PSM_ACTIVITY_TIMER, {emitEvent: false});
  }

  private disableEdrxMode() {
    this.parentForm.get('edrxCycle').disable({emitEvent: false});
    this.parentForm.get('edrxCycle').reset(DEFAULT_EDRX_CYCLE, {emitEvent: false});
    this.parentForm.get('pagingTransmissionWindow').disable({emitEvent: false});
    this.parentForm.get('pagingTransmissionWindow').reset(DEFAULT_PAGING_TRANSMISSION_WINDOW, {emitEvent: false});
  }
}
