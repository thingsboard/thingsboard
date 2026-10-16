// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
    styleUrls: [],
    standalone: false
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
